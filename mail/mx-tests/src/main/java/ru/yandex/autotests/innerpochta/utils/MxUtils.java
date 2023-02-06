package ru.yandex.autotests.innerpochta.utils;

import ch.ethz.ssh2.Connection;
import com.sun.mail.smtp.SMTPTransport;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.TelnetDebugSaver;

import javax.mail.MessagingException;
import javax.mail.Session;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * User: alex89
 * Date: 06.05.13
 */

public class MxUtils {
    public static final String NSLS_LOG_FILE = "/app/log/notsolitesrv.tskv";
    public static final Pattern MID_LOG_PATTERN = compile("mid=([0-9]+)");
    private static final Logger LOG = LogManager.getLogger(MxUtils.class);
    private static final Pattern LMTP_RESPONSE_VALIDATION_PATTERN =
            compile("250 2.0.0 Ok; ([_A-Za-z0-9-]+) (.*)");


    public static String getMessageIdByServerResponse(String serverResponse) {
        //Выделяем из ответа 250 2.0.0 Ok; kWRuLUmB none none
        //id-сообщения kWRuLUmB
        Matcher idFormat = LMTP_RESPONSE_VALIDATION_PATTERN.matcher(serverResponse.trim());
        Assert.assertTrue("Не удалось выделить id письма из ответа:" + serverResponse, idFormat.find());
        return idFormat.group(1);
    }

    public static String sendbyNsls(TestMessage m, String host, int port) throws IOException,
            MessagingException {
        LMTPTransport smt = LMTPTransport.getLMTPTransport(createLmtpSession());
        smt.connect(host, port, null, null);

        smt.sendMessage(m, m.getAllRecipients());
        return smt.getLastServerResponse();
    }

    public static String sendByNsls(TestMessage m) throws IOException,
                MessagingException {
        return sendbyNsls(m, mxTestProps().getNslsHost(), mxTestProps().getNslsPort());
    }

    public static String sendMessageByNsls(TestMessage m) throws IOException,
            MessagingException {
        String serverResponse = sendByNsls(m);
        String messageId = getMessageIdByServerResponse(serverResponse);
        LOG.info("id отправленного письма из lmtp-сессии: {" + messageId + "}");
        return messageId;
    }

    public static String sendByNwsmtp(TestMessage m, String server, int port,
                                      @Nullable String login, @Nullable String pwd, @Nullable String notify)
            throws IOException, MessagingException, InterruptedException {
        Properties ps = new Properties();
        ps.put("mail.smtp.ssl.trust", server);
        ps.put("mail.smtp.connectiontimeout", "10000");
        if (mxTestProps().isUseTls()) {
            ps.put("mail.smtp.starttls.enable", "true");
        }
        if (notify != null) {
            ps.put("mail.smtp.dsn.notify", notify);
        }
        ps.put("mail.debug", "true");
        Session sess = Session.getInstance(ps, null);
        SMTPTransport smt = new SMTPTransport(sess, sess.getTransport("smtp").getURLName());
        smt.connect(server, port, login, pwd);

        smt.sendMessage(m, m.getAllRecipients());
        return smt.getLastServerResponse();
    }

    public static String sendByNwsmtp(TestMessage m, String server, int port, @Nullable String login, @Nullable String pwd)
            throws IOException, MessagingException, InterruptedException {
        return sendByNwsmtp(m, server, port, login, pwd, null);
    }

    public static String sendByNwsmtp(TestMessage m, String server, int port, User sender)
            throws InterruptedException, MessagingException, IOException {
        return sendByNwsmtp(m, server, port, sender.getEmail(), sender.getPassword(), null);
    }

    public static String sendByNwsmtp(TestMessage m, String server, int port)
            throws IOException, MessagingException, InterruptedException {
        return sendByNwsmtp(m, server, port, null);
    }

    public static String sendByNwsmtpWithResultIgnore(TestMessage m, String server, int port) {
        try {
            return sendByNwsmtp(m, server, port);
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    public static String sendByNwsmtpWithResultIgnore(TestMessage m, String server, int port,
                                                      String login, String pwd) {
        try {
            return sendByNwsmtp(m, server, port, login, pwd, null);
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    public static String sendByNwsmtpWithResultIgnore(TestMessage m, String server, int port, User sender) {
        try {
            return sendByNwsmtp(m, server, port, sender);
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    public static String sendByNwsmtpWithSSL(TestMessage m, String server, int port,
                                                  @Nullable String login, @Nullable String pwd)   {
        try {
            Properties ps = new Properties();
            ps.put("mail.smtp.ssl.enable", "true");
            ps.put("mail.smtp.connectiontimeout", "10000");
            ps.put("mail.smtp.from", login);
            ps.put("mail.debug", "true");
            Session sess = Session.getInstance(ps, null);
            SMTPTransport smt = new SMTPTransport(sess, sess.getTransport("smtp").getURLName());
            smt.connect(server, port, login, pwd);
            smt.sendMessage(m, m.getAllRecipients());
            return smt.getLastServerResponse();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    public static String sendByNwsmtpWithSSL(TestMessage m, String server, int port, User sender)   {
        return sendByNwsmtpWithSSL(m, server, port, sender.getEmail(), sender.getPassword());
    }

    public static Session createLmtpSession() throws MessagingException, IOException {
        Properties sessionProperties = new Properties();
        sessionProperties.put("mail.smtp.connectiontimeout", "5000");
        sessionProperties.put("mail.smtp.ehlo", "false");
        Session sess = Session.getDefaultInstance(sessionProperties, null);
        PrintStream os;
        os = TelnetDebugSaver.getOutput();
        sess.setDebugOut(os);
        sess.setDebug(true);
        return sess;
    }

    public static String getInfoFromNsls(Connection conn, String messageId) throws IOException {
        return SSHCommands.executeCommAndResturnResultAsString(conn, "sleep 2; tail -c 15M -q " + NSLS_LOG_FILE + " | fgrep " + messageId, LOG);
    }

    public static String extractedParamFromLog(Pattern pattern, String sessionLog) {
        Matcher logParamMatcher = pattern.matcher(sessionLog);
        if (logParamMatcher.find()) {
            return logParamMatcher.group(1);
        }
        return "";
    }
}
