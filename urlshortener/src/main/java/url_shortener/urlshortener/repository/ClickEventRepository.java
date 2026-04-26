package url_shortener.urlshortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import url_shortener.urlshortener.model.ClickEvent;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByUrlId(Long urlId);

    @Query("""
        SELECT c.country, COUNT(c) as hits
        FROM ClickEvent c
        WHERE c.url.id = :urlId
        GROUP BY c.country
        ORDER BY hits DESC
        """)
    List<Object[]> countByCountryForUrl(@Param("urlId") Long urlId);

    @Query("""
        SELECT DATE(c.createdAt), COUNT(c)
        FROM ClickEvent c
        WHERE c.url.id = :urlId AND c.createdAt >= :since
        GROUP BY DATE(c.createdAt)
        ORDER BY DATE(c.createdAt)
        """)
    List<Object[]> dailyClicksForUrl(@Param("urlId") Long urlId,
                                     @Param("since") LocalDateTime since);
}
