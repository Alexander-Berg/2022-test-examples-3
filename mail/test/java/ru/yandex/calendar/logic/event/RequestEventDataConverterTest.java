package ru.yandex.calendar.logic.event;

import java.util.Map;

import org.joda.time.DateTimeZone;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.logic.beans.generated.EventAttachment;
import ru.yandex.calendar.logic.beans.generated.EventFields;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.util.data.AliasedMapDataProvider;
import ru.yandex.calendar.util.data.DataProvider;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.TestBase;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author akirakozov
 */
public class RequestEventDataConverterTest extends TestBase {

    @Test
    public void convertEventDataOnUpdateLite() {
        Map<String, String> map = Cf.hashMap();
        map.put("id", "1");
        map.put("start_ts", "2011-03-04T19:00:00");
        map.put("end_ts", "2011-03-04T20:00:00");
        map.put("is_all_day", "0");
        map.put("apply_to_future_events", "0");
        DataProvider eDp = new AliasedMapDataProvider(map);
        EventData eventData = RequestEventDataConverter.convert(MoscowTime.TZ, MoscowTime.TZ, eDp, null);

        // test this situation: https://jira.yandex-team.ru/browse/CAL-2859
        Assert.A.isTrue(!eventData.getEvent().isFieldSet(EventFields.RECURRENCE_ID));
    }

    /**
     * @see AbstractEventDataConverter#moveStartToDayOfRepetitionIfNeeded
     */
    @Test
    public void moveStartToRepetitionDayThisWeek() {
        Map<String, String> map = Cf.hashMap();
        map.put("is_all_day", "0");
        map.put("r", "1");
        map.put("r_type", "weekly");
        map.put("r_r_weekly_days", "mon,sat");
        map.put("start_ts", "2012-03-20T19:00:00"); // tue
        map.put("end_ts", "2012-03-20T20:00:00");
        DataProvider eDp = new AliasedMapDataProvider(map);
        EventData eventData = RequestEventDataConverter.convert(MoscowTime.TZ, MoscowTime.TZ, eDp,  null);

        Assert.equals(MoscowTime.instant(2012, 3, 24, 19, 0), eventData.getEvent().getStartTs());
        Assert.equals(MoscowTime.instant(2012, 3, 24, 20, 0), eventData.getEvent().getEndTs());
    }

    @Test
    public void moveStartToRepetitionDayNextWeek() {
        Map<String, String> map = Cf.hashMap();
        map.put("is_all_day", "0");
        map.put("r", "1");
        map.put("r_type", "weekly");
        map.put("r_r_weekly_days", "mon,tue,wed");
        map.put("start_ts", "2012-03-23T19:00:00"); // fri
        map.put("end_ts", "2012-03-23T20:00:00");

        DataProvider eDp = new AliasedMapDataProvider(map);
        EventData eventData = RequestEventDataConverter.convert(MoscowTime.TZ, MoscowTime.TZ, eDp,  null);

        Assert.equals(MoscowTime.instant(2012, 3, 26, 19, 0), eventData.getEvent().getStartTs());
        Assert.equals(MoscowTime.instant(2012, 3, 26, 20, 0), eventData.getEvent().getEndTs());
    }

    @Test
    public void convertEventAttachments() {
        Map<String, String> map = Cf.hashMap();
        map.put("start_ts", "2012-12-20");
        map.put("end_ts", "2012-12-21");
        map.put("is_all_day", "1");

        DataProvider eDp = new AliasedMapDataProvider(map);
        EventData eventData = RequestEventDataConverter.convert(DateTimeZone.UTC, DateTimeZone.UTC, eDp, null);
        Assert.none(eventData.getAttachmentsO());

        map.put("as", "1");
        map.put("as_a[0]", "1");
        map.put("as_a[0]_id", "AAA");
        map.put("as_a[0]_filename", "aaa");
        map.put("as_a[1]", "1");
        map.put("as_a[1]_id", "BBB");
        map.put("as_a[1]_filename", "bbb");

        eventData = RequestEventDataConverter.convert(DateTimeZone.UTC, DateTimeZone.UTC, eDp, null);
        Assert.notEmpty(eventData.getAttachmentsO());
        Assert.equals(Cf.list("AAA", "BBB"), eventData.getAttachmentsO().get().map(EventAttachment.getUrlF()));
    }
}

