package ru.yandex.market.notification.safe.task.job.misc;

import org.junit.Test;

import ru.yandex.market.notification.safe.model.vo.SendResultInfo;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link SendResultInfoFetcher}.
 *
 * @author Vladislav Bauer
 */
public class SendResultInfoFetcherTest {

    @Test
    public void testProcessPositive() {
        final SendResultInfo expected = SendResultInfo.empty();
        final SendResultInfo actual = SendResultInfoFetcher.process(
            () -> expected,
            () -> null
        );

        assertThat(expected, equalTo(actual));
    }

    @Test
    public void testProcessNegative() {
        final String errorMessage = "I am error message";
        final SendResultInfo actual = SendResultInfoFetcher.process(
            () -> { throw new RuntimeException(errorMessage); },
            () -> "I am log message"
        );

        assertThat(actual.hasError(), equalTo(true));
        assertThat(actual.getAllErrors(), hasSize(1));
        assertThat(actual.getAllErrors(), contains(errorMessage));
    }

}
