package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import steps.PartnerInfoSteps;
import steps.orderSteps.OrderSteps;
import steps.utils.TestableClock;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.failover.OrderEventFailoverableService;
import ru.yandex.market.delivery.mdbapp.components.service.last_mile.LastMileMappingService;
import ru.yandex.market.delivery.mdbapp.components.service.lms.PartnerExternalParamsUpdater;
import ru.yandex.market.delivery.mdbapp.components.service.marketid.LegalInfoReceiver;
import ru.yandex.market.delivery.mdbapp.components.service.yt.YtLogisticsPointService;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.LastMileMapping;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.LastMileMappingRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.OrderEventsFailoverRepository;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.testutils.LmsTestUtils;
import ru.yandex.market.delivery.mdbapp.testutils.ResourceUtils;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.AbstractOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.RouteOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.mqm.client.MqmClient;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;
import ru.yandex.market.tpl.internal.client.TplInternalClient;
import ru.yandex.market.tpl.internal.client.model.DeliveryServiceByLocationRequestDto;
import ru.yandex.market.tpl.internal.client.model.DeliveryServiceByLocationResponseDto;
import ru.yandex.market.tpl.internal.client.model.GeoCoordinates;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.testutils.MockUtils.prepareMockServer;

public class BlueFlowTest extends MockContextualTest {
    private static final Set<OptionalOrderPart> OPTIONAL_ORDER_PARTS = EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS);

    @Autowired
    private TestableClock clock;

    @Autowired
    @Qualifier("orderEventsPoller0")
    private OrderEventsPoller poller;

    @Autowired
    private OrderEventFailoverableService failoverService;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private LomClient lomClient;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private TplInternalClient tplInternalClient;

    @Autowired
    @Qualifier("commonJsonMapper")
    private ObjectMapper objectMapper;

    @Autowired
    private OrderEventsFailoverRepository orderEventsFailoverRepository;

    @Autowired
    private LastMileMappingService lastMileMappingService;

    @Autowired
    private FeatureProperties featureProperties;

    private MockRestServiceServer checkouterMockServer;

    @Autowired
    private LastMileMappingRepository lastMileMappingRepository;

    @Autowired
    private PartnerExternalParamsUpdater partnerExternalParamsUpdater;

    @Autowired
    private LegalInfoReceiver legalInfoReceiver;

    @Autowired
    private YtLogisticsPointService ytLogisticsPointService;

    @Autowired
    private MqmClient mqmClient;

    private final Collection<LastMileMapping> initialLastMileMappings = new LinkedList<>();

    @BeforeEach
    public void setUp() {
        initialLastMileMappings.addAll(lastMileMappingRepository.findAll());
        checkouterMockServer = MockRestServiceServer.createServer(checkouterRestTemplate);
        clock.setFixed(Instant.parse("2019-07-20T00:00:00Z"), ZoneOffset.UTC);
    }

    @AfterEach
    public void tearDown() {
        orderEventsFailoverRepository.deleteAll();
        lastMileMappingRepository.deleteAll();
        lastMileMappingRepository.saveAll(initialLastMileMappings);
        Mockito.when(lmsClient.getPartnerExternalParams(Mockito.anySet())).thenReturn(List.of());
        verifyNoMoreInteractions(mbiApiClient, legalInfoReceiver, lmsClient, lomClient);
        partnerExternalParamsUpdater.update();
        checkouterMockServer.reset();
    }

    @Test
    @DisplayName("Создать заказ в лавку через комбинатор используя lms")
    public void lavkaThroughCombinatorCreateOrderUsingLms() throws Exception {
        prepareDropshipAndPostamatMocks("/data/events/lavka_through_combinator_order_create.json");
        mockRouteLomFlowWithAutocommit();

        LogisticsPointFilter pointFilter = LogisticsPointFilter.newBuilder()
            .type(PointType.PICKUP_POINT)
            .partnerIds(Set.of(1005471L))
            .externalIds(Set.of("b70dae67-6fdc-45df-8895-80db0bfc6d3d"))
            .build();

        when(lmsClient.getLogisticsPoints(refEq(
            pointFilter
        ))).thenReturn(List.of(LogisticsPointResponse.newBuilder().id(10000967618L).build()));

        poller.poll();

        verify(lmsClient).getLogisticsPoints(refEq(pointFilter));
        verifyRouteOrderSent("/data/lom/lavka_through_combinator_order_create.json", false);
        verify(failoverService, never()).storeError(any(), anyString(), any());
        validateLomSearchOrder("32347271");
    }

    @Test
    @DisplayName("Создать заказ в лавку через комбинатор используя yt")
    public void lavkaThroughCombinatorCreateOrderUsingYt() throws Exception {
        prepareDropshipAndPostamatMocks("/data/events/lavka_through_combinator_order_create.json");
        mockRouteLomFlowWithAutocommit();

        doReturn(10000967618L)
            .when(ytLogisticsPointService)
            .getLogisticsPointId(1005471L, "b70dae67-6fdc-45df-8895-80db0bfc6d3d");

        poller.poll();

        verifyRouteOrderSent("/data/lom/lavka_through_combinator_order_create.json", false);
        verify(failoverService, never()).storeError(any(), anyString(), any());
        validateLomSearchOrder("32347271");
    }

    @Test
    @DisplayName("Создать pickup заказ через комбинатор")
    public void marketPickupThroughCombinatorCreateOrder() throws Exception {
        prepareDropshipAndPostamatMocks("/data/events/market_pickup_through_combinator_order_create.json");
        mockRouteLomFlowWithAutocommit();

        poller.poll();

        verifyRouteOrderSent("/data/lom/lavka_through_combinator_order_create.json", false);
        verify(failoverService, never()).storeError(any(), anyString(), any());
        validateLomSearchOrder("32347271");
    }

    @Test
    @DisplayName("Создать дропшип заказ через сц с маппингом средней и последней мили")
    public void blueDropshipSortingCenterLastMileMiddleMileMappingMarketPickupCreateOrder() throws Exception {
        prepareDropshipAndPostamatMocks("/data/events/blue_dropship_sc_market_pickup__create_order_with_route.json");
        mockRouteLomFlowWithAutocommit();

        lastMileMappingService.add(
            new LastMileMapping(LmsTestUtils.PICKUP_POINT_PARTNER_ID, LmsTestUtils.MIDDLE_MILE_DS_ID, false)
        );
        LmsTestUtils.mockGetFfToDsPostamatPartnerRelation(lmsClient, 1005471L);

        when(lmsClient.getLogisticsPoint(LmsTestUtils.PICKUP_POINT_ID))
            .thenReturn(Optional.of(
                LmsTestUtils.createPickupLogisticsPoint(
                    LmsTestUtils.PICKUP_POINT_ID, LmsTestUtils.PICKUP_POINT_PARTNER_ID, null
                )
            ));

        poller.poll();

        verifyRouteOrderSent("/data/lom/blue_dropship_sc_market_pickup__create_order_combinator.json", false);
        verify(failoverService, never()).storeError(any(), anyString(), any());
        validateLomSearchOrder("12814925");
    }

    @Test
    @DisplayName("Создать дропшип заказ через сц")
    public void blueDropshipSortingCenterCreateOrderWithRoute() throws Exception {
        prepareDropshipAndPostamatMocks("/data/events/blue_dropship_create_order_with_route.json");
        mockRouteLomFlowWithAutocommit();

        poller.poll();

        verifyRouteOrderSent("/data/lom/blue_dropship_sorting_center_create_order_with_route.json", true);
        verify(failoverService, never()).storeError(any(), anyString(), any());
        validateLomSearchOrder("12814925");
    }

    @Test
    @SneakyThrows
    @DisplayName("Обработка ошибки при обновлении доставки у заказа в некорректном статусе")
    public void dropshipCreationError() {
        doNothing().when(mqmClient).pushMonitoringEvent(any());
        prepareDropshipAndPostamatMocks("/data/events/dropship_create_order_failed_pending_status.json");
        when(lomClient.searchOrders(any(), eq(OPTIONAL_ORDER_PARTS), any()))
            .thenReturn(PageResult.empty(new Pageable(1, 1, null)));
        when(lomClient.createOrder(any(RouteOrderRequestDto.class), eq(true))).thenThrow(
            new HttpTemplateException(
                HttpStatus.BAD_REQUEST.value(),
                "Following validation errors occurred..."
            )
        );
        when(checkouterAPI.getOrder(anyLong(), eq(ClientRole.SYSTEM), any()))
            .thenReturn(OrderSteps.getOrderWithParcelBoxes(12814924L));

        when(checkouterAPI.updateOrderDelivery(anyLong(), eq(ClientRole.SYSTEM), any(), any()))
            .thenThrow(new ErrorCodeException(
                "code",
                "Logistic order entities are updatable in statuses...",
                HttpStatus.BAD_REQUEST.value()
            ));

        poller.poll();

        verify(failoverService).storeError(
            any(OrderHistoryEvent.class),
            eq("java.lang.RuntimeException: Order 12814924 wasn't successfully created in LOM: "
                + "Http request exception: status <400>, response body <Following validation errors occurred...>."),
            any()
        );
        validateLomSearchOrder("12814924");
        verify(lomClient).createOrder(any(RouteOrderRequestDto.class), eq(true));
    }

    @Test
    @DisplayName("Ошибка создания дропшип заказа через сц")
    public void blueDropshipSortingCenterCreateOrderFailedAndRetriedWithFailure() throws Exception {
        prepareDropshipAndPostamatMocks("/data/events/blue_dropship__create_order_failed.json");
        when(lomClient.searchOrders(any(), eq(OPTIONAL_ORDER_PARTS), any())).thenReturn(PageResult.of(
            List.of(new OrderDto()
                .setId(1L)
                .setExternalId("11")
                .setStatus(OrderStatus.VALIDATION_ERROR)
            ),
            1,
            1,
            1
        ));
        poller.poll();

        verify(failoverService, atLeastOnce()).storeError(any(), anyString(), any());
        validateLomSearchOrder("12814924");
    }

    @Test
    @DisplayName("Создание дропшип заказа через сц после ошибки")
    public void blueDropshipSortingCenterCreateOrderFailedBeforeButOrderActuallyCreated() throws Exception {
        prepareDropshipAndPostamatMocks("/data/events/blue_dropship__create_order_failed.json");
        when(lomClient.searchOrders(any(), eq(OPTIONAL_ORDER_PARTS), any())).thenReturn(PageResult.of(
            List.of(new OrderDto()
                .setId(1L)
                .setExternalId("11")
                .setStatus(OrderStatus.PROCESSING)
            ),
            1,
            1,
            1
        ));
        poller.poll();

        validateLomSearchOrder("12814924");
        verify(failoverService, never()).storeError(any(), anyString(), any());
    }

    @Test
    @DisplayName("Создание заказа в постамат с маппингом последней мили")
    public void bluePostamatCreateOrderResolveLastMile() throws Exception {
        prepareDropshipAndPostamatMocks("/data/events/blue_postamat_create_order_with_route.json");
        mockRouteLomFlowWithAutocommit();

        lastMileMappingService.add(new LastMileMapping(LmsTestUtils.LAST_MILE_DS_ID, null, true));
        LmsTestUtils.mockGetFfToDsPostamatPartnerRelation(lmsClient, LmsTestUtils.LAST_MILE_DS_ID);

        when(tplInternalClient.getDeliveryServiceByLocation(
            DeliveryServiceByLocationRequestDto.builder().geoCoordinates(
                GeoCoordinates.builder()
                    .latitude(new BigDecimal("55.669579391302314"))
                    .longitude(new BigDecimal("37.660359117033614"))
                    .build()
            ).build()
        ))
            .thenReturn(DeliveryServiceByLocationResponseDto.builder().deliveryServiceId(LmsTestUtils.MIDDLE_MILE_DS_ID)
                .build());

        poller.poll();

        checkouterMockServer.verify();
        verifyRouteOrderSent("/data/lom/blue_postamat_create_order_combinator.json", false);
        validateLomSearchOrder("60734220");
        verify(failoverService, never()).storeError(any(), anyString(), any());
    }

    @Test
    @DisplayName("Создание заказа в пикап с маппингом средней и последней мили")
    public void lastMileMiddleMileMappingMarketPickupCreateOrder() throws Exception {
        prepareDropshipAndPostamatMocks("/data/events/blue_market_pickup_create_order_with_route.json");
        mockRouteLomFlowWithAutocommit();

        lastMileMappingService.add(
            new LastMileMapping(LmsTestUtils.PICKUP_POINT_PARTNER_ID, LmsTestUtils.MIDDLE_MILE_DS_ID, false)
        );
        LmsTestUtils.mockGetFfToDsPostamatPartnerRelation(lmsClient, 1005471L);

        when(lmsClient.getLogisticsPoint(LmsTestUtils.PICKUP_POINT_ID))
            .thenReturn(Optional.of(
                LmsTestUtils.createPickupLogisticsPoint(
                    LmsTestUtils.PICKUP_POINT_ID, LmsTestUtils.PICKUP_POINT_PARTNER_ID, null
                )
            ));

        poller.poll();

        checkouterMockServer.verify();
        verifyRouteOrderSent("/data/lom/blue_market_pickup_create_order_combinator.json", false);
        validateLomSearchOrder("60734220");
        verify(failoverService, never()).storeError(any(), anyString(), any());
    }

    @Test
    @DisplayName("Создать FF заказ")
    public void blueFfCreateOrderWithRoute() throws Exception {
        prepareMockServer(
            checkouterMockServer,
            "/orders/events",
            "/data/events/blue_ff_create_order_with_route.json"
        );

        mockRouteLomFlowWithAutocommit();

        poller.poll();

        checkouterMockServer.verify();
        verifyZeroInteractions(lmsClient);
        verifyRouteOrderSent("/data/lom/blue_ff_create_order_with_route.json", true);
        validateLomSearchOrder("6151681");
        verify(failoverService, never()).storeError(any(), anyString(), any());
    }

    @Test
    @DisplayName("Создать FF B2B заказ")
    public void blueFfCreateB2BOrderWithRoute() throws Exception {
        prepareMockServer(
            checkouterMockServer,
            "/orders/events",
            "/data/events/blue_ff_b2b_create_order_with_route.json"
        );

        mockRouteLomFlowWithAutocommit();

        poller.poll();

        checkouterMockServer.verify();
        verifyZeroInteractions(lmsClient);
        verifyRouteOrderSent("/data/lom/blue_ff_b2b_create_order_with_route.json", true);
        validateLomSearchOrder("6151681");
        verify(failoverService, never()).storeError(any(), anyString(), any());
    }

    @Test
    @DisplayName("Создать FF заказ с тегом DELAYED_RDD_NOTIFICATION")
    public void blueFfCreateOrderWithDelayedRddTagWithRoute() throws Exception {
        prepareMockServer(
            checkouterMockServer,
            "/orders/events",
            "/data/events/blue_ff_create_order_with_route_with_delayed_rdd_tag.json"
        );

        mockRouteLomFlowWithAutocommit();

        poller.poll();

        checkouterMockServer.verify();
        verifyZeroInteractions(lmsClient);
        verifyRouteOrderSent("/data/lom/blue_ff_create_order_with_route_with_delayed_rdd_tag.json", true);
        validateLomSearchOrder("6151681");
        verify(failoverService, never()).storeError(any(), anyString(), any());
    }

    @Test
    @DisplayName("Создать FF заказ со всеми возможными тегами")
    public void blueFfCreateOrderWithAllTagsWithRoute() throws Exception {
        prepareMockServer(
            checkouterMockServer,
            "/orders/events",
            "/data/events/blue_ff_create_order_with_route_with_all_tags.json"
        );

        mockRouteLomFlowWithAutocommit();

        poller.poll();

        checkouterMockServer.verify();
        verifyZeroInteractions(lmsClient);
        verifyRouteOrderSent("/data/lom/blue_ff_create_order_with_route_with_all_tags.json", true);
        validateLomSearchOrder("6151681");
        verify(failoverService, never()).storeError(any(), anyString(), any());
    }

    @Test
    @DisplayName("Создать кроссдок заказ")
    public void blueCrossdockCreateOrderWithRoute() throws Exception {
        prepareMockServer(
            checkouterMockServer,
            "/orders/events",
            "/data/events/blue_crossdock_create_order_with_route.json"
        );

        LmsTestUtils.mockGetCrossdockPartner(lmsClient);

        mockRouteLomFlowWithAutocommit();

        poller.poll();

        checkouterMockServer.verify();
        verifyRouteOrderSent("/data/lom/blue_crossdock_create_order_with_route.json", true);
        validateLomSearchOrder("6151681");
        verify(failoverService, never()).storeError(any(), anyString(), any());
    }

    @Test
    @DisplayName("Создание дропшип заказа без сц")
    public void blueDropshipWithoutScCreateOrderWithRoute() throws Exception {
        prepareMockServer(
            checkouterMockServer,
            "/orders/events",
            "/data/events/blue_dropship_without_sc_create_order_with_route.json"
        );

        LmsTestUtils.mockGetDropshipWithoutScPartner(lmsClient);

        mockRouteLomFlowWithAutocommit();

        poller.poll();

        verifyRouteOrderSent("/data/lom/blue_dropship_without_sc_create_order_with_route.json", true);
        validateLomSearchOrder("6151681");
        verify(failoverService, never()).storeError(any(), anyString(), any());
    }

    @Test
    @DisplayName("Сокрытие названия заказа для экспресс заказа")
    public void expressDeliveryServiceHideItemNameTest() throws Exception {
        prepareDropshipAndPostamatMocks("/data/events/blue_create_order_with_express_delivery.json");
        mockRouteLomFlowWithAutocommit();

        poller.poll();

        verifyRouteOrderSent("/data/lom/blue_sorting_center_create_order_with_express_delivery.json", true);
        validateLomSearchOrder("12814925");
    }

    private void validateLomSearchOrder(String externalId) {
        verify(lomClient).searchOrders(
            eq(
                OrderSearchFilter.builder()
                    .externalIds(Set.of(externalId))
                    .senderIds(Set.of(431782L))
                    .build()
            ),
            eq(Set.of(OptionalOrderPart.CHANGE_REQUESTS)),
            any(Pageable.class)
        );
    }

    private void prepareDropshipAndPostamatMocks(String eventPath) throws Exception {
        prepareDropshipAndPostamatMocks(eventPath, PartnerInfoSteps.getPartnerInfoDTO(431782L));
    }

    private void prepareDropshipAndPostamatMocks(String eventPath, PartnerInfoDTO partnerInfoDTO) throws Exception {
        prepareMockServer(
            checkouterMockServer,
            "/orders",
            eventPath
        );

        // 47798 dropship -> 145 sc -> 1003937 ds
        // 171 returnSc
        LmsTestUtils.mockGet145ActiveLogisticsPoint(lmsClient);
        LmsTestUtils.mockGetDropshipLogisticsPoint(lmsClient);
        LmsTestUtils.mockGetDropshipToScPartnerRelation(lmsClient);
        LmsTestUtils.mockGetDropshipFfToDsPartnerRelation(lmsClient);
        LmsTestUtils.mockGetDropshipPartner(lmsClient);
        LmsTestUtils.mockGet145Partner(lmsClient);
        LmsTestUtils.mockGetPostamatLogisticPoint(lmsClient);
        LmsTestUtils.mockGetPostamatMiddleMileLogisticsPoint(lmsClient);
        LmsTestUtils.mockGetPostamatPickupLogisticsPoint(lmsClient);
    }

    private void mockDefaultLomFlowWithAutocommit() {
        when(lomClient.createOrder(any(WaybillOrderRequestDto.class), eq(true))).thenAnswer(getCreateOrderAnswer());
        when(lomClient.searchOrders(any(), eq(OPTIONAL_ORDER_PARTS), any()))
            .thenReturn(PageResult.empty(new Pageable(1, 1, null)));
    }

    private void mockRouteLomFlowWithAutocommit() {
        when(lomClient.createOrder(any(RouteOrderRequestDto.class), eq(true))).thenAnswer(getCreateOrderAnswer());
        when(lomClient.searchOrders(any(), eq(OPTIONAL_ORDER_PARTS), any()))
            .thenReturn(PageResult.empty(new Pageable(1, 1, null)));
    }

    private Answer<Object> getCreateOrderAnswer() {
        return (invocation) -> {
            OrderDto order = mapRequestToOrderDto(invocation.getArgument(0));
            // LOM перезаписывает всю dto, кроме id и status, заказ был закоммичен сразу и теперь PROCESSING
            FieldUtils.writeField(order, "id", 1L, true);
            FieldUtils.writeField(order, "status", OrderStatus.PROCESSING, true);
            return order;
        };
    }

    private void verifyOrderSent() throws Exception {
        verifyOrderSent("/data/lom/blue_dropship_sorting_center__create_order_failed.json");
    }

    private void verifyOrderSent(String orderPayload) throws Exception {
        ArgumentCaptor<WaybillOrderRequestDto> orderCaptor = ArgumentCaptor.forClass(WaybillOrderRequestDto.class);
        verify(lomClient).createOrder(orderCaptor.capture(), eq(true));

        String objectJson = objectMapper.writeValueAsString(orderCaptor.getValue());

        assertOrderJson(orderPayload, true, objectJson);
    }

    private void verifyRouteOrderSent(String orderPayload, boolean isIgnoringExtraFields) throws Exception {
        ArgumentCaptor<RouteOrderRequestDto> orderCaptor = ArgumentCaptor.forClass(RouteOrderRequestDto.class);
        verify(lomClient).createOrder(orderCaptor.capture(), eq(true));

        String objectJson = objectMapper.writeValueAsString(orderCaptor.getValue());

        assertOrderJson(orderPayload, isIgnoringExtraFields, objectJson);
    }

    private void assertOrderJson(String orderPayload, boolean isIgnoringExtraFields, String objectJson)
        throws IOException {
        assertThatJson(objectJson).when(
                Option.IGNORING_ARRAY_ORDER,
                isIgnoringExtraFields ? Option.IGNORING_EXTRA_FIELDS : Option.TREATING_NULL_AS_ABSENT
            )
            .withTolerance(0.0)
            .isEqualTo(ResourceUtils.getFileContent(orderPayload));
    }

    @Nonnull
    private OrderDto mapRequestToOrderDto(AbstractOrderRequestDto requestDto) {
        return new OrderDto()
            .setExternalId(requestDto.getExternalId())
            .setPlatformClientId(requestDto.getPlatformClientId())
            .setDeliveryType(requestDto.getDeliveryType())
            .setPickupPointId(requestDto.getPickupPointId())
            .setDeliveryInterval(requestDto.getDeliveryInterval())
            .setSenderId(requestDto.getSenderId())
            .setSenderName(requestDto.getSenderName())
            .setSenderPhone(requestDto.getSenderPhone())
            .setSenderUrl(requestDto.getSenderUrl())
            .setSenderTaxSystem(requestDto.getSenderTaxSystem())
            .setRecipient(requestDto.getRecipient())
            .setCredentials(requestDto.getCredentials())
            .setCost(requestDto.getCost())
            .setItems(requestDto.getItems())
            .setUnits(requestDto.getUnits())
            .setContacts(requestDto.getContacts())
            .setMarketIdFrom(requestDto.getMarketIdFrom())
            .setReturnSortingCenterId(requestDto.getReturnSortingCenterId())
            .setMaxAbsentItemsPricePercent(requestDto.getMaxAbsentItemsPricePercent());
    }

    @Nonnull
    private LogisticsPointFilter logisticPointsFilterByPartnerIdAndActive(Long partnerId, Boolean active) {
        return LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(partnerId))
            .type(PointType.WAREHOUSE)
            .active(active)
            .build();
    }
}
