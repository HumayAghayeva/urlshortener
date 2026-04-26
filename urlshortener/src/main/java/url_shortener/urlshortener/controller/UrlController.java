package url_shortener.urlshortener.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import url_shortener.urlshortener.dto.UrlDtos.*;
import url_shortener.urlshortener.service.UrlService;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Tag(name = "URL Shortener", description = "Create, manage, and resolve short URLs")
public class UrlController {

    private final UrlService urlService;

    // ── REDIRECT (hot path) ─────────────────────────────────

    @GetMapping("/{shortCode}")
    @Operation(summary = "Redirect to original URL")
    @ApiResponse(responseCode = "301", description = "Permanent redirect")
    @ApiResponse(responseCode = "404", description = "Short code not found")
    @ApiResponse(responseCode = "410", description = "URL has expired")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request
    ) {
        String originalUrl = urlService.resolveUrl(
                shortCode,
                getClientIp(request),
                request.getHeader(HttpHeaders.USER_AGENT),
                request.getHeader(HttpHeaders.REFERER)
        );
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(originalUrl))
                .build();
    }

    // ── CRUD API ────────────────────────────────────────────

    @PostMapping("/api/v1/urls")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a short URL")
    public UrlResponse createUrl(
            @Valid @RequestBody CreateUrlRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        return urlService.createShortUrl(request, userId(user));
    }

    @GetMapping("/api/v1/urls/{shortCode}")
    @Operation(summary = "Get URL metadata")
    public UrlResponse getUrl(@PathVariable String shortCode) {
        return urlService.getUrlInfo(shortCode);
    }

    @PatchMapping("/api/v1/urls/{id}")
    @Operation(summary = "Update a URL (owner only)")
    public UrlResponse updateUrl(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUrlRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        return urlService.updateUrl(id, request, userId(user));
    }

    @DeleteMapping("/api/v1/urls/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a URL (owner only)")
    public void deleteUrl(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user
    ) {
        urlService.deleteUrl(id, userId(user));
    }

    @GetMapping("/api/v1/urls")
    @Operation(summary = "List URLs for the authenticated user")
    public PageResponse<UrlResponse> listUrls(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return urlService.getUserUrls(userId(user), pageable);
    }

    @GetMapping("/api/v1/urls/{shortCode}/analytics")
    @Operation(summary = "Get click analytics for a URL (owner only)")
    public UrlAnalyticsResponse getAnalytics(
            @PathVariable String shortCode,
            @AuthenticationPrincipal UserDetails user
    ) {
        return urlService.getAnalytics(shortCode, userId(user));
    }

    // ── HELPERS ─────────────────────────────────────────────

    private String userId(UserDetails user) {
        return user != null ? user.getUsername() : null;
    }

    /** Extract real client IP, respecting common proxy headers. */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null ? realIp : request.getRemoteAddr();
    }
}
