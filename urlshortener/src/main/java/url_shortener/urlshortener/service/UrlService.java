package url_shortener.urlshortener.service;
import url_shortener.urlshortener.dto.UrlDtos.*;
import org.springframework.data.domain.Pageable;

public interface UrlService {

    UrlResponse createShortUrl(CreateUrlRequest request, String userId);

    /** Resolve a short code → original URL (and record a click) */
    String resolveUrl(String shortCode, String ip, String userAgent, String referer);

    UrlResponse getUrlInfo(String shortCode);

    UrlResponse updateUrl(Long id, UpdateUrlRequest request, String userId);

    void deleteUrl(Long id, String userId);

    PageResponse<UrlResponse> getUserUrls(String userId, Pageable pageable);

    UrlAnalyticsResponse getAnalytics(String shortCode, String userId);
}

