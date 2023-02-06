package ru.yandex.market.notification.safe.task.job.misc;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class QuadraticRetryTimeCalculatorTest {

    private RetryTimeCalculator calculator;

    @BeforeEach
    public void before() {
        calculator = new QuadraticRetryTimeCalculator();
    }

    @Test
    public void retryTimeShift() {
        assertThat(calculator.retryTimeShift(1), Matchers.equalTo(60_000L));
        assertThat(calculator.retryTimeShift(2), Matchers.equalTo(300_000L));
        assertThat(calculator.retryTimeShift(3), Matchers.equalTo(900_000L));
        assertThat(calculator.retryTimeShift(10), Matchers.equalTo(15_180_000L));
    }

}
