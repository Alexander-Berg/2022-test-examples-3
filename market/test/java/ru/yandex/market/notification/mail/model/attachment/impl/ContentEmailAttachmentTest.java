package ru.yandex.market.notification.mail.model.attachment.impl;

import org.junit.Test;

import ru.yandex.market.notification.test.model.AbstractModelTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link ContentEmailAttachment}.
 *
 * @author Vladislav Bauer
 */
public class ContentEmailAttachmentTest extends AbstractModelTest {

    @Test
    public void testConstruction() {
        final String name = "name";
        final ContentEmailAttachment attachment = ContentEmailAttachment.create(name);

        assertThat(attachment.getName(), equalTo(name));
    }

    @Test
    public void testBasicMethods() {
        final ContentEmailAttachment attachment = ContentEmailAttachment.create("");
        final ContentEmailAttachment sameAttachment = ContentEmailAttachment.create("");
        final ContentEmailAttachment otherAttachment = ContentEmailAttachment.create("not");

        checkBasicMethods(attachment, sameAttachment, otherAttachment);
    }

}
