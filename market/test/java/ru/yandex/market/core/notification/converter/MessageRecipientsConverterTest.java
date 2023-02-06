package ru.yandex.market.core.notification.converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.core.message.model.MessageRecipients;
import ru.yandex.market.notification.mail.model.address.ComposedEmailAddress;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Тесты для {@link MessageRecipientsConverter}.
 *
 * @author avetokhin 30/08/16.
 */
public class MessageRecipientsConverterTest {

    private static String FROM = "from@yandex.ru";
    private static String REPLY_TO = "reply_to@yandex.ru";
    private static List<String> TO = Arrays.asList("to1@yandex.ru", "to2@yandex.ru");
    private static List<String> CC = Arrays.asList("cc1@yandex.ru", "cc2@yandex.ru");
    private static List<String> BCC = Collections.singletonList("bcc@yandex.ru");

    private MessageRecipientsConverter converter = new MessageRecipientsConverter();


    /**
     * Если передать в конвертер null, он должен вернуть null.
     */
    @Test
    public void nullTest() {
        assertThat(converter.convert(null), nullValue());
    }

    /**
     * При передаче not null параметра должна производиться корректная конвертация.
     */
    @Test
    public void convertTest() {
        final MessageRecipients recipients = new MessageRecipients();
        recipients.setMailFrom(FROM);
        recipients.setReplyTo(REPLY_TO);
        recipients.setToAddressList(TO);
        recipients.setCcAddressList(CC);
        recipients.setBccAddressList(BCC);

        final ComposedEmailAddress address = converter.convert(recipients);

        assertThat(address.getFrom(), notNullValue());
        assertThat(address.getFrom(), containsInAnyOrder(FROM));

        assertThat(address.getReplyTo(), notNullValue());
        assertThat(address.getReplyTo(), containsInAnyOrder(REPLY_TO));

        assertThat(address.getTo(), notNullValue());
        assertThat(address.getTo(), equalTo(new HashSet<>(TO)));

        assertThat(address.getCc(), notNullValue());
        assertThat(address.getCc(), equalTo(new HashSet<>(CC)));

        assertThat(address.getBcc(), notNullValue());
        assertThat(address.getBcc(), equalTo(new HashSet<>(BCC)));

    }

}
