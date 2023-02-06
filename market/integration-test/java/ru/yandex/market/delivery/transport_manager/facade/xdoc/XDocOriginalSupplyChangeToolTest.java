package ru.yandex.market.delivery.transport_manager.facade.xdoc;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.service.PropertyService;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.PutSupplyRequestDTO;
import ru.yandex.market.ff.client.enums.RequestStatus;

import static org.mockito.ArgumentMatchers.argThat;

class XDocOriginalSupplyChangeToolTest extends AbstractContextualTest {
    public static final int X_DOC_PARTNER_SUPPLY_TO_FF = 21;
    @Autowired
    private XDocOriginalSupplyChangeTool xDocOriginalSupplyChangeTool;

    @Autowired
    private FulfillmentWorkflowClientApi ffwfClient;

    @Autowired
    private PropertyService<TmPropertyKey> propertyService;

    @DatabaseSetup({
        "/repository/transportation/xdoc_to_ff_transportations.xml",
        "/repository/transportation/xdoc_to_dc_transportations.xml",
    })
    @Test
    void changeTransportationToFFStatusByDcRequestId() {
        Mockito
            .when(propertyService.getBoolean(TmPropertyKey.ENABLE_XDOCK_STATUS_CHANGE_FOR_FF_SUPPLY))
            .thenReturn(true);

        RequestStatus newStatus = RequestStatus.SENT_TO_XDOC_SERVICE;

        xDocOriginalSupplyChangeTool.changeTransportationToFFStatusByDcRequestId(22L, newStatus);

        Mockito.verify(ffwfClient).changeXDockRequestStatus(3L, newStatus);
        Mockito.verifyNoMoreInteractions(ffwfClient);
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_to_ff_transportations.xml",
        "/repository/transportation/xdoc_to_dc_transportations.xml",
    })
    @Test
    void updateRootRequestInboundDate() {
        Mockito
            .when(propertyService.getBoolean(TmPropertyKey.ENABLE_XDOCK_DATE_CHANGE_FOR_FF_SUPPLY))
            .thenReturn(true);

        LocalDateTime newDateTime = LocalDateTime.of(2021, 12, 12, 3, 0, 0);

        xDocOriginalSupplyChangeTool.changeTransportationToFFDateTimeByDcRequestId(
            22L,
            newDateTime
        );
        var expected = new PutSupplyRequestDTO();
        expected.setType(X_DOC_PARTNER_SUPPLY_TO_FF);
        expected.setDate(OffsetDateTime.of(newDateTime.plusMinutes(15), ZoneOffset.of("+0300")));
        expected.setLogisticsPointId(1000002L);
        expected.setRequestId(3L);
        expected.setRemainingShelfLifeStartDate(OffsetDateTime.of(newDateTime, ZoneOffset.of("+0300")));
        Mockito.verify(ffwfClient).createEmptySupplyRequest(argThat(argument ->
            argument.getRequestId().equals(expected.getRequestId())
            && argument.getDate().equals(expected.getDate())
            && argument.getType() == expected.getType()
            && argument.getRemainingShelfLifeStartDate().equals(expected.getRemainingShelfLifeStartDate())
            && argument.getLogisticsPointId().equals(expected.getLogisticsPointId())
        ));
        Mockito.verifyNoMoreInteractions(ffwfClient);
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_to_ff_transportations_break_bulk_xdock.xml"
    })
    @Test
    void updateRootRequestInboundDateBreakBulkXdock() {
        Mockito
            .when(propertyService.getBoolean(TmPropertyKey.ENABLE_XDOCK_DATE_CHANGE_FOR_FF_SUPPLY))
            .thenReturn(true);

        LocalDateTime newDateTime = LocalDateTime.of(2021, 12, 12, 3, 0, 0);
        xDocOriginalSupplyChangeTool.changeTransportationToFFDateTimeByDcRequestId(
            1L,
            newDateTime
        );
        Mockito.verifyNoMoreInteractions(ffwfClient);
    }
}
