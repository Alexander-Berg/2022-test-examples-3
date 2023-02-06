package ru.yandex.market.delivery.mdbapp.scheduled;

import java.util.Collections;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import steps.LocationSteps;
import steps.shopSteps.PaymentStatusSteps;
import steps.shopSteps.ShopSteps;

import ru.yandex.market.checkout.checkouter.client.CheckouterDeliveryAPI;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tariff.TariffData;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.delivery.export.client.DeliveryExportClient;
import ru.yandex.market.delivery.export.client.payload.TariffOptions;
import ru.yandex.market.delivery.export.client.payload.TariffParams;
import ru.yandex.market.delivery.mdbapp.IntegrationTest;
import ru.yandex.market.delivery.mdbapp.components.geo.Location;
import ru.yandex.market.delivery.mdbapp.components.service.marketid.LegalInfoReceiver;
import ru.yandex.market.delivery.mdbapp.exception.PaymentMethodException;
import ru.yandex.market.delivery.mdbapp.integration.endpoint.LabelsEventHandler;
import ru.yandex.market.delivery.mdbapp.integration.enricher.CrossDockOrderEnricher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.OrderExternalEnricher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.TariffDataEnricher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.InletFetcher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.LocationFetcher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.LogisticsPointFetcher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.ReturnInletFetcher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.ShopFetcher;
import ru.yandex.market.delivery.mdbapp.integration.gateway.OrderEventsGateway;
import ru.yandex.market.delivery.mdbapp.integration.payload.GetTariffData;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPoint;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPointPair;
import ru.yandex.market.delivery.mdbapp.integration.transformer.GetLabelsRequestTransformer;
import ru.yandex.market.delivery.mdbapp.integration.transformer.GetTariffDataTransformer;
import ru.yandex.market.delivery.mdbapp.integration.transformer.OrderToExtendedOrdersTransformer;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayValidationException;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffDto;
import ru.yandex.market.logistics.tarifficator.model.enums.DeliveryMethod;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;
import ru.yandex.market.mbi.api.client.entity.shops.ShopOrgInfo;
import ru.yandex.market.sc.internal.client.ScIntClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_DELIVERY_UPDATED;
import static steps.logisticsPointSteps.LogisticPointSteps.getDefaultOutlet;
import static steps.orderSteps.OrderEventSteps.getOrderHistoryEvent;
import static steps.orderSteps.OrderSteps.getRedMultipleOrder;
import static steps.orderSteps.OrderSteps.getRedSingleOrder;

@RunWith(SpringRunner.class)
@MockBean({
    LMSClient.class,
    PechkinHttpClient.class,
    LegalInfoReceiver.class,
    ScIntClient.class,
})
@IntegrationTest
public class CrossDockFlowOrderEventsTest {

    private static final String YADO_TARIFF_CODE = "RM";
    private static final String TARIFFICATOR_TARIFF_CODE = "TRM";
    private static final long DELIVERY_SERVICE_ID = 123L;
    private static final long TARIFFICATOR_TARIFF_ID = 2234562L;
    private static final long YADO_TARIFF_ID = 223L;

    private static final TariffDto TARIFFICATOR_TARIFF = TariffDto.builder()
        .id(TARIFFICATOR_TARIFF_ID)
        .code(TARIFFICATOR_TARIFF_CODE)
        .partnerId(DELIVERY_SERVICE_ID)
        .deliveryMethod(DeliveryMethod.PICKUP)
        .currency("RUB")
        .build();

    private static final TariffParams YADO_TARIFF = new TariffParams()
        .setId(YADO_TARIFF_ID)
        .setCode(YADO_TARIFF_CODE)
        .setCarrierId(DELIVERY_SERVICE_ID)
        .setCurrency("RUB")
        .setDeliveryMethod("PICKUP")
        .setTariffOptions(new TariffOptions());

    @Autowired
    private OrderEventsGateway gateway;

    @MockBean
    private ShopFetcher shopFetcher;

    @MockBean
    private LocationFetcher locationFetcher;

    @MockBean
    private InletFetcher inletFetcher;

    @MockBean
    private LogisticsPointFetcher logisticsPointFetcher;

