package ru.yandex.market.notification.mail.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import javax.mail.internet.AddressException;

import org.junit.Test;

import ru.yandex.market.notification.mail.model.address.ComposedEmailAddress;
import ru.yandex.market.notification.mail.model.address.EmailAddress;
import ru.yandex.market.notification.mail.model.attachment.EmailAttachment;
import ru.yandex.market.notification.mail.model.attachment.impl.ByteArrayEmailAttachment;
import ru.yandex.market.notification.mail.model.attachment.impl.ContentEmailAttachment;
import ru.yandex.market.notification.mail.model.attachment.impl.UriEmailAttachment;
import ru.yandex.market.notification.test.util.DataSerializerUtils;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для сериализации/десериализации моделей для работы с email'ами.
 *
 * @author Vladislav Bauer
 */
public class EmailDataSerialTest {

    @Test
    public void testEmailAddress() {
        final EmailAddress.Type type = EmailAddress.Type.FROM;
        final String email = "vbauer@yandex-team.ru";
        final EmailAddress address = EmailAddress.create(email, type);

        final byte[] data = DataSerializerUtils.serialize(address);
        final EmailAddress restoredObject = DataSerializerUtils.deserialize(data, EmailAddress.class);
        assertThat(restoredObject.getEmail(), equalTo(email));
        assertThat(restoredObject.getType(), equalTo(type));
    }

    @Test
    public void testEmailComposedAddress() throws AddressException {
        final Set<String> from = Collections.singleton("god@yandex.ru");
        final Set<String> to = Collections.singleton("test1@yandex.ru");
        final Set<String> cc = Collections.singleton("test2@yandex.ru");
        final Set<String> bcc = Collections.singleton("test3@yandex.ru");
        final Set<String> replyTo = Collections.singleton("test4@yandex.ru");

        final ComposedEmailAddress address = ComposedEmailAddress.create(from, to, cc, bcc, replyTo);

        final byte[] data = DataSerializerUtils.serialize(address);
        final ComposedEmailAddress restoredObject = DataSerializerUtils.deserialize(data, ComposedEmailAddress.class);

        assertThat(restoredObject.getFrom(), containsInAnyOrder(from.toArray(new String[to.size()])));
        assertThat(restoredObject.getTo(), containsInAnyOrder(to.toArray(new String[to.size()])));
        assertThat(restoredObject.getCc(), containsInAnyOrder(cc.toArray(new String[cc.size()])));
        assertThat(restoredObject.getBcc(), containsInAnyOrder(bcc.toArray(new String[bcc.size()])));
        assertThat(restoredObject.getReplyTo(), containsInAnyOrder(replyTo.toArray(new String[replyTo.size()])));
    }

    @Test
    public void testEmailContent() throws Exception {
        final String byteArrayAttachmentName = "byte array attachment";
        final byte[] byteArrayAttachmentData = {1, 9, 8, 8};
        final ByteArrayEmailAttachment byteArrayEmailAttachment =
            ByteArrayEmailAttachment.create(byteArrayAttachmentName, byteArrayAttachmentData);

        final String uriAttachmentName = "uri attachment";
        final String uriAttachmentUri = "http://ya.ru";
        final UriEmailAttachment uriEmailAttachment = UriEmailAttachment.create(uriAttachmentName, uriAttachmentUri);

        final String contentAttachmentName = "content attachment";
        final ContentEmailAttachment contentEmailAttachment = ContentEmailAttachment.create(contentAttachmentName);

        final Collection<EmailAttachment> attachments = new ArrayList<>();
        attachments.add(byteArrayEmailAttachment);
        attachments.add(uriEmailAttachment);
        attachments.add(contentEmailAttachment);

        final String subject = "subject";
        final String body = "body";
        final String formattedBody = "<html><body>body</body></html>";
        final EmailContent content = EmailContent.create(subject, body, formattedBody, attachments);

        final byte[] data = DataSerializerUtils.serialize(content);
        final byte[] expectedData = DataSerializerUtils.loadResource(getClass(), "EmailContent.xml");
        final String dataText = DataSerializerUtils.toString(data);
        final String expectedDataText = DataSerializerUtils.toString(expectedData);
        assertThat(dataText, equalTo(expectedDataText));

        final EmailContent restoredObject = DataSerializerUtils.deserialize(data, EmailContent.class);
        assertThat(restoredObject.getBody(), equalTo(body));
        assertThat(restoredObject.getFormattedBody(), equalTo(formattedBody));
        assertThat(restoredObject.getSubject(), equalTo(subject));

        final Collection<EmailAttachment> restoredAttachments = restoredObject.getAttachments();
        assertThat(restoredAttachments, notNullValue());
        assertThat(restoredAttachments, hasSize(3));

        final Iterator<EmailAttachment> iterator = restoredAttachments.iterator();
        final ByteArrayEmailAttachment restoredAttachment1 = (ByteArrayEmailAttachment) iterator.next();
        assertThat(restoredAttachment1, notNullValue());
        assertThat(restoredAttachment1.getName(), equalTo(byteArrayAttachmentName));
        assertArrayEquals(restoredAttachment1.getContent(), byteArrayAttachmentData);

        final UriEmailAttachment restoredAttachment2 = (UriEmailAttachment) iterator.next();
        assertThat(restoredAttachment2, notNullValue());
        assertThat(restoredAttachment2.getName(), equalTo(uriAttachmentName));
        assertThat(restoredAttachment2.getUri(), equalTo(uriAttachmentUri));

        final ContentEmailAttachment restoredAttachment3 = (ContentEmailAttachment) iterator.next();
        assertThat(restoredAttachment3, notNullValue());
        assertThat(restoredAttachment3.getName(), equalTo(contentAttachmentName));
    }

}
