package ru.yandex.calendar.logic.sending.real;

import java.util.Properties;

import lombok.val;
import org.junit.jupiter.api.Test;

import ru.yandex.calendar.logic.domain.PassportAuthDomains;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.commune.mail.MailMessage;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.property.PropertiesHolder;

import static org.assertj.core.api.Assertions.assertThat;

public class MailHacksTest {
    @Test
    public void composeCalendarEnvironmentHeader() {
        val domain = PassportAuthDomains.PUBLIC.toString().toLowerCase();
        val p = new Properties();
        p.put("auth.domains", domain);
        p.put("yandex.environment.type", EnvironmentType.TESTS.getValue());
        PropertiesHolder.set(p);

        val msg = MailHacks.addCalendarHeaders(MailMessage.empty(), ActionInfo.webTest());

        assertThat(msg.getHeader(MailHeaders.X_CALENDAR_ENV).get())
                .isEqualTo(EnvironmentType.TESTS.getValue());
        assertThat(msg.getHeader(MailHeaders.X_CALENDAR_DOMAIN).get())
                .isEqualTo(domain);
    }
}
