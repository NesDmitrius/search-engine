package skillbox.SearchEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skillbox.SearchEngine.model.IndexEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    List<IndexEntity> findIndexEntitiesByPageId (Integer pageId);
    List<IndexEntity> findIndexEntitiesByLemmaId (Integer lemmaId);
    Optional<IndexEntity> findIndexEntityByPageIdAndLemmaId(Integer pageId, Integer lemmaId);
}
