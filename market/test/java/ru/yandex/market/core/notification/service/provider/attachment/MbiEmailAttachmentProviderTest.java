package ru.yandex.market.core.notification.service.provider.attachment;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.notification.model.data.MbiNotificationData;
import ru.yandex.market.notification.mail.model.attachment.EmailAttachment;
import ru.yandex.market.notification.mail.model.attachment.impl.UriEmailAttachment;
import ru.yandex.market.notification.model.context.NotificationContentProviderContext;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.model.data.NotificationData;
import ru.yandex.market.notification.simple.model.context.NotificationContextImpl;
import ru.yandex.market.notification.simple.model.data.ArrayListNotificationData;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.notification.simple.service.provider.context.NotificationContentProviderContextImpl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Тест для {@link MbiEmailAttachmentProvider}.
 *
 * @author avetokhin 03/08/16.
 */
public class MbiEmailAttachmentProviderTest {

    private static final String ATTACHMENT_NAME = "report";
    private static final String ATTACHMENT_FILENAME = "test.csv";


    /**
     * Должен корректно отработать и извлечь аттач из {@link MbiNotificationData}.
     */
    @Test
    public void testProvide() {
        // Подготовить данные.
        final MbiNotificationData data = new MbiNotificationData(
                null, null, UriEmailAttachment.create(ATTACHMENT_NAME, ATTACHMENT_FILENAME)
        );

        final MbiEmailAttachmentProvider provider = new MbiEmailAttachmentProvider();

        // Получить вложения.
        final Collection<EmailAttachment> attachments = provider.provide(prepareContext(data));

        assertThat(attachments, notNullValue());
        assertThat(attachments, hasSize(1));

        final EmailAttachment attachment = attachments.iterator().next();
        assertThat(attachment, notNullValue());
        assertThat(attachment.getName(), equalTo(ATTACHMENT_NAME));
        assertThat(attachment, instanceOf(UriEmailAttachment.class));
        assertThat(attachment.cast(UriEmailAttachment.class).getUri(), equalTo(ATTACHMENT_FILENAME));
    }

    /**
     * Не должен ничего извлечь если тип данных не {@link MbiNotificationData}.
     */
    @Test
    public void testProvideWithInvalidDataType() {
        // Подготовить данные.
        final ArrayListNotificationData data = new ArrayListNotificationData();

        final MbiEmailAttachmentProvider provider = new MbiEmailAttachmentProvider();

        // Получить вложения.
        final Collection<EmailAttachment> attachments = provider.provide(prepareContext(data));

        assertThat(attachments, notNullValue());
        assertThat(attachments, hasSize(0));
    }

    private NotificationContentProviderContext prepareContext(final NotificationData data) {
        final NotificationContext originContext = new NotificationContextImpl(
                new CodeNotificationType(1L),
                NotificationPriority.NORMAL,
                NotificationTransport.EMAIL, Collections.emptyList(), Instant.now(), data, null, false
        );

        return new NotificationContentProviderContextImpl(originContext, null, Collections.emptyList());
    }

}
