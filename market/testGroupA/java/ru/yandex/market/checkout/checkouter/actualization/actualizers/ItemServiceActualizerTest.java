package ru.yandex.market.checkout.checkouter.actualization.actualizers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.checkouter.yauslugi.model.YaServicePartnerDescriptionDto;
import ru.yandex.market.checkout.checkouter.yauslugi.model.YaServiceTimeSlotDto;
import ru.yandex.market.checkout.checkouter.yauslugi.model.YaServiceTimeSlotsResponse;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.ItemServiceProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.report.OfferServiceBuilder;
import ru.yandex.market.checkout.util.yauslugi.YaUslugiServiceTestConfigurer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.common.util.collections.CollectionUtils.emptyIfNull;
import static ru.yandex.common.util.collections.CollectionUtils.firstOrNull;

public class ItemServiceActualizerTest extends AbstractWebTestBase {

    @Autowired
    private CheckouterProperties checkouterProperties;
    @Autowired
    private OrderCreateHelper orderCreateHelper;
    @Autowired
    protected OrderService orderService;
    @Autowired
    private YaUslugiServiceTestConfigurer yaUslugiServiceTestConfigurer;
    @Autowired
    private CheckouterFeatureWriter featureWriter;

    @BeforeEach
    public void setup() {
        checkouterProperties.setEnableServicesPrepay(false);
        checkouterProperties.setEnableItemServiceTimeslots(true);
    }

