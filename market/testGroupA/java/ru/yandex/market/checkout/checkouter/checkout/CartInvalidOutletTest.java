package ru.yandex.market.checkout.checkouter.checkout;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.carter.InMemoryAppender;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.log.Loggers;
import ru.yandex.market.checkout.checkouter.log.cart.CartLoggingEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @link https://st.yandex-team.ru/MARKETCHECKOUT-4360
 * <p>
 * Должны выдавать CartDiff, если магазин ответил item.delivery=true, но после наших проверок опций не осталось.
 */
public class CartInvalidOutletTest extends AbstractWebTestBase {

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

    @DisplayName("Должны писать в cart-diff.log, если магазин вернул какую-то опцию и delivery=true, " +
            "но в результате проверки мы эту опцию удалили")
    @Tag(Tags.AUTO)
    @Test
    public void shouldWriteCartDiffIfNoOptionsLeft() {
        Order order = OrderProvider.getColorOrder(Color.WHITE);
        order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        //Доставка в другой регион отсутствует
        order.setDelivery(DeliveryProvider.getEmptyDelivery(193L));

        Parameters parameters = new Parameters(order);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setPushApiDeliveryResponse(DeliveryResponseProvider.buildPickupDeliveryResponse());
        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(cart, notNullValue());
        var events = appender.getRaw();
        var itemDeliveryEvent = events.stream()
                .filter(e -> e.getMessage().contains(CartLoggingEvent.ITEM_DELIVERY.name()))
                .findFirst();
        assertTrue(itemDeliveryEvent.isPresent());
        ILoggingEvent event = itemDeliveryEvent.get();
        new JsonPathExpectationsHelper("$.event")
                .assertValue(event.getFormattedMessage(), CartLoggingEvent.ITEM_DELIVERY.name());
    }

    @Test
    @DisplayName("кейс с ПИ Дбс, при котором аутлеты придут с репорта, не придут с пуш апи")
    public void shouldSendRequestWithSpecifiedOutletId_1() {
        Order order = OrderProvider.getColorOrder(Color.WHITE);
        Parameters parameters = WhiteParametersProvider.shopPickupDeliveryParameters(order);
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setEmptyPushApiDeliveryResponse();
        parameters.setCheckCartErrors(false);
        long reportOutletId = 419584L;
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .outletId(reportOutletId)
                                .buildPickupOption(getClock())
                        ).build());
        orderCreateHelper.cart(parameters);
        Set<String> requestOutletIds = reportMock.getServeEvents().getRequests().stream()
                .filter(r -> r.getRequest().getQueryParams().get("place").firstValue().equals("outlets"))
                .flatMap(r -> Stream.of(r.getRequest().getQueryParams().get("outlets").firstValue().split(",")))
                .collect(Collectors.toSet());
        assertThat(requestOutletIds, hasSize(1));
        assertTrue(requestOutletIds.contains(String.valueOf(reportOutletId)));
    }

    @Test
    @DisplayName("кейс с АПИ Дбс, при котором аутлеты придут с репорта, и придут с пуш апи")
    public void shouldSendRequestWithSpecifiedOutletId_2() {
        Order order = OrderProvider.getColorOrder(Color.WHITE);
        Parameters parameters = WhiteParametersProvider.shopPickupDeliveryParameters(order);
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setPushApiDeliveryResponse(DeliveryResponseProvider.buildPickupDeliveryResponse());
        parameters.setCheckCartErrors(false);
        long reportOutletId = 419584L;
        long pushApiOutletId = 419585L;
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .outletId(reportOutletId)
                                .buildPickupOption(getClock())
                        ).build());
        orderCreateHelper.cart(parameters);
        Set<String> requestOutletIds = reportMock.getServeEvents().getRequests().stream()
                .filter(r -> r.getRequest().getQueryParams().get("place").firstValue().equals("outlets"))
                .flatMap(r -> Stream.of(r.getRequest().getQueryParams().get("outlets").firstValue().split(",")))
                .collect(Collectors.toSet());
        assertThat(requestOutletIds, hasSize(2));
        assertTrue(requestOutletIds.contains(String.valueOf(reportOutletId)));
        assertTrue(requestOutletIds.contains(String.valueOf(pushApiOutletId)));
    }

    @Test
    @DisplayName("кейс с АПИ Дбс, при котором аутлеты не придут с репорта, но придут с пуш апи")
    public void shouldSendRequestWithSpecifiedOutletId_3() {
        Order order = OrderProvider.getColorOrder(Color.WHITE);
        Parameters parameters = WhiteParametersProvider.shopPickupDeliveryParameters(order);
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setCheckCartErrors(false);
//        замокать actual_delivery на пустой ответ
//        пуш апи должен по умолчанию замокан на outletCode=20697, данный код мапится на 419585 код аутлета
        long pushApiOutletId = 419585L;
        parameters.getReportParameters().setActualDelivery(ActualDeliveryProvider.defaultActualDelivery());
        orderCreateHelper.cart(parameters);
        Set<String> requestOutletIds = reportMock.getServeEvents().getRequests().stream()
                .filter(r -> r.getRequest().getQueryParams().get("place").firstValue().equals("outlets"))
                .flatMap(r -> Stream.of(r.getRequest().getQueryParams().get("outlets").firstValue().split(",")))
                .collect(Collectors.toSet());
        assertThat(requestOutletIds, hasSize(1));
        assertTrue(requestOutletIds.contains(String.valueOf(pushApiOutletId)));
    }

    @Test
    @DisplayName("кейс с ПИ Дбс, при котором нет магазинных аутлетов")
    public void shouldNotRequestToOutletsPlace() {
        Order order = OrderProvider.getColorOrder(Color.WHITE);
        Parameters parameters = WhiteParametersProvider.shopPickupDeliveryParameters(order);
        parameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setCheckCartErrors(false);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .partnerType(DeliveryPartnerType.YANDEX_MARKET)
                                .outletId(419585L)
                                .buildPickupOption(getClock())
                        ).build());
        parameters.setEmptyPushApiDeliveryResponse();
        orderCreateHelper.cart(parameters);
        Optional<ServeEvent> requestToOutletsPlace = reportMock.getServeEvents().getRequests().stream()
                .filter(r -> r.getRequest().getQueryParams().get("place").firstValue().equals("outlets"))
                .findFirst();
        assertFalse(requestToOutletsPlace.isPresent());
    }
}
