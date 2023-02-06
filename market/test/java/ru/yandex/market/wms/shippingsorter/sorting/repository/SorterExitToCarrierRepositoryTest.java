package ru.yandex.market.wms.shippingsorter.sorting.repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.entity.SorterExitToCarrierEntity;
import ru.yandex.market.wms.shippingsorter.sorting.entity.id.Carrier;
import ru.yandex.market.wms.shippingsorter.sorting.entity.id.OperationDayId;
import ru.yandex.market.wms.shippingsorter.sorting.entity.id.SorterExitId;
import ru.yandex.market.wms.shippingsorter.sorting.entity.id.SorterExitToCarrierId;
import ru.yandex.market.wms.shippingsorter.sorting.exception.SorterExitToCarrierNotFoundException;

import static java.util.Arrays.asList;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class SorterExitToCarrierRepositoryTest extends IntegrationTest {

    @Autowired
    private SorterExitToCarrierRepository sorterExitToCarrierRepository;

    @Test
    @DatabaseSetup("/sorting/repository/sorter-exit-to-carrier/before.xml")
    @ExpectedDatabase("/sorting/repository/sorter-exit-to-carrier/before.xml")
    public void findTest_exist() {
        SorterExitToCarrierId id = SorterExitToCarrierId.of(391L);

        SorterExitToCarrierEntity expectedEntity = SorterExitToCarrierEntity.builder()
                .sorterExitToCarrierId(id)
                .sorterExitId(SorterExitId.of("SR1_CH-19"))
                .carrier(Carrier.of("158758"))
                .operationDayId(OperationDayId.of(18262L))
                .scheduledShipDate(getDateTime("2020-01-01 00:00"))
                .weightMax(100000000)
                .weightMin(0)
                .timeFrom(LocalTime.of(0, 0, 0))
                .timeTo(LocalTime.of(23, 59, 59))
                .rowVersion(3)
                .build();

        SorterExitToCarrierEntity sorterExitToCarrierEntity = sorterExitToCarrierRepository.find(id);

        Assertions.assertEquals(expectedEntity, sorterExitToCarrierEntity);
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-exit-to-carrier/before.xml")
    @ExpectedDatabase("/sorting/repository/sorter-exit-to-carrier/before.xml")
    public void findTest_SorterExitToCarrierNotFoundException() {
        SorterExitToCarrierId id = SorterExitToCarrierId.of(392L);

        Assertions.assertThrows(
                SorterExitToCarrierNotFoundException.class,
                () -> sorterExitToCarrierRepository.find(id)
        );
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-exit-to-carrier/before.xml")
    @ExpectedDatabase("/sorting/repository/sorter-exit-to-carrier/before.xml")
    public void findAllTest() {
        SorterExitToCarrierEntity expectedEntity1 = SorterExitToCarrierEntity.builder()
                .sorterExitToCarrierId(SorterExitToCarrierId.of(391L))
                .sorterExitId(SorterExitId.of("SR1_CH-19"))
                .carrier(Carrier.of("158758"))
                .operationDayId(OperationDayId.of(18262L))
                .scheduledShipDate(getDateTime("2020-01-01 00:00"))
                .weightMax(100000000)
                .weightMin(0)
                .timeFrom(LocalTime.of(0, 0, 0))
                .timeTo(LocalTime.of(23, 59, 59))
                .rowVersion(3)
                .build();

        SorterExitToCarrierEntity expectedEntity2 = SorterExitToCarrierEntity.builder()
                .sorterExitToCarrierId(SorterExitToCarrierId.of(393L))
                .sorterExitId(SorterExitId.of("SR1_CH-20"))
                .carrier(Carrier.of("158758"))
                .operationDayId(OperationDayId.of(18262L))
                .scheduledShipDate(getDateTime("2020-01-01 00:00"))
                .weightMax(1000000)
                .weightMin(13)
                .timeFrom(LocalTime.of(0, 0, 0))
                .timeTo(LocalTime.of(23, 59, 59))
                .rowVersion(2)
                .build();

        SorterExitToCarrierEntity expectedEntity3 = SorterExitToCarrierEntity.builder()
                .sorterExitToCarrierId(SorterExitToCarrierId.of(394L))
                .sorterExitId(SorterExitId.of("SR1_CH-21"))
                .carrier(Carrier.of("158759"))
                .operationDayId(OperationDayId.of(18262L))
                .scheduledShipDate(getDateTime("2020-01-01 00:00"))
                .weightMax(10000)
                .weightMin(100)
                .timeFrom(LocalTime.of(0, 0, 0))
                .timeTo(LocalTime.of(23, 59, 59))
                .rowVersion(1)
                .build();

        List<SorterExitToCarrierEntity> entities = sorterExitToCarrierRepository.findAll();

        Assertions.assertEquals(asList(expectedEntity1, expectedEntity2, expectedEntity3), entities);
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-exit-to-carrier/before.xml")
    @ExpectedDatabase("/sorting/repository/sorter-exit-to-carrier/before.xml")
    public void findAllByCarrierCodeTest() {
        String carrierCode = "158758";

        SorterExitToCarrierEntity expectedEntity1 = SorterExitToCarrierEntity.builder()
                .sorterExitToCarrierId(SorterExitToCarrierId.of(391L))
                .sorterExitId(SorterExitId.of("SR1_CH-19"))
                .carrier(Carrier.of(carrierCode))
                .operationDayId(OperationDayId.of(18262L))
                .scheduledShipDate(getDateTime("2020-01-01 00:00"))
                .weightMax(100000000)
                .weightMin(0)
                .timeFrom(LocalTime.of(0, 0, 0))
                .timeTo(LocalTime.of(23, 59, 59))
                .rowVersion(3)
                .build();

        SorterExitToCarrierEntity expectedEntity2 = SorterExitToCarrierEntity.builder()
                .sorterExitToCarrierId(SorterExitToCarrierId.of(393L))
                .sorterExitId(SorterExitId.of("SR1_CH-20"))
                .carrier(Carrier.of(carrierCode))
                .operationDayId(OperationDayId.of(18262L))
                .scheduledShipDate(getDateTime("2020-01-01 00:00"))
                .weightMax(1000000)
                .weightMin(13)
                .timeFrom(LocalTime.of(0, 0, 0))
                .timeTo(LocalTime.of(23, 59, 59))
                .rowVersion(2)
                .build();

        List<SorterExitToCarrierEntity> entities = sorterExitToCarrierRepository.findAll(carrierCode);

        Assertions.assertEquals(asList(expectedEntity1, expectedEntity2), entities);
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-exit-to-carrier/before.xml")
    @ExpectedDatabase("/sorting/repository/sorter-exit-to-carrier/before.xml")
    public void findAllByCarrierCodeAndZoneTest() {
        String carrierCode = "158758";
        String zone = "SSORT_ZONE";

        SorterExitToCarrierEntity expectedEntity1 = SorterExitToCarrierEntity.builder()
                .sorterExitToCarrierId(SorterExitToCarrierId.of(391L))
                .sorterExitId(SorterExitId.of("SR1_CH-19"))
                .carrier(Carrier.of(carrierCode))
                .operationDayId(OperationDayId.of(18262L))
                .scheduledShipDate(getDateTime("2020-01-01 00:00"))
                .weightMax(100000000)
                .weightMin(0)
                .timeFrom(LocalTime.of(0, 0, 0))
                .timeTo(LocalTime.of(23, 59, 59))
                .rowVersion(3)
                .build();

        List<SorterExitToCarrierEntity> entities = sorterExitToCarrierRepository.findAll(carrierCode, zone);

        Assertions.assertEquals(Collections.singletonList(expectedEntity1), entities);
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-exit-to-carrier/before.xml")
    @ExpectedDatabase(
            value = "/sorting/repository/sorter-exit-to-carrier/after-insert.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void insertTest() {
        String userId = "some_user";

        SorterExitToCarrierEntity entity = SorterExitToCarrierEntity.builder()
                .sorterExitId(SorterExitId.of("SR1_CH-22"))
                .carrier(Carrier.of("158750"))
                .operationDayId(OperationDayId.of(18262L))
                .scheduledShipDate(getDateTime("2020-01-01 00:00"))
                .weightMax(100000000)
                .weightMin(0)
                .timeFrom(LocalTime.of(0, 0, 0))
                .timeTo(LocalTime.of(23, 59, 59))
                .rowVersion(3)
                .build();

        sorterExitToCarrierRepository.insert(entity, userId);
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-exit-to-carrier/before.xml")
    @ExpectedDatabase(
            value = "/sorting/repository/sorter-exit-to-carrier/after-update.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void updateTest() {
        String userId = "another_user";

        SorterExitToCarrierEntity entity = SorterExitToCarrierEntity.builder()
                .sorterExitToCarrierId(SorterExitToCarrierId.of(391L))
                .sorterExitId(SorterExitId.of("SR1_CH-19"))
                .carrier(Carrier.of("158500"))
                .operationDayId(OperationDayId.of(18312L))
                .scheduledShipDate(getDateTime("2020-01-01 00:00"))
                .weightMax(100)
                .weightMin(0)
                .timeFrom(LocalTime.of(0, 0, 0))
                .timeTo(LocalTime.of(23, 59, 59))
                .rowVersion(3)
                .build();

        sorterExitToCarrierRepository.update(entity, userId);
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-exit-to-carrier/before.xml")
    @ExpectedDatabase(
            value = "/sorting/repository/sorter-exit-to-carrier/after-delete.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void deleteTest() {
        SorterExitToCarrierId id = SorterExitToCarrierId.of(391L);

        sorterExitToCarrierRepository.delete(id);
    }

    @Test
    @DatabaseSetup("/sorting/repository/sorter-exit-to-carrier/before.xml")
    @ExpectedDatabase("/sorting/repository/sorter-exit-to-carrier/before.xml")
    public void findByExitAndCarrierTest_exist() {
        SorterExitId id = SorterExitId.of("SR1_CH-21");
        Carrier carrier = Carrier.of("158759");

        SorterExitToCarrierEntity expectedEntity = SorterExitToCarrierEntity.builder()
                .sorterExitToCarrierId(SorterExitToCarrierId.of(394L))
                .sorterExitId(id)
                .carrier(carrier)
                .operationDayId(OperationDayId.of(18262L))
                .scheduledShipDate(getDateTime("2020-01-01 00:00"))
                .weightMax(10000)
                .weightMin(100)
                .timeFrom(LocalTime.of(0, 0, 0))
                .timeTo(LocalTime.of(23, 59, 59))
                .rowVersion(1)
                .build();

        List<SorterExitToCarrierEntity> entities = sorterExitToCarrierRepository.findByExitAndCarrier(id, carrier);

        Assertions.assertEquals(Collections.singletonList(expectedEntity), entities);
    }

    private LocalDateTime getDateTime(String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return LocalDateTime.parse(value, formatter);
    }
}
