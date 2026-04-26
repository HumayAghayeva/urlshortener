package url_shortener.urlshortener.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import url_shortener.urlshortener.exception.AliasAlreadyExistsException;
import url_shortener.urlshortener.exception.UrlExpiredException;
import url_shortener.urlshortener.exception.UrlNotFoundException;
import url_shortener.urlshortener.model.ClickEvent;
import url_shortener.urlshortener.model.Url;
import url_shortener.urlshortener.repository.ClickEventRepository;
import url_shortener.urlshortener.repository.UrlRepository;
import url_shortener.urlshortener.dto.UrlDtos.*;
import url_shortener.urlshortener.util.Base62CodeGenerator;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)

public class UrlServiceImpl implements UrlService {

    private static final int MAX_COLLISION_RETRIES = 5;

    private final UrlRepository urlRepository;
    private final ClickEventRepository clickEventRepository;
    private final Base62CodeGenerator codeGenerator;
    private final UrlMapper urlMapper;
    private final MeterRegistry meterRegistry;

    @Value("${app.base-url}")
    private String baseUrl;

    // ── CREATE ──────────────────────────────────────────────

    @Override
    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request, String userId) {

        String shortCode = resolveShortCode(request.getCustomAlias());

        LocalDateTime expiresAt = request.getTtlDays() != null
                ? LocalDateTime.now().plusDays(request.getTtlDays())
                : null;

        Url url = Url.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode(shortCode)
                .customAlias(request.getCustomAlias())
                .expiresAt(expiresAt)
                .userId(userId)
                .build();

        Url saved = urlRepository.save(url);
        log.info("Created short URL: {} → {}", shortCode, request.getOriginalUrl());
        meterRegistry.counter("urls.created").increment();

        return toResponse(saved);
    }

    private String resolveShortCode(String customAlias) {
        if (customAlias != null) {
            if (urlRepository.existsByCustomAlias(customAlias)) {
                throw new AliasAlreadyExistsException(customAlias);
            }
            return customAlias;
        }
        // Generate unique code with collision retry
        for (int i = 0; i < MAX_COLLISION_RETRIES; i++) {
            String code = codeGenerator.generate();
            if (!urlRepository.existsByShortCode(code)) {
                return code;
            }
        }
        // Fallback: longer code
        return codeGenerator.generate(9);
    }

    // ── RESOLVE (redirect) ──────────────────────────────────

    @Override
    @Transactional
    @Cacheable(value = "urls", key = "#shortCode")
    public String resolveUrl(String shortCode, String ip, String userAgent, String referer) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (!url.isActive() || url.isExpired()) {
            throw new UrlExpiredException(shortCode);
        }

        // Async-friendly: increment via update query (avoids dirty-check overhead)
        urlRepository.incrementClickCount(url.getId());

        // Save click event for analytics
        ClickEvent event = ClickEvent.builder()
                .url(url)
                .ipAddress(anonymizeIp(ip))
                .userAgent(truncate(userAgent, 512))
                .referer(truncate(referer, 100))
                .build();
        clickEventRepository.save(event);

        meterRegistry.counter("urls.redirects", "shortCode", shortCode).increment();
        return url.getOriginalUrl();
    }

    // ── READ ────────────────────────────────────────────────

    @Override
    public UrlResponse getUrlInfo(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .map(this::toResponse)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
    }

    @Override
    public PageResponse<UrlResponse> getUserUrls(String userId, Pageable pageable) {
        Page<Url> page = urlRepository.findByUserIdAndActiveTrue(userId, pageable);
        List<UrlResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<UrlResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // ── UPDATE ──────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = "urls", key = "#result.shortCode")
    public UrlResponse updateUrl(Long id, UpdateUrlRequest request, String userId) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new UrlNotFoundException("id=" + id));

        validateOwnership(url, userId);

        if (request.getOriginalUrl() != null) url.setOriginalUrl(request.getOriginalUrl());
        if (request.getActive() != null)      url.setActive(request.getActive());
        if (request.getTtlDays() != null) {
            url.setExpiresAt(LocalDateTime.now().plusDays(request.getTtlDays()));
        }

        return toResponse(urlRepository.save(url));
    }

    // ── DELETE ──────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = "urls", key = "#id")
    public void deleteUrl(Long id, String userId) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new UrlNotFoundException("id=" + id));
        validateOwnership(url, userId);
        url.setActive(false);            // soft delete
        urlRepository.save(url);
        log.info("Soft-deleted URL id={}", id);
    }

    // ── ANALYTICS ───────────────────────────────────────────

    @Override
    public UrlAnalyticsResponse getAnalytics(String shortCode, String userId) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        validateOwnership(url, userId);

        List<Object[]> byCountry = clickEventRepository.countByCountryForUrl(url.getId());
        Map<String, Long> countryMap = new LinkedHashMap<>();
        byCountry.forEach(row -> countryMap.put((String) row[0], (Long) row[1]));

        List<Object[]> daily = clickEventRepository
                .dailyClicksForUrl(url.getId(), LocalDateTime.now().minusDays(30));
        List<DailyClick> dailyClicks = daily.stream()
                .map(row -> DailyClick.builder()
                        .date(row[0].toString())
                        .clicks((Long) row[1])
                        .build())
                .collect(Collectors.toList());

        return UrlAnalyticsResponse.builder()
                .urlId(url.getId())
                .shortCode(shortCode)
                .totalClicks(url.getClickCount())
                .clicksByCountry(countryMap)
                .dailyClicks(dailyClicks)
                .build();
    }

    // ── SCHEDULED TASKS ─────────────────────────────────────

    /** Runs daily at midnight to deactivate expired URLs. */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deactivateExpiredUrls() {
        int count = urlRepository.deactivateExpiredUrls(LocalDateTime.now());
        log.info("Deactivated {} expired URLs", count);
    }

    // ── HELPERS ─────────────────────────────────────────────

    private UrlResponse toResponse(Url url) {
        UrlResponse response = urlMapper.toResponse(url);
        response.setShortUrl(baseUrl + "/" + url.getShortCode());
        return response;
    }

    private void validateOwnership(Url url, String userId) {
        if (userId != null && !userId.equals(url.getUserId())) {
            throw new UrlNotFoundException("URL not found for this user");
        }
    }

    /** Replace last octet of IPv4 for GDPR-friendly storage. */
    private String anonymizeIp(String ip) {
        if (ip == null) return null;
        int lastDot = ip.lastIndexOf('.');
        return lastDot > 0 ? ip.substring(0, lastDot) + ".0" : ip;
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}

