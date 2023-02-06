package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.PartnerCategoryOuterClass;
import NMarketIndexer.Common.Common;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import yandex.market.combinator.v0.CombinatorOuterClass;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.order.ActualDeliveryUtils;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.service.combinator.CombinatorGrpcClient;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OutletResponse;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.datacamp.DatacampConfigurer;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_PARSE_OUTLETS_FROM_PUSH_API;
import static ru.yandex.market.checkout.checkouter.order.DatacampOrderItemInflater.DATACAMP_PRICE_MODIFIER;
import static ru.yandex.market.checkout.common.xml.outlets.OutletType.DEPOT;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.defaultWhiteParameters;

public class CreateFakeOrderDbsTest extends AbstractWebTestBase {

    @Autowired
    private DatacampConfigurer datacampConfigurer;

    @Autowired
    private CombinatorGrpcClient combinatorGrpcClient;

    @Autowired
    private CheckouterFeatureWriter checkouterFeatureWriter;

    @BeforeEach
    void start() {
        checkouterProperties.setAsyncActualDeliveryRequest(false);
    }

    @AfterEach
    void end() {
        datacampConfigurer.reset();
        Mockito.reset(combinatorGrpcClient);

        checkouterFeatureWriter.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, false);
    }

    @Test
    public void testDbsOrderCartFakePushApiOptionsNotPresent() {
        final long businessId = 1000L;

        Parameters parameters = getPushApiDbsOrderParameters(businessId);

        when(combinatorGrpcClient.getCourierOptions(any()))
                .thenReturn(CombinatorOuterClass.DeliveryOptionsForUser.newBuilder()
                        .addOptions(CombinatorOuterClass.DeliveryOption.newBuilder()
                                .setDeliveryType(yandex.market.combinator.common.Common.DeliveryType.COURIER)
                                .build())
                        .build());

        datacampConfigurer.mockSearchBusinessOffers(SearchBusinessOffersRequest
                        .builder()
                        .setBusinessId(businessId)
                        .setPartnerId(parameters.getShopId())
                        .addOfferIds(parameters.getItems()
                                .stream()
                                .map(OfferItem::getFeedOfferId)
                                .map(FeedOfferId::getId)
                                .collect(Collectors.toList()))
                        .setIsFull(true)
                        .setPageRequest(SeekSliceRequest.firstN(parameters.getItems().size()))
                        .build(),
                SearchBusinessOffersResult.builder()
                        .setOffers(parameters.getItems()
                                .stream()
                                .map(item -> mapToUnitedOffer(item, parameters.getShopId(), businessId, false))
                                .collect(Collectors.toList())
                        ).build()
        );

        var multiCart = orderCreateHelper.cartFake(parameters);

        assertTrue(multiCart.getCarts().get(0).getDeliveryOptions().isEmpty());
    }

    @Test
    public void testDbsOrderCartFakeDeliveryOptionsNotPresent() {
        final long businessId = 1000L;

        Parameters parameters = getPushApiDbsOrderParameters(
                businessId,
                DeliveryProvider.shopSelfDelivery()
                        .free().courier(true).vatType(VatType.VAT_10)
                        .buildResponse(DeliveryResponse::new)
        );

        datacampConfigurer.mockSearchBusinessOffers(SearchBusinessOffersRequest
                        .builder()
                        .setBusinessId(businessId)
                        .setPartnerId(parameters.getShopId())
                        .addOfferIds(parameters.getItems()
                                .stream()
                                .map(OfferItem::getFeedOfferId)
                                .map(FeedOfferId::getId)
                                .collect(Collectors.toList()))
                        .setIsFull(true)
                        .setPageRequest(SeekSliceRequest.firstN(parameters.getItems().size()))
                        .build(),
                SearchBusinessOffersResult.builder()
                        .setOffers(parameters.getItems()
                                .stream()
                                .map(item -> mapToUnitedOffer(item, parameters.getShopId(), businessId, false))
                                .collect(Collectors.toList())
                        ).build()
        );

        var multiCart = orderCreateHelper.cartFake(parameters);

        assertTrue(multiCart.getCarts().get(0).getDeliveryOptions().isEmpty());
    }

    @Test
    public void testDbsOrderCartFakeMergedOptions() {
        final long businessId = 1000L;

        Parameters parameters = getPushApiDbsOrderParameters(
                businessId,
                DeliveryProvider.shopSelfDelivery().free().courier(true)
                        .buildResponse(DeliveryResponse::new)
        );

        final LocalDate fromDate = LocalDate.now();
        final LocalDate toDate = LocalDate.now().plusDays(1);

        parameters.getOrder().getDelivery().setPrice(BigDecimal.ZERO);
        parameters.getOrder().setDeliveryOptions(List.of());
        parameters.getOrder().getDelivery().setRegionId(213L);

        when(combinatorGrpcClient.getCourierOptions(any()))
                .thenReturn(CombinatorOuterClass.DeliveryOptionsForUser.newBuilder()
                        .addOptions(CombinatorOuterClass.DeliveryOption.newBuilder()
                                .setDeliveryType(yandex.market.combinator.common.Common.DeliveryType.COURIER)
                                .setCost(0)
                                .build())
                        .build());

        when(combinatorGrpcClient.getPickupPointsGrouped(any()))
                .thenReturn(CombinatorOuterClass.PickupPointsGrouped.newBuilder()
                        .addGroups(CombinatorOuterClass.PickupPointsGrouped.Group.newBuilder()
                                .setCost(0)
                                .setServiceId(99)
                                .setIsMarketBranded(true)
                                .addPoints(CombinatorOuterClass.PointIds.newBuilder()
                                        .setLogisticPointId(11111)
                                        .setDsbsPointId("")
                                        .setRegionId(213)
                                        .build()
                                )
                                .build())
                        .addGroups(CombinatorOuterClass.PickupPointsGrouped.Group.newBuilder()
                                .setCost(0)
                                .setServiceId(99)
                                .setIsMarketBranded(false)
                                .setDateFrom(CombinatorOuterClass.Date.newBuilder()
                                        .setYear(fromDate.getYear())
                                        .setMonth(fromDate.getMonthValue())
                                        .setDay(fromDate.getDayOfMonth())
                                        .build())
                                .setDateTo(CombinatorOuterClass.Date.newBuilder()
                                        .setYear(toDate.getYear())
                                        .setMonth(toDate.getMonthValue())
                                        .setDay(toDate.getDayOfMonth())
                                        .build())
                                .addPoints(CombinatorOuterClass.PointIds.newBuilder()
                                        .setLogisticPointId(321)
                                        .setDsbsPointId("123456")
                                        .setRegionId(213)
                                        .build())
                                .build()
                        )
                        .build());

        datacampConfigurer.mockSearchBusinessOffers(SearchBusinessOffersRequest
                        .builder()
                        .setBusinessId(businessId)
                        .setPartnerId(parameters.getShopId())
                        .addOfferIds(parameters.getItems()
                                .stream()
                                .map(OfferItem::getFeedOfferId)
                                .map(FeedOfferId::getId)
                                .collect(Collectors.toList()))
                        .setIsFull(true)
                        .setPageRequest(SeekSliceRequest.firstN(parameters.getItems().size()))
                        .build(),
                SearchBusinessOffersResult.builder()
                        .setOffers(parameters.getItems()
                                .stream()
                                .map(item -> mapToUnitedOffer(item, parameters.getShopId(), businessId, false))
                                .collect(Collectors.toList())
                        ).build()
        );

        var multiCart = orderCreateHelper.cartFake(parameters);

        assertEquals(1, multiCart.getCarts().get(0).getDeliveryOptions().size());
    }

    @Test
    public void testDbsDigitalOrder() {
        final long businessId = 1000L;

        Parameters parameters = getPushApiDbsOrderParameters(
                businessId,
                DeliveryProvider.shopSelfDelivery().free().type(DeliveryType.DIGITAL)
                        .buildResponse(DeliveryResponse::new)
        );

        final LocalDate fromDate = LocalDate.now();
        final LocalDate toDate = LocalDate.now().plusDays(1);

        parameters.getOrder().getDelivery().setPrice(BigDecimal.ZERO);
        parameters.getOrder().setDeliveryOptions(List.of());
        parameters.getOrder().getDelivery().setRegionId(213L);

        when(combinatorGrpcClient.getCourierOptions(any()))
                .thenReturn(CombinatorOuterClass.DeliveryOptionsForUser.newBuilder()
                        .addOptions(CombinatorOuterClass.DeliveryOption.newBuilder()
                                .setDeliveryType(yandex.market.combinator.common.Common.DeliveryType.COURIER)
                                .setCost(0)
                                .build())
                        .build());

        datacampConfigurer.mockSearchBusinessOffers(SearchBusinessOffersRequest
                        .builder()
                        .setBusinessId(businessId)
                        .setPartnerId(parameters.getShopId())
                        .addOfferIds(parameters.getItems()
                                .stream()
                                .map(OfferItem::getFeedOfferId)
                                .map(FeedOfferId::getId)
                                .collect(Collectors.toList()))
                        .setIsFull(true)
                        .setPageRequest(SeekSliceRequest.firstN(parameters.getItems().size()))
                        .build(),
                SearchBusinessOffersResult.builder()
                        .setOffers(parameters.getItems()
                                .stream()
                                .map(item -> mapToUnitedOffer(item, parameters.getShopId(), businessId, true))
                                .collect(Collectors.toList())
                        ).build()
        );

        var multiCart = orderCreateHelper.cartFake(parameters);

        assertEquals(1, multiCart.getCarts().get(0).getDeliveryOptions().size());
    }

    @Test
    public void testCartNoOptionsPresent() {
        final long businessId = 1000L;

        Parameters parameters = getPushApiDbsOrderParameters(businessId);

        datacampConfigurer.mockSearchBusinessOffers(SearchBusinessOffersRequest
                        .builder()
                        .setBusinessId(businessId)
                        .setPartnerId(parameters.getShopId())
                        .addOfferIds(parameters.getItems()
                                .stream()
                                .map(OfferItem::getFeedOfferId)
                                .map(FeedOfferId::getId)
                                .collect(Collectors.toList()))
                        .setIsFull(true)
                        .setPageRequest(SeekSliceRequest.firstN(parameters.getItems().size()))
                        .build(),
                SearchBusinessOffersResult.builder()
                        .setOffers(parameters.getItems()
                                .stream()
                                .map(item -> mapToUnitedOffer(item, parameters.getShopId(), businessId, false))
                                .collect(Collectors.toList())
                        ).build()
        );

        var multiCart = orderCreateHelper.cartFake(parameters);

        assertTrue(multiCart.getCarts().get(0).getDeliveryOptions().isEmpty());
    }

    @Test
    public void testDbsOrderCheckoutFake() {
        final long businessId = 1000L;
        final LocalDate fromDate = LocalDate.now().plusDays(1);
        final LocalDate toDate = LocalDate.now().plusDays(3);

        Parameters parameters = getPushApiDbsOrderParameters(
                businessId,
                DeliveryProvider.shopSelfDelivery()
                        .free().courier(true).vatType(VatType.VAT_10)
                        .buildResponse(DeliveryResponse::new)
        );

        Delivery delivery = parameters.getOrder().getDelivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setDeliveryDates(DeliveryProvider.getDeliveryDates(fromDate, toDate));
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        delivery.setPaymentOptions(Set.of(PaymentMethod.CARD_ON_DELIVERY));
        delivery.setServiceName(ActualDeliveryUtils.mapDeliveryTypeToName(DeliveryType.DELIVERY));
        delivery.setDeliveryServiceId(OrderCreateHelper.FAKE_DELIVERY_SERVICE_ID);
        delivery.setRegionId(213L);

        when(combinatorGrpcClient.getCourierOptions(any()))
                .thenReturn(CombinatorOuterClass.DeliveryOptionsForUser.newBuilder()
                        .addOptions(CombinatorOuterClass.DeliveryOption.newBuilder()
                                .setDeliveryType(yandex.market.combinator.common.Common.DeliveryType.COURIER)
                                .addPaymentMethods(yandex.market.combinator.common.Common.PaymentMethod.PREPAYMENT)
                                .build())
                        .build());

        datacampConfigurer.mockSearchBusinessOffers(SearchBusinessOffersRequest
                        .builder()
                        .setBusinessId(businessId)
                        .setPartnerId(parameters.getShopId())
                        .addOfferIds(parameters.getItems()
                                .stream()
                                .map(OfferItem::getFeedOfferId)
                                .map(FeedOfferId::getId)
                                .collect(Collectors.toList()))
                        .setIsFull(true)
                        .setPageRequest(SeekSliceRequest.firstN(parameters.getItems().size()))
                        .build(),
                SearchBusinessOffersResult.builder()
                        .setOffers(parameters.getItems()
                                .stream()
                                .map(item -> mapToUnitedOffer(item, parameters.getShopId(), businessId, false))
                                .collect(Collectors.toList())
                        ).build()
        );

        Order order = orderCreateHelper.createOrderFake(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(Color.WHITE, order.getRgb());
        assertEquals(order.getDelivery().getDeliveryPartnerType(), DeliveryPartnerType.SHOP);
    }

    @Test
    public void testDbsOrderCheckoutFakeViaOutlet() {
        checkouterFeatureWriter.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, true);

        final long businessId = 1000L;
        final LocalDate fromDate = LocalDate.now().plusDays(1);
        final LocalDate toDate = LocalDate.now().plusDays(3);

        DeliveryResponse pushApiDeliveryOption = DeliveryProvider.shopSelfDelivery()
                .free().pickup(false).regionId(213L).id("123").outletId(123L)
                .buildResponse(DeliveryResponse::new);

        pushApiDeliveryOption.setOutlets(
                List.of(new ShopOutlet() {{
                    setId(123L);
                }}
        ));

        pushApiDeliveryOption.setOutletResponses(
                List.of(new OutletResponse(123456L, "123", 99L, DEPOT))
        );

        Parameters parameters = getPushApiDbsOrderParameters(businessId, pushApiDeliveryOption);

        Delivery delivery = parameters.getOrder().getDelivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setDeliveryDates(DeliveryProvider.getDeliveryDates(fromDate, toDate));
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        delivery.setPaymentOptions(Set.of(PaymentMethod.CARD_ON_DELIVERY));
        delivery.setServiceName(ActualDeliveryUtils.mapDeliveryTypeToName(DeliveryType.DELIVERY));
        delivery.setDeliveryServiceId(OrderCreateHelper.FAKE_DELIVERY_SERVICE_ID);
        delivery.setVat(VatType.VAT_10);
        delivery.setRegionId(213L);

        when(combinatorGrpcClient.getCourierOptions(any())).thenThrow(new RuntimeException("Some exception"));

        when(combinatorGrpcClient.getPickupPointsGrouped(any()))
                .thenReturn(CombinatorOuterClass.PickupPointsGrouped.newBuilder()
                        .addGroups(CombinatorOuterClass.PickupPointsGrouped.Group.newBuilder()
                                .setCost(0)
                                .setServiceId(99)
                                .setIsMarketBranded(false)
                                .setDateFrom(CombinatorOuterClass.Date.newBuilder()
                                        .setYear(fromDate.getYear())
                                        .setMonth(fromDate.getMonthValue())
                                        .setDay(fromDate.getDayOfMonth())
                                        .build())
                                .setDateTo(CombinatorOuterClass.Date.newBuilder()
                                        .setYear(toDate.getYear())
                                        .setMonth(toDate.getMonthValue())
                                        .setDay(toDate.getDayOfMonth())
                                        .build())
                                .addPoints(CombinatorOuterClass.PointIds.newBuilder()
                                        .setLogisticPointId(321)
                                        .setDsbsPointId("123456")
                                        .setRegionId(213)
                                        .build())
                                .build())
                        .addGroups(CombinatorOuterClass.PickupPointsGrouped.Group.newBuilder()
                                .setCost(0)
                                .setServiceId(99)
                                .setIsMarketBranded(false)
                                .setDateFrom(CombinatorOuterClass.Date.newBuilder()
                                        .setYear(fromDate.getYear())
                                        .setMonth(fromDate.getMonthValue())
                                        .setDay(fromDate.getDayOfMonth())
                                        .build())
                                .setDateTo(CombinatorOuterClass.Date.newBuilder()
                                        .setYear(toDate.getYear())
                                        .setMonth(toDate.getMonthValue())
                                        .setDay(toDate.getDayOfMonth())
                                        .build())
                                .addPoints(CombinatorOuterClass.PointIds.newBuilder()
                                        .setLogisticPointId(654)
                                        .setDsbsPointId("")
                                        .setRegionId(213)
                                        .build())
                                .build())
                        .build());

        datacampConfigurer.mockSearchBusinessOffers(SearchBusinessOffersRequest
                        .builder()
                        .setBusinessId(businessId)
                        .setPartnerId(parameters.getShopId())
                        .addOfferIds(parameters.getItems()
                                .stream()
                                .map(OfferItem::getFeedOfferId)
                                .map(FeedOfferId::getId)
                                .collect(Collectors.toList()))
                        .setIsFull(true)
                        .setPageRequest(SeekSliceRequest.firstN(parameters.getItems().size()))
                        .build(),
                SearchBusinessOffersResult.builder()
                        .setOffers(parameters.getItems()
                                .stream()
                                .map(item -> mapToUnitedOffer(item, parameters.getShopId(), businessId, false))
                                .collect(Collectors.toList())
                        ).build()
        );

        var multiCart = orderCreateHelper.cartFake(parameters);

        assertEquals(1, multiCart.getCarts().get(0).getDeliveryOptions().size());

        Delivery option = multiCart.getCarts().get(0).getDeliveryOptions().get(0);
        assertNotNull(option.getOutlets());
        assertEquals(1, option.getOutlets().size());
        assertEquals(123456L, option.getOutlets().get(0).getId());
        assertEquals(213L, option.getOutlets().get(0).getRegionId());
    }

    @Test
    public void testDbsCourierOrderCheckoutFakeViaPI() {
        checkouterFeatureWriter.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, true);

        final long businessId = 1000L;
        final LocalDate fromDate = LocalDate.now().plusDays(1);
        final LocalDate toDate = LocalDate.now().plusDays(3);

        Parameters parameters = getPIDbsOrderParameters(businessId);

        Delivery delivery = parameters.getOrder().getDelivery();
        delivery.setRegionId(213L);

        when(combinatorGrpcClient.getCourierOptions(any()))
                .thenReturn(CombinatorOuterClass.DeliveryOptionsForUser.newBuilder()
                        .addOptions(CombinatorOuterClass.DeliveryOption.newBuilder()
                                .setDeliveryType(yandex.market.combinator.common.Common.DeliveryType.COURIER)
                                .setCost(0)
                                .setDateFrom(CombinatorOuterClass.Date.newBuilder()
                                        .setYear(fromDate.getYear())
                                        .setMonth(fromDate.getMonthValue())
                                        .setDay(fromDate.getDayOfMonth())
                                        .build())
                                .setDateTo(CombinatorOuterClass.Date.newBuilder()
                                        .setYear(toDate.getYear())
                                        .setMonth(toDate.getMonthValue())
                                        .setDay(toDate.getDayOfMonth())
                                        .build())
                                .addPaymentMethods(yandex.market.combinator.common.Common.PaymentMethod.CARD)
                                .build())
                        .build());

        datacampConfigurer.mockSearchBusinessOffers(SearchBusinessOffersRequest
                        .builder()
                        .setBusinessId(businessId)
                        .setPartnerId(parameters.getShopId())
                        .addOfferIds(parameters.getItems()
                                .stream()
                                .map(OfferItem::getFeedOfferId)
                                .map(FeedOfferId::getId)
                                .collect(Collectors.toList()))
                        .setIsFull(true)
                        .setPageRequest(SeekSliceRequest.firstN(parameters.getItems().size()))
                        .build(),
                SearchBusinessOffersResult.builder()
                        .setOffers(parameters.getItems()
                                .stream()
                                .map(item -> mapToUnitedOffer(item, parameters.getShopId(), businessId, false))
                                .collect(Collectors.toList())
                        ).build()
        );

        var multiCart = orderCreateHelper.cartFake(parameters);

        assertEquals(1, multiCart.getCarts().get(0).getDeliveryOptions().size());

        Delivery option = multiCart.getCarts().get(0).getDeliveryOptions().get(0);
        assertEquals(DeliveryType.DELIVERY, option.getType());
        assertTrue(option.getPaymentOptions().size() > 0);
    }

    @Test
    public void testDbsOrderCheckoutFakeViaPI() {
        checkouterFeatureWriter.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, true);

        final long businessId = 1000L;
        final LocalDate fromDate = LocalDate.now().plusDays(1);
        final LocalDate toDate = LocalDate.now().plusDays(3);

        Parameters parameters = getPIDbsOrderParameters(businessId);

        Delivery delivery = parameters.getOrder().getDelivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setDeliveryDates(DeliveryProvider.getDeliveryDates(fromDate, toDate));
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        delivery.setPaymentOptions(Set.of(PaymentMethod.CARD_ON_DELIVERY));
        delivery.setServiceName(ActualDeliveryUtils.mapDeliveryTypeToName(DeliveryType.DELIVERY));
        delivery.setDeliveryServiceId(OrderCreateHelper.FAKE_DELIVERY_SERVICE_ID);
        delivery.setRegionId(213L);

        parameters.getOrder().setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);

        when(combinatorGrpcClient.getPickupPointsGrouped(any()))
                .thenReturn(CombinatorOuterClass.PickupPointsGrouped.newBuilder()
                        .addGroups(CombinatorOuterClass.PickupPointsGrouped.Group.newBuilder()
                                .setCost(49)
                                .setServiceId(99)
                                .setIsMarketBranded(false)
                                .setDateFrom(CombinatorOuterClass.Date.newBuilder()
                                        .setYear(fromDate.getYear())
                                        .setMonth(fromDate.getMonthValue())
                                        .setDay(fromDate.getDayOfMonth())
                                        .build())
                                .setDateTo(CombinatorOuterClass.Date.newBuilder()
                                        .setYear(toDate.getYear())
                                        .setMonth(toDate.getMonthValue())
                                        .setDay(toDate.getDayOfMonth())
                                        .build())
                                .addPoints(CombinatorOuterClass.PointIds.newBuilder()
                                        .setLogisticPointId(567L)
                                        .setDsbsPointId("456")
                                        .setRegionId(213)
                                        .build())
                                .addPaymentMethods(yandex.market.combinator.common.Common.PaymentMethod.CARD)
                                .build())
                        .build());

        datacampConfigurer.mockSearchBusinessOffers(SearchBusinessOffersRequest
                        .builder()
                        .setBusinessId(businessId)
                        .setPartnerId(parameters.getShopId())
                        .addOfferIds(parameters.getItems()
                                .stream()
                                .map(OfferItem::getFeedOfferId)
                                .map(FeedOfferId::getId)
                                .collect(Collectors.toList()))
                        .setIsFull(true)
                        .setPageRequest(SeekSliceRequest.firstN(parameters.getItems().size()))
                        .build(),
                SearchBusinessOffersResult.builder()
                        .setOffers(parameters.getItems()
                                .stream()
                                .map(item -> mapToUnitedOffer(item, parameters.getShopId(), businessId, false))
                                .collect(Collectors.toList())
                        ).build()
        );

        var multiCart = orderCreateHelper.cartFake(parameters);

        Delivery option = multiCart.getCarts().get(0).getDeliveryOptions().get(0);
        assertNotNull(option.getOutlets());
        assertEquals(1, option.getOutlets().size());
        assertEquals(456L, option.getOutlets().get(0).getId());
        assertEquals(213L, option.getOutlets().get(0).getRegionId());
    }

    private Parameters getPushApiDbsOrderParameters(long businessId, DeliveryResponse... deliveryResponse) {
        var params = defaultWhiteParameters();

        params.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        params.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        params.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder()
                .build());

        params.getOrder().setBusinessId(businessId);
        params.getOrder().getItems().forEach(item -> {
            item.setShowInfo(null);
            item.setCartShowInfo(null);
            item.setFee(BigDecimal.valueOf(0.2));
            item.setShowUid(OrderCreateHelper.FAKE_SHOW_UID);
            item.setSupplierId(params.getShopId());
            item.setWarehouseId(145);
        });

        params.setContext(Context.SELF_CHECK);
        params.setCheckCartErrors(false);
        params.cartResultActions().andDo(log()).andExpect(jsonPath("$.validationErrors").doesNotExist());

        params.setPushApiDeliveryResponse(deliveryResponse);

        return params;
    }

    private Parameters getPIDbsOrderParameters(long businessId) {
        var params = defaultWhiteParameters();

        params.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        params.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        params.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder().build());

        params.getOrder().setBusinessId(businessId);
        params.getOrder().getItems().forEach(item -> {
            item.setShowInfo(null);
            item.setCartShowInfo(null);
            item.setFee(BigDecimal.valueOf(0.2));
            item.setShowUid(OrderCreateHelper.FAKE_SHOW_UID);
            item.setSupplierId(params.getShopId());
            item.setWarehouseId(145);
        });

        params.setContext(Context.SELF_CHECK);
        params.setCheckCartErrors(false);
        params.cartResultActions().andDo(log()).andExpect(jsonPath("$.validationErrors").doesNotExist());

        params.setShopAdmin(true);

        return params;
    }

    private static DataCampUnitedOffer.UnitedOffer mapToUnitedOffer(
            OrderItem orderItem,
            long partnerId,
            long businessId,
            boolean digital
    ) {
        return DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(DataCampOffer.Offer.newBuilder()
                        .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setBusinessId((int) businessId)
                                .setOfferId(orderItem.getOfferId())
                                .setExtra(DataCampOfferIdentifiers.OfferExtraIdentifiers.newBuilder()
                                        .setWareMd5(orderItem.getWareMd5())
                                        .build())
                                .build()
                        )
                        .setContent(DataCampOfferContent.OfferContent.newBuilder()
                                .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                        .setCategoryId(orderItem.getCategoryId())
                                        .build())
                                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                        .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                                                .setDownloadable(
                                                        DataCampOfferMeta.Flag.newBuilder().setFlag(digital).build()
                                                ).setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                                                        .setId(Long.parseLong(orderItem.getFeedCategoryId()))
                                                        .build())
                                                .build())
                                        .build())
                                .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                .setMarketSkuName(orderItem.getOfferName())
                                                .build())
                                        .build())
                                .build()
                        )
                        .build())
                .putService(
                        (int) partnerId,
                        DataCampOffer.Offer.newBuilder()
                                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                        .setBusinessId((int) businessId)
                                        .setOfferId(orderItem.getOfferId())
                                        .setShopId(orderItem.getSupplierId().intValue())
                                        .build()
                                )
                                .setMeta(DataCampOfferMeta.OfferMeta.newBuilder()
                                        .setRgb(DataCampOfferMeta.MarketColor.WHITE)
                                        .build()
                                )
                                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder()
                                        .setOriginalPriceFields(DataCampOfferPrice.OriginalPriceFields.newBuilder()
                                                .setVat(DataCampOfferPrice.VatValue.newBuilder()
                                                        .setValue(DataCampOfferPrice.Vat.valueOf(
                                                                orderItem.getVat().name())
                                                        ).build())
                                                .getDefaultInstanceForType())
                                        .setBasic(DataCampOfferPrice.PriceBundle.newBuilder()
                                                .setBinaryPrice(Common.PriceExpression.newBuilder()
                                                        .setPrice(
                                                                orderItem.getPrice()
                                                                        .multiply(DATACAMP_PRICE_MODIFIER)
                                                                        .longValue()
                                                        ).build())
                                                .build())
                                        .build())
                                .build())
                .putActual(
                        (int) partnerId,
                        DataCampUnitedOffer.ActualOffers.newBuilder()
                                .putWarehouse(
                                        orderItem.getWarehouseId(),
                                        DataCampOffer.Offer.newBuilder()
                                                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                                        .setFeedId(orderItem.getFeedOfferId().getFeedId().intValue())
                                                        .build()
                                                ).build()
                                )
                                .build())
                .build();
    }
}
