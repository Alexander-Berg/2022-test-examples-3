package ru.yandex.market.delivery.transport_manager.facade;

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.exception.FFWFRestrictionsBrokenException;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.CourierDTO;
import ru.yandex.market.ff.client.dto.PutSupplyRequestDTO;
import ru.yandex.market.ff.client.dto.RealSupplierInfoDTO;
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
    "/repository/facade/courier.xml",
    "/repository/facade/movement_supply_transportation.xml",
    "/repository/register/register.xml",
    "/repository/facade/register_unit_with_defect.xml",
    "/repository/facade/transportation_unit_register.xml",
})
public class FfWfInboundFacadeTest extends AbstractContextualTest {
    @Autowired
    private FfWfInboundFacade ffWfInboundFacade;
    @Autowired
    private FulfillmentWorkflowClientApi ffwfClient;
    @Autowired
    private TransportationMapper mapper;

    @ParameterizedTest
    @MethodSource("getPutOutboundParameters")
    @ExpectedDatabase(value = "/repository/facade/inbound_unit_with_external_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void putInboundTest(long transportationId, RequestType requestType, StockType stockType) {
        ShopRequestDTO shopRequestDTO = new ShopRequestDTO();
        shopRequestDTO.setId(56L);
        shopRequestDTO.setStatus(RequestStatus.CREATED);

        when(ffwfClient.createEmptySupplyRequest(any()))
            .thenReturn(shopRequestDTO);
        ffWfInboundFacade.putInbound(mapper.getById(transportationId), false);

        PutSupplyRequestDTO expectedRequest = new PutSupplyRequestDTO();
        expectedRequest.setType(requestType.getId());
        expectedRequest.setExternalRequestId("TMU3");
        expectedRequest.setLogisticsPointId(2L);
        expectedRequest.setDate(OffsetDateTime.parse("2020-07-12T12:00+03:00"));
        expectedRequest.setIsConfirmed(false);
        CourierDTO courierDTO = FfEntityFactory.createCourier();
        expectedRequest.setCourier(courierDTO);
        expectedRequest.setShipper(FfEntityFactory.createShipper());
        expectedRequest.setStockType(stockType);
        expectedRequest.setTransportationId("TM" + transportationId);
        expectedRequest.setApiType(ApiType.FULFILLMENT);
        RealSupplierInfoDTO realSupplier = new RealSupplierInfoDTO();
        realSupplier.setId("00005");
        realSupplier.setName("Supplier name");
        expectedRequest.setRealSupplier(realSupplier);

        var captor = ArgumentCaptor.forClass(PutSupplyRequestDTO.class);
        verify(ffwfClient).createEmptySupplyRequest(captor.capture());
        softly.assertThat(captor.getValue()).isEqualToComparingFieldByFieldRecursively(expectedRequest);
    }

    @Test
    @ExpectedDatabase(value = "/repository/facade/inbound_unit_with_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void putInboundTest_badRequestReceived() {
        when(ffwfClient.createEmptySupplyRequest(any()))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        ffWfInboundFacade.putInbound(mapper.getById(1L), false);
        verify(ffwfClient).createEmptySupplyRequest(any());
    }

    @Test
    @DatabaseSetup(value = "/repository/facade/inbound_processed.xml", type = DatabaseOperation.UPDATE)
    void putInboundTestAlreadyProcessed() {
        ffWfInboundFacade.putInbound(mapper.getById(1L), false);
        verify(ffwfClient, never()).createEmptyWithdrawRequest(any());
    }

    @Test
    @DatabaseSetup(value = "/repository/facade/inbound_plan_register_sent.xml", type = DatabaseOperation.UPDATE)
    void putInboundTestIncorrectStatus() {
        softly.assertThatThrownBy(() -> {
                ffWfInboundFacade.putInbound(mapper.getById(1L), false);
            })
            .isInstanceOf(FFWFRestrictionsBrokenException.class);
        verify(ffwfClient, never()).createEmptyWithdrawRequest(any());
    }

    @Test
    void testThrowExceptionForIllegalUpdate() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> ffWfInboundFacade.putInbound(mapper.getById(1L), true)
        );
    }

    private static Stream<Arguments> getPutOutboundParameters() {
        return Stream.of(
            Arguments.of(1L, RequestType.ORDERS_SUPPLY, null),
            Arguments.of(3L, RequestType.MOVEMENT_SUPPLY, StockType.DEFECT)
        );
    }
}
