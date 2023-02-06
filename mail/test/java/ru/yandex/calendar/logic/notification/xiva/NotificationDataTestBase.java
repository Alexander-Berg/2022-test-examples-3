package ru.yandex.calendar.logic.notification.xiva;

import javax.annotation.PostConstruct;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;

abstract class NotificationDataTestBase extends AbstractConfTest {
    @Autowired
    protected TestEventManager eventManager;

    protected DateTime dayInPast;
    protected DateTime dayCloserInPast;
    protected DateTime dayInFutureInsideZone;
    protected DateTime dayFurtherInFutureInsideZone;
    protected DateTime dayInFutureOutsideZone;
    protected DateTime dayFurtherInFutureOutsideZone;
    protected Repetition weeklyRepetitionWithOccurenceInZone;
    protected Repetition weeklyRepetitionWithoutOccurenceInZone;
    protected Repetition dailyRepetitionWithoutDueTs;
    protected Repetition dailyRepetitionWithDueTsInFuture;
    protected Repetition dailyRepetitionWithDueTsInPast;

    private final ListF<String> daysOfWeek = Cf.list("mon", "tue", "wed", "thu", "fri", "sat", "sun");

    @PostConstruct
    public void setupTestData() {
        Instant now = Instant.now();
        long secondsAndMillis = now.getMillis() % (60 * 1000);
        DateTime nowWithoutSecondAndMillis = now.minus(secondsAndMillis).toDateTime(eventManager.defaultTimeZone);

        dayInPast = nowWithoutSecondAndMillis.minusWeeks(1).plusHours(1);
        dayCloserInPast = dayInPast.plusDays(1);
        dayInFutureInsideZone = nowWithoutSecondAndMillis.plusHours(1);
        dayFurtherInFutureInsideZone = nowWithoutSecondAndMillis.plusHours(2);
        dayInFutureOutsideZone = nowWithoutSecondAndMillis
                .plusDays(XivaNotificationNeedSendChecker.DEFAULT_ALLOWABLE_OFFSET_IN_DAYS + 1)
                .plusHours(1);
        dayFurtherInFutureOutsideZone = nowWithoutSecondAndMillis
                .plusDays(XivaNotificationNeedSendChecker.DEFAULT_ALLOWABLE_OFFSET_IN_DAYS + 1)
                .plusHours(2);

        int dayOfWeekNow = now.toDateTime(eventManager.defaultTimeZone).getDayOfWeek() - 1;
        int dayOfWeekInsideZone = (dayOfWeekNow + 1) % 7;
        int dayOfWeekOutsideZone = (dayOfWeekNow + XivaNotificationNeedSendChecker.DEFAULT_ALLOWABLE_OFFSET_IN_DAYS + 1) % 7;
        weeklyRepetitionWithOccurenceInZone = TestManager.createWeeklyRepetition(daysOfWeek.get(dayOfWeekInsideZone));
        weeklyRepetitionWithoutOccurenceInZone = TestManager.createWeeklyRepetition(daysOfWeek.get(dayOfWeekOutsideZone));

        dailyRepetitionWithoutDueTs = TestManager.createDailyRepetitionTemplate();

        dailyRepetitionWithDueTsInFuture = TestManager.createDailyRepetitionTemplate();
        dailyRepetitionWithDueTsInFuture.setDueTs(dayFurtherInFutureOutsideZone.toInstant());

        dailyRepetitionWithDueTsInPast = TestManager.createDailyRepetitionTemplate();
        dailyRepetitionWithDueTsInPast.setDueTs(dayCloserInPast.toInstant());
    }
}
