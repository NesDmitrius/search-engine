package skillbox.SearchEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skillbox.SearchEngine.model.PageEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    List<PageEntity> findBySite_UrlLike(String siteUrl);

    Optional<PageEntity> findByPathAndSiteId(String pagePath, Integer siteId);

    int countPageEntitiesBySiteId(int siteId);

}
