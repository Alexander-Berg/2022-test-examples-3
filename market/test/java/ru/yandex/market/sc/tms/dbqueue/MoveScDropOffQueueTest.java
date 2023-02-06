package ru.yandex.market.sc.tms.dbqueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;

import ru.yandex.market.sc.core.dbqueue.ScQueueType;
import ru.yandex.market.sc.core.dbqueue.dropoff.MoveScDropOffProducer;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.external.telegram.TelegramNotificationService;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTmsTest
public class MoveScDropOffQueueTest {

    private static final long OLD_SC_ID = 1001L;
    private static final long NEW_SC_ID = 1002L;
    private static final String MESSAGE = "Заказы из %s (%s) перенесены в %s (%s)";
    private static final String ERROR_MESSAGE = "Ошибка переноса заказов из %s (%s) в %s (%s): ";

    @Autowired
    TestFactory testFactory;

    @Autowired
    DbQueueTestUtil dbQueueTestUtil;

    @Autowired
    MoveScDropOffProducer moveScDropOffProducer;

    @Autowired
    ScOrderRepository scOrderRepository;

    @SpyBean
    private TelegramNotificationService telegramNotificationService;

    @SpyBean
    private JdbcOperations jdbcOperations;

    SortingCenter oldSortingCenter;
    SortingCenter newSortingCenter;

    @BeforeEach
    void init() {
        oldSortingCenter = testFactory.storedSortingCenter(OLD_SC_ID);
        newSortingCenter = testFactory.storedSortingCenter(NEW_SC_ID);
        oldSortingCenter.setYandexId(String.valueOf(OLD_SC_ID));
        newSortingCenter.setYandexId(String.valueOf(NEW_SC_ID));
    }

    @Test
    void moveScTest() {
        var order1 = testFactory.create(order(oldSortingCenter).externalId("12775551-YD4897759").build()).get();
        var order2 = testFactory.create(order(newSortingCenter).externalId("12775551-YD4897760").build()).get();

        assertThat(order1.getSortingCenter()).isEqualTo(oldSortingCenter);
        assertThat(order2.getSortingCenter()).isEqualTo(newSortingCenter);

        moveScDropOffProducer.produce(oldSortingCenter, newSortingCenter);

        dbQueueTestUtil.executeSingleQueueItem(ScQueueType.MOVE_SC_DROP_OFF);

        order1 = scOrderRepository.findByIdOrThrow(order1.getId());
        order2 = scOrderRepository.findByIdOrThrow(order2.getId());

        assertThat(order1.getSortingCenter()).isEqualTo(newSortingCenter);
        assertThat(order2.getSortingCenter()).isEqualTo(newSortingCenter);

        verify(telegramNotificationService, times(1))
                .send(anyString(), eq(messageForTelegram(MESSAGE, oldSortingCenter, newSortingCenter)));
    }

    @Test
    void moveScFailTest() {
        moveScDropOffProducer.produce(oldSortingCenter, newSortingCenter);

        doThrow(new DataAccessException("") {
        })
                .when(jdbcOperations).execute(anyString(), any(PreparedStatementCallback.class));

        dbQueueTestUtil.executeSingleQueueItem(ScQueueType.MOVE_SC_DROP_OFF);

        verify(telegramNotificationService, times(1))
                .send(anyString(), eq(messageForTelegram(ERROR_MESSAGE, oldSortingCenter, newSortingCenter)));
    }

    private String messageForTelegram(String template, SortingCenter oldSortingCenter, SortingCenter newSortingCenter) {
        return String.format(
                template,
                oldSortingCenter.getPartnerId(),
                oldSortingCenter.getPartnerName(),
                newSortingCenter.getPartnerId(),
                newSortingCenter.getPartnerName()
        );
    }
}
