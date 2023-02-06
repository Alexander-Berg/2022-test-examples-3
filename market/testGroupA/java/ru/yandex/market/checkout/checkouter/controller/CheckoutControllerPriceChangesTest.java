package ru.yandex.market.checkout.checkouter.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.carter.InMemoryAppender;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.ItemPriceReason;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.log.Loggers;
import ru.yandex.market.checkout.checkouter.log.cart.CartLoggingEvent;
import ru.yandex.market.checkout.checkouter.log.cart.DiffLog;
import ru.yandex.market.checkout.checkouter.log.cart.serialization.CartJsonLoggerObjectMapperFactory;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.market.checkout.util.OrderUtils.firstOrder;

public class CheckoutControllerPriceChangesTest extends AbstractWebTestBase {

    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(Loggers.CART_DIFF_JSON_LOG);
    private static final String MARKET_REQUEST_ID = "TEST_MARKET_REQUEST_ID";
    private InMemoryAppender inMemoryAppender;
    private Level oldLevel;

    private Parameters parameters;

    @BeforeEach
    public void configure() {
        RequestContextHolder.createContext(MARKET_REQUEST_ID);
        inMemoryAppender = new InMemoryAppender();
        LOGGER.addAppender(inMemoryAppender);
        inMemoryAppender.start();
        oldLevel = LOGGER.getLevel();
        LOGGER.setLevel(Level.INFO);

        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        parameters.setCheckOrderCreateErrors(false);
        parameters.setUseErrorMatcher(false);
    }

    @AfterEach
    public void clear() {
        inMemoryAppender.clear();
        LOGGER.detachAppender(inMemoryAppender);
        LOGGER.setLevel(oldLevel);
    }

    @Test
    void shouldWriteRequestIdToLogOnPriceChange() throws IOException {
        Order order = parameters.getOrder();
        OrderItem firstItem = order.getItems().iterator().next();

        parameters.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(firstItem)
                .price(BigDecimal.valueOf(100))
                .build()));

        firstItem.setBuyerPrice(BigDecimal.valueOf(90));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Order firstOrder = firstOrder(multiCart);

        OrderItem resultItem = firstOrder.getItems().iterator().next();

        assertThat(resultItem.getChanges(), not(empty()));
        assertThat(resultItem.getChanges(), hasItem(ItemChange.PRICE));

        DiffLog log = readLog();

        assertThat(log.getMarketRequestId(), startsWith(MARKET_REQUEST_ID));
    }

    @Test
    void shouldWriteChangeReasonToLogOnPriceChange() throws IOException {
        Order order = parameters.getOrder();
        OrderItem firstItem = order.getItems().iterator().next();

        parameters.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(firstItem)
                .price(BigDecimal.valueOf(100))
                .build()));

        firstItem.setBuyerPrice(BigDecimal.valueOf(90));

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Order firstOrder = firstOrder(multiCart);

        OrderItem resultItem = firstOrder.getItems().iterator().next();

        assertThat(resultItem.getChanges(), not(empty()));
        assertThat(resultItem.getChanges(), hasItem(ItemChange.PRICE));

        DiffLog log = readLog();

        assertThat(log.getAdditionalLoggingInfo().getChangeReason(), is(ItemPriceReason.REPORT.getCode()));
    }

    private DiffLog readLog() throws IOException {
        return inMemoryAppender.getRaw()
                .stream()
                .map(CheckoutControllerPriceChangesTest::parse)
                .filter(Objects::nonNull)
                .filter(x -> CartLoggingEvent.ITEM_PRICE.equals(x.getEvent()))
                .findFirst()
                .orElseThrow();
    }

    private static DiffLog parse(ILoggingEvent x) {
        try {
            return CartJsonLoggerObjectMapperFactory.getObjectMapper()
                    .readValue(x.getFormattedMessage(), DiffLog.class);
        } catch (IOException ignored) {

        }
        return null;
    }
}
