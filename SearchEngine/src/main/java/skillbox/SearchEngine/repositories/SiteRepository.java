package skillbox.SearchEngine.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import skillbox.SearchEngine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    @Query("SELECT s FROM SiteEntity s WHERE s.url = :url")
    Optional<SiteEntity> findByUrl (@Param("url") String url);

    @Query("SELECT s FROM SiteEntity s WHERE s.url = :url AND s.status = 'INDEXED'")
    Optional<SiteEntity> findByUrlAndStatus (@Param("url") String url);

    @Query("SELECT s FROM SiteEntity s WHERE s.status = 'INDEXED'")
    List<SiteEntity> findAllByStatus();

    @Modifying
    @Transactional
    void deleteByUrl(String url);
}