    @MockBean
    private ReturnInletFetcher returnInletFetcher;

    @SpyBean
    private GetTariffDataTransformer getTariffDataTransformer;

    @MockBean
    private CheckouterDeliveryAPI checkouterDeliveryAPI;

    @MockBean
    private DeliveryClient lgwDeliveryClient;

    @MockBean
    private DeliveryExportClient deliveryExportClient;

    @MockBean
    private TarifficatorClient tarifficatorClient;

    @SpyBean
    private OrderToExtendedOrdersTransformer orderToExtendedOrdersTransformer;

    @SpyBean
    private CrossDockOrderEnricher crossDockOrderEnricher;

    @SpyBean
    private GetLabelsRequestTransformer getLabelsRequestTransformer;

    @SpyBean
    private LabelsEventHandler labelsEventHandler;

    @SpyBean
    private OrderExternalEnricher redOrderEnricher;

    @SpyBean
    private TariffDataEnricher tariffDataEnricher;

    @Before
    public void setup() {
        Shop shop = ShopSteps.getDefaultShop(
            1,
            Collections.singletonList(new ShopOrgInfo(
                "TYPE",
                "OGRN",
                "NAME",
                "FACT_ADDRESS",
                "JURIDICAL_ADDRESS",
                "ya_money",
                "registration_number",
                "info_url"
            )),
            PaymentStatusSteps.getPaymentStatus()
        );

        Location location = LocationSteps.getLocation();

        when(shopFetcher.fetch(any(Order.class))).thenReturn(shop);
        when(locationFetcher.fetch(any(Order.class))).thenReturn(location);
        when(locationFetcher.fetch(any(Shop.class))).thenReturn(location);
        when(locationFetcher.fetch(any(LogisticsPoint.class))).thenReturn(location);
        when(logisticsPointFetcher.fetch(any(Order.class))).thenReturn(getDefaultOutlet());
        when(inletFetcher.fetch(any(Order.class))).thenReturn(new LogisticsPointPair(getDefaultOutlet(), null));
        when(returnInletFetcher.doFetch(any(Long.class))).thenReturn(new LogisticsPointPair(getDefaultOutlet(), null));
    }

    @Test
    @Sql(value = "/data/clean-create-parcel-order-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void createRedOrderFlowSingleSuccess() throws GatewayApiException, GatewayValidationException {
        OrderHistoryEvent event = getOrderHistoryEventSingleParcel();

        gateway.processEvent(event);
        verifyMockOrderEnricher();
    }

    @Test
    @Sql(value = "/data/clean-create-parcel-order-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void createRedOrderFlowMultiplePartlyReadySuccess() throws GatewayApiException, GatewayValidationException {
        OrderHistoryEvent event = getOrderHistoryEventMultipleParcel();

        gateway.processEvent(event);
        verify(redOrderEnricher).enrich(any());
        verify(orderToExtendedOrdersTransformer).transform(any());
        verify(crossDockOrderEnricher, times(2)).enrich(any());
        verify(lgwDeliveryClient, times(2)).createOrder(any(), any());
    }

