package ru.yandex.market.checkout.checkouter.actualization;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.carter.InMemoryAppender;
import ru.yandex.market.checkout.checkouter.actual.ActualItem;
import ru.yandex.market.checkout.checkouter.log.Loggers;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.ActualizeHelper;
import ru.yandex.market.checkout.helpers.utils.ActualizeParameters;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public class ActualizeTest extends AbstractWebTestBase {

    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(Loggers.CART_DIFF_JSON_LOG);

    private static final BigDecimal REPORT_PRICE = new BigDecimal(97154);

    @Autowired
    private ActualizeHelper actualizeHelper;

    private InMemoryAppender inMemoryAppender;
    private Level oldLevel;

    @BeforeEach
    public void setUp() {
        inMemoryAppender = new InMemoryAppender();
        LOGGER.addAppender(inMemoryAppender);
        inMemoryAppender.clear();
        inMemoryAppender.start();
        oldLevel = LOGGER.getLevel();
        LOGGER.setLevel(Level.INFO);
    }

    @AfterEach
    public void tearDown() {
        LOGGER.detachAppender(inMemoryAppender);
        LOGGER.setLevel(oldLevel);
    }

    @Test
    public void shouldActualize() throws Exception {
        ActualItem actualItem = actualizeHelper.actualizeItem(new ActualizeParameters());

        assertThat(actualItem.getCount(), greaterThan(0));
    }

    @Test
    public void shouldNotWriteCartDiffLog() throws Exception {
        ActualizeParameters parameters = new ActualizeParameters();
        parameters.setMockPushApi(false);

        ActualItem actualItemRequest = parameters.getActualItem();

        OrderItem item = new OrderItem();
        item.setFeedOfferId(actualItemRequest.getFeedOfferId());
        item.setCount(1);
        item.setPrice(new BigDecimal(600));

        pushApiConfigurer.mockCart(
                Collections.singletonList(item),
                OrderProvider.SHOP_ID,
                List.of(DeliveryResponseProvider.buildDeliveryResponse()),
                OrderAcceptMethod.PUSH_API,
                false);

        ActualItem actualItem = actualizeHelper.actualizeItem(parameters);

        assertThat(actualItem.getCount(), greaterThan(0));

        assertThat(actualItem.getPrice(), comparesEqualTo(REPORT_PRICE));

        List<ILoggingEvent> events = inMemoryAppender.getRaw();
        assertThat(events, hasSize(0));
    }

    @Test
    public void shouldBeOkWithEmptyPrice() throws Exception {
        ActualizeParameters parameters = new ActualizeParameters();
        parameters.setMockPushApi(false);

        ActualItem actualItemRequest = parameters.getActualItem();

        OrderItem item = new OrderItem();
        item.setFeedOfferId(actualItemRequest.getFeedOfferId());
        item.setCount(1);
        item.setPrice(null);

        pushApiConfigurer.mockCart(
                Collections.singletonList(item),
                OrderProvider.SHOP_ID,
                List.of(DeliveryResponseProvider.buildDeliveryResponse()),
                OrderAcceptMethod.PUSH_API,
                false);

        ActualItem actualItem = actualizeHelper.actualizeItem(parameters);

        assertThat(actualItem.getCount(), greaterThan(0));

        assertThat(actualItem.getPrice(), comparesEqualTo(REPORT_PRICE));

        List<ILoggingEvent> events = inMemoryAppender.getRaw();
        assertThat(events, hasSize(0));
    }
}
