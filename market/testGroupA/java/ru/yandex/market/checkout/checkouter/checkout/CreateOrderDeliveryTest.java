package ru.yandex.market.checkout.checkouter.checkout;


import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.carter.InMemoryAppender;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.checkouter.log.Loggers;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class CreateOrderDeliveryTest extends AbstractWebTestBase {

    public static final String SHOP_OUTLET_CODE = "asdasd";

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    private InMemoryAppender appender;
    private Level oldLevel;

    @BeforeEach
    public void mockLogger() {
        appender = new InMemoryAppender();
        appender.start();

        Logger logger = (Logger) LoggerFactory.getLogger(Loggers.CART_DIFF_JSON_LOG);
        logger.addAppender(appender);
        oldLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    public void removeMock() {
        Logger logger = (Logger) LoggerFactory.getLogger(Loggers.CART_DIFF_JSON_LOG);
        logger.detachAppender(appender);
        logger.setLevel(oldLevel);
    }

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-119
     * https://testpalm.yandex-team.ru/testcase/checkouter-50
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа с типом доставки = PICKUP и outletId")
    @Test
    public void shouldCreateOrderWithDeliveryTypePickup() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setColor(Color.WHITE);
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertNotNull(order);
        Assertions.assertNotNull(order.getId());
        Assertions.assertEquals(DeliveryType.PICKUP, order.getDelivery().getType());
        Assertions.assertTrue(order.getDelivery().isSelfDeliveryService(), "selfDeliveryService");
        Assertions.assertEquals(DeliveryPartnerType.SHOP, order.getDelivery().getDeliveryPartnerType());
    }

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-122
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа с типом доставки = PICKUP и outletCode")
    @Test
    public void shouldCreateOrderWithDeliveryTypePickupByOutletCode() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setColor(Color.WHITE);
        parameters.setShopId(775L);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.getReportParameters().setDeliveryPartnerTypes(List.of("SHOP"));
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDelivery());
        parameters.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder().build());
        parameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfPickupDeliveryByOutletCode()
                .outletCode(SHOP_OUTLET_CODE)
                .buildResponse(DeliveryResponse::new));

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertNotNull(order);
        Assertions.assertNotNull(order.getId());
        Assertions.assertEquals(DeliveryType.PICKUP, order.getDelivery().getType());
        Assertions.assertEquals(DeliveryPartnerType.SHOP, order.getDelivery().getDeliveryPartnerType());
        Assertions.assertEquals(SHOP_OUTLET_CODE, order.getDelivery().getOutletCode());
    }

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-120
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа с типом доставки = DELIVERY")
    @Test
    public void shouldCreateOrderWithDeliveryTypeDelivery() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertNotNull(order);
        Assertions.assertNotNull(order.getId());
        Assertions.assertEquals(DeliveryType.DELIVERY, order.getDelivery().getType());
    }

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-124
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа с типом доставки = DELIVERY")
    @Test
    public void shouldCreateOrderWithDeliveryTypeDeliveryAndPrice0() throws Exception {
        var deliveryBuilder = DeliveryProvider.shopSelfDelivery().free();
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.setDeliveryServiceId(null);
        parameters.getOrder().setDelivery(deliveryBuilder.build());
        parameters.setPushApiDeliveryResponse(deliveryBuilder.buildResponse(DeliveryResponse::new));

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertNotNull(order);
        Assertions.assertNotNull(order.getId());
        Assertions.assertEquals(DeliveryType.DELIVERY, order.getDelivery().getType());
        Assertions.assertTrue(order.getDelivery().isFree());
        Assertions.assertEquals(BigDecimal.ZERO, order.getDelivery().getPrice());
        Assertions.assertEquals(order.getBuyerItemsTotal(), order.getBuyerTotal());
    }

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-121
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа с типом доставки = POST")
    @Test
    public void shouldCreateOrderWithDeliveryTypePost() throws Exception {
        var deliveryBuilder = DeliveryProvider.shopSelfPostDelivery();
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setDeliveryType(DeliveryType.POST);
        parameters.getOrder().setDelivery(deliveryBuilder.build());
        parameters.setPushApiDeliveryResponse(deliveryBuilder.buildResponse(DeliveryResponse::new));

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertNotNull(order);
        Assertions.assertNotNull(order.getId());
        Assertions.assertEquals(DeliveryType.POST, order.getDelivery().getType());
    }

    @Test
    public void shouldCreateOrderWithDeliveryInterval() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setColor(Color.WHITE);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        parameters.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder().build());
        RawDeliveryIntervalsCollection rawDeliveryIntervals = new RawDeliveryIntervalsCollection();
        rawDeliveryIntervals.add(new RawDeliveryInterval(
                DateUtil.addDay(new Date(), 1),
                LocalTime.of(9, 0),
                LocalTime.of(11, 0)
        ));

        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildDeliveryResponse();
        deliveryResponse.setRawDeliveryIntervals(rawDeliveryIntervals);

        parameters.setPushApiDeliveryResponse(deliveryResponse);

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertTrue(order.getDelivery().getDeliveryDates().hasTime());
    }

    @DisplayName("Не писать в cart-diff.log, если магазин вернул опцию и delivery=true, но нет опций оплаты")
    @Tag(Tags.AUTO)
    @Test
    public void shouldNotWriteCartDiffIfDeliveryOptionHasNoPaymentOption() throws Exception {
        DeliveryResponse deliveryOption = DeliveryResponseProvider.buildDeliveryResponse();
        deliveryOption.setPaymentOptions(Collections.emptySet());
        DeliveryResponse pickupOption = DeliveryResponseProvider.buildPickupDeliveryResponse();
        pickupOption.setPaymentOptions(Collections.emptySet());
        Parameters parameters = setupGlobalSandboxOrderParameters(deliveryOption, pickupOption);
        parameters.setPaymentType(null);
        parameters.setPaymentMethod(null);
        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(cart, notNullValue());
        assertThat(cart.getCarts(), notNullValue());
        assertThat(appender.getRaw(), empty());
    }

    @DisplayName("Не писать в cart-diff.log, если выкинули все опции доставки")
    @Tag(Tags.AUTO)
    @Test
    public void shouldNotWriteCartDiffIfNoOptionsLeft() throws Exception {
        DeliveryResponse pickupOption = DeliveryResponseProvider.buildPickupDeliveryResponse();
        pickupOption.setPaymentOptions(Collections.emptySet());
        Parameters parameters = setupGlobalSandboxOrderParameters(pickupOption);
        parameters.setPaymentType(null);
        parameters.setPaymentMethod(null);
        parameters.setCheckCartErrors(false);
        parameters.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder()
                .build());

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart, notNullValue());
        assertThat(cart.getCarts(), notNullValue());
        assertThat(appender.getRaw(), empty());
    }

    @DisplayName("При актуализации для синих заказов не учитываться deliveryMethods из offerinfo")
    @Test
    public void createBlueOrderWithoutDeliveryMethodsFromOfferInfo() {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withPartnerInterface(true)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.getReportParameters().setDeliveryMethods(null);

        Order order = orderCreateHelper.createOrder(parameters);
        assertNotNull(order.getId());
    }

    @Nonnull
    private Parameters setupGlobalSandboxOrderParameters(DeliveryResponse... deliveryOptions) {
        Parameters parameters = new Parameters();
        parameters.setShopId(775L);
        parameters.setupGlobal();
        parameters.setPushApiDeliveryResponse(deliveryOptions);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        ShopMetaData shopMetaData = ShopMetaDataBuilder.createCopy(ShopSettingsHelper.getDefaultMeta())
                .withProdClass(PaymentClass.OFF)
                .build();
        parameters.addShopMetaData(775L, shopMetaData);
        parameters.setSandbox(true);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        return parameters;
    }
}
