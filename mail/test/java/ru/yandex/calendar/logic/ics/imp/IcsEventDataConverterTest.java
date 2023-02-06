package ru.yandex.calendar.logic.ics.imp;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Rdate;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsComponent;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVAlarm;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsRelated;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsTzId;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.*;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.Freq;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.IcsRecur;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.type.IcsRecurRulePartByDay;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.time.TimeUtils;

/**
 * @author Stepan Koltsov
 */
public class IcsEventDataConverterTest {

    @Test
    public void convertExdates() {
        DateTimeZone tz = DateTimeZone.forID("US/Central");
        IcsExDate icsExDate = new IcsExDate("20030406T100000", Cf.list(new IcsTzId("US/Central")));

        IcsDtStart icsDtStart = new IcsDtStart("20030406T100000Z");
        IcsDtEnd icsDtEnd = new IcsDtEnd("20030406T100000Z");

        IcsVEvent icsVEvent = new IcsVEvent(Cf.<IcsProperty>list(icsExDate, icsDtStart, icsDtEnd), Cf.<IcsComponent>list());
        EventData data = IcsEventDataConverter.convert(IcsVTimeZones.fallback(MoscowTime.TZ), icsVEvent, true);
        ListF<Rdate> rdates = data.getRdates();
        Assert.A.hasSize(1, rdates);
        Rdate rdate = rdates.single();
        DateTimeFormatter p = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss").withZone(tz);
        Assert.A.equals(p.parseDateTime("20030406T100000").toInstant(), rdate.getStartTs());
    }

    @Test
    public void convertNotifications() {
        Instant eventStart = TestDateTimes.utc(2012, 12, 20, 12, 0);
        IcsDtStart icsDtStart = new IcsDtStart(eventStart);
        IcsDtEnd icsDtEnd = new IcsDtEnd(eventStart.plus(Duration.standardHours(1)));

        IcsVAlarm icsVAlarm = new IcsVAlarm();
        icsVAlarm = icsVAlarm.withAction(IcsAction.DISPLAY);

        icsVAlarm = icsVAlarm.withTrigger(IcsTrigger.createDuration("-PT15M", false, Option.of(IcsRelated.END)));
        IcsVEvent icsVEvent = new IcsVEvent(Cf.<IcsProperty>list(icsDtStart, icsDtEnd), Cf.<IcsComponent>list(icsVAlarm));
        EventData eventData = IcsEventDataConverter.convert(IcsVTimeZones.fallback(DateTimeZone.UTC), icsVEvent, false);

        ListF<Notification> notifications = eventData.getEventUserData().getNotifications();
        Assert.equals(45L, notifications.single().getOffset().getStandardMinutes());

        icsVAlarm = icsVAlarm.withTrigger(IcsTrigger.createDuration("PT20M", false, Option.of(IcsRelated.START)));
        icsVEvent = new IcsVEvent(Cf.<IcsProperty>list(icsDtStart, icsDtEnd), Cf.<IcsComponent>list(icsVAlarm));
        eventData = IcsEventDataConverter.convert(IcsVTimeZones.fallback(DateTimeZone.UTC), icsVEvent, false);

        notifications = eventData.getEventUserData().getNotifications();
        Assert.equals(20L, notifications.single().getOffset().getStandardMinutes());

        icsVAlarm = icsVAlarm.withTrigger(IcsTrigger.createDateTime(eventStart.minus(Duration.standardMinutes(30))));
        icsVEvent = new IcsVEvent(Cf.<IcsProperty>list(icsDtStart, icsDtEnd), Cf.<IcsComponent>list(icsVAlarm));
        eventData = IcsEventDataConverter.convert(IcsVTimeZones.fallback(DateTimeZone.UTC), icsVEvent, false);

        notifications = eventData.getEventUserData().getNotifications();
        Assert.equals(-30L, notifications.single().getOffset().getStandardMinutes());
    }

    @Test
    public void convertRruleMonthlyCount() {
        IcsRRule icsRrule = new IcsRRule("FREQ=MONTHLY;COUNT=3");
        Repetition rrule = IcsEventDataConverter.convertRrule(
                TestDateTimes.moscow(2010, 12, 22, 17, 10), icsRrule, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);

        Assert.A.equals(RegularRepetitionRule.MONTHLY_NUMBER, rrule.getType());
        Assert.A.some(TestDateTimes.moscow(2011, 2, 23, 17, 10), rrule.getDueTs());
    }

    @Test
    public void convertRruleDailyCount() {
        IcsRRule icsRrule = new IcsRRule("FREQ=DAILY;COUNT=3");
        Repetition rrule = IcsEventDataConverter.convertRrule(
                TestDateTimes.moscow(2011, 11, 11, 11, 11), icsRrule, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);

        Assert.A.equals(RegularRepetitionRule.DAILY, rrule.getType());
        Assert.A.some(TestDateTimes.moscow(2011, 11, 14, 11, 11), rrule.getDueTs());
    }

    @Test
    public void convertRruleMonthlyCountByMonthDay() {
        IcsRRule icsRrule = new IcsRRule("FREQ=MONTHLY;COUNT=3;BYMONTHDAY=22");
        Repetition rrule = IcsEventDataConverter.convertRrule(
                TestDateTimes.moscow(2010, 12, 22, 17, 10), icsRrule, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);

        Assert.A.equals(RegularRepetitionRule.MONTHLY_NUMBER, rrule.getType());
        Assert.A.some(TestDateTimes.moscow(2011, 2, 23, 17, 10), rrule.getDueTs());
    }

    @Test
    public void convertNonMondayStartedWeekdayEvent() {
        IcsVEvent event = new IcsVEvent();
        IcsRecur recur = new IcsRecur(Freq.WEEKLY).withPart(new IcsRecurRulePartByDay("MO,TU,WE,TH,FR"));
        event = event.withProperties(Cf.<IcsProperty>list(new IcsRRule(recur)));

        event = event.withDtStart(TestDateTimes.moscow(2011, 11, 11, 11, 11)); // this is friday
        event = event.withDtEnd(TestDateTimes.moscow(2011, 11, 11, 12, 12));
        IcsEventDataConverter.convert(IcsVTimeZones.fallback(MoscowTime.TZ), event, true);
    }

    @Test
    public void convertConferenceUrl() {
        IcsDtStart icsDtStart = new IcsDtStart("20030406T100000Z");
        IcsDtEnd icsDtEnd = new IcsDtEnd("20030406T100000Z");
        IcsConference icsConference = new IcsConference("http://url", Cf.list());


        IcsVEvent icsVEvent = new IcsVEvent(Cf.<IcsProperty>list(icsDtStart, icsDtEnd, icsConference), Cf.<IcsComponent>list());
        EventData data = IcsEventDataConverter.convert(IcsVTimeZones.fallback(MoscowTime.TZ), icsVEvent, true);
        Assert.A.equals(data.getEvent().getConferenceUrl().getOrNull(), "http://url");
    }
} //~
