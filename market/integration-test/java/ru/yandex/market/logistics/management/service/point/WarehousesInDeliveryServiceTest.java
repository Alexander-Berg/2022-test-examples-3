package ru.yandex.market.logistics.management.service.point;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.PutReferenceWarehouseInDeliveryStatus;
import ru.yandex.market.logistics.management.domain.entity.type.WarehouseInDeliveryCreationStatus;
import ru.yandex.market.logistics.management.repository.PutReferenceWarehouseInDeliveryStatusRepository;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
@Sql("/data/service/point/warehouse_in_delivery_data.sql")
class WarehousesInDeliveryServiceTest extends AbstractContextualTest {
    @Autowired
    private WarehousesInDeliveryService service;

    @Autowired
    private PutReferenceWarehouseInDeliveryStatusRepository repository;

    @Test
    void checkUnprocessedPairsPutting() {
        service.putNewUnprocessedPairsIntoTheTableWithNewStatus();
        List<PutReferenceWarehouseInDeliveryStatus> records = repository.findAll();

        softly.assertThat(records)
            .as("Warehouses and delivery partner pairs was not processed properly")
            .usingElementComparatorOnFields("partnerId", "warehouseId", "status")
            .containsExactlyInAnyOrderElementsOf(getExpectedRecordsListAfterPutting());
    }

    @Test
    void checkTwiceMethodCalling() {
        service.putNewUnprocessedPairsIntoTheTableWithNewStatus();
        service.putNewUnprocessedPairsIntoTheTableWithNewStatus();
        List<PutReferenceWarehouseInDeliveryStatus> records = repository.findAll();

        softly.assertThat(records)
            .as("Warehouses and delivery partner pairs was not processed properly")
            .usingElementComparatorOnFields("partnerId", "warehouseId", "status")
            .containsExactlyInAnyOrderElementsOf(getExpectedRecordsListAfterPutting());
    }

    private List<PutReferenceWarehouseInDeliveryStatus> getExpectedRecordsListAfterPutting() {
        return Arrays.asList(
            generateRecord(2L, 1L, WarehouseInDeliveryCreationStatus.CREATED),
            generateRecord(2L, 3L, WarehouseInDeliveryCreationStatus.NEW),
            generateRecord(2L, 4L, WarehouseInDeliveryCreationStatus.NEW),
            generateRecord(5L, 1L, WarehouseInDeliveryCreationStatus.NEW),
            generateRecord(5L, 3L, WarehouseInDeliveryCreationStatus.NEW),
            generateRecord(5L, 4L, WarehouseInDeliveryCreationStatus.NEW)
        );
    }

    private PutReferenceWarehouseInDeliveryStatus generateRecord(
        Long partnerId,
        Long warehouseId,
        WarehouseInDeliveryCreationStatus recordStatus
    ) {
        PutReferenceWarehouseInDeliveryStatus record = new PutReferenceWarehouseInDeliveryStatus();
        record.setPartnerId(partnerId);
        record.setWarehouseId(warehouseId);
        record.setStatus(recordStatus);

        return record;
    }
}
