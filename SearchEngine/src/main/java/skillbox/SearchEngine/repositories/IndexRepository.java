package skillbox.SearchEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skillbox.SearchEngine.model.IndexEntity;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
}
