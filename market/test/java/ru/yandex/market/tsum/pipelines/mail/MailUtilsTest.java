package ru.yandex.market.tsum.pipelines.mail;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MailUtilsTest {
    private static final ZonedDateTime ZDT = ZonedDateTime.of(2020, 01, 16, 23, 30, 18, 0, ZoneId.of("Europe/Moscow"));

    @Test
    public void makeVersionSimple() {
        assertEquals("20200116-2330.r100-42.my_topic", MailUtils.makeVersion(ZDT, 100, 42, "my_topic"));
    }

    @Test
    public void makeVersionNoReviewNoTopic() {
        assertEquals("20200116-2330.r100", MailUtils.makeVersion(ZDT, 100, 0, ""));
    }

    @Test
    public void makeVersionNoReview() {
        assertEquals("20200116-2330.r100.my_topic", MailUtils.makeVersion(ZDT, 100, 0, "my_topic"));
    }

    @Test
    public void makeVersionEmptyTopic() {
        assertEquals("20200116-2330.r100-42", MailUtils.makeVersion(ZDT, 100, 42, ""));
    }

    @Test
    public void getQueueSimple() {
        assertEquals("MAILDEV", MailUtils.getQueue("MAILDEV-1234"));
    }

    @Test
    public void getQueueLowercase() {
        assertEquals("maildev", MailUtils.getQueue("maildev-1234"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getQueueNoHyphen() {
        MailUtils.getQueue("MAILDEV_1234");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getQueueEmpty() {
        MailUtils.getQueue("-1234");
    }
}
