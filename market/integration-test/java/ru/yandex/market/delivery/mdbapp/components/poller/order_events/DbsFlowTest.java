package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.jsonunit.core.Option;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import steps.LocationSteps;
import steps.PartnerInfoSteps;
import steps.shopSteps.PaymentStatusSteps;
import steps.shopSteps.ShopSteps;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.geo.Location;
import ru.yandex.market.delivery.mdbapp.components.queue.parcel.cancel.ChangeRequestCancelParcelDto;
import ru.yandex.market.delivery.mdbapp.components.service.marketid.LegalInfoReceiver;
import ru.yandex.market.delivery.mdbapp.configuration.queue.ChangeRequestCancelParcelQueue;
import ru.yandex.market.delivery.mdbapp.integration.endpoint.LgwDSCreateOrderEventHandler;
import ru.yandex.market.delivery.mdbapp.integration.enricher.DbsOrderEnricher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.DbsOrderExternalEnricher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.LocationFetcher;
import ru.yandex.market.delivery.mdbapp.integration.payload.CreateLgwDsOrder;
import ru.yandex.market.delivery.mdbapp.integration.payload.ExtendedOrder;
import ru.yandex.market.delivery.mdbapp.integration.payload.ExtendedParcelOrder;
import ru.yandex.market.delivery.mdbapp.integration.router.DbsRouter;
import ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsByTypeRouter;
import ru.yandex.market.delivery.mdbapp.integration.transformer.OrderToExtendedOrdersTransformer;
import ru.yandex.market.delivery.mdbapp.testutils.ResourceUtils;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.RouteOrderRequestDto;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.BusinessOwnerDTO;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;
import ru.yandex.market.mbi.api.client.entity.shops.ShopOrgInfo;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_STATUS_UPDATED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_SUBSTATUS_UPDATED;

@DisplayName("Настройка потока обработки dropship by seller заказов")
class DbsFlowTest extends MockContextualTest {

