package skillbox.SearchEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skillbox.SearchEngine.model.IndexEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    List<IndexEntity> findIndexEntitiesByPageId (Integer pageId);
}
