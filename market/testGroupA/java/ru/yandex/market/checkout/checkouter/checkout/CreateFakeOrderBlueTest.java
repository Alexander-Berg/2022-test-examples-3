package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.ActualDeliveryUtils;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentRecord;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.datacamp.DatacampConfigurer;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.checkouter.order.DatacampOrderItemInflater.DATACAMP_PRICE_MODIFIER;

public class CreateFakeOrderBlueTest extends AbstractWebTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(CreateFakeOrderBlueTest.class);

    @Autowired
    private DatacampConfigurer datacampConfigurer;

    @BeforeEach
    void start() {
        checkouterProperties.setAsyncActualDeliveryRequest(false);
    }

    @AfterEach
    void end() {
        datacampConfigurer.reset();
    }

    @Test
    public void shouldAllowToCreateBlueNotFullfilmentOrderFake() {
        final long businessId = 1000L;
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        parameters.getOrder().setBusinessId(businessId);
        parameters.getOrder().getItems().forEach(item -> {
            item.setShowInfo(null);
            item.setCartShowInfo(null);
            item.setFee(BigDecimal.valueOf(0.2));
            item.setShowUid(OrderCreateHelper.FAKE_SHOW_UID);
        });
        DeliveryDates dates = DeliveryProvider.getDeliveryDates(LocalDate.now(), LocalDate.now().plusDays(2));
        Delivery delivery = parameters.getOrder().getDelivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setDeliveryDates(dates);
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        delivery.setPaymentOptions(Set.of(PaymentMethod.CARD_ON_DELIVERY));
        delivery.setServiceName(ActualDeliveryUtils.mapDeliveryTypeToName(DeliveryType.DELIVERY));
        delivery.setDeliveryServiceId(OrderCreateHelper.FAKE_DELIVERY_SERVICE_ID);
        parameters.configuration().checkout().orderOption(parameters.getOrder().getLabel())
                .setDeliveryServiceId(OrderCreateHelper.FAKE_DELIVERY_SERVICE_ID);

        String label = parameters.getOrder().getLabel();
        parameters.configuration().checkout().orderOption(label).setDeliveryType(DeliveryType.DELIVERY);
        parameters.configuration().checkout().orderOption(label)
                .setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);

        Delivery deliveryOption = new Delivery() {{
            setType(DeliveryType.DELIVERY);
            setPrice(BigDecimal.ZERO);
            setBuyerPrice(BigDecimal.ZERO);
            setDeliveryDates(dates);
            setPaymentOptions(new HashSet<>(Arrays.asList(PaymentMethod.CARD_ON_DELIVERY,
                    PaymentMethod.CASH_ON_DELIVERY)));
            setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
            setPaymentRecords(Set.of(new PaymentRecord(PaymentMethod.CARD_ON_DELIVERY),
                    new PaymentRecord(PaymentMethod.CASH_ON_DELIVERY)));
            setServiceName(ActualDeliveryUtils.mapDeliveryTypeToName(DeliveryType.DELIVERY));
            setDeliveryServiceId(OrderCreateHelper.FAKE_DELIVERY_SERVICE_ID);
        }};
        parameters.getOrder().setDeliveryOptions(List.of(deliveryOption));
        parameters.configuration()
                .cart()
                .mocks(parameters.getOrder().getLabel())
                .setPushApiDeliveryResponses(
                        List.of(DeliveryProvider.yandexDelivery()
                                .free()
                                .buildResponse(DeliveryResponse::new))
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
                                .map(item -> mapToUnitedOffer(item, parameters.getShopId(), businessId))
                                .collect(Collectors.toList())
                        ).build()
        );

        parameters.setContext(Context.SELF_CHECK);
        parameters.setCheckCartErrors(false);
        parameters.cartResultActions().andDo(log()).andExpect(jsonPath("$.validationErrors").doesNotExist());

        var multiCart = orderCreateHelper.cartFake(parameters);
        Order order = orderCreateHelper.createOrderFake(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(Color.BLUE, order.getRgb());
        assertEquals(Boolean.FALSE, order.isFulfilment());
        assertFalse(multiCart.hasFailures());
    }

    @Test
    public void shouldSetBlueFieldsForBlueNotFullfilmentFakeOrder() {
        final long businessId = 1000L;
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        parameters.getOrder().setBusinessId(businessId);
        parameters.getOrder().getItems().forEach(item -> {
            item.setShowInfo(null);
            item.setCartShowInfo(null);
            item.setFee(BigDecimal.valueOf(0.2));
            item.setShowUid(OrderCreateHelper.FAKE_SHOW_UID);
        });
        DeliveryDates dates = DeliveryProvider.getDeliveryDates(LocalDate.now(), LocalDate.now().plusDays(2));
        Delivery delivery = parameters.getOrder().getDelivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setDeliveryDates(dates);
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        delivery.setPaymentOptions(Set.of(PaymentMethod.CARD_ON_DELIVERY));
        delivery.setServiceName(ActualDeliveryUtils.mapDeliveryTypeToName(DeliveryType.DELIVERY));
        delivery.setDeliveryServiceId(OrderCreateHelper.FAKE_DELIVERY_SERVICE_ID);
        parameters.configuration().checkout().orderOption(parameters.getOrder().getLabel())
                .setDeliveryServiceId(OrderCreateHelper.FAKE_DELIVERY_SERVICE_ID);

        String label = parameters.getOrder().getLabel();
        parameters.configuration().checkout().orderOption(label).setDeliveryType(DeliveryType.DELIVERY);
        parameters.configuration().checkout().orderOption(label)
                .setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);

        Delivery deliveryOption = new Delivery() {{
            setType(DeliveryType.DELIVERY);
            setPrice(BigDecimal.ZERO);
            setBuyerPrice(BigDecimal.ZERO);
            setDeliveryDates(dates);
            setPaymentOptions(new HashSet<>(Arrays.asList(PaymentMethod.CARD_ON_DELIVERY,
                    PaymentMethod.CASH_ON_DELIVERY)));
            setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
            setPaymentRecords(Set.of(new PaymentRecord(PaymentMethod.CARD_ON_DELIVERY),
                    new PaymentRecord(PaymentMethod.CASH_ON_DELIVERY)));
            setServiceName(ActualDeliveryUtils.mapDeliveryTypeToName(DeliveryType.DELIVERY));
            setDeliveryServiceId(OrderCreateHelper.FAKE_DELIVERY_SERVICE_ID);
        }};
        parameters.getOrder().setDeliveryOptions(List.of(deliveryOption));
        parameters.configuration()
                .cart()
                .mocks(parameters.getOrder().getLabel())
                .setPushApiDeliveryResponses(
                        List.of(DeliveryProvider.yandexDelivery()
                                .free()
                                .buildResponse(DeliveryResponse::new))
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
                                .map(item -> mapToUnitedOffer(item, parameters.getShopId(), businessId))
                                .collect(Collectors.toList())
                        ).build()
        );
        parameters.setContext(Context.SELF_CHECK);
        parameters.setCheckCartErrors(false);
        parameters.cartResultActions().andDo(log()).andExpect(jsonPath("$.validationErrors").doesNotExist());

        var multiCart = orderCreateHelper.cartFake(parameters);
        Order order = orderCreateHelper.createOrderFake(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(Color.BLUE, order.getRgb());
        assertEquals(Boolean.FALSE, order.isFulfilment());

        order.getItems().forEach(
                item -> {
                    Assertions.assertNotNull(item.getSku());
                    Assertions.assertNotNull(item.getShopSku());
                    Assertions.assertNotNull(item.getMsku());
                    Assertions.assertNotNull(item.getSupplierId());
                }
        );
        assertFalse(multiCart.hasFailures());
    }

    private static DataCampUnitedOffer.UnitedOffer mapToUnitedOffer(
            OrderItem orderItem,
            long partnerId,
            long businessId
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
                                        .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                                                .setName(DataCampOfferMeta.StringValue.newBuilder()
                                                        .setValue(orderItem.getOfferName())
                                                        .build())

                                                .build())
                                        .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                                                .setCategory(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
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
                                        .setRgb(DataCampOfferMeta.MarketColor.BLUE)
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
