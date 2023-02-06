package ru.yandex.market.delivery.transport_manager.facade;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.converter.ffwf.RequestSubtypeIds;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSubtype;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.exception.FFWFRestrictionsBrokenException;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.CourierDTO;
import ru.yandex.market.ff.client.dto.PutWithdrawRequestDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.enums.ApiType;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DatabaseSetup({
    "/repository/facade/transportation_with_deps.xml",
    "/repository/facade/legal_info.xml",
    "/repository/facade/legal_info_1.xml",
    "/repository/facade/return_transportation.xml",
    "/repository/facade/courier.xml",
    "/repository/facade/movement_supply_transportation.xml",
    "/repository/facade/xdock_transport_transportation.xml",
    "/repository/register/register.xml",
    "/repository/facade/register_unit_with_defect.xml",
    "/repository/facade/transportation_unit_register.xml"
})
class FfWfOutboundFacadeTest extends AbstractContextualTest {
    @Autowired
    private FfWfOutboundFacade ffWfOutboundFacade;
    @Autowired
    private FulfillmentWorkflowClientApi ffwfClient;
    @Autowired
    private TransportationMapper transportationMapper;

    @ParameterizedTest
    @MethodSource("getPutOutboundParameters")
    @ExpectedDatabase(value = "/repository/facade/outbound_unit_with_external_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void putOutboundTest(
        long transportationId,
        Integer expectedFfRequestType,
        StockType stockType,
        Long supplyRequestId
    ) {
        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(56L);
        shopRequestDTO.setStatus(RequestStatus.CREATED);

        when(ffwfClient.createEmptyWithdrawRequest(any()))
            .thenReturn(shopRequestDTO);
        ffWfOutboundFacade.putOutbound(transportationMapper.getById(transportationId));

        PutWithdrawRequestDTO expectedRequest = new PutWithdrawRequestDTO();
        expectedRequest.setType(expectedFfRequestType);
        expectedRequest.setSupplyRequestId(supplyRequestId);
        expectedRequest.setExternalRequestId("TMU2");
        expectedRequest.setLogisticsPointId(2L);
        expectedRequest.setDate(OffsetDateTime.parse("2020-07-10T12:00+03:00"));
        CourierDTO courierDTO = FfEntityFactory.createCourier();
        expectedRequest.setCourier(courierDTO);
        expectedRequest.setReceiver(FfEntityFactory.createReceiver());
        expectedRequest.setStockType(stockType);
        expectedRequest.setTransportationId("TM" + transportationId);
        expectedRequest.setApiType(ApiType.FULFILLMENT);
        // для FF-сущностей не определён equals, поэтому проверка через isEqualToComparingFieldByFieldRecursively
        var captor = ArgumentCaptor.forClass(PutWithdrawRequestDTO.class);
        verify(ffwfClient).createEmptyWithdrawRequest(captor.capture());
        if (Objects.equals(captor.getValue().getRequestType(), RequestType.MOVEMENT_WITHDRAW.getId())) {
            expectedRequest.setIgnoreItemsWithErrors(true);
        }
        softly.assertThat(captor.getValue()).isEqualToComparingFieldByFieldRecursively(expectedRequest);
    }

    @Test
    @ExpectedDatabase(value = "/repository/facade/outbound_unit_with_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void putOutboundTest_badRequestReceived() {
        when(ffwfClient.createEmptyWithdrawRequest(any()))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        ffWfOutboundFacade.putOutbound(transportationMapper.getById(1L));
        verify(ffwfClient).createEmptyWithdrawRequest(any());
    }

    @Test
    @DatabaseSetup(value = "/repository/facade/outbound_processed.xml", type = DatabaseOperation.UPDATE)
    void putOutboundTestAlreadyProcessed() {
        ffWfOutboundFacade.putOutbound(transportationMapper.getById(1));
        verify(ffwfClient, never()).createEmptyWithdrawRequest(any());
    }

    @Test
    @DatabaseSetup(value = "/repository/facade/outbound_plan_register_sent.xml", type = DatabaseOperation.UPDATE)
    void putOutboundTestIncorrectStatus() {
        softly.assertThatThrownBy(() -> {
                ffWfOutboundFacade.putOutbound(transportationMapper.getById(1L));
            })
            .isInstanceOf(FFWFRestrictionsBrokenException.class);
        verify(ffwfClient, never()).createEmptyWithdrawRequest(any());
    }

    private static Stream<Arguments> getPutOutboundParameters() {
        int breakBulkXDockType = RequestSubtypeIds.id(
            TransportationType.XDOC_TRANSPORT,
            TransportationSubtype.BREAK_BULK_XDOCK,
            TransportationUnitType.OUTBOUND
        );

        return Stream.of(
            Arguments.of(1L, RequestType.ORDERS_WITHDRAW.getId(), null, null),
            Arguments.of(2L, RequestType.ORDERS_RETURN_WITHDRAW.getId(), null, null),
            Arguments.of(3L, RequestType.MOVEMENT_WITHDRAW.getId(), StockType.DEFECT, null),
            Arguments.of(4L, breakBulkXDockType, null, 1000L)
        );
    }

}