    @Test
    @Sql(value = "/data/clean-create-parcel-order-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void createRedOrderFlowMultiplePartlyReadyWithTrackCodesSuccess()
        throws GatewayApiException, GatewayValidationException {

        OrderHistoryEvent event = getOrderHistoryEventMultipleParcelPartlyTracksReady();

        gateway.processEvent(event);
        verifyMockOrderEnricher();
    }

    @Test
    @Sql(value = "/data/clean-create-parcel-order-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void createRedOrderFlowMultiplePartlyReadyWithZeroWeightSuccess()
        throws GatewayApiException, GatewayValidationException {

        OrderHistoryEvent event = getOrderHistoryEventMultipleParcelPartlyWeightReady();

        gateway.processEvent(event);
        verifyMockOrderEnricher();
    }

    @Test
    @Sql(value = "/data/clean-create-parcel-order-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void createRedOrderFlowMultipleDuplicate() throws GatewayApiException, GatewayValidationException {
        OrderHistoryEvent event = getOrderHistoryEventMultipleParcel();

        gateway.processEvent(event);
        gateway.processEvent(event);
        verify(orderToExtendedOrdersTransformer, times(2)).transform(any());
        verify(crossDockOrderEnricher, times(2)).enrich(any());
        verify(lgwDeliveryClient, times(2)).createOrder(any(), any());
    }

    @Test
    @Sql(value = "/data/clean-create-parcel-order-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void createRedOrderFlowMultipleDuplicateWithErrors() throws GatewayApiException, GatewayValidationException {
        OrderHistoryEvent event = getOrderHistoryEventMultipleParcel();

        doThrow(new RuntimeException())
            .doNothing()
            .when(lgwDeliveryClient).createOrder(any(), any());

        IntStream.range(0, 2).forEach(count -> {
            try {
                gateway.processEvent(event);
            } catch (Exception e) {
            }
        });
        verify(orderToExtendedOrdersTransformer, times(2)).transform(any());
        verify(crossDockOrderEnricher, times(3)).enrich(any());
        verify(lgwDeliveryClient, times(3)).createOrder(any(), any());
    }

    @Test
    @Sql(value = "/data/clean-create-parcel-order-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/data/prepare-create-parcel-order-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void createRedOrderFlowMultiplePartlyDuplicate() throws GatewayApiException, GatewayValidationException {
        OrderHistoryEvent event = getOrderHistoryEventMultipleParcel();

        gateway.processEvent(event);
        verifyMockOrderEnricher();
    }

    @Test
    @Sql(value = "/data/clean-create-parcel-order-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void createRedOrderFlowWrongPaymentMethod() {
        OrderHistoryEvent event = getOrderHistoryEventBankPayment();

        try {
            gateway.processEvent(event);
        } catch (Exception ex) {
            assertThat(ex.getCause().getClass()).as("Exception type raised").isEqualTo(PaymentMethodException.class);
        }
    }

    @Test
    @Sql(value = "/data/clean-get-parcel-label-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void getRedFlowLabelsSuccess() throws GatewayApiException, GatewayValidationException {
        OrderHistoryEvent event = getOrderHistoryEventSingleParcelCreated();

        gateway.processEvent(event);
        verifyMockLabelsHandler();
    }

    @Test
    @Sql(value = "/data/clean-get-parcel-label-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void getRedFlowRepeatedLabelsRequest() throws GatewayApiException, GatewayValidationException {
        OrderHistoryEvent event = getOrderHistoryEventSingleParcelCreated();

        gateway.processEvent(event);
        gateway.processEvent(event);
        verify(getLabelsRequestTransformer, times(2)).transform(any());
        verify(labelsEventHandler).handleLabelEvent(any());
        verify(lgwDeliveryClient).getLabels(any(), any());
    }

    @Test
    @Sql(value = "/data/clean-get-parcel-label-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void getRedFlowRepeatedMultiLabelsRequestWithFails() throws GatewayApiException, GatewayValidationException {
        OrderHistoryEvent event = getOrderHistoryEventMultiParcelCreated();

        doThrow(new RuntimeException())
            .doNothing()
            .when(lgwDeliveryClient)
            .getLabels(any(), any());

        IntStream.range(0, 2).forEach(count -> {
            try {
                gateway.processEvent(event);
            } catch (Exception e) {
            }
        });
        verify(getLabelsRequestTransformer, times(2)).transform(any());
        verify(labelsEventHandler, times(3)).handleLabelEvent(any());
        verify(lgwDeliveryClient, times(3)).getLabels(any(), any());
    }

    @Test
    @Sql(value = "/data/clean-get-parcel-label-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void getRedFlowRepeatedMultiLabelsRequest() throws GatewayApiException, GatewayValidationException {
        OrderHistoryEvent event = getOrderHistoryEventMultiParcelCreated();

        gateway.processEvent(event);
        gateway.processEvent(event);
        verify(getLabelsRequestTransformer, times(2)).transform(any());
        verify(labelsEventHandler, times(2)).handleLabelEvent(any());
        verify(lgwDeliveryClient, times(2)).getLabels(any(), any());
    }

    @Test
    @Sql(value = "/data/clean-get-parcel-label-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void getRedFlowRepeatedMultiLabelsRequestWithPartialTrackCodesReady()
        throws GatewayApiException, GatewayValidationException {

        OrderHistoryEvent event = getOrderHistoryEventMultiParcelCreatedPartlySetTrackCodes();

        gateway.processEvent(event);
        verifyMockLabelsHandler();
    }

    @Test
    @Sql(value = "/data/clean-get-parcel-label-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(value = "/data/prepare-get-parcel-label-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void getRedFlowPartlyRepeatedMultiLabelsRequest() throws GatewayApiException, GatewayValidationException {
        OrderHistoryEvent event = getOrderHistoryEventMultiParcelCreated();

        gateway.processEvent(event);
        verifyMockLabelsHandler();
    }

    @Test
    @Sql(value = "/data/clean-get-parcel-label-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void doNotFailOnParcelWithNoItems() throws GatewayValidationException {
        OrderHistoryEvent event = getOrderHistoryEventWithNullParcelItems();
        gateway.processEvent(event);
        verify(crossDockOrderEnricher, times(2)).enrich(any());
    }

    @Test
    @Sql(value = "/data/clean-get-tariff-data-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    public void shouldGetTariffDataFromTarifficatorIfTariffIdIsPercent() {
        doReturn(TARIFFICATOR_TARIFF).when(tarifficatorClient).getTariff(TARIFFICATOR_TARIFF_ID);
        OrderHistoryEvent event = getOrderHistoryEventSingleParcelTarifficatorTariffId();

        gateway.processEvent(event);
        verifyMockTariffDataHandler(TARIFFICATOR_TARIFF_ID, TARIFFICATOR_TARIFF_CODE);
    }

    @Test
    @DisplayName("Выключен флоу обогащения тарифа")
    @Sql(value = "/data/clean-get-tariff-data-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(value = "/data/get-tariff-flow-disabled-flag.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(value = "/data/clean_internal_variable.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    public void noTariffHandlingIfGetTariffIsDisabled() {
        gateway.processEvent(getOrderHistoryEventSingleParcelTarifficatorTariffId());

        verify(checkouterDeliveryAPI, never()).putTariffData(
            any(Long.class),
            any(ClientInfo.class),
            any(TariffData.class)
        );
        verifyZeroInteractions(getTariffDataTransformer, tariffDataEnricher);
    }

    @Test
    @Sql(value = "/data/clean-get-tariff-data-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    public void shouldGetTariffDataFromYadoIfTariffIdIsPercent() {
        doReturn(YADO_TARIFF).when(deliveryExportClient).getTariffParams(YADO_TARIFF_ID);
        OrderHistoryEvent event = getOrderHistoryEventSingleParcelYadoTariffId();

        gateway.processEvent(event);
        verifyMockTariffDataHandler(YADO_TARIFF_ID, YADO_TARIFF_CODE);
    }

    @Test
    @Sql(value = "/data/clean-create-parcel-order-metadata-store.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    public void shouldCreateOrderIfTariffIdAndTariffDataIsProvided() throws GatewayApiException {
        OrderHistoryEvent event = getOrderHistoryEventSingleParcelTariffData();

        gateway.processEvent(event);
        verifyMockOrderEnricher();
    }

    private void verifyMockOrderEnricher() throws GatewayApiException {
        verify(redOrderEnricher).enrich(any());
        verify(orderToExtendedOrdersTransformer).transform(any());
        verify(crossDockOrderEnricher).enrich(any());
        verify(lgwDeliveryClient).createOrder(any(), any());
    }

    private void verifyMockLabelsHandler() throws GatewayApiException {
        verify(getLabelsRequestTransformer).transform(any());
        verify(labelsEventHandler).handleLabelEvent(any());
        verify(lgwDeliveryClient).getLabels(any(), any());
    }

    private void verifyMockTariffDataHandler(long tariffId, String tariffCode) {
        long orderId = 123L;
        GetTariffData getTariffData = new GetTariffData(orderId, tariffId);
        TariffData tariffData = new TariffData();
        tariffData.setTariffCode(tariffCode);
        getTariffData.setTariffData(tariffData);

        verify(getTariffDataTransformer).getTariffData(argThat(
            orderWrapper -> Objects.equals(
                tariffId,
                orderWrapper.getOrder().getDelivery().getTariffId()
            )
        ));
        verify(tariffDataEnricher).getTariffData(argThat(
            data -> Objects.equals(data.toString(), getTariffData.toString())
                && Objects.equals(data.getTariffData().toString(), tariffData.toString())
        ));
        verify(checkouterDeliveryAPI).putTariffData(eq(orderId), any(ClientInfo.class), refEq(tariffData));
    }

    private OrderHistoryEvent getOrderHistoryEventSingleParcel() {
        OrderHistoryEvent event = getOrderHistoryEvent();

        Order redOrderBefore = getRedSingleOrder();
        redOrderBefore.getDelivery().setDeliveryServiceId(DELIVERY_SERVICE_ID);
        redOrderBefore.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        setEmptyTracksToOrderParcel(redOrderBefore, 0);
        setParcelItemsToParcel(redOrderBefore, 0);
        event.setOrderBefore(redOrderBefore);

        Order redOrderAfter = getRedSingleOrder();
        redOrderAfter.getDelivery().setDeliveryServiceId(DELIVERY_SERVICE_ID);
        redOrderAfter.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        setEmptyTracksToOrderParcel(redOrderAfter, 0);
        redOrderAfter.getDelivery().getParcels().get(0).setWeight(10L);
        setParcelItemsToParcel(redOrderAfter, 0);
        redOrderAfter.getDelivery().setTariffData(getTariffData());

        event.setOrderAfter(redOrderAfter);

        return event;
    }

    @Nonnull
    private OrderHistoryEvent getOrderHistoryEventSingleParcelTarifficatorTariffId() {
        OrderHistoryEvent event = getOrderHistoryEvent();

        Order redOrderBefore = getRedSingleOrder();
        redOrderBefore.getDelivery().setTariffId(TARIFFICATOR_TARIFF_ID);
        event.setOrderBefore(redOrderBefore);

        Order redOrderAfter = getRedSingleOrder();
        redOrderAfter.getDelivery().setTariffId(TARIFFICATOR_TARIFF_ID);
        event.setOrderAfter(redOrderAfter);

        return event;
    }

    @Nonnull
    private OrderHistoryEvent getOrderHistoryEventSingleParcelYadoTariffId() {
        OrderHistoryEvent event = getOrderHistoryEvent();

        Order redOrderBefore = getRedSingleOrder();
        redOrderBefore.getDelivery().setTariffId(YADO_TARIFF_ID);
        event.setOrderBefore(redOrderBefore);

        Order redOrderAfter = getRedSingleOrder();
        redOrderAfter.getDelivery().setTariffId(YADO_TARIFF_ID);
        event.setOrderAfter(redOrderAfter);

        return event;
    }

    private OrderHistoryEvent getOrderHistoryEventSingleParcelTariffData() {
        OrderHistoryEvent event = getOrderHistoryEventSingleParcelTarifficatorTariffId();

        setParcelItemsToParcel(event.getOrderBefore(), 0);
        setEmptyTracksToOrderParcel(event.getOrderBefore(), 0);

        setParcelItemsToParcel(event.getOrderAfter(), 0);
        setEmptyTracksToOrderParcel(event.getOrderAfter(), 0);
        event.getOrderAfter().getDelivery().setTariffData(getTariffData());

        return event;
    }

    private OrderHistoryEvent getOrderHistoryEventSingleParcelCreated() {
        OrderHistoryEvent orderHistoryEvent = getOrderHistoryEventSingleParcel();
        orderHistoryEvent.setType(ORDER_DELIVERY_UPDATED);

        setAllParcelsFields(orderHistoryEvent.getOrderBefore(), false);
        setAllParcelsFields(orderHistoryEvent.getOrderAfter(), false);

        setStatusToParcel(orderHistoryEvent.getOrderBefore(), 0, ParcelStatus.NEW);
        setStatusToParcel(orderHistoryEvent.getOrderAfter(), 0, ParcelStatus.CREATED);
        setParcelIdAndUrl(orderHistoryEvent.getOrderAfter(), 0, 123L);

        return orderHistoryEvent;
    }

    private OrderHistoryEvent getOrderHistoryEventMultiParcel() {
        Order redOrderBefore = getRedMultipleOrder();
        Order redOrderAfter = getRedMultipleOrder();
        redOrderBefore.getDelivery().setDeliveryServiceId(DELIVERY_SERVICE_ID);
        redOrderAfter.getDelivery().setDeliveryServiceId(DELIVERY_SERVICE_ID);
        redOrderBefore.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        redOrderAfter.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);

        OrderHistoryEvent event = getOrderHistoryEvent();
        event.setOrderBefore(redOrderBefore);
        event.setOrderAfter(redOrderAfter);

        return event;
    }

    private OrderHistoryEvent getOrderHistoryEventMultiParcelCreated() {
        OrderHistoryEvent orderHistoryEvent = getOrderHistoryEventMultiParcel();
        orderHistoryEvent.setType(ORDER_DELIVERY_UPDATED);

        setAllParcelsFields(orderHistoryEvent.getOrderBefore(), false);
        setAllParcelsFields(orderHistoryEvent.getOrderAfter(), false);

        setStatusToAllParcels(orderHistoryEvent.getOrderBefore(), ParcelStatus.NEW);
        setStatusToAllParcels(orderHistoryEvent.getOrderAfter(), ParcelStatus.CREATED);

        setParcelIdAndUrl(orderHistoryEvent.getOrderAfter(), 0, 123L);
        setParcelIdAndUrl(orderHistoryEvent.getOrderAfter(), 1, 124L);

        return orderHistoryEvent;
    }

    private OrderHistoryEvent getOrderHistoryEventMultiParcelCreatedPartlySetTrackCodes() {
        OrderHistoryEvent orderHistoryEvent = getOrderHistoryEventMultiParcelCreated();

        setEmptyTracksToOrderParcel(orderHistoryEvent.getOrderAfter(), 0);
        orderHistoryEvent.getOrderAfter().getDelivery().getParcels().get(0).setWeight(0L);
        setParcelItemsToParcel(orderHistoryEvent.getOrderAfter(), 1);

        return orderHistoryEvent;
    }

    private OrderHistoryEvent getOrderHistoryEventWithNullParcelItems() {
        OrderHistoryEvent orderHistoryEvent = getOrderHistoryEventMultipleParcel();
        orderHistoryEvent.getOrderAfter().getDelivery().getParcels().get(1).setParcelItems(null);
        return orderHistoryEvent;
    }

    private OrderHistoryEvent getOrderHistoryEventMultipleParcel() {
        OrderHistoryEvent event = getOrderHistoryEvent();

        Order redOrderBefore = getRedMultipleOrder();
        redOrderBefore.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        redOrderBefore.getDelivery().getParcels().get(0).setWeight(0L);
        setStatusToParcel(redOrderBefore, 0, ParcelStatus.UNKNOWN);
        setEmptyTracksToOrderParcel(redOrderBefore, 0);
        event.setOrderBefore(redOrderBefore);

        Order redOrderAfter = getRedMultipleOrder();
        redOrderAfter.setStatus(OrderStatus.PROCESSING);
        redOrderAfter.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        setWeightToParcel(redOrderAfter, 0);
        setStatusToParcel(redOrderAfter, 0, ParcelStatus.NEW);
        Track track = new Track();
        track.setTrackCode("");
        redOrderAfter.getDelivery().getParcels().get(0).setTracks(Collections.singletonList(track));
        redOrderAfter.getDelivery().getParcels().get(1).setTracks(Collections.singletonList(track));
        redOrderAfter.getDelivery().setTariffData(getTariffData());
        event.setOrderAfter(redOrderAfter);

        setParcelItemsToParcel(redOrderBefore, 0);
        setParcelItemsToParcel(redOrderBefore, 1);

        setParcelItemsToParcel(redOrderAfter, 0);
        setParcelItemsToParcel(redOrderAfter, 1);

        return event;
    }

    private OrderHistoryEvent getOrderHistoryEventMultipleParcelPartlyTracksReady() {
        OrderHistoryEvent event = getOrderHistoryEventMultipleParcel();

        setTrackToOrderParcel(event.getOrderAfter(), 0);
        setEmptyTracksToOrderParcel(event.getOrderBefore(), 1);
        setEmptyTracksToOrderParcel(event.getOrderAfter(), 1);
        setWeightToParcel(event.getOrderAfter(), 0);
        setWeightToParcel(event.getOrderAfter(), 1);

        return event;
    }

    private OrderHistoryEvent getOrderHistoryEventMultipleParcelPartlyWeightReady() {
        OrderHistoryEvent event = getOrderHistoryEventMultipleParcel();

        setEmptyTracksToOrderParcel(event.getOrderAfter(), 0);
        setEmptyTracksToOrderParcel(event.getOrderAfter(), 1);
        setWeightToParcel(event.getOrderAfter(), 0);
        event.getOrderAfter().getDelivery().getParcels().get(1).setWeight(0L);

        return event;
    }

    private OrderHistoryEvent getOrderHistoryEventBankPayment() {
        Order redOrder = getRedSingleOrder();
        redOrder.setPaymentMethod(PaymentMethod.BANK_CARD);

        OrderHistoryEvent event = getOrderHistoryEvent();
        event.setOrderBefore(redOrder);
        event.setOrderAfter(redOrder);

        return event;
    }

    private void setAllParcelsFields(Order order, boolean emptyTracks) {
        for (Parcel parcel : order.getDelivery().getParcels()) {
            setParcelFields(parcel, null, emptyTracks);
        }
    }

    private void setParcelFields(Parcel parcel, ParcelStatus status, boolean emptyTracks) {
        if (emptyTracks) {
            setEmptyTracksToOrderParcel(parcel);
        } else {
            setTrackToOrderParcel(parcel);
        }
        setParcelItemsToParcel(parcel);
        setStatusToParcel(parcel, status);
        setWeightToParcel(parcel);
    }

    private void setEmptyTracksToOrderParcel(Order order, int parcelNum) {
        setEmptyTracksToOrderParcel(order.getDelivery().getParcels().get(parcelNum));
    }

    private void setEmptyTracksToOrderParcel(Parcel parcel) {
        parcel.setTracks(Collections.emptyList());
    }

    private void setTrackToOrderParcel(Order order, int parcelNum) {
        order.getDelivery().setDeliveryServiceId(DELIVERY_SERVICE_ID);
        setTrackToOrderParcel(order.getDelivery().getParcels().get(parcelNum));
    }

    private void setTrackToOrderParcel(Parcel parcel) {
        Track track = new Track();
        track.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        track.setTrackCode("123");
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        parcel.setTracks(Collections.singletonList(track));
    }

    private void setParcelItemsToParcel(Order order, int parcelNum) {
        setParcelItemsToParcel(order.getDelivery().getParcels().get(parcelNum));
    }

    private void setParcelItemsToParcel(Parcel parcel) {
        parcel.setParcelItems(Collections.singletonList(new ParcelItem(1L, 2)));
    }

    private void setStatusToAllParcels(Order order, ParcelStatus status) {
        for (Parcel parcel : order.getDelivery().getParcels()) {
            setStatusToParcel(parcel, status);
        }
    }

    private void setStatusToParcel(Order order, int parcelNum, ParcelStatus status) {
        setStatusToParcel(order.getDelivery().getParcels().get(parcelNum), status);
    }

    private void setStatusToParcel(Parcel parcel, ParcelStatus status) {
        parcel.setStatus(status);
    }

    private void setWeightToParcel(Order order, int parcelNum) {
        setWeightToParcel(order.getDelivery().getParcels().get(parcelNum));
    }

    private void setWeightToParcel(Parcel parcel) {
        parcel.setWeight(10L);
    }

    private void setParcelIdAndUrl(Order order, int parcelNum, Long id) {
        setParcelIdAndUrl(order.getDelivery().getParcels().get(parcelNum), id);
    }

    private void setParcelIdAndUrl(Parcel parcel, Long id) {
        parcel.setId(id);
        parcel.setLabelURL(null);
    }

    private TariffData getTariffData() {
        TariffData tariffData = new TariffData();
        tariffData.setTariffCode(YADO_TARIFF_CODE);
        return tariffData;
    }
}
