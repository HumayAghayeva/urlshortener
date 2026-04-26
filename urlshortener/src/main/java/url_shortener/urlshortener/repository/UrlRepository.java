package url_shortener.urlshortener.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import url_shortener.urlshortener.model.Url;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    Optional<Url> findByCustomAlias(String customAlias);

    boolean existsByShortCode(String shortCode);

    boolean existsByCustomAlias(String customAlias);

    Page<Url> findByUserIdAndActiveTrue(String userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.id = :id")
    void incrementClickCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Url u SET u.active = false WHERE u.expiresAt < :now AND u.active = true")
    int deactivateExpiredUrls(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(u) FROM Url u WHERE u.userId = :userId AND u.active = true")
    long countActiveByUserId(@Param("userId") String userId);
}
