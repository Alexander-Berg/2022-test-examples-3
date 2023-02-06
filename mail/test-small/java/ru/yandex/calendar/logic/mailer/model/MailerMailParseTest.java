package ru.yandex.calendar.logic.mailer.model;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsMethod;
import ru.yandex.calendar.logic.mailer.logbroker.MailAttach;
import ru.yandex.commune.mail.MailMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.calendar.LoadFileUtils.getResourceAsFileInputStreamSource;

public class MailerMailParseTest {
    @Test
    public void parse() {
        val message = MailMessage.parse(getResourceAsFileInputStreamSource("mailer/reply.eml"));
        assertThat(message.getHeader("y-exchange-calendar")).contains("Yes");

        val parsed = MailerMail.parseOrRefusal(message, Mockito.mock(MailAttach.class))
                .fold(m -> m, r -> { throw new AssertionError(r); });

        assertThat(parsed.messageId).isEqualTo("<b8a6890002a541aea9c83ee72c021b39@PRINCE21-N1.ld.yandex.ru>");
        assertThat(parsed.sender.getEmail().getEmail()).isEqualTo("calendartestuser@yandex-team.ru");
        assertThat(parsed.date.toString()).isEqualTo("2017-08-14T18:30:43.000Z");

        assertThat(parsed.ics.getMethod()).isEqualTo(IcsMethod.REPLY);
    }

    @Test
    public void transformMessageId() {
        assertThat(MailerMail.transformMessageId("<b8a6890002@PRINCE21-N1.ld.yandex.ru>"))
                .isEqualTo("b8a6890002_PRINCE21-N1.ld.yandex.ru@calendar.yandex");
    }

}
