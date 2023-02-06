package ru.yandex.market.delivery.transport_manager.service.dbqueue;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.dbqueue.DbQueueEntity;
import ru.yandex.market.delivery.transport_manager.domain.entity.dbqueue.DbQueueEntityIdType;
import ru.yandex.market.delivery.transport_manager.provider.service.dbqueue.AdminDbQueueEntitySupplier;
import ru.yandex.market.delivery.transport_manager.provider.service.dbqueue.AdminTransportationEntitySupplier;

@DatabaseSetup(value = "/repository/health/dbqueue/empty.xml", connection = "dbUnitDatabaseConnectionDbQueue")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class AdminDbQueueEntitySupplierTest extends AbstractContextualTest {
    @Autowired
    private List<AdminDbQueueEntitySupplier> entitySuppliers;

    @Autowired
    private AdminTransportationEntitySupplier transportationEntitySupplier;

    @Test
    void testAllEntityTypeIdsAreProvided() {
        entitySuppliers.forEach(this::checkSupplierSupportsAllListedTypes);
    }

    @Test
    @DatabaseSetup("/repository/dbqueue/transportation.xml")
    void testTransportationSupplier() {
        List<DbQueueEntity> entities = transportationEntitySupplier.getEntities(1L);

        assertContainsExactlyInAnyOrder(
            entities,
            new DbQueueEntity().setId(1L).setIdType(DbQueueEntityIdType.MOVEMENT_ID),
            new DbQueueEntity().setId(1L).setIdType(DbQueueEntityIdType.TRANSPORTATION_ID),
            new DbQueueEntity().setId(12L).setIdType(DbQueueEntityIdType.TRANSPORTATION_UNIT_REQUEST_ID),
            new DbQueueEntity().setId(1L).setIdType(DbQueueEntityIdType.TRANSPORTATION_UNIT_ID),
            new DbQueueEntity().setId(2L).setIdType(DbQueueEntityIdType.TRANSPORTATION_UNIT_ID),
            new DbQueueEntity().setId(1L).setIdType(DbQueueEntityIdType.REGISTER_ID),
            new DbQueueEntity().setId(5L).setIdType(DbQueueEntityIdType.TRANSPORTATION_UNIT_DOCUMENT)
        );
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/route/route.xml",
        "/repository/route_schedule/route_schedule.xml",
        "/repository/trip/trips.xml"
    })
    void testTransportationContainsTripType() {
        List<DbQueueEntity> entities = transportationEntitySupplier.getEntities(100L);

        assertContainsExactlyInAnyOrder(
                entities,
                new DbQueueEntity().setId(100L).setIdType(DbQueueEntityIdType.MOVEMENT_ID),
                new DbQueueEntity().setId(100L).setIdType(DbQueueEntityIdType.TRANSPORTATION_ID),
                new DbQueueEntity().setId(100L).setIdType(DbQueueEntityIdType.TRANSPORTATION_UNIT_ID),
                new DbQueueEntity().setId(101L).setIdType(DbQueueEntityIdType.TRANSPORTATION_UNIT_ID),
                new DbQueueEntity().setId(10L).setIdType(DbQueueEntityIdType.TRIP)
        );
    }

    private void checkSupplierSupportsAllListedTypes(AdminDbQueueEntitySupplier supplier) {
        supplier.getRelatedIdTypes().forEach(type -> {
            Object function = supplier.getIdTypeMapping().get(type);
            if (function == null) {
                throw new RuntimeException(
                    String.format(
                        "Supplier with admin type %s doesn't support type %s." +
                            " Please, add it to supplier entity-function map.",
                        supplier.getType(),
                        type
                    )
                );
            }
        });
    }
}
