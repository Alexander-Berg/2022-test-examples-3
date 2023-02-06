package ru.yandex.direct.ansiblejuggler.model.notifications;

import org.junit.Test;

import ru.yandex.direct.juggler.JugglerStatus;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.ansiblejuggler.Util.dumpAsString;
import static ru.yandex.direct.ansiblejuggler.Util.getLocalResource;

public class OnChangeNotificationSerializationTest {
    @Test
    public void OnStatusChangeSimpleNotification() {
        String expected = getLocalResource("notifications/OnStatusChangeNotification.yaml");
        String actual =
                dumpAsString(new OnChangeNotification(ChangeType.STATUS).withNotificationMethod(NotificationMethod.SMS)
                        .withStatus(
                                JugglerStatus.CRIT).withRecipient("ppc"));
        assertEquals(expected, actual);
    }

    @Test
    public void OnDescriptionChangeSimpleNotification() {
        String expected = getLocalResource("notifications/OnDescriptionChangeNotification.yaml");
        String actual =
                dumpAsString(
                        new OnChangeNotification(ChangeType.DESCRIPTION).withNotificationMethod(NotificationMethod.SMS)
                                .withStatus(
                                        JugglerStatus.CRIT).withRecipient("ppc"));
        assertEquals(expected, actual);
    }

    @Test
    public void OnStatusChangeComplexNotification() {
        String expected = getLocalResource("notifications/OnStatusChangeNotificationComplex.yaml");
        String actual =
                dumpAsString(new OnChangeNotification(ChangeType.STATUS)
                        .withNotificationMethod(NotificationMethod.TELEGRAM)
                        .withNotificationMethod(NotificationMethod.SMS)
                        .withNotificationMethod(NotificationMethod.EMAIL)
                        .withStatus(JugglerStatus.WARN)
                        .withStatus(JugglerStatus.CRIT)
                        .withStatus(JugglerStatus.OK)
                        .withRecipient("ppc").withRecipient("direct-admin"));
        assertEquals(expected, actual);
    }

    @Test
    public void OnDescriptionChangeComplexNotification() {
        String expected = getLocalResource("notifications/OnDescriptionChangeNotificationComplex.yaml");
        String actual =
                dumpAsString(new OnChangeNotification(ChangeType.DESCRIPTION)
                        .withNotificationMethod(NotificationMethod.TELEGRAM)
                        .withNotificationMethod(NotificationMethod.SMS)
                        .withNotificationMethod(NotificationMethod.EMAIL)
                        .withStatus(JugglerStatus.WARN)
                        .withStatus(JugglerStatus.CRIT)
                        .withStatus(JugglerStatus.OK)
                        .withRecipient("ppc").withRecipient("direct-admin"));
        assertEquals(expected, actual);
    }
}