    private static final long PARTNER_ID = 10808728L;
    private static final Set<OptionalOrderPart> OPTIONAL_ORDER_PARTS = EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS);
    private static final long EVENT_ID = 1373958617L;
    private static final String REQUEST_ID = "1624965596595/b02868f31c3269bea54628c9e5c50500/17";
    private static final OrderSearchFilter FILTER = OrderSearchFilter.builder()
        .externalIds(Set.of("32782197"))
        .senderIds(Set.of(PARTNER_ID))
        .build();

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    private MockRestServiceServer checkouterMockServer;

    @Autowired
    @Qualifier("orderEventsPoller0")
    private OrderEventsPoller poller;

    @SpyBean
    private OrderEventsByTypeRouter orderEventsByTypeRouter;

    @SpyBean
    private DbsRouter dbsRouter;

    @SpyBean
    private DbsOrderExternalEnricher dbsOrderExternalEnricher;

    @SpyBean
    private DbsOrderEnricher dbsOrderEnricher;

    @SpyBean
    private OrderToExtendedOrdersTransformer orderToExtendedOrdersTransformer;

    @SpyBean
    private LomClient lomClient;

    @MockBean
    private LocationFetcher locationFetcher;

    @SpyBean
    private LgwDSCreateOrderEventHandler lgwDSCreateOrderEventHandler;

    @MockBean
    private DeliveryClient lgwDeliveryClient;

    @Autowired
    private LegalInfoReceiver legalInfoReceiver;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private MbiApiClient mbiApiClient;

    @SpyBean
    @Qualifier(ChangeRequestCancelParcelQueue.QUEUE_PRODUCER)
    private QueueProducer<ChangeRequestCancelParcelDto> cancelParcelProducer;

    @BeforeEach
    void setUp() {
        when(legalInfoReceiver.findAccountByPartnerIdAndPartnerType(PARTNER_ID, CampaignType.SHOP.name()))
            .thenReturn(Optional.of(MarketAccount.newBuilder().setMarketId(1).build()));
        when(mbiApiClient.getPartnerInfo(PARTNER_ID)).thenReturn(PartnerInfoSteps.getPartnerInfoDTO(PARTNER_ID));
        when(mbiApiClient.getPartnerSuperAdmin(PARTNER_ID))
            .thenReturn(new BusinessOwnerDTO(1, 1, "", Set.of()));
        when(lomClient.searchOrders(eq(FILTER), eq(OPTIONAL_ORDER_PARTS), any(Pageable.class)))
            .thenReturn(PageResult.empty(Pageable.unpaged()));
        when(lomClient.searchOrders(eq(FILTER), eq(Set.of()), any(Pageable.class), eq(false)))
            .thenReturn(PageResult.of(List.of(new OrderDto().setId(100L).setStatus(OrderStatus.PROCESSING)), 1, 1, 1));
        when(lomClient.createOrder(any(RouteOrderRequestDto.class), eq(true)))
            .thenReturn(new OrderDto().setStatus(OrderStatus.PROCESSING));

        TimeZone moscowZone = TimeZone.getTimeZone(ZoneId.of("Europe/Moscow"));
        TimeZone.setDefault(moscowZone);
        objectMapper.getDateFormat().getCalendar().setTimeZone(moscowZone);

        checkouterMockServer = MockRestServiceServer.createServer(checkouterRestTemplate);

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

        when(mbiApiClient.getShop(PARTNER_ID)).thenReturn(shop);
        when(locationFetcher.fetch(any(Order.class))).thenReturn(location);
        when(locationFetcher.fetch(any(Shop.class))).thenReturn(location);
    }

    @AfterEach
    void tearDown() {
        checkouterMockServer.reset();
        verifyNoMoreInteractions(lomClient);
    }

    @Test
    @DisplayName("Курьерка: Успешное создание dsbs заказа напрямую в lgw")
    void dbsCourierOrderCreating() throws Exception {
        mockCheckouter("data/events/dropship_by_seller_order_create.json");

        poller.poll();

        verifyExpectedDbsOrderEvent(HistoryEventType.ORDER_DELIVERY_UPDATED);
        verify(dbsOrderExternalEnricher).enrich(any(ExtendedOrder.class));
        verify(locationFetcher).fetch(any(Order.class));
        verify(locationFetcher).fetch(any(Shop.class));
        verify(locationFetcher).fetchNullable(any(Shop.class));
        verify(orderToExtendedOrdersTransformer).transform(any(ExtendedOrder.class));
        verify(dbsOrderEnricher).enrich(any(ExtendedParcelOrder.class));
        verify(mbiApiClient, times(2)).getShop(anyLong());

        ru.yandex.market.logistic.gateway.common.model.delivery.Order order = objectMapper.readValue(
            ResourceUtils.getFileContent("data/lgw/dropship_by_seller_order.json"),
            ru.yandex.market.logistic.gateway.common.model.delivery.Order.class
        );
        Partner partner = objectMapper.readValue(
            ResourceUtils.getFileContent("data/lgw/dropship_by_seller_partner.json"),
            Partner.class
        );
        CreateLgwDsOrder createLgwDsOrder = new CreateLgwDsOrder(order, partner);

        verify(lgwDSCreateOrderEventHandler).handleLgwOrderEvent(createLgwDsOrder);
        verify(lgwDeliveryClient).createOrder(createLgwDsOrder.getOrder(), createLgwDsOrder.getPartner());
    }

    @Test
    @DisplayName("Курьерка: Не обрабатываются события, в которых нет изменения службы доставки с DBS на DBS")
    void dbsOrderWillNotBeCreatedWhenDbsDeliveryServiceIdDoesNotChange() throws Exception {
        mockCheckouter("data/events/dropship_by_seller_order_create_without_delivery_service_id_change.json");
        poller.poll();
        verifyExpectedDbsOrderEvent(HistoryEventType.ORDER_DELIVERY_UPDATED);
    }

    @Test
    @DisplayName("Курьерка: Не обрабатываются события, в которых нет изменения службы доставки с не DBS на не DBS")
    void dbsOrderWillNotBeCreatedWhenNotDbsDeliveryServiceIdDoesNotChange() throws Exception {
        mockCheckouter("data/events/dropship_by_seller_order_create_without_delivery_service_id_change_non_dbs.json");
        poller.poll();
        verifyExpectedDbsOrderEvent(HistoryEventType.ORDER_DELIVERY_UPDATED);
    }

    @Test
    @DisplayName("ПВЗ: Успешное создание dbs заказа напрямую в lom")
    void dbsPickupOrderCreating() throws Exception {
        mockCheckouter("data/events/dropship_by_seller_pickup_order_create.json");

        poller.poll();

        verifyExpectedDbsOrderEvent(ORDER_STATUS_UPDATED);

        verifyPickupOrderCreation();
        checkouterMockServer.verify();
    }

    @Test
    @DisplayName("ПВЗ: Успешное создание dbs заказа через дропофф напрямую в lom")
    void dbsPickupDropoffOrderCreating() throws Exception {
        mockCheckouter("data/events/dbs_dropoff_order_create.json");

        poller.poll();

        verifyExpectedDbsOrderEvent(ORDER_STATUS_UPDATED);

        verifyDropoffCreation();
        checkouterMockServer.verify();
    }

    @Test
    @DisplayName("ПВЗ: Успешное создание dsbs заказа напрямую в lom - обновление сабстатуса")
    void dbsPickupOrderCreatingSubstatusChanged() throws Exception {
        mockCheckouter("data/events/dropship_by_seller_pickup_order_create_update_substatus.json");

        poller.poll();

        verifyExpectedDbsOrderEvent(ORDER_SUBSTATUS_UPDATED);
        verifyPickupOrderCreation();
    }

    @Test
    @DisplayName("ПВЗ через дропофф: Успешное создание dsbs заказа напрямую в lom - обновление сабстатуса")
    void dbsPickupDropoffOrderCreatingSubstatusChanged() throws Exception {
        mockCheckouter("data/events/dbs_dropoff_order_create_update_substatus.json");

        poller.poll();

        verifyExpectedDbsOrderEvent(ORDER_SUBSTATUS_UPDATED);
        verifyDropoffCreation();
    }

    @Test
    @DisplayName("ПВЗ: Успешная отмена заказа")
    public void dbsCancelPickupOrder() throws Exception {
        cancelSuccess("data/events/dropship_by_seller_pickup_order_cancel.json");
    }

    @Test
    @DisplayName("ПВЗ через дропофф: Успешная отмена заказа")
    void dbsDropoffCancelPickupOrder() throws Exception {
        cancelSuccess("data/events/dbs_dropoff_order_cancel.json");
    }

    private void cancelSuccess(String s) throws Exception {
        mockCheckouter(s);

        poller.poll();

        verifyExpectedDbsOrderEvent(HistoryEventType.ORDER_CHANGE_REQUEST_CREATED);

        ArgumentCaptor<EnqueueParams<ChangeRequestCancelParcelDto>> enqueueParamsCaptor = ArgumentCaptor.forClass(
            EnqueueParams.class
        );
        verify(cancelParcelProducer).enqueue(enqueueParamsCaptor.capture());
        var payload = (ChangeRequestCancelParcelDto) enqueueParamsCaptor.getValue().getPayload();
        softly.assertThat(payload)
            .usingRecursiveComparison()
            .isEqualTo(new ChangeRequestCancelParcelDto(32907198L, 6154258L, null, 1138877L, 9999L));
    }

    private void verifyExpectedDbsOrderEvent(HistoryEventType historyEventType) {
        ArgumentCaptor<OrderHistoryEvent> orderEventCaptor = ArgumentCaptor.forClass(OrderHistoryEvent.class);
        ArgumentCaptor<OrderHistoryEvent> orderEventCaptorDropship = ArgumentCaptor.forClass(OrderHistoryEvent.class);
        verify(orderEventsByTypeRouter).route(orderEventCaptor.capture());
        verify(dbsRouter).route(orderEventCaptorDropship.capture());

        softly.assertThat(orderEventCaptorDropship.getValue().getType()).isEqualTo(historyEventType);

        OrderHistoryEvent routedEvent = orderEventCaptor.getValue();
        softly.assertThat(routedEvent.getId()).isEqualTo(EVENT_ID);
        softly.assertThat(routedEvent.getRequestId()).isEqualTo(REQUEST_ID);
        softly.assertThat(routedEvent.getType()).isEqualTo(historyEventType);

        softly.assertThat(routedEvent.getOrderBefore()).isNotNull();
        softly.assertThat(routedEvent.getOrderAfter()).isNotNull();
        softly.assertThat(routedEvent.getOrderAfter().getRgb()).isEqualTo(Color.WHITE);
    }

    private void mockCheckouter(String responseFilePath) throws Exception {
        checkouterMockServer.expect(
            ExpectedCount.manyTimes(),
            requestTo(StringContains.containsString("/orders/events"))
        ).andRespond(
            withSuccess(
                ResourceUtils.getFileContent(responseFilePath),
                MediaType.APPLICATION_JSON_UTF8
            ));
    }

    private void verifyPickupOrderCreation() throws Exception {
        verifyCreation("/data/lom/dbs_pickup_order.json");
    }

    private void verifyDropoffCreation() throws Exception {
        verifyCreation("/data/lom/dbs_dropoff.json");
    }

    private void verifyCreation(String responsePath) throws IOException {
        verify(lomClient).searchOrders(eq(FILTER), eq(OPTIONAL_ORDER_PARTS), any(Pageable.class));
        verify(locationFetcher).fetch(any(Order.class));

        ArgumentCaptor<RouteOrderRequestDto> orderCaptor = ArgumentCaptor.forClass(RouteOrderRequestDto.class);
        verify(lomClient).createOrder(orderCaptor.capture(), eq(true));

        String objectJson = objectMapper.writeValueAsString(orderCaptor.getValue());

        assertThatJson(objectJson).when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
            .withTolerance(0.0)
            .isEqualTo(ResourceUtils.getFileContent(responsePath));
    }
}
