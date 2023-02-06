package ru.yandex.market.delivery.gruzin.service;

import java.time.Instant;
import java.util.List;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.content.provider.TextContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.delivery.gruzin.model.CargoUnitCreateDto;
import ru.yandex.market.delivery.gruzin.model.CargoUnitsCreateDto;
import ru.yandex.market.delivery.gruzin.model.UnitCargoType;
import ru.yandex.market.delivery.gruzin.model.UnitType;
import ru.yandex.market.delivery.gruzin.model.WarehouseId;
import ru.yandex.market.delivery.gruzin.model.WarehouseIdType;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.config.s3.MdsS3Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class CargoUnitsSnapshotStorageTest extends AbstractContextualTest {
    public static final CargoUnitsCreateDto CARGO_UNITS = new CargoUnitsCreateDto()
        .setPartnerId(145L)
        .setSnapshotDateTime(Instant.parse("2017-09-11T07:30:00Z"))
        .setTargetWarehouse(new WarehouseId(WarehouseIdType.LOGISTIC_POINT, 100000172L))
        .setUnits(List.of(
            new CargoUnitCreateDto()
                .setId("DRP0001")
                .setUnitType(UnitType.PALLET)
                .setUnitCargoType(UnitCargoType.INTERWAREHOUSE_FIT)
                .setCreationDate(Instant.parse("2017-09-10T17:00:00Z"))
                .setPlannedOutboundDate(Instant.parse("2017-09-11T17:00:00Z")),
            new CargoUnitCreateDto()
                .setId("BOX0001")
                .setParentId("DRP0001")
                .setUnitType(UnitType.BOX)
                .setUnitCargoType(UnitCargoType.INTERWAREHOUSE_FIT)
                .setCreationOutboundId("TMU12345")
                .setCreationDate(Instant.parse("2017-09-11T07:16:01Z"))
                .setPlannedOutboundDate(Instant.parse("2017-09-11T17:00:00Z"))
        ));
    @Autowired
    private CargoUnitsSnapshotStorage storage;

    @Autowired
    private MdsS3Client client;

    @Autowired
    private MdsS3Properties s3Properties;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(client);
    }

    @Test
    @SneakyThrows
    void saveCargoUnits() {
        String fileName = storage.saveCargoUnits(CARGO_UNITS);

        verify(client).upload(
            eq(ResourceLocation.create(s3Properties.getGruzinBucketName(), fileName)),
            argThat(this::assertTextContentProvider)
        );

        verify(client).getUrl(any());
    }

    @SneakyThrows
    private boolean assertTextContentProvider(ContentProvider argument) {
        if (!(argument instanceof TextContentProvider)) {
            return true;
        }
        JSONCompareResult result = JSONCompare.compareJSON(
            ((TextContentProvider) argument).getText(),
            extractFileContent("controller/gruzin/cargo_units_with_points.json"),
            JSONCompareMode.STRICT
        );
        assertThat(result.failed())
            .withFailMessage(result.getMessage())
            .isFalse();

        return !result.failed();
    }

    @Test
    void getCargoUnits() {
        String fileName = "1.json";

        when(client.download(
            eq(ResourceLocation.create(s3Properties.getGruzinBucketName(), fileName)),
            any()
        ))
            .thenReturn(extractFileContent("controller/gruzin/cargo_units_with_points.json"));

        CargoUnitsCreateDto cargoUnits = storage.getCargoUnits(fileName);
        assertThat(cargoUnits)
            .isEqualTo(CARGO_UNITS);

        verify(client).download(
            eq(ResourceLocation.create(s3Properties.getGruzinBucketName(), fileName)),
            any()
        );
    }
}
