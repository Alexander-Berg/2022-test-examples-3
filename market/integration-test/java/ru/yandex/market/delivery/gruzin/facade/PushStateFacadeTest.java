package ru.yandex.market.delivery.gruzin.facade;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.delivery.gruzin.model.CargoUnitCreateDto;
import ru.yandex.market.delivery.gruzin.model.CargoUnitsCreateDto;
import ru.yandex.market.delivery.gruzin.model.UnitCargoType;
import ru.yandex.market.delivery.gruzin.model.UnitType;
import ru.yandex.market.delivery.gruzin.model.WarehouseId;
import ru.yandex.market.delivery.gruzin.model.WarehouseIdType;
import ru.yandex.market.delivery.gruzin.task.apply_cargo_units_state.ApplyCargoUnitsStateDto;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class PushStateFacadeTest extends AbstractContextualTest {
    public static final String FILE_NAME = "test.json";
    @Autowired
    private PushStateFacade facade;

    @Autowired
    private MdsS3Client s3Client;

    @Autowired
    private LMSClient lmsClient;

    @ExpectedDatabase(
        value = "/service/distribution_center_unit/after/push_cargo_units.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void pushState() {
        String json = extractFileContent("controller/gruzin/cargo_units.json");

        when(s3Client.download(
            eq(ResourceLocation.create("gruzin-storage-test", FILE_NAME)),
            any()
        ))
            .thenReturn(json);

        when(lmsClient.getLogisticsPoints(eq(
            LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(145L, 172L))
                .type(PointType.WAREHOUSE)
                .active(true)
                .build()
        )))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder().id(100000145L).partnerId(145L).build(),
                LogisticsPointResponse.newBuilder().id(100000172L).partnerId(172L).build()
            ));

        facade.pushState(new ApplyCargoUnitsStateDto(FILE_NAME));
    }

    @ExpectedDatabase(
        value = "/service/distribution_center_unit/after/push_cargo_units_null_target.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void pushStateNullTarget() {
        String json = extractFileContent("controller/gruzin/cargo_units_null_target.json");

        when(s3Client.download(
            eq(ResourceLocation.create("gruzin-storage-test", FILE_NAME)),
            any()
        ))
            .thenReturn(json);

        when(lmsClient.getLogisticsPoints(eq(
            LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(145L))
                .type(PointType.WAREHOUSE)
                .active(true)
                .build()
        )))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder().id(100000145L).partnerId(145L).build()
            ));

        facade.pushState(new ApplyCargoUnitsStateDto(FILE_NAME));
    }

    @Test
    void getLogisticPointIdsByPartnerIdDuplicates() {
        when(lmsClient.getLogisticsPoints(eq(
            LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(145L, 172L))
                .type(PointType.WAREHOUSE)
                .active(true)
                .build()
        )))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder().id(100000145L).partnerId(145L).build(),
                LogisticsPointResponse.newBuilder().id(100000172L).partnerId(172L).build(),
                LogisticsPointResponse.newBuilder().id(110000172L).partnerId(172L).build()
            ));

        softly.assertThatThrownBy(() ->
                facade.getLogisticPointIdsByPartnerId(new CargoUnitsCreateDto()
                    .setPartnerId(145L)
                    .setSnapshotDateTime(Instant.parse("2017-09-11T07:30:00Z"))
                    .setTargetWarehouse(new WarehouseId(WarehouseIdType.PARTNER, 172L))
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
                    ))
                )
            )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Multiple logistics points for one partner: 172: 100000172,110000172");

    }
}
