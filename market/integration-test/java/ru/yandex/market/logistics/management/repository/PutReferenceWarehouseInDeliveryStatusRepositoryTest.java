package ru.yandex.market.logistics.management.repository;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.entity.type.WarehouseInDeliveryCreationStatus;

import static org.assertj.core.api.Assertions.assertThat;

class PutReferenceWarehouseInDeliveryStatusRepositoryTest extends AbstractContextualAspectValidationTest {

    @Autowired
    private PutReferenceWarehouseInDeliveryStatusRepository putReferenceWarehouseInDeliveryStatusRepository;

    @Test
    @DatabaseSetup("/data/repository/put_reference_warehouse_in_delivery_status_repository_setup.xml")
    void countByStatusGroupByPartnerId() {
        List<GroupedWarehouseReference> actual =
            putReferenceWarehouseInDeliveryStatusRepository
                .countWarehousesWithStatusGroupByPartnerId(WarehouseInDeliveryCreationStatus.COULD_NOT_BE_PUSHED);

        assertThat(actual).containsExactlyInAnyOrder(
            new GroupedWarehouseReference(201L, 2L),
            new GroupedWarehouseReference(202L, 1L)
        );
    }
}
