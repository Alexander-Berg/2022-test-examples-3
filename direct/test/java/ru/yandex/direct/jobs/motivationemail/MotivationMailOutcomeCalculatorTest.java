package ru.yandex.direct.jobs.motivationemail;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.core.entity.motivationmail.MotivationMailNotificationType;
import ru.yandex.direct.core.entity.motivationmail.MotivationMailStats;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.motivationmail.MotivationMailStats.ClientType.DESKTOP;

class MotivationMailOutcomeCalculatorTest {
    private static final Duration DAYS6 = Duration.ofDays(6).minusHours(3);
    private static final Duration DAYS7 = Duration.ofDays(7);
    private static final Duration DAYS14 = Duration.ofDays(14).minusHours(14);
    private static final Duration DAYS15 = Duration.ofDays(15);
    private static final Duration DAYS31 = Duration.ofDays(31);

    static Object[][] testData() {
        return new Object[][]{
                // Даже если есть кампании - отправляем приветственное письмо всегда
                {null, null, 0, 0, 0, MotivationMailNotificationType.WELCOME},
                {null, null, 0, 0, 1, MotivationMailNotificationType.WELCOME},
                {null, null, 0, 1, 0, MotivationMailNotificationType.WELCOME},
                {null, null, 1, 0, 0, MotivationMailNotificationType.WELCOME},
                {null, null, 1, 0, 1, MotivationMailNotificationType.WELCOME},
                {null, null, 0, 1, 1, MotivationMailNotificationType.WELCOME},
                {null, null, 1, 1, 0, MotivationMailNotificationType.WELCOME},
                {null, null, 1, 1, 1, MotivationMailNotificationType.WELCOME},

                // пока нет кампаний - писем не пишем
                {MotivationMailNotificationType.WELCOME, null, 0, 0, 0, MotivationMailNotificationType.WELCOME},
                {MotivationMailNotificationType.WELCOME, DAYS7, 0, 0, 0, MotivationMailNotificationType.WELCOME},
                {MotivationMailNotificationType.WELCOME, DAYS15, 0, 0, 0, MotivationMailNotificationType.WELCOME},

                // появились промодерированные кампании
                {MotivationMailNotificationType.WELCOME, null, 1, 0, 0, MotivationMailNotificationType.MODERATE},
                // и принятые и промодерированные - приоритет у письма про оплату
                {MotivationMailNotificationType.WELCOME, null, 1, 1, 0, MotivationMailNotificationType.PAY},

                // появились принятые кампании
                {MotivationMailNotificationType.WELCOME, null, 0, 1, 0, MotivationMailNotificationType.PAY},
                {MotivationMailNotificationType.MODERATE, null, 0, 1, 0, MotivationMailNotificationType.PAY},
                {MotivationMailNotificationType.MODERATE_6DAY, null, 0, 1, 0, MotivationMailNotificationType.PAY},
                {MotivationMailNotificationType.MODERATE_14DAY, null, 0, 1, 0, MotivationMailNotificationType.PAY},

                {MotivationMailNotificationType.MODERATE, DAYS6, 1, 0, 0, MotivationMailNotificationType.MODERATE},
                {MotivationMailNotificationType.MODERATE, DAYS7, 1, 0, 0, MotivationMailNotificationType.MODERATE_6DAY},
                {MotivationMailNotificationType.MODERATE_6DAY, DAYS14, 1, 0, 0,
                        MotivationMailNotificationType.MODERATE_6DAY},
                {MotivationMailNotificationType.MODERATE_6DAY, DAYS15, 1, 0, 0,
                        MotivationMailNotificationType.MODERATE_14DAY},

                {MotivationMailNotificationType.PAY, DAYS6, 0, 1, 0, MotivationMailNotificationType.PAY},
                {MotivationMailNotificationType.PAY, DAYS7, 0, 1, 0, MotivationMailNotificationType.PAY_6DAY},
                {MotivationMailNotificationType.PAY_6DAY, DAYS14, 0, 1, 0, MotivationMailNotificationType.PAY_6DAY},
                {MotivationMailNotificationType.PAY_6DAY, DAYS15, 0, 1, 0, MotivationMailNotificationType.PAY_14DAY},

                // из любой нотификации при наличии оплаты - удаляем
                {MotivationMailNotificationType.WELCOME, null, 0, 0, 1, null},
                {MotivationMailNotificationType.MODERATE, null, 0, 0, 1, null},
                {MotivationMailNotificationType.MODERATE_6DAY, null, 0, 0, 1, null},
                {MotivationMailNotificationType.MODERATE_14DAY, null, 0, 0, 1, null},
                {MotivationMailNotificationType.PAY, null, 0, 0, 1, null},

                // конечная нотификация - удаляем в любом случае
                {MotivationMailNotificationType.PAY_14DAY, null, 0, 0, 0, null},
                {MotivationMailNotificationType.PAY_14DAY, DAYS31, 0, 0, 0, null},
                {MotivationMailNotificationType.PAY_14DAY, null, 0, 0, 1, null},
                {MotivationMailNotificationType.PAY_14DAY, null, 0, 1, 0, null},
                {MotivationMailNotificationType.PAY_14DAY, null, 1, 0, 0, null},
                {MotivationMailNotificationType.PAY_14DAY, null, 0, 1, 1, null},
                {MotivationMailNotificationType.PAY_14DAY, null, 1, 1, 0, null},
                {MotivationMailNotificationType.PAY_14DAY, null, 1, 0, 1, null},
                {MotivationMailNotificationType.PAY_14DAY, null, 1, 1, 1, null},

                // запись "протухла", удаляем
                {MotivationMailNotificationType.WELCOME, DAYS31, 0, 0, 0, null},
                {MotivationMailNotificationType.MODERATE, DAYS31, 1, 0, 0, null},
                {MotivationMailNotificationType.MODERATE_6DAY, DAYS31, 1, 0, 0, null},
                {MotivationMailNotificationType.MODERATE_14DAY, DAYS31, 0, 0, 0, null},
                {MotivationMailNotificationType.PAY, DAYS31, 0, 1, 0, null},
                {MotivationMailNotificationType.PAY_6DAY, DAYS31, 0, 1, 0, null},
                {MotivationMailNotificationType.PAY_14DAY, DAYS31, 0, 1, 0, null},
        };
    }

    @ParameterizedTest(name = "lastNotification = {0}, newCamps: {1}, acceptedCamps: {2}, moneyCamps: {3}")
    @MethodSource("testData")
    void do_test(
            MotivationMailNotificationType lastSentNotification,
            Duration lastNotificationInterval, int newCampsCount,
            int acceptedCampsCount,
            int moneyCampsCount,
            MotivationMailNotificationType expectedNotification)
    {
        Instant now = Instant.now();
        Clock nowClock = Clock.fixed(now, ZoneId.systemDefault());
        LocalDateTime lastNotificationTime = lastNotificationInterval == null ? null :
                LocalDateTime.ofInstant(now.minus(lastNotificationInterval), ZoneId.systemDefault());

        MotivationMailStats stats = new MotivationMailStats(0L, lastSentNotification,
                lastNotificationTime, newCampsCount, acceptedCampsCount, moneyCampsCount, DESKTOP);
        MotivationMailNotificationType actual = new MotivationMailOutcomeCalculator(nowClock).calculateOutcome(stats);

        assertThat(actual, is(expectedNotification));
    }
}
