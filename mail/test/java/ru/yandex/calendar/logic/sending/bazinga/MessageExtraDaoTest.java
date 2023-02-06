package ru.yandex.calendar.logic.sending.bazinga;

import java.time.Duration;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsAttendee;
import ru.yandex.calendar.logic.sending.param.MessageExtra;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageExtraDaoTest extends AbstractConfTest {
    @Autowired
    private MessageExtraDao messageExtraDao;

    @Test
    public void shouldSaveLoadAndDeleteExpiredExtra() {
        var email = "amosov-f@yandex-team.ru";
        var expected = new MessageExtra(new IcsCalendar().addProperty(new IcsAttendee(new Email(email))));
        long id = messageExtraDao.save(expected, Duration.ZERO);
        var actual = messageExtraDao.load(id).get();
        assertThat(actual.properties.getProperties()).hasSize(expected.properties.getProperties().size());

        messageExtraDao.deleteExpired();
        assertThat(messageExtraDao.load(id)).isEmpty();
    }
}