    @Test
    @DisplayName("Установка в день доставки (без таймслотов)")
    public void testInstallationRightAfterDelivery() {
        checkouterProperties.setEnableItemServiceTimeslots(false);

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        var itemService = ItemServiceProvider.defaultItemService();
        itemService.setDate(null);
        itemService.setFromTime((LocalTime) null);
        itemService.setToTime((LocalTime) null);
        itemService.setCount(2);
        var orderItem = parameters.getOrder().getItems().iterator().next();
        orderItem.setServices(Collections.singleton(itemService));
        orderItem.setCount(1);
        parameters.getReportParameters().setOffers(List.of(
                FoundOfferBuilder.createFrom(orderItem)
                        .configure(WhiteParametersProvider.whiteOffer())
                        .services(List.of(OfferServiceBuilder.createFrom(itemService).build()))
                        .build()
        ));
        var createdMultiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            var cart = multiCart.getCarts().iterator().next();
            var is = cart.getItems().iterator().next().getServices().iterator().next();
            is.setDate(null);
            is.setFromTime((LocalTime) null);
            is.setToTime((LocalTime) null);
            is.setCount(2);
        });
        Long orderId = firstOrNull(emptyIfNull(createdMultiOrder.getOrders())).getId();
        Order actualOrder = orderService.getOrder(orderId, ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES));

        assertNotNull(actualOrder);
        var actualOrderItem = actualOrder.getItems().iterator().next();
        assertNotNull(actualOrderItem);
        var actualItemService = actualOrderItem.getServices().iterator().next();
        assertNotNull(actualItemService);
        // предоплата выключена
        assertEquals(PaymentType.POSTPAID, actualItemService.getPaymentType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, actualItemService.getPaymentMethod());
        // количество услуг соответствует количеству товаров
        assertEquals(orderItem.getCount(), actualItemService.getCount());

        assertEquals(actualOrder.getDelivery().getDeliveryDates().getToDate(),
                actualItemService.getDate());
        assertEquals(LocalTime.of(14, 0), actualItemService.getFromTime());
        assertEquals(LocalTime.of(20, 0), actualItemService.getToTime());
        assertEquals(checkouterProperties.getItemServiceDefaultPartnerInfo().getInn(), actualItemService.getInn());
        assertEquals(checkouterProperties.getItemServiceDefaultPartnerInfo().getVatType(),
                actualItemService.getVat().getTrustId());
        // яуслуги не вызывались
        yaUslugiServiceTestConfigurer.verifyZeroInteractions();
    }

    @Test
    @DisplayName("Установка в день доставки (без таймслотов, предоплата включена)")
    public void testInstallationRightAfterDeliveryServicePrepay() {
        checkouterProperties.setEnableItemServiceTimeslots(false);
        checkouterProperties.setEnableServicesPrepay(true);

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        var itemService = ItemServiceProvider.defaultItemService();
        itemService.setDate(null);
        itemService.setFromTime((LocalTime) null);
        itemService.setToTime((LocalTime) null);
        itemService.setCount(2);
        var orderItem = parameters.getOrder().getItems().iterator().next();
        orderItem.setServices(Collections.singleton(itemService));
        orderItem.setCount(1);
        parameters.getReportParameters().setOffers(List.of(
                FoundOfferBuilder.createFrom(orderItem)
                        .configure(WhiteParametersProvider.whiteOffer())
                        .services(List.of(OfferServiceBuilder.createFrom(itemService).build()))
                        .build()
        ));
        var createdMultiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            var cart = multiCart.getCarts().iterator().next();
            var is = cart.getItems().iterator().next().getServices().iterator().next();
            is.setDate(null);
            is.setFromTime((LocalTime) null);
            is.setToTime((LocalTime) null);
            is.setCount(2);
        });
        Long orderId = firstOrNull(emptyIfNull(createdMultiOrder.getOrders())).getId();
        Order actualOrder = orderService.getOrder(orderId, ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES));

        assertNotNull(actualOrder);
        var actualOrderItem = actualOrder.getItems().iterator().next();
        assertNotNull(actualOrderItem);
        var actualItemService = actualOrderItem.getServices().iterator().next();
        assertNotNull(actualItemService);
        // предоплата включена
        assertEquals(actualOrder.getPaymentType(), actualItemService.getPaymentType());
        assertEquals(actualOrder.getPaymentMethod(), actualItemService.getPaymentMethod());
        // количество услуг соответствует количеству товаров
        assertEquals(orderItem.getCount(), actualItemService.getCount());

        assertEquals(actualOrder.getDelivery().getDeliveryDates().getToDate(),
                actualItemService.getDate());
        assertEquals(LocalTime.of(14, 0), actualItemService.getFromTime());
        assertEquals(LocalTime.of(20, 0), actualItemService.getToTime());
        assertEquals(checkouterProperties.getItemServiceDefaultPartnerInfo().getInn(), actualItemService.getInn());
        assertEquals(checkouterProperties.getItemServiceDefaultPartnerInfo().getVatType(),
                actualItemService.getVat().getTrustId());
        // яуслуги не вызывались
        yaUslugiServiceTestConfigurer.verifyZeroInteractions();
    }

    @Test
    @DisplayName("В методе /cart не должны актуализироваться таймслоты")
    public void testNoTimeslotsActualizationOnCart() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        var itemService = ItemServiceProvider.defaultItemService();
        itemService.setDate(null);
        itemService.setFromTime((LocalTime) null);
        itemService.setToTime((LocalTime) null);
        itemService.setInn(null);
        itemService.setVat(null);
        itemService.setCount(2);
        var orderItem = parameters.getOrder().getItems().iterator().next();
        orderItem.setServices(Collections.singleton(itemService));
        orderItem.setCount(1);
        parameters.getReportParameters().setOffers(List.of(
                FoundOfferBuilder.createFrom(orderItem)
                        .configure(WhiteParametersProvider.whiteOffer())
                        .services(List.of(OfferServiceBuilder.createFrom(itemService).build()))
                        .build()
        ));
        var multiCart = orderCreateHelper.cart(parameters);
        var actualCart = firstOrNull(emptyIfNull(multiCart.getCarts()));

        assertNotNull(actualCart);
        var actualOrderItem = actualCart.getItems().iterator().next();
        assertNotNull(actualOrderItem);
        var actualItemService = actualOrderItem.getServices().iterator().next();
        assertNotNull(actualItemService);
        // предоплата выключена
        assertEquals(PaymentType.POSTPAID, actualItemService.getPaymentType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, actualItemService.getPaymentMethod());
        // количество услуг соответствует количеству товаров
        assertEquals(orderItem.getCount(), actualItemService.getCount());

        assertNull(actualItemService.getDate());
        assertNull(actualItemService.getFromTime());
        assertNull(actualItemService.getToTime());
        assertNull(actualItemService.getInn());
        assertNull(actualItemService.getVat());
        // яуслуги не вызывались
        yaUslugiServiceTestConfigurer.verifyZeroInteractions();
    }

    @Test
    @DisplayName("Таймслоты. Установка в день доставки")
    public void testTimeslotsInstallationRightAfterDelivery() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        var itemService = ItemServiceProvider.defaultItemService();
        itemService.setDate(null);
        itemService.setFromTime((LocalTime) null);
        itemService.setToTime((LocalTime) null);
        itemService.setCount(2);
        var orderItem = parameters.getOrder().getItems().iterator().next();
        orderItem.setServices(Collections.singleton(itemService));
        orderItem.setCount(1);
        parameters.getReportParameters().setOffers(List.of(
                FoundOfferBuilder.createFrom(orderItem)
                        .configure(WhiteParametersProvider.whiteOffer())
                        .services(List.of(OfferServiceBuilder.createFrom(itemService).build()))
                        .build()
        ));
        var createdOrder = orderCreateHelper.createOrder(parameters);
        assertNotNull(createdOrder);
        Order actualOrder = orderService.getOrder(createdOrder.getId(), ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES));

        assertNotNull(actualOrder);
        var actualOrderItem = actualOrder.getItems().iterator().next();
        assertNotNull(actualOrderItem);
        var actualItemService = actualOrderItem.getServices().iterator().next();
        assertNotNull(actualItemService);
        // предоплата выключена
        assertEquals(PaymentType.POSTPAID, actualItemService.getPaymentType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, actualItemService.getPaymentMethod());
        // количество услуг соответствует количеству товаров
        assertEquals(orderItem.getCount(), actualItemService.getCount());

        // date = delivery date
        assertEquals(actualOrder.getDelivery().getDeliveryDates().getToDate(),
                actualItemService.getDate());
        // крайний поздний интервал
        assertEquals(LocalTime.of(14, 0), actualItemService.getFromTime());
        assertEquals(LocalTime.of(20, 0), actualItemService.getToTime());
        // дефолтный партнер
        assertEquals(checkouterProperties.getItemServiceDefaultPartnerInfo().getInn(), actualItemService.getInn());
        assertEquals(checkouterProperties.getItemServiceDefaultPartnerInfo().getVatType(),
                actualItemService.getVat().getTrustId());
        // яуслуги не вызывались
        yaUslugiServiceTestConfigurer.verifyZeroInteractions();
    }

    @Test
    @DisplayName("Таймслоты. Пришли тайм интервалы")
    public void testTimeslotsTimeIntervalUsage() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        var itemService = ItemServiceProvider.defaultItemService();
        // нано обрежется при сериализации/десериализации
        var date = Date.from(LocalDateTime.now()
                .withNano(0)
                .atZone(getClock().getZone())
                .toInstant());
        itemService.setDate(date);
        itemService.setFromTime(LocalTime.of(12, 0));
        itemService.setToTime(LocalTime.of(16, 0));
        itemService.setCount(2);
        var orderItem = parameters.getOrder().getItems().iterator().next();
        orderItem.setServices(Collections.singleton(itemService));
        orderItem.setCount(1);
        parameters.getReportParameters().setOffers(List.of(
                FoundOfferBuilder.createFrom(orderItem)
                        .configure(WhiteParametersProvider.whiteOffer())
                        .services(List.of(OfferServiceBuilder.createFrom(itemService).build()))
                        .build()
        ));
        var createdOrder = orderCreateHelper.createOrder(parameters);
        assertNotNull(createdOrder);
        Order actualOrder = orderService.getOrder(createdOrder.getId(), ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES));

        assertNotNull(actualOrder);
        var actualOrderItem = actualOrder.getItems().iterator().next();
        assertNotNull(actualOrderItem);
        var actualItemService = actualOrderItem.getServices().iterator().next();
        assertNotNull(actualItemService);
        // предоплата выключена
        assertEquals(PaymentType.POSTPAID, actualItemService.getPaymentType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, actualItemService.getPaymentMethod());
        // количество услуг соответствует количеству товаров
        assertEquals(orderItem.getCount(), actualItemService.getCount());

        // дата не должна измениться
        assertEquals(date.getTime(), actualItemService.getDate().getTime());
        // крайний поздний интервал
        assertEquals(LocalTime.of(12, 0), actualItemService.getFromTime());
        assertEquals(LocalTime.of(16, 0), actualItemService.getToTime());
        // дефолтный партнер
        assertEquals(checkouterProperties.getItemServiceDefaultPartnerInfo().getInn(), actualItemService.getInn());
        assertEquals(checkouterProperties.getItemServiceDefaultPartnerInfo().getVatType(),
                actualItemService.getVat().getTrustId());
        // яуслуги не вызывались
        yaUslugiServiceTestConfigurer.verifyZeroInteractions();
    }

    @Test
    @DisplayName("Таймслоты. Пришел таймслот")
    public void testTimeslotsTimeSlotUsage() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        var itemService = ItemServiceProvider.defaultItemService();
        // нано обрежется при сериализации/десериализации
        var localDateTime = LocalDateTime.now().withNano(0);
        var date = Date.from(localDateTime.atZone(getClock().getZone()).toInstant());
        itemService.setDate(date);
        itemService.setFromTime((LocalTime) null);
        itemService.setToTime((LocalTime) null);
        itemService.setCount(2);
        var orderItem = parameters.getOrder().getItems().iterator().next();
        orderItem.setServices(Collections.singleton(itemService));
        orderItem.setCount(1);
        // mock ЯУслуги
        var response = new YaServiceTimeSlotsResponse();
        response.setTimeslots(List.of(
                new YaServiceTimeSlotDto() {{
                    setId("1");
                    setDate(localDateTime);
                    setPartnerId("partner_1");
                }},
                new YaServiceTimeSlotDto() {{
                    setId("2");
                    setDate(localDateTime.plusHours(1));
                    setPartnerId("partner_2");
                }}
        ));
        response.setPartnerDescription(Map.of(
                "partner_1", new YaServicePartnerDescriptionDto() {{
                    setName("Partner 1");
                    setInn("INN 1");
                    setVat(VatType.VAT_20.getTrustId());
                    setDuration(100);
                }},
                "partner_2", new YaServicePartnerDescriptionDto() {{
                    setName("Partner 2");
                    setInn("INN 2");
                    setVat(VatType.NO_VAT.getTrustId());
                    setDuration(100);
                }}
        ));
        yaUslugiServiceTestConfigurer.mockGetTimeslots(response);

        parameters.getReportParameters().setOffers(List.of(
                FoundOfferBuilder.createFrom(orderItem)
                        .configure(WhiteParametersProvider.whiteOffer())
                        .services(List.of(OfferServiceBuilder.createFrom(itemService).build()))
                        .build()
        ));

        var createdOrder = orderCreateHelper.createOrder(parameters);
        assertNotNull(createdOrder);
        Order actualOrder = orderService.getOrder(createdOrder.getId(), ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES));

        assertNotNull(actualOrder);
        var actualOrderItem = actualOrder.getItems().iterator().next();
        assertNotNull(actualOrderItem);
        var actualItemService = actualOrderItem.getServices().iterator().next();
        assertNotNull(actualItemService);
        // предоплата выключена
        assertEquals(PaymentType.POSTPAID, actualItemService.getPaymentType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, actualItemService.getPaymentMethod());
        // количество услуг соответствует количеству товаров
        assertEquals(orderItem.getCount(), actualItemService.getCount());

        // дата не должна измениться
        assertEquals(date.getTime(), actualItemService.getDate().getTime());
        // крайний поздний интервал
        assertNull(actualItemService.getFromTime());
        assertNull(actualItemService.getToTime());
        // дефолтный партнер
        assertEquals("INN 1", actualItemService.getInn());
        assertEquals(VatType.VAT_20.getTrustId(), actualItemService.getVat().getTrustId());
        // яуслуги не вызывались
        var uslugiEvents = yaUslugiServiceTestConfigurer.getYaUslugiMock().getAllServeEvents();
        assertThat(uslugiEvents, hasSize(1));
    }

    @Test
    @DisplayName("Пришел ИД услуги, которой нет на репорте")
    public void testItemServiceFiltering() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        var itemService = ItemServiceProvider.defaultItemService();
        var date = Date.from(LocalDateTime.now()
                .withNano(0)
                .atZone(getClock().getZone())
                .toInstant());
        itemService.setDate(date);
        itemService.setFromTime(LocalTime.of(12, 0));
        itemService.setToTime(LocalTime.of(16, 0));
        itemService.setCount(2);
        var orderItem = parameters.getOrder().getItems().iterator().next();
        orderItem.setServices(Collections.singleton(itemService));
        orderItem.setCount(1);
        parameters.getReportParameters().setOffers(List.of(
                FoundOfferBuilder.createFrom(orderItem)
                        .configure(WhiteParametersProvider.whiteOffer())
                        .services(List.of(OfferServiceBuilder.createFrom(itemService).build()))
                        .build()
        ));
        itemService.setServiceId(ThreadLocalRandom.current().nextLong());
        parameters.setCheckCartErrors(false);
        var createdOrder = orderCreateHelper.createOrder(parameters);
        assertNotNull(createdOrder);
        Order actualOrder = orderService.getOrder(createdOrder.getId(), ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES));

        assertNotNull(actualOrder);
        assertNull(actualOrder.getServicesTotal());
        assertEquals(0, actualOrder.getTotal().compareTo(BigDecimal.valueOf(260L)));
        var actualOrderItem = actualOrder.getItems().iterator().next();
        assertNotNull(actualOrderItem);
        assertThat(actualOrderItem.getServices(), emptyIterable());
    }
}
