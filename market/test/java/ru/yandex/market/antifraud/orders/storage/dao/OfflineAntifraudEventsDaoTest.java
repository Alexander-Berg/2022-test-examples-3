package ru.yandex.market.antifraud.orders.storage.dao;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.storage.entity.xurma.OrderCancelEvent;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
public class OfflineAntifraudEventsDaoTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    private OfflineAntifraudEventsDao offlineAntifraudEventsDao;

    @Before
    public void init() {
        offlineAntifraudEventsDao = new OfflineAntifraudEventsDao(jdbcTemplate);
    }

    @Test
    public void findCancelsByOrderId() {
        OrderCancelEvent o1 = offlineAntifraudEventsDao.save(OrderCancelEvent.builder().orderId(123L).puid(23L).build());
        OrderCancelEvent o2 = offlineAntifraudEventsDao.save(OrderCancelEvent.builder().orderId(124L).puid(23L).build());

        List<OrderCancelEvent> entities = offlineAntifraudEventsDao.findEntriesByOrderId(123L);
        assertThat(entities).contains(o1);
        assertThat(entities).doesNotContain(o2);
    }


    @Test
    public void findEntries() {
        OrderCancelEvent o1 = offlineAntifraudEventsDao.save(OrderCancelEvent.builder().orderId(124L).eventName("ban_user").ruleName("rule_1").puid(23L).build());
        OrderCancelEvent o2 = offlineAntifraudEventsDao.save(OrderCancelEvent.builder().orderId(124L).eventName("cancel_order").ruleName("rule_1").puid(23L).build());

        List<OrderCancelEvent> entities = offlineAntifraudEventsDao.findEntries(124L, "rule_1", "cancel_order");
        assertThat(entities).contains(o2);
        assertThat(entities).doesNotContain(o1);
    }
}
