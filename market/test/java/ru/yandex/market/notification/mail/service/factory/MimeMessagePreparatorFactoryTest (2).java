package ru.yandex.market.notification.mail.service.factory;

import java.util.Collections;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.MimeMessagePreparator;

import ru.yandex.market.notification.mail.model.EmailContent;
import ru.yandex.market.notification.mail.model.address.ComposedEmailAddress;
import ru.yandex.market.notification.mail.service.registry.EmailAttachmentContentProviderRegistry;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

/**
 * Unit-тесты для {@link MimeMessagePreparatorFactory}.
 *
 * @author Vladislav Bauer
 */
public class MimeMessagePreparatorFactoryTest {

    @Test
    public void testCreate() throws Exception {
        var registry = mock(EmailAttachmentContentProviderRegistry.class);
        var factory = new MimeMessagePreparatorFactory(registry);

        var content = EmailContent.create("subject", "body", "formattedBody", Collections.emptySet());
        var address = ComposedEmailAddress.create(
            Collections.singleton("vbauer@yandex-team.ru"),
            Collections.singleton("bauer.vlad@gmail.com"),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.singleton("vbauer@mailforspam.com")
        );

        var preparator = factory.createPreparator(content, address);
        assertThat(preparator, notNullValue());

        var session = Session.getDefaultInstance(new Properties());
        var mimeMessage = new MimeMessage(session);
        preparator.prepare(mimeMessage);

        assertThat(mimeMessage.getSubject(), equalTo(content.getSubject()));
        assertThat(mimeMessage.getContent(), notNullValue());
        assertThat(mimeMessage.getFrom().length, equalTo(address.getFrom().size()));
        assertThat(mimeMessage.getReplyTo().length, equalTo(address.getReplyTo().size()));
    }

}
