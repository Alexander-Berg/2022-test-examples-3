package ru.yandex.search.document.mail;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.stream.RawField;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.function.FalsePredicate;
import ru.yandex.test.util.TestBase;

public class MailMetaInfoTest extends TestBase {
    private static final String EMAIL = "dpotapov@yandex-team.ru";
    private static final String HDR_CC = "hdr_cc";
    private static final String TIMEMARK = "100500";
    private static final String RECEIVED = "123456789";
    private static final String RECEIVED_DATE = "received_date";

    @Test
    public void testCopyAndRemove() throws MimeException {
        MailMetaInfo meta = new MailMetaInfo(-1, -1, FalsePredicate.INSTANCE);
        meta.add(new RawField("To", EMAIL));
        meta.add(new RawField("Cc", EMAIL + ' ' + EMAIL));

        MailMetaInfo copy = new MailMetaInfo(meta);

        Assert.assertEquals(EMAIL + ' ' + EMAIL, meta.get(HDR_CC));
        Assert.assertEquals(EMAIL + ' ' + EMAIL, copy.get(HDR_CC));
    }

    @Test
    public void testTimeMark() throws MimeException {
        MailMetaInfo meta = new MailMetaInfo(-1, -1, FalsePredicate.INSTANCE);
        meta.add(new RawField("X-Yandex-TimeMark", TIMEMARK));
        Assert.assertEquals(TIMEMARK, meta.get(RECEIVED_DATE));
        meta.add(new RawField("X-Yandex-Received", RECEIVED));
        Assert.assertEquals(RECEIVED, meta.get(RECEIVED_DATE));
        meta = new MailMetaInfo(-1, -1, FalsePredicate.INSTANCE);
        meta.add(new RawField("X-Yandex-received", RECEIVED));
        Assert.assertEquals(RECEIVED, meta.get(RECEIVED_DATE));
        meta.add(new RawField("X-Yandex-Timemark", TIMEMARK));
        Assert.assertEquals(RECEIVED, meta.get(RECEIVED_DATE));
    }
}

