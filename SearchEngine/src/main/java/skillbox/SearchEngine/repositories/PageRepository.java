package skillbox.SearchEngine.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import skillbox.SearchEngine.model.PageEntity;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    @Modifying
    @Transactional
    void deleteBySite_IdLike(Integer id);
    List<PageEntity> findBySite_UrlLike(String siteUrl);

}
