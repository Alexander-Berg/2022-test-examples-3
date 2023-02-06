package ru.yandex.reminders.logic.flight.shift;

import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.misc.test.Assert;

/**
 * @author Eugene Voytitsky
 */
public class SendingTimeUtilsTest {

    private static final DateTimeZone TZ = DateTimeZone.forID("Europe/Moscow");
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withZone(TZ);

    // tests for https://jira.yandex-team.ru/browse/DARIA-28984?focusedCommentId=4515924

    @Test
    public void calcFlightShiftSmsSendTs1() {
        for (String time : Cf.list("2013-11-01 08:01:00", "2013-11-01 13:30:00", "2013-11-01 22:59:00")) {
            Instant now = parse(time);

            Assert.equals(now,
                    SendingTimeUtils.calcFlightShiftSmsSendTs(now, parse("2013-11-01 17:00:00"), TZ));
            Assert.equals(now,
                    SendingTimeUtils.calcFlightShiftSmsSendTs(now, parse("2013-11-02 03:00:00"), TZ));
        }
    }

    @Test
    public void calcFlightShiftSmsSendTs2() {
        for (String time : Cf.list("2013-11-01 23:01:00", "2013-11-01 23:59:00",
                "2013-11-02 00:01:00", "2013-11-02 07:59:00"))
        {
            Instant now = parse(time);

            Assert.equals(now,
                    SendingTimeUtils.calcFlightShiftSmsSendTs(now, parse("2013-11-02 07:30:00"), TZ));
            Assert.equals(now,
                    SendingTimeUtils.calcFlightShiftSmsSendTs(now, parse("2013-11-02 11:00:00"), TZ));
        }
    }

    @Test
    public void calcFlightShiftSmsSendTs3() {
        for (String time : Cf.list("2013-11-01 23:01:00", "2013-11-01 23:59:00",
                "2013-11-02 00:01:00", "2013-11-02 07:59:00"))
        {
            Instant now = parse(time);

            Assert.equals(parse("2013-11-02 08:00:00"),
                    SendingTimeUtils.calcFlightShiftSmsSendTs(now, parse("2013-11-02 13:00:00"), TZ));
            Assert.equals(parse("2013-11-02 08:00:00"),
                    SendingTimeUtils.calcFlightShiftSmsSendTs(now, parse("2013-11-03 06:00:00"), TZ));
            Assert.equals(now,
                    SendingTimeUtils.calcFlightShiftSmsSendTs(now, parse("2013-11-01 06:00:00"), TZ));
        }
    }


    // tests for delaying sms sendTs

    @Test
    public void delayFlightShiftSmsSendTs1() {
        Instant sendTs = parse("2013-11-01 10:00:00");

        Instant expectedSendTs = parse("2013-11-01 10:30:00");

        Assert.equals(expectedSendTs,
                SendingTimeUtils.delayFlightShiftSmsSendTs(
                        sendTs, parse("2013-11-01 15:00:00"), parse("2013-11-01 16:00:00"), TZ));

        Assert.equals(expectedSendTs,
                SendingTimeUtils.delayFlightShiftSmsSendTs(
                        sendTs, parse("2013-11-01 16:00:00"), parse("2013-11-01 15:00:00"), TZ));
    }

    @Test
    public void delayFlightShiftSmsSendTs2() {
        Instant sendTs = parse("2013-11-01 10:00:00");

        Instant expectedSendTs = sendTs;

        Assert.equals(expectedSendTs,
                SendingTimeUtils.delayFlightShiftSmsSendTs(
                        sendTs, parse("2013-11-01 14:59:59"), parse("2013-11-01 16:00:00"), TZ));

        Assert.equals(expectedSendTs,
                SendingTimeUtils.delayFlightShiftSmsSendTs(
                        sendTs, parse("2013-11-01 16:00:00"), parse("2013-11-01 14:59:59"), TZ));
    }

