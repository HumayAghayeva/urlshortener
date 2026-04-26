package url_shortener.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class UrlDtos {

    private UrlDtos() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateUrlRequest {

        @NotBlank(message = "Original URL must not be blank")
        @Pattern(
                regexp = "^(https?://).+",
                message = "URL must start with http:// or https://"
        )
        @Size(max = 2048, message = "URL must not exceed 2048 characters")
        private String originalUrl;

        /**
         * Optional user-chosen alias (letters, digits, hyphens only)
         */
        @Pattern(
                regexp = "^[a-zA-Z0-9-]{3,50}$",
                message = "Alias must be 3–50 characters: letters, digits, or hyphens"
        )
        private String customAlias;

        /**
         * Optional TTL in days. Null = no expiry.
         */
        @Min(value = 1, message = "TTL must be at least 1 day")
        @Max(value = 3650, message = "TTL must not exceed 3650 days (10 years)")
        private Integer ttlDays;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateUrlRequest {

        @Pattern(
                regexp = "^(https?://).+",
                message = "URL must start with http:// or https://"
        )
        @Size(max = 2048)
        private String originalUrl;

        private Boolean active;

        @Min(1)
        @Max(3650)
        private Integer ttlDays;
    }

    // ────────────────────────────────────────────────────────
    // RESPONSE DTOs
    // ────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UrlResponse {
        private Long id;
        private String shortCode;
        private String shortUrl;
        private String originalUrl;
        private String customAlias;
        private LocalDateTime expiresAt;
        private Long clickCount;
        private boolean active;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UrlAnalyticsResponse {
        private Long urlId;
        private String shortCode;
        private Long totalClicks;
        private Map<String, Long> clicksByCountry;
        private List<DailyClick> dailyClicks;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyClick {
        private String date;
        private Long clicks;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private LocalDateTime timestamp;
        private Map<String, String> fieldErrors;

    }
}
