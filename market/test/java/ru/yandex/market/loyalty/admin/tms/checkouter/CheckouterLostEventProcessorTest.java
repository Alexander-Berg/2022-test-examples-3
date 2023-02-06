package ru.yandex.market.loyalty.admin.tms.checkouter;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.buildOrder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;

@TestFor(CheckouterLostEventProcessor.class)
public class CheckouterLostEventProcessorTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {

    @Autowired
    private CheckouterLostEventProcessor checkouterLostEventProcessor;
    @Autowired
    private Clock clock;
    @YtHahn
    @MockBean
    private JdbcTemplate ytJdbcTemplate;
    @Autowired
    private CheckouterClient checkouterClient;

    @Test
    public void testCheckouterLostEventImportFromYt() {
        Map<Object, OrderHistoryEvent> events = prepareEvents(0L, 300).stream()
                .collect(Collectors.toMap(
                        OrderHistoryEvent::getId,
                        Function.identity()
                ));
        configurationService.set(ConfigurationService.LOST_CHECKOUTER_EVENTS_YT_PATH, "table_path");

        Mockito.when(ytJdbcTemplate.query(any(String.class),
                any(Object[].class), any(RowMapper.class)
        )).then(invocation -> {
            Object[] params = invocation.getArgument(1, Object[].class);
            Long lastProcessedEvent = (Long) params[0];
            return events.values().stream()
                    .map(OrderHistoryEvent::getId)
                    .filter(id -> id > lastProcessedEvent)
                    .collect(Collectors.toList());
        });
        Mockito.when(checkouterClient.orderHistoryEvents().getOrderHistoryEvent(anyLong()))
                .then(invocation -> {
                    Long eventId = invocation.getArgument(0, Long.class);
                    return events.get(eventId);
                });
        checkouterLostEventProcessor.importLostEvents();
    }

    private List<OrderHistoryEvent> prepareEvents(long fromEventId, int amount) {
        return LongStream.range(fromEventId, fromEventId + amount + 1)
                .mapToObj(id -> CheckouterUtils.getEvent(
                        buildOrder(OrderStatus.PROCESSING, id, null,
                                DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                        HistoryEventType.ORDER_STATUS_UPDATED, clock))
                .peek(event -> event.setId(event.getOrderAfter().getId()))
                .collect(Collectors.toList());
    }
}
