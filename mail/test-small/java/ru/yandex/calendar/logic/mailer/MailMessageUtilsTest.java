package ru.yandex.calendar.logic.mailer;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import ru.yandex.calendar.logic.domain.PassportAuthDomains;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.sending.real.MailHeaders;
import ru.yandex.calendar.logic.user.UserOrMaillist;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.commune.mail.DefaultContent;
import ru.yandex.commune.mail.MailMessage;
import ru.yandex.commune.mail.MailUtils;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.io.ByteArrayInputStreamSource;
import ru.yandex.misc.io.InputStreamSourceUtils;
import ru.yandex.misc.property.PropertiesHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.calendar.LoadFileUtils.getResourceAsFileInputStreamSource;

public class MailMessageUtilsTest extends CalendarTestBase {
    private static Properties oldProperties;

    @BeforeAll
    public static void init() {
        if (EnvironmentType.getActive() == EnvironmentType.TESTS && PropertiesHolder.isInitialized()) {
            oldProperties = PropertiesHolder.properties();
        }
        val p = new Properties();
        p.put("auth.domains", PassportAuthDomains.YT.toString().toLowerCase());
        PropertiesHolder.set(p);
    }

    @AfterAll
    public static void teardown() {
        if (oldProperties != null) {
            PropertiesHolder.set(oldProperties);
        }
    }

    @Test
    public void transformForReturn() {
        val parsed = MailMessage.parse(getResourceAsFileInputStreamSource("mailer/forwarded.eml"));

        val user = UserOrMaillist.maillist(PassportUid.cons(1), new Email("ml@yandex-team.ru"));
        final var transformed = MailMessageUtils.transformForReturn(user, parsed, ActionInfo.webTest());

        val parsedTransformed = MailMessage.parse(InputStreamSourceUtils.bytes(transformed.serializeToBytes()));

        assertThat(parsedTransformed.getMessageId().toOptional())
                .contains("1508865449973.11853_yandex-team.ru@calendar.yandex");
        assertThat(parsedTransformed.getSubject().toOptional())
                .contains("FW: Без названия");

        assertThat(parsedTransformed.getHeader(MailHeaders.TO).toOptional())
                .contains("ml@mail.yandex-team.ru");
        assertThat(parsedTransformed.getHeader(MailHeaders.BCC).toOptional())
                .contains("ml-7bf24f@yandex-team.ru");

        assertThat(((DefaultContent) parsedTransformed.getBottomPlainTextParts().single().getContent()).getValue())
                .isEqualTo("Отправлено");
    }

    @Test
    public void pictured() {
        var message = MailMessage.parse(getResourceAsFileInputStreamSource("mailer/pictured.eml"));

        message = MailMessage.parse(InputStreamSourceUtils.bytes(message.serializeToBytes()));
        val picture = message.getBottomPartsWithTypeSubtype("image/png").single();

        assertThat(picture.getHeader("Content-ID").toOptional()).isPresent();
        assertThat(picture.getHeader("Content-Disposition"))
                .extracting(s -> s.contains("size=4647"))
                .contains(true);
    }

    @Test
    public void addressHeader() throws MessagingException {
        val message = MailMessage.parse(new ByteArrayInputStreamSource((""
                + "Date: Fri, 30 Mar 2018 07:35:25 +0000\n"
                + "From: Artemiy Arakchaa <artemiy@yandex-team.ru>, Alexandr Grebenyuk\n"
                + "        <agrebenyuk@yandex-team.ru>\n"
                + "Sender: =?windows-1251?B?zuv84+AgyOLg7e7i4A==?= <olga-ivanova@yandex-team.ru>\n")
                .getBytes()));

        val user = UserOrMaillist.maillist(PassportUid.cons(1), new Email("ml@yandex-team.ru"));
        val copied = MailMessageUtils.transformForReturn(user, message, ActionInfo.webTest());

        val converted = MailUtils.toMimeMessage(copied);

        val sender = (InternetAddress) converted.getSender();
        val from = StreamEx.of(converted.getFrom())
                .select(InternetAddress.class).toImmutableList();

        assertThat(sender.getPersonal()).isEqualTo("Ольга Иванова");
        assertThat(sender.getAddress()).isEqualTo("olga-ivanova@yandex-team.ru");

        assertThat(from.get(0).getPersonal()).isEqualTo("Artemiy Arakchaa");
        assertThat(from.get(from.size() - 1).getAddress()).isEqualTo("agrebenyuk@yandex-team.ru");
    }
}
