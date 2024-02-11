package skillbox.SearchEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skillbox.SearchEngine.model.LemmaEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {

    List<LemmaEntity> findLemmaEntitiesBySiteId(Integer siteId);

    int countLemmaEntitiesBySiteId(int siteId);

}
