package ru.yandex.market.logistics.management.configuration;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Table;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.Type;
import javax.transaction.Transactional;

import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.logistics.management.service.export.partner.customerinfo.dto.PartnerCustomerInfoView;

@TestComponent
public class PostgresDatabaseCleaner {
    private static final Set<Class<?>> ENTITY_TO_SKIP = Set.of(PartnerCustomerInfoView.class);

    private final EntityManager entityManager;

    public PostgresDatabaseCleaner(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void truncate() {
        Metamodel metamodel = entityManager.getMetamodel();

        String query = metamodel.getManagedTypes()
            .stream()
            .map(Type::getJavaType)
            .filter(t -> !ENTITY_TO_SKIP.contains(t))
            .map(c -> c.getAnnotation(Table.class))
            .filter(Objects::nonNull)
            .map(t -> t.schema().isEmpty() ?
                t.name() :
                String.join(".", t.schema(), t.name()))
            .collect(Collectors.joining(", ", "TRUNCATE TABLE ", " RESTART IDENTITY CASCADE"));

        entityManager.createNativeQuery(query).executeUpdate();
    }
}
