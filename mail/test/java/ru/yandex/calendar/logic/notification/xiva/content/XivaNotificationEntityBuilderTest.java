package ru.yandex.calendar.logic.notification.xiva.content;

import java.util.Optional;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.logic.beans.generated.Settings;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.misc.test.Assert;

import static ru.yandex.calendar.logic.notification.xiva.content.TemporaryLocalizedStrings.TODAY;
import static ru.yandex.calendar.logic.notification.xiva.content.TemporaryLocalizedStrings.TOMORROW;
import static ru.yandex.calendar.logic.notification.xiva.content.TemporaryLocalizedStrings.YESTERDAY;

public class XivaNotificationEntityBuilderTest {
    private final XivaNotificationEntityBuilder builder = new XivaNotificationEntityBuilder();

    private Settings settings;
    private final Language defaultLanguage = Language.ENGLISH;
    private final DateTime now = DateTime.now(DateTimeZone.UTC);

    @Before
    public void setUp() {
        settings = new Settings();
        settings.setTimezoneJavaid(DateTimeZone.UTC.getID());
        settings.setLanguage(defaultLanguage);
    }

    @Test
    public void formatYesterdayDate() {
        Instant yesterday = now.minusDays(1).plusHours(1).toInstant();
        String description = buildNotificationDescription(yesterday);

        Assert.assertContains(description, YESTERDAY.getName(defaultLanguage));
    }

    @Test
    public void formatTodayDate() {
        Instant today = now.toInstant();
        String description = buildNotificationDescription(today);

        Assert.assertContains(description,TODAY.getName(defaultLanguage));
    }

    @Test
    public void formatTomorrowDate() {
        Instant tomorrow = now.plusDays(1).toInstant();
        String description = buildNotificationDescription(tomorrow);

        Assert.assertContains(description, TOMORROW.getName(defaultLanguage));
    }

    @Test
    public void formatFarInThePastDate() {
        Instant farInThePast = now.minusDays(7).toInstant();
        String description = buildNotificationDescription(farInThePast);

        String dayOfWeek = TemporaryLocalizedStrings.dayOfWeek(farInThePast.getMillis(),
                settings.getTimezoneJavaid(),
                settings.getLanguage());
        String date = TemporaryLocalizedStrings.date(farInThePast.getMillis(),
                settings.getTimezoneJavaid(),
                settings.getLanguage());
        String correctFormattedDate = dayOfWeek + ", " + date;

        Assert.assertContains(description, correctFormattedDate);
    }

    @Test
    public void formatFarInTheFutureDate() {
        Instant farInTheFuture = now.plusDays(7).toInstant();
        String description = buildNotificationDescription(farInTheFuture);

        String dayOfWeek = TemporaryLocalizedStrings.dayOfWeek(farInTheFuture.getMillis(),
                settings.getTimezoneJavaid(),
                settings.getLanguage());
        String date = TemporaryLocalizedStrings.date(farInTheFuture.getMillis(),
                settings.getTimezoneJavaid(),
                settings.getLanguage());
        String correctFormattedDate = dayOfWeek + ", " + date;

        Assert.assertContains(description, correctFormattedDate);
    }

    private String buildNotificationDescription(Instant instant) {
        return builder.build(1L,
                1L,
                "test_name",
                instant,
                instant,
                XivaNotificationType.CREATED,
                settings,
                Optional.empty(),
                Cf.list()
        ).getPayload().getNotification().getDescription();
    }
}
