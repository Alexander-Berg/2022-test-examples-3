package ru.yandex.travel.orders.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.orders.entities.NotifierStateChange;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class NotifierStateChangeRepositoryTest {
    @Autowired
    private NotifierStateChangeRepository notifierStateChangeRepository;

    @Test
    public void testGetOrdersWaitingStateRefresh() {
        assertThat(notifierStateChangeRepository.getOrdersWaitingStateRefresh(
                NotifierStateChangeRepository.NO_EXCLUDE_IDS,
                Instant.now(),
                10
        )).isEmpty();

        var uuid = UUID.randomUUID();
        var stateChange = NotifierStateChange.createNotifierStateChange(uuid);
        stateChange.setCreatedAt(Instant.now().minus(Duration.ofMinutes(5)));
        notifierStateChangeRepository.save(stateChange);

        assertThat(notifierStateChangeRepository.getOrdersWaitingStateRefresh(
                NotifierStateChangeRepository.NO_EXCLUDE_IDS,
                Instant.now().minus(Duration.ofMinutes(6)),
                10
        )).isEmpty();
        assertThat(notifierStateChangeRepository.getOrdersWaitingStateRefresh(
                NotifierStateChangeRepository.NO_EXCLUDE_IDS,
                Instant.now(),
                10
        )).isNotEmpty();
        assertThat(notifierStateChangeRepository.getOrdersWaitingStateRefresh(
                Set.of(uuid),
                Instant.now(),
                10
        )).isEmpty();
    }

    @Test
    public void testCountOrdersWaitingStateRefresh() {
        assertThat(notifierStateChangeRepository.countOrdersWaitingStateRefresh(
                NotifierStateChangeRepository.NO_EXCLUDE_IDS,
                Instant.now()
        )).isZero();

        var uuid1 = UUID.randomUUID();
        var stateChange = NotifierStateChange.createNotifierStateChange(uuid1);
        stateChange.setCreatedAt(Instant.now().minus(Duration.ofMinutes(10)));
        notifierStateChangeRepository.save(stateChange);

        assertThat(notifierStateChangeRepository.countOrdersWaitingStateRefresh(
                NotifierStateChangeRepository.NO_EXCLUDE_IDS,
                Instant.now()
        )).isOne();
        assertThat(notifierStateChangeRepository.countOrdersWaitingStateRefresh(
                NotifierStateChangeRepository.NO_EXCLUDE_IDS,
                Instant.now().minus(Duration.ofMinutes(11))
        )).isZero();

        stateChange = NotifierStateChange.createNotifierStateChange(uuid1);
        stateChange.setCreatedAt(Instant.now().minus(Duration.ofMinutes(8)));
        notifierStateChangeRepository.save(stateChange);

        assertThat(notifierStateChangeRepository.countOrdersWaitingStateRefresh(
                NotifierStateChangeRepository.NO_EXCLUDE_IDS,
                Instant.now()
        )).isOne();
        assertThat(notifierStateChangeRepository.countOrdersWaitingStateRefresh(
                NotifierStateChangeRepository.NO_EXCLUDE_IDS,
                Instant.now().minus(Duration.ofMinutes(9))
        )).isOne();
        assertThat(notifierStateChangeRepository.countOrdersWaitingStateRefresh(
                NotifierStateChangeRepository.NO_EXCLUDE_IDS,
                Instant.now().minus(Duration.ofMinutes(11))
        )).isZero();


        var uuid2 = UUID.randomUUID();
        stateChange = NotifierStateChange.createNotifierStateChange(uuid2);
        stateChange.setCreatedAt(Instant.now().minus(Duration.ofMinutes(6)));
        notifierStateChangeRepository.save(stateChange);

        assertThat(notifierStateChangeRepository.countOrdersWaitingStateRefresh(
                NotifierStateChangeRepository.NO_EXCLUDE_IDS,
                Instant.now()
        )).isEqualTo(2);
        assertThat(notifierStateChangeRepository.countOrdersWaitingStateRefresh(
                NotifierStateChangeRepository.NO_EXCLUDE_IDS,
                Instant.now().minus(Duration.ofMinutes(7))
        )).isOne();
        assertThat(notifierStateChangeRepository.countOrdersWaitingStateRefresh(
                NotifierStateChangeRepository.NO_EXCLUDE_IDS,
                Instant.now().minus(Duration.ofMinutes(9))
        )).isOne();
        assertThat(notifierStateChangeRepository.countOrdersWaitingStateRefresh(
                NotifierStateChangeRepository.NO_EXCLUDE_IDS,
                Instant.now().minus(Duration.ofMinutes(11))
        )).isZero();
    }

    @Test
    public void testFindMaxIdForOrder() {
        var uuid1 = UUID.randomUUID();
        assertThat(notifierStateChangeRepository.findMaxIdForOrder(uuid1)).isNull();

        var stateChange = NotifierStateChange.createNotifierStateChange(uuid1);
        notifierStateChangeRepository.save(stateChange);
        assertThat(notifierStateChangeRepository.findMaxIdForOrder(uuid1)).isEqualTo(101L);

        var uuid2 = UUID.randomUUID();
        stateChange = NotifierStateChange.createNotifierStateChange(uuid2);
        notifierStateChangeRepository.save(stateChange);
        assertThat(notifierStateChangeRepository.findMaxIdForOrder(uuid1)).isEqualTo(101L);

        stateChange = NotifierStateChange.createNotifierStateChange(uuid1);
        notifierStateChangeRepository.save(stateChange);
        assertThat(notifierStateChangeRepository.findMaxIdForOrder(uuid1)).isEqualTo(103L);
    }

    @Test
    public void testCleanupByOrderIdAndIdLessThanEqual() {
        var uuid1 = UUID.randomUUID();
        var stateChange = NotifierStateChange.createNotifierStateChange(uuid1);
        notifierStateChangeRepository.save(stateChange);
        stateChange = NotifierStateChange.createNotifierStateChange(uuid1);
        notifierStateChangeRepository.save(stateChange);
        stateChange = NotifierStateChange.createNotifierStateChange(uuid1);
        notifierStateChangeRepository.save(stateChange);

        var uuid2 = UUID.randomUUID();
        stateChange = NotifierStateChange.createNotifierStateChange(uuid2);
        notifierStateChangeRepository.save(stateChange);
        stateChange = NotifierStateChange.createNotifierStateChange(uuid2);
        notifierStateChangeRepository.save(stateChange);


        assertThat(notifierStateChangeRepository.count()).isEqualTo(5);

        notifierStateChangeRepository.cleanupByOrderIdAndIdLessThanEqual(uuid1, 0L);

        assertThat(notifierStateChangeRepository.count()).isEqualTo(5);

        notifierStateChangeRepository.cleanupByOrderIdAndIdLessThanEqual(
                uuid1,
                notifierStateChangeRepository.findMaxIdForOrder(uuid1)
        );

        assertThat(notifierStateChangeRepository.count()).isEqualTo(2);

        notifierStateChangeRepository.cleanupByOrderIdAndIdLessThanEqual(
                uuid2,
                notifierStateChangeRepository.findMaxIdForOrder(uuid2)
        );

        assertThat(notifierStateChangeRepository.count()).isEqualTo(0);
    }
}
