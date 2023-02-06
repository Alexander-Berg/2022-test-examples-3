package ru.yandex.autotests.innerpochta.imap.steps;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.props;

public class SmtpSteps {
    private static final Logger LOG = LogManager.getLogger(SmtpSteps.class);
    private static final Session session = createSession();
    private String loginGroup;

    private MimeMessage msg = new MimeMessage(session);

    public SmtpSteps(String loginGroup) {
        this.loginGroup = loginGroup;
        try {
            //устанавливаем обязательные поля
            msg.setSubject(Util.getRandomString());
            msg.setText(Util.getLongString());
        } catch (MessagingException e) {
            Logger.getLogger(this.getClass()).error("Не удалось сформировать сообщения для посылки по smtp ", e);
        }
    }

    private static Session createSession() {
        //http://www.coderanch.com/t/531593/java/java/Sending-Email-Java-error-message
        Properties props = new Properties();
        props.setProperty("mail.from", props().getSenderLogin());
        props.setProperty("mail.transport.protocol", "smtp");
        //465 для SSL, 26 для TLS
        props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.smtp.host", props().getSmtpServer());
        props.setProperty("mail.smtp.port", Integer.toString(props().getSmtpPort()));
        props.setProperty("mail.smtp.user", props().getSenderLogin());
        props.setProperty("mail.smtp.auth", "false");
        return Session.getInstance(props);
    }

    public SmtpSteps subj(String subj) throws MessagingException {
        msg.setSubject(subj);
        return this;
    }

    public SmtpSteps text(String text) throws MessagingException {
        msg.setText(text);
        return this;
    }

    @Step("Отправляем письмо через <smtp>")
    public void send() throws MessagingException {
        msg.setFrom();
        msg.setRecipients(Message.RecipientType.TO, props().account(loginGroup).getSelfEmail());
        msg.setSentDate(new Date());
        Transport.send(msg);
        LOG.info(String.format("Message <%s> has been sent to: %s", msg.getSubject(),
                props().account(loginGroup).getSelfEmail()));
    }
}
