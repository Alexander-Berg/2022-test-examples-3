package ru.yandex.direct.core.entity.statistics.repository;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.statistics.model.FraudAndGiftClicks;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.statistics.model.FraudAndGiftClicks.ZERO;

@Ignore("Ходит в yt, для ручного запуска")
@CoreTest
@RunWith(SpringRunner.class)
public class OrderStatFraudRepositoryGetOrdersToFraudClicksSmokeTest {

    @Autowired
    private OrderStatFraudRepository orderStatFraudRepository;

    @Test
    public void getOrdersToFraudClicks_withEmptyResult() {
        Long orderId = 0L;
        FraudAndGiftClicks result = orderStatFraudRepository.getFraudClicks(
                Collections.singleton(orderId),
                LocalDate.of(2019, 9, 2),
                LocalDate.of(2019, 10, 2));

        assertThat(result).isEqualTo(ZERO);
    }

    @Test
    public void getOrdersToFraudClicks_withNonEmptyResult() {
        Long orderId = 3920202L;
        FraudAndGiftClicks result = orderStatFraudRepository.getFraudClicks(
                Collections.singleton(orderId),
                LocalDate.of(2019, 1, 24),
                LocalDate.of(2019, 1, 28));

        assertThat(result).isEqualTo(new FraudAndGiftClicks(1785L, 1L, 0L, 0L));
    }

    @Test
    public void getOrdersToFraudClicks_oneDay() {
        Long orderId = 3920202L;
        FraudAndGiftClicks result = orderStatFraudRepository.getFraudClicks(
                Collections.singleton(orderId),
                LocalDate.of(2019, 1, 24),
                LocalDate.of(2019, 1, 24));

        assertThat(result).isEqualTo(new FraudAndGiftClicks(483L, 0L, 0L, 0L));
    }

    @Test
    public void getOrdersToFraudClicks_oldDayStat() {
        Long orderId = 198151L;
        FraudAndGiftClicks result = orderStatFraudRepository.getFraudClicks(
                Collections.singleton(orderId),
                LocalDate.of(2010, 2, 3),
                LocalDate.of(2010, 2, 3));
        assertThat(result).isEqualTo(new FraudAndGiftClicks(431L, 69L, 0L, 0L));
    }

    @Test
    public void getOrdersToFraudClicks_fraudShows() {
        Long orderId = 19781031L;
        FraudAndGiftClicks result = orderStatFraudRepository.getFraudClicks(
                Collections.singleton(orderId),
                LocalDate.of(2020, 1, 15),
                LocalDate.of(2020, 2, 7));
        assertThat(result).isEqualTo(new FraudAndGiftClicks(5L, 1L, 1846L, 495L));
    }
}
