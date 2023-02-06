package ru.yandex.calendar.logic.notification;

import org.junit.Test;

import ru.yandex.calendar.logic.notification.ControlDataNotification.CalendarNotificationType;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.inside.passport.PassportSid;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class ControlDataNotificationTest extends CalendarTestBase {
    private static final PassportSid sid = PassportSid.BALANCE;
    private static final PassportUid uid = new PassportUid(59989682L);
    private static final String externalId = "invoice_4444037";
    private static final String nowTs = "2009-11-16T15:49:39";
    private static final CalendarNotificationType notificationType = CalendarNotificationType.DELETE;
    private static final String shortParamListCheckSum = "b207601a46304646e4ea19b75a441dbc";
    private static final String fullParamListCheckSum = "00a6e210635043c0fe9921b23d864780";

    @Test
    public void isSupportedBy() {
        Assert.A.isTrue(ControlDataNotification.isSupportedBy(PassportSid.BALANCE));
        Assert.A.isFalse(ControlDataNotification.isSupportedBy(PassportSid.TV));
    }

    @Test
    public void isValid() {
        ControlDataNotification.ensureValid(sid, uid, externalId, shortParamListCheckSum);
    }

    @Test
    public void controlDataEvaluation() {
        String checkSum = ControlDataNotification.evalControlData(uid, externalId, nowTs, notificationType.toLowerCase());
        Assert.A.equals(fullParamListCheckSum, checkSum);
    }

    @Test
    public void getFullNotifitcationUrl() {
        String url = ControlDataNotification.getFullNotificationUrl("http://host.ru/xml.xml", uid, externalId,  nowTs, notificationType, fullParamListCheckSum);
        Assert.A.equals("http://host.ru/xml.xml?ntf_type=delete&uid=59989682&external_id=invoice_4444037&ntf_ts=2009-11-16T15%3A49%3A39&control_data=00a6e210635043c0fe9921b23d864780", url);
    }
}
