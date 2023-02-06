package ru.yandex.market.delivery.transport_manager.service.distribution_center.yt;

import java.time.OffsetDateTime;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.domain.yt.wms.DcStateDto;
import ru.yandex.market.delivery.transport_manager.service.TransportationService;
import ru.yandex.market.delivery.transport_manager.service.yt.YtCommonReader;

import static org.mockito.Mockito.doReturn;

@ParametersAreNonnullByDefault
class PullDcStateFromYtServiceTest extends AbstractContextualTest {

    @Autowired
    YtCommonReader<DcStateDto> ytReader;

    @Autowired
    private PullDcStateFromYtService service;

    @Autowired
    private TransportationService transportationService;

    @Test
    @DisplayName("Выгрузка WMS DC logistic_units YT: успешный пулл состояния")
    @DatabaseSetup("/service/pull_dc_state_from_yt/existing_state.xml")
    @DatabaseSetup("/repository/distribution_unit_center/transportations_to_ff_break_bulk_xdock.xml")
    @ExpectedDatabase(
        value = "/service/pull_dc_state_from_yt/new_state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void sync() {
        String[] palletDcIds = {"PAL1", "PAL2"};
        String[] boxDcIds = {"BOX1", "BOX2"};
        Long outboundExternalId = 11L;

        doReturn(Set.of(
            mockDcStateDto(UnitType.PALLET, palletDcIds[0], outboundExternalId),
            mockDcStateDto(UnitType.PALLET, palletDcIds[1], null),
            mockDcStateDto(UnitType.BOX, boxDcIds[0], outboundExternalId, palletDcIds[1]),
            mockDcStateDto(UnitType.BOX, boxDcIds[1], outboundExternalId, palletDcIds[1])
        )).
            when(ytReader).getTableData(
                DcStateDto.class,
                "//home/market/testing/delivery/transport_manager/wms/logistic_units"
            );

        service.sync();
    }

    private DcStateDto mockDcStateDto(
        UnitType unitType,
        String dcUnitId,
        Long outboundExternalId
    ) {
        return mockDcStateDto(unitType, dcUnitId, outboundExternalId, null);
    }

    private DcStateDto mockDcStateDto(
        UnitType unitType,
        String dcUnitId,
        Long outboundExternalId,
        String parentId
    ) {
        OffsetDateTime outboundTime = OffsetDateTime.parse("2022-03-03T12:00:00+03:00");

        return DcStateDto.builder()
            .type(unitType)
            .dcUnitId(dcUnitId)
            .outboundExternalId(outboundExternalId)
            .outboundTime(outboundTime)
            .updatedTime(outboundTime)
            .parentId(parentId)
            .build();
    }

}
