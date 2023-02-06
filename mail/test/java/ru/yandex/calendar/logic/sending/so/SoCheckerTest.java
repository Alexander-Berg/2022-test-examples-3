package ru.yandex.calendar.logic.sending.so;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.val;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang.mutable.MutableObject;
import org.joda.time.Instant;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function0V;
import ru.yandex.calendar.RemoteInfo;
import ru.yandex.calendar.frontend.web.cmd.run.CommandRunException;
import ru.yandex.calendar.frontend.webNew.WebNewTestBase;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.sending.param.CommonEventMessageParameters;
import ru.yandex.calendar.logic.sending.param.EventInviteeNamesI18n;
import ru.yandex.calendar.logic.sending.param.EventLocation;
import ru.yandex.calendar.logic.sending.param.EventMessageInfo;
import ru.yandex.calendar.logic.sending.param.EventMessageParameters;
import ru.yandex.calendar.logic.sending.param.EventMessageTimezone;
import ru.yandex.calendar.logic.sending.param.EventTimeParameters;
import ru.yandex.calendar.logic.sending.param.InvitationMessageParameters;
import ru.yandex.calendar.logic.sending.param.MessageOverrides;
import ru.yandex.calendar.logic.sending.param.NotificationMessageParameters;
import ru.yandex.calendar.logic.sending.param.Recipient;
import ru.yandex.calendar.logic.sending.param.Sender;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.logic.user.NameI18n;
import ru.yandex.calendar.micro.so.SoCheckClient;
import ru.yandex.commune.mail.MailAddress;
import ru.yandex.inside.passport.PassportSid;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.net.LocalhostUtils;
import ru.yandex.misc.reflection.ClassX;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.web.servletContainer.SingleWarJetty;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

public class SoCheckerTest extends WebNewTestBase {
    @Test
    public void collect() {
        var checker = new SoChecker("http://localhost:7080", Timeout.unlimited(), mock(SoCheckClient.class));
        checker = Mockito.spy(checker);

        ArgumentCaptor<SoRequestData> dataCaptor = ArgumentCaptor.forClass(SoRequestData.class);

        ArgumentCaptor<ListF<Email>> recipientsCaptor = ArgumentCaptor.forClass(
                ClassX.wrap(ListF.class).<ListF<Email>>uncheckedCast().getClazz());

        Mockito.doNothing().when(checker).checkNoSpam(
                any(), recipientsCaptor.capture(), any(), dataCaptor.capture(), any());

        ActionInfo actionInfo = ActionInfo.webTest();

        checker.checkNoSpam(Cf.list(
                consMail("victim", MailType.EVENT_INVITATION, Option.empty()),
                consMail("friend", MailType.EVENT_ON_LAYER_ADDED, Option.empty())), actionInfo);

        Assert.equals(MailType.EVENT_INVITATION, dataCaptor.getValue().operation);
        Assert.equals("victim", recipientsCaptor.getValue().single().getLocalPart());

        checker.checkNoSpam(Cf.list(
                consMail("invite", MailType.EVENT_INVITATION, Option.empty()),
                consMail("update", MailType.EVENT_UPDATE, Option.empty())), actionInfo);

        Assert.equals(MailType.EVENT_UPDATE, dataCaptor.getValue().operation);
        Assert.hasSize(2, recipientsCaptor.getValue());

        checker.checkNoSpam(Cf.list(
                consMail("recur", MailType.EVENT_UPDATE, Option.of(Instant.now())),
                consMail("master", MailType.EVENT_UPDATE, Option.empty())), actionInfo);

        Assert.equals("master", dataCaptor.getValue().subject);
        Assert.hasSize(2, recipientsCaptor.getValue());
    }

    @Test
    public void request() {
        SingleWarJetty jetty = new SingleWarJetty();

        jetty.setLookupServletsInContext(false);

        MutableInt status = new MutableInt();
        MutableObject response = new MutableObject();

        jetty.addServletMapping("/", new HttpServlet() {
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setStatus(status.intValue());
                resp.getWriter().write((String) response.getValue());
            }
        });

        val checker = new SoChecker("http://localhost:7080", Timeout.unlimited(), mock(SoCheckClient.class));

        SoRequestData data = new SoRequestData(MailType.EVENT_UPDATE,
                new RemoteInfo(Option.of(LocalhostUtils.localAddress()), Option.of("yandexuid")),
                new SoRequestData.User(PassportUid.cons(1), Option.of("first")),
                consEvent("subject", Option.empty()), new SoRequestData.Extended(1, ActionSource.WEB));

        Function0V request = () -> checker.checkNoSpam(
                consSender(), Cf.list("o@o", "O@O").map(Email::new), Option.empty(), data, ActionInfo.webTest());

        jetty.start();
        try {
            response.setValue("{ \"resolution\": \"HAM\" }");

            status.setValue(200);
            request.apply();

            response.setValue("{ \"resolution\": \"SPAM\" }");
            Assert.assertThrows(request::apply, CommandRunException.class);

            status.setValue(500);
            request.apply();

        } finally {
            jetty.stop();
        }
    }

    private static Sender consSender() {
        return new Sender(
                Option.of(PassportUid.cons(1)), Option.of("first"),
                new NameI18n("First", "First"), Option.empty(), new Email("first@ya.ru"));
    }

    private static EventMessageInfo consEvent(String name, Option<Instant> recurrenceId) {
        EventTimeParameters time = new EventTimeParameters(
                MoscowTime.now().toLocalDateTime(), MoscowTime.now().toLocalDateTime(),
                false, false, Option.empty(), EventMessageTimezone.create(MoscowTime.TZ, Instant.now()));

        return new EventMessageInfo(
                27, 19, "externalId", recurrenceId, time,
                name, "description", EventLocation.location("location"),
                PassportSid.CALENDAR, Option.empty(), Option.empty());
    }

    private static EventMessageParameters consMail(String recipient, MailType type, Option<Instant> recurrenceId) {
        Sender sender = consSender();

        MailAddress to = new MailAddress(new Email(recipient + "@ya.ru"));

        CommonEventMessageParameters common = new CommonEventMessageParameters(
                Language.RUSSIAN, LocalDateTime.now(MoscowTime.TZ), sender, Recipient.of(to, Option.empty()),
                sender.getEmail(), "", false, MessageOverrides.EMPTY);

        EventMessageInfo event = consEvent(recipient, recurrenceId);

        if (type != MailType.EVENT_INVITATION && type != MailType.EVENT_UPDATE) {
            return new NotificationMessageParameters(common, event);
        }

        return new InvitationMessageParameters(
                common, event, Option.empty(), Option.empty(), Option.empty(),
                new EventInviteeNamesI18n(Cf.list(), Cf.list(), Cf.list()),
                Option.empty(), Decision.YES, false, type);
    }
}
