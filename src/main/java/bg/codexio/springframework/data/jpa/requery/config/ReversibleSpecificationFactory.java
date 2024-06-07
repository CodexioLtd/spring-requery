package bg.codexio.springframework.data.jpa.requery.config;

import bg.codexio.springframework.data.jpa.requery.resolver.ReversibleSpecification;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class ReversibleSpecificationFactory {
    private final EntityManager entityManager;

    public ReversibleSpecificationFactory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public <T> ReversibleSpecification<T> create(
            Specification<T> specification,
            Class<T> clazz
    ) {
        var criteriaBuilder = this.entityManager.getCriteriaBuilder();
        var query = criteriaBuilder.createQuery(clazz);
        var root = query.from(clazz);

        return new ReversibleSpecification<>(
                specification,
                root,
                query,
                criteriaBuilder
        );
    }
}