    @Test
    public void delayFlightShiftSmsSendTs3() {
        Instant sendTs = parse("2013-11-01 22:40:00");

        Instant expectedSendTs = sendTs;

        Assert.equals(expectedSendTs,
                SendingTimeUtils.delayFlightShiftSmsSendTs(
                        sendTs, parse("2013-11-02 15:00:00"), parse("2013-11-02 16:00:00"), TZ));

        Assert.equals(expectedSendTs,
                SendingTimeUtils.delayFlightShiftSmsSendTs(
                        sendTs, parse("2013-11-02 16:00:00"), parse("2013-11-02 15:00:00"), TZ));
    }

    @Test
    public void delayFlightShiftSmsSendTs4() {
        Instant sendTs = parse("2014-02-10 23:50:00");

        Instant expectedSendTs = parse("2014-02-11 00:20:00");

        Assert.equals(expectedSendTs,
                SendingTimeUtils.delayFlightShiftSmsSendTs(
                        sendTs, parse("2014-02-11 06:40:00"), parse("2014-02-11 05:40:00"), TZ));

        Assert.equals(expectedSendTs,
                SendingTimeUtils.delayFlightShiftSmsSendTs(
                        sendTs, parse("2014-02-11 05:40:00"), parse("2014-02-11 06:40:00"), TZ));
    }


    // tests for calcFlightReminderAutoSmsSendTs()

    @Test
    public void calcFlightReminderAutoSmsSendTs1() {
        LocalDateTime sendTs = parseLocal("2013-11-01 22:59:00");
        Assert.equals(sendTs, SendingTimeUtils.calcFlightReminderAutoSmsSendTs(sendTs));
    }

    @Test
    public void calcFlightReminderAutoSmsSendTs2() {
        LocalDateTime sendTs = parseLocal("2013-11-01 23:01:00");
        Assert.equals(parseLocal("2013-11-02 08:00:00"), SendingTimeUtils.calcFlightReminderAutoSmsSendTs(sendTs));
    }

    @Test
    public void calcFlightReminderAutoSmsSendTs3() {
        LocalDateTime sendTs = parseLocal("2013-11-02 00:01:00");
        Assert.equals(parseLocal("2013-11-02 08:00:00"), SendingTimeUtils.calcFlightReminderAutoSmsSendTs(sendTs));
    }

    @Test
    public void calcFlightReminderAutoSmsSendTs4() {
        LocalDateTime sendTs = parseLocal("2013-11-02 07:59:00");
        Assert.equals(parseLocal("2013-11-02 08:00:00"), SendingTimeUtils.calcFlightReminderAutoSmsSendTs(sendTs));
    }

    @Test
    public void calcFlightReminderAutoSmsSendTs5() {
        LocalDateTime sendTs = parseLocal("2013-11-02 08:01:00");
        Assert.equals(sendTs, SendingTimeUtils.calcFlightReminderAutoSmsSendTs(sendTs));
    }

    @Test
    public void thisNight() {
        Assert.equals(parse("2014-02-11 23:00:00"), SendingTimeUtils.thisNight(parse("2014-02-11 22:00:00"), TZ));
        Assert.equals(parse("2014-02-11 23:00:00"), SendingTimeUtils.thisNight(parse("2014-02-11 23:00:00"), TZ));
        Assert.equals(parse("2014-02-11 23:00:00"), SendingTimeUtils.thisNight(parse("2014-02-11 23:30:00"), TZ));
        Assert.equals(parse("2014-02-11 23:00:00"), SendingTimeUtils.thisNight(parse("2014-02-12 00:00:00"), TZ));
        Assert.equals(parse("2014-02-11 23:00:00"), SendingTimeUtils.thisNight(parse("2014-02-12 01:00:00"), TZ));
        Assert.equals(parse("2014-02-12 23:00:00"), SendingTimeUtils.thisNight(parse("2014-02-12 08:00:00"), TZ));
        Assert.equals(parse("2014-02-12 23:00:00"), SendingTimeUtils.thisNight(parse("2014-02-12 08:01:00"), TZ));
    }


    private Instant parse(String s) {
        return FORMATTER.parseDateTime(s).toInstant();
    }

    private LocalDateTime parseLocal(String s) {
        return FORMATTER.parseLocalDateTime(s);
    }
}
