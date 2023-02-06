package ru.yandex.market.notification.mail.model.attachment.impl;

import org.junit.Test;

import ru.yandex.market.notification.test.model.AbstractModelTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link UriEmailAttachment}.
 *
 * @author Vladislav Bauer
 */
public class UriEmailAttachmentTest extends AbstractModelTest {

    @Test
    public void testConstruction() {
        final String name = "name";
        final String uri = "uri";
        final UriEmailAttachment attachment = UriEmailAttachment.create(name, uri);

        assertThat(attachment.getName(), equalTo(name));
        assertThat(attachment.getUri(), equalTo(uri));
    }

    @Test
    public void testBasicMethods() {
        final UriEmailAttachment attachment = UriEmailAttachment.create("", "");
        final UriEmailAttachment sameAttachment = UriEmailAttachment.create("", "");
        final UriEmailAttachment otherAttachment = UriEmailAttachment.create("not", "same");

        checkBasicMethods(attachment, sameAttachment, otherAttachment);
    }

}
