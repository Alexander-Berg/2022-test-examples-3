package ru.yandex.market.checkout.checkouter.log.cart;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.carter.InMemoryAppender;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.PushApiCartResponseFetcher;
import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartItemResponse;
import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartResponse;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.log.Loggers;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public class DiffLogTest extends AbstractWebTestBase {

    public static final String ORDER_SCHEMA_JSON_RESOURCE = "files/order-schema.json";
    public static final String SHOP_OUTLET_CODE = "asdasd";
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DiffLogTest.class);
    private static final String MARKET_REQUEST_ID = "TEST_MARKET_REQUEST_ID";
    private static final Logger logger = (Logger) LoggerFactory.getLogger(Loggers.CART_DIFF_JSON_LOG);
    @Autowired
    private ObjectMapper checkouterAnnotationObjectMapper;
    @Autowired
    private PushApiCartResponseFetcher cartFetcher;
    private InMemoryAppender inMemoryAppender;
    private Level oldLevel;

    @BeforeEach
    public void setUp() {
        RequestContextHolder.createContext(MARKET_REQUEST_ID);
        inMemoryAppender = new InMemoryAppender();
        logger.addAppender(inMemoryAppender);
        inMemoryAppender.clear();
        inMemoryAppender.start();
        oldLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    public void tearDown() {
        logger.detachAppender(inMemoryAppender);
        logger.setLevel(oldLevel);
    }


    /**
     * checkouter-213: Diff loggs: записть diff лога при изменении item count в ответе магазина
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-213
     */
    @Epic(Epics.DIFFLOGS)
    @Story(Stories.CHECKOUT)
    @DisplayName("записть diff лога при изменении item count в ответе магазина ")
    @Test
    public void itemCountDiffLogTest() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        OrderItem orderItem = parameters.getOrder().getItems().iterator().next();
        orderItem.setCount(2);
        parameters.setCheckCartErrors(false);
        parameters.setMarketRequestId(MARKET_REQUEST_ID);

        Mockito.doAnswer(a -> new PushApiCartResponse(
                List.of(
                        new PushApiCartItemResponse(
                                orderItem.getFeedId(),
                                orderItem.getOfferId(),
                                orderItem.getBundleId(),
                                1,
                                orderItem.getPrice(),
                                orderItem.getVat(),
                                orderItem.getDelivery(),
                                orderItem.getSellerInn()
                        )
                ),
                List.of(),
                List.of()
        )).when(cartFetcher).fetch(any());

        orderCreateHelper.cart(parameters);

        String message = getLoggedEvent();

        JsonTest.checkJson(message, "$.logType", LogType.CART_DIFF.name());
        JsonTest.checkJson(message, "$.event", CartLoggingEvent.ITEM_COUNT.name());
        JsonTest.checkJson(message, "$..item.count", 2);

        checkSchema(message);

        JsonTest.checkJsonMatcher(message, "$.marketRequestId", startsWith(MARKET_REQUEST_ID));
    }

    /**
     * checkouter-214: Diff loggs: записть diff лога при изменении условий доставки в ответе магазина
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-214
     */
    @Epic(Epics.DIFFLOGS)
    @DisplayName("diff.log, если ответ магазина о доставке не совпадает с ожидаемым")
    @Test
    public void deliveryDiffLogTest() throws Exception {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfPickupDeliveryByOutletCode()
                .outletCode(SHOP_OUTLET_CODE)
                .buildResponse(DeliveryResponse::new));
        parameters.getOrder().setDelivery(DeliveryProvider.shopSelfPickupDeliveryByMarketOutletId().build());
        parameters.setCheckCartErrors(false);
        parameters.setMarketRequestId(MARKET_REQUEST_ID);

        orderCreateHelper.cart(parameters);

        String message = getLoggedEvents().stream()
                .filter(m -> m.contains(CartLoggingEvent.DELIVERY.name()))
                .findFirst().orElseThrow();

        JsonTest.checkJson(message, "$.logType", LogType.CART_DIFF.name());
        JsonTest.checkJson(message, "$.event", CartLoggingEvent.DELIVERY.name());

        checkSchema(message);

        JsonTest.checkJsonMatcher(message, "$.marketRequestId", startsWith(MARKET_REQUEST_ID));
    }

    /**
     * checkouter-215: Diff loggs: записть diff лога если из ответа магазина исчезла возможность доставки
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-215
     */
    @Epic(Epics.DIFFLOGS)
    @DisplayName("diff.log, если из ответа магазина исчезла возможность доставки")
    @Test
    public void itemDeliveryDiffLogTest() throws Exception {
        Parameters parameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.getReportParameters().setDeliveryPartnerTypes(List.of("SHOP"));
        parameters.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder()
                .build());
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfPickupDeliveryByOutletCode()
                .outletCode("not_existed_code")
                .buildResponse(DeliveryResponse::new));
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDelivery(193L));
        parameters.setMarketRequestId(MARKET_REQUEST_ID);
        parameters.setCheckCartErrors(false);

        orderCreateHelper.cart(parameters);

        String message = getLoggedEvents().stream()
                .filter(m -> m.contains("ITEM_DELIVERY"))
                .findFirst().orElseThrow();

        JsonTest.checkJson(message, "$.logType", LogType.CART_DIFF.name());
        JsonTest.checkJson(message, "$.event", CartLoggingEvent.ITEM_DELIVERY.name());

        checkSchema(message);

        JsonTest.checkJsonMatcher(message, "$.marketRequestId", startsWith(MARKET_REQUEST_ID));
    }

    /**
     * checkouter-216: Diff loggs: запись diff лога если в ответе репорта изменилась цена товара
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-216
     */
    @Epic(Epics.DIFFLOGS)
    @DisplayName("diff.log, если изменилась цена товара")
    @Test
    public void itemPriceDiffLogTest() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        orderCreateHelper.initializeMock(parameters);
        MultiCart multiCart = parameters.getBuiltMultiCart();
        OrderItem item = multiCart.getCarts().get(0).getItems().stream().findFirst().get();
        parameters.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(item).build()));
        int originalBuyerPrice = item.getBuyerPrice().intValue();
        item.setBuyerPrice(item.getBuyerPrice().add(new BigDecimal(50)));
        int changedItemPrice = item.getBuyerPrice().intValue();
        parameters.turnOffErrorChecks();
        parameters.setUseErrorMatcher(false);
        orderCreateHelper.cart(parameters);

        String message = getLoggedEvent();

        JsonTest.checkJson(message, "$.logType", LogType.CART_DIFF.name());
        JsonTest.checkJson(message, "$.event", CartLoggingEvent.ITEM_PRICE.name());
        JsonTest.checkJson(message, "$..item.buyerPrice", changedItemPrice);
        JsonTest.checkJson(message, "$..additionalLoggingInfo.actualBuyerPrice", originalBuyerPrice);

        checkSchema(message);

        JsonTest.checkJsonMatcher(message, "$.marketRequestId", startsWith(MARKET_REQUEST_ID));
    }

    /**
     * checkouter-217: Diff loggs: запись diff лога если в ответе магазина изменился срок доставки
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-217
     */
    @Epic(Epics.DIFFLOGS)
    @DisplayName("diff.log, если изменился срок доставки")
    @Test
    public void deliveryDatesDiffLogTest() throws Exception {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setCheckCartErrors(false);
        parameters.setMarketRequestId(MARKET_REQUEST_ID);
        parameters.getBuiltMultiCart().getCarts().get(0).setDelivery(DeliveryProvider.shopSelfDelivery()
                .nextDays(0, 0)
                .build());
        orderCreateHelper.cart(parameters);

        List<String> messages =
                getLoggedEvents().stream().filter(m -> m.contains("\"DELIVERY_DATES\"")).collect(Collectors.toList());
        assertThat(messages.size(), is(1));
        String message = messages.get(0);

        JsonTest.checkJson(message, "$.logType", LogType.CART_DIFF.name());
        JsonTest.checkJson(message, "$.event", CartLoggingEvent.DELIVERY_DATES.name());

        checkSchema(message);

        JsonTest.checkJsonMatcher(message, "$.marketRequestId", startsWith(MARKET_REQUEST_ID));
    }

    /**
     * checkouter-218: Diff loggs: запись diff лога если в ответе магазина изменился метод оплаты
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-218
     */
    @Epic(Epics.DIFFLOGS)
    @DisplayName("diff.log, если изменился метод оплаты")
    @Test
    public void paymentMethodDiffLogTest() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        parameters.setMarketRequestId(MARKET_REQUEST_ID);

        PaymentMethod missingPaymentMethod = PaymentMethod.CARD_ON_DELIVERY;
        parameters.setPaymentMethod(missingPaymentMethod);

        parameters.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder()
                .addDelivery(DeliveryProvider.yandexDelivery()
                        .paymentOptions(List.of(PaymentMethod.YANDEX))
                        .today()
                        .buildActualDeliveryOption())
                .build());
        parameters.getOrder().setDelivery(DeliveryProvider.yandexDelivery()
                .today()
                .build());

        orderCreateHelper.cart(parameters);

        String message = getLoggedEvent();

        JsonTest.checkJson(message, "$.logType", LogType.CART_DIFF.name());
        JsonTest.checkJson(message, "$.event", CartLoggingEvent.PAYMENT_METHOD.name());

        checkSchema(message);

        JsonTest.checkJsonMatcher(message, "$.marketRequestId", startsWith(MARKET_REQUEST_ID));
    }

    /**
     * checkouter-219: Diff loggs: запись diff лога если магазин не принял заказ
     *
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-219
     */
    @Epic(Epics.DIFFLOGS)
    @DisplayName("diff.log, если магазин не принял заказ")
    @Test
    public void acceptFailedDiffLogTest() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setAcceptOrder(false);
        parameters.setCheckOrderCreateErrors(false);
        parameters.setMarketRequestId(MARKET_REQUEST_ID);
        orderCreateHelper.createOrder(parameters);

        String message = getLoggedEvent();

        JsonTest.checkJson(message, "$.logType", LogType.CART_ACCEPT.name());
        JsonTest.checkJson(message, "$.event", CartLoggingEvent.ACCEPT_FAILED.name());

        checkSchema(message);

        JsonTest.checkJsonMatcher(message, "$.marketRequestId", startsWith(MARKET_REQUEST_ID));
    }

    private String getLoggedEvent() {
        return Iterables.getOnlyElement(getLoggedEvents());
    }

    private List<String> getLoggedEvents() {
        List<ILoggingEvent> loggingEvents = null;
        try {
            loggingEvents = inMemoryAppender.getRaw();
        } catch (NoSuchElementException e) {
            System.out.println("Diff log had expected to be written.");
        }
        assertFalse(loggingEvents.stream().allMatch(Predicate.isEqual(null)), "Diff log had expected to be written.");
        List<String> messages =
                loggingEvents.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
        assertThat("Diff log had expected to be written.", messages, not(contains(isEmptyString())));
        return messages;
    }

    private void checkSchema(String message) throws IOException, ProcessingException {
        JsonValidator jsonValidator = JsonSchemaFactory.byDefault().getValidator();
        JsonNode schema = checkouterAnnotationObjectMapper.readTree(Resources.getResource(ORDER_SCHEMA_JSON_RESOURCE));
        ProcessingReport report = jsonValidator.validate(schema, checkouterAnnotationObjectMapper.readTree(message),
                true);
        report.forEach(m -> {
            if (m.getLogLevel() == LogLevel.ERROR) {
                LOG.error(m.toString());
            }
        });
        assertTrue(report.isSuccess());
    }
}
