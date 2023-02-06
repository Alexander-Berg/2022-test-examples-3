package ru.yandex.market.checkout.checkouter.log;

import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.carter.InMemoryAppender;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class OrderEventLoggerTest extends AbstractWebTestBase {

    private final Logger logger = (Logger) LoggerFactory.getLogger(Loggers.ORDER_EVENT_TSKV_LOG);

    private InMemoryAppender appender;
    private Level oldLevel;

    @BeforeEach
    public void mockLogger() {
        appender = new InMemoryAppender();
        appender.clear();
        appender.start();
        logger.addAppender(appender);
        oldLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    public void removeMock() {
        logger.detachAppender(appender);
        logger.setLevel(oldLevel);
    }

    @Test
    public void shouldWriteToOrderEventLogIfOrderCreated() {
        String metaInfo = "ololo";

        Parameters parameters = new Parameters();
        parameters.setMetaInfo(metaInfo);
        Order order = orderCreateHelper.createOrder(parameters);

        List<Map<String, String>> logs = appender.getTskvMaps();

        assertThat(logs, hasSize(1));

        Map<String, String> tskvMap = Iterables.getOnlyElement(logs);

        Assertions.assertEquals(String.valueOf(order.getId()), tskvMap.get("order_id"));
        Assertions.assertEquals(String.valueOf(order.getBuyer().getUid()), tskvMap.get("uid"));
        Assertions.assertEquals(String.valueOf(order.getShopId()), tskvMap.get("shop_id"));
        Assertions.assertEquals(metaInfo, tskvMap.get("meta"));
    }

    @Test
    public void shouldWriteToOrderEventLogIfPushFailed() {
        String metaInfo = "ololo";

        Parameters parameters = new Parameters();
        parameters.setCheckOrderCreateErrors(false);
        parameters.setMockPushApi(false);
        parameters.setMetaInfo(metaInfo);

        pushApiConfigurer.mockCart(parameters.getOrder(), parameters.getPushApiDeliveryResponses(), false);
        pushApiConfigurer.mockAcceptFailure(parameters.getOrder());

        Order order = orderCreateHelper.createMultiOrder(parameters).getOrderFailures().get(0).getOrder();
        order = orderService.getOrder(order.getId());

        Assertions.assertEquals(OrderStatus.PLACING, order.getStatus());

        List<Map<String, String>> logs = appender.getTskvMaps();

        assertThat(logs, hasSize(1));

        Map<String, String> tskvMap = Iterables.getOnlyElement(logs);

        Assertions.assertEquals(String.valueOf(order.getId()), tskvMap.get("order_id"));
        Assertions.assertEquals(String.valueOf(order.getBuyer().getUid()), tskvMap.get("uid"));
        Assertions.assertEquals(String.valueOf(order.getShopId()), tskvMap.get("shop_id"));
        Assertions.assertEquals(metaInfo, tskvMap.get("meta"));
    }
}
