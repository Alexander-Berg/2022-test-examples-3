package ru.yandex.autotests.smtpgate.tests.utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.yandex.autotests.innerpochta.tests.utils.SshConnectionRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;
import static ru.yandex.autotests.innerpochta.tests.utils.MxUtils.getInfoFromMaillog;
import static ru.yandex.autotests.innerpochta.tests.utils.MxUtils.getInfoFromNsls;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateProperties.smtpgateProps;

/**
 * User: alex89
 * Date: 05.08.2016
 */
public class SmtpgateData {
    private static final Logger LOG = LogManager.getLogger(SmtpgateData.class);
    public static final Pattern RESP_PATTERN =
            compile("\\{\"mid\":\"(?<mid>[0-9]+)\"\\,\"imap_id\":\"(?<imapId>[0-9]+)\"\\}");
    public static final Pattern STORE_RESP_PATTERN =
            compile("\\{\"mid\":\"(?<mid>[0-9]+)\"\\,\"imap_id\":\"(?<imapId>[0-9]+)\"\\}");
    private static final String SESSION_ID_PATTERN_IN_NWSMTP_LOG =
            "status=sent \\(250 2.0.0 Ok; (?<sessionId>[0-9A-Za-z-]+) ";
    private static final String SESSION_ID_PATTERN_IN_NWSMTP_LOG_2 =
            "status=sent \\(250 2.0.0 Ok: queued on [a-zA-Z0-9.-]+ as (?<sessionId>[0-9A-Za-z-]+)";
    private static final String SESSION_ID_PATTERN_STRING =
            " (?<sessionId>[0-9A-Za-z-]+):? message stored in db=pg backed for %s";
    private static final Pattern HINT_PATTERN = compile("X-Yandex-Hint: (?<hint>.*)");
    public static final Pattern COLLECT_RESP_PATTERN = compile("\\{\"session_id\":\"(?<sessionId>[0-9A-Za-z-]+)\"\\," +
            "\"message\":\"(?<message>[0-9A-Za-z -.,]?)\"\\}");

    public static final Pattern SEND_SYSTEM_MAIL_RESP_PATTERN =
            compile("\\{\"error\":\"Success\",\"explanation\":\"Ok: queued on "
                    + smtpgateProps().getHost().replace(".", "\\.") + " " +
                    "as [0-9]{10}\\-(?<sessionId>[0-9a-zA-Z-]+)\"\\}");


    public static final Pattern SAVE_RESP_PATTERN =
            compile("\\{\"error\":\"Success\",\"mid\":\"(?<mid>[0-9]+)\",\"explanation\":\"\"\\}");
    public static final Pattern SAVE_SEND_MSG_FAILED_RESP_PATTERN =
            compile("\\{\"error\":\"SendMessageFailed\",\"mid\":\"\"\\}");
    public static final Pattern SAVE_SPAM_RESP_PATTERN =
            compile("\\{\"error\":\"Spam\",\"mid\":\"\",\"explanation\":\"suspicion of spam\"\\}");
    public static final Pattern SAVE_STRONG_SPAM_RESP_PATTERN =
            compile("\\{\"error\":\"StrongSpam\",\"mid\":\"\",\"explanation\":\"suspicion of spam\"\\}");
    public static final Pattern SAVE_VIRUS_RESP_PATTERN =
            compile("\\{\"error\":\"Virus\",\"mid\":\"\",\"explanation\":\"infected by virus\"\\}");

    public static String prepareHint(String hint) {
        String[] hints = hint.trim().split(" ");
        Arrays.sort(hints);
        return String.join(" ", hints);
    }

    public static String getHintInformationByAppendResponse(String responseString, String login,
                                                            SshConnectionRule sshConnectionRule) throws IOException {
        Matcher responseMatcher = RESP_PATTERN.matcher(responseString);
        responseMatcher.find();
        String sessionLogWithMid = getInfoFromMaillog(sshConnectionRule.getConn(), responseMatcher.group("mid"));
        LOG.info(sessionLogWithMid);
        Matcher sessionIdMatcher = compile(String.format(SESSION_ID_PATTERN_STRING, login)).matcher(sessionLogWithMid);
        String sessionId;
        if (sessionIdMatcher.find()) {
            sessionId = sessionIdMatcher.group("sessionId");
        } else {
            throw new RuntimeException("sessionId не нашли!");
        }
        LOG.info(getInfoFromMaillog(sshConnectionRule.getConn(), sessionId));

        String debugLog = getInfoFromNsls(sshConnectionRule.getConn(), sessionId);
        LOG.info(debugLog);

        Matcher hintMatcher = HINT_PATTERN.matcher(debugLog);
        if (hintMatcher.find()) {
            return prepareHint(hintMatcher.group("hint"));
        } else {
            return "";
        }
    }


    public static List<String> getHintInformationByStoreResponse(String responseString, String login,
                                                                 SshConnectionRule sshConnectionRule)
            throws IOException {
        Matcher responseMatcher = STORE_RESP_PATTERN.matcher(responseString);
        responseMatcher.find();
        String sessionLogWithMid = getInfoFromMaillog(sshConnectionRule.getConn(), responseMatcher.group("mid"));
        LOG.info(sessionLogWithMid);
        Matcher sessionIdMatcher = compile(String.format(SESSION_ID_PATTERN_STRING, login)).matcher(sessionLogWithMid);
        String sessionId;
        if (sessionIdMatcher.find()) {
            sessionId = sessionIdMatcher.group("sessionId");
        } else {
            throw new RuntimeException("sessionId не нашли!");
        }
        LOG.info(getInfoFromMaillog(sshConnectionRule.getConn(), sessionId));

        String debugLog = getInfoFromNsls(sshConnectionRule.getConn(), sessionId);
        LOG.info(debugLog);
        List<String> extractedHints = new ArrayList<>();

        Matcher hintMatcher = HINT_PATTERN.matcher(debugLog);
        while (hintMatcher.find()) {
            extractedHints.add(prepareHint(hintMatcher.group("hint")));
        }
        return extractedHints;
    }

    public static List<String> getHintInformationBySaveResponse(String responseString, String login,
                                                                SshConnectionRule sshConnectionRule)
            throws IOException {
        Matcher responseMatcher = SAVE_RESP_PATTERN.matcher(responseString);
        responseMatcher.find();
        String sessionLogWithMid = getInfoFromMaillog(sshConnectionRule.getConn(), responseMatcher.group("mid"));
        LOG.info(sessionLogWithMid);
        Matcher sessionIdMatcher = compile(String.format(SESSION_ID_PATTERN_STRING, login)).matcher(sessionLogWithMid);
        String sessionId;
        if (sessionIdMatcher.find()) {
            sessionId = sessionIdMatcher.group("sessionId");
        } else {
            throw new RuntimeException("sessionId не нашли!");
        }
        LOG.info(getInfoFromMaillog(sshConnectionRule.getConn(), sessionId));

        String debugLog = getInfoFromNsls(sshConnectionRule.getConn(), sessionId);
        LOG.info(debugLog);
        List<String> extractedHints = new ArrayList<>();

        Matcher hintMatcher = HINT_PATTERN.matcher(debugLog);
        while (hintMatcher.find()) {
            extractedHints.add(prepareHint(hintMatcher.group("hint")));
        }
        return extractedHints;
    }


    public static List<String> getHintInformationBySendSystemMailResponse(String responseString,
                                                                          SshConnectionRule sshConnectionRule)
            throws IOException {
        Matcher responseMatcher = SEND_SYSTEM_MAIL_RESP_PATTERN.matcher(responseString);
        responseMatcher.find();
        String sessionLogNwsmtp = getInfoFromMaillog(sshConnectionRule.getConn(), responseMatcher.group("sessionId"));
        LOG.info(sessionLogNwsmtp);
        Matcher sessionIdMatcher = compile(SESSION_ID_PATTERN_IN_NWSMTP_LOG).matcher(sessionLogNwsmtp);
        String sessionId;
        if (sessionIdMatcher.find()) {
            sessionId = sessionIdMatcher.group("sessionId");
        } else {
            throw new RuntimeException("sessionId не нашли!");
        }
        LOG.info(getInfoFromMaillog(sshConnectionRule.getConn(), sessionId));
        String debugLog = getInfoFromNsls(sshConnectionRule.getConn(), sessionId);
        LOG.info(debugLog);
        List<String> extractedHints = new ArrayList<>();

        Matcher hintMatcher = HINT_PATTERN.matcher(debugLog);
        while (hintMatcher.find()) {
            extractedHints.add(prepareHint(hintMatcher.group("hint")));
        }
        return extractedHints;
    }


    public static List<String> getHintInformationBySendMailResponse(String responseString,
                                                                    SshConnectionRule sshConnectionRule)
            throws IOException {
        Matcher responseMatcher = SEND_SYSTEM_MAIL_RESP_PATTERN.matcher(responseString);
        responseMatcher.find();
        String sessionLogNwsmtp = getInfoFromMaillog(sshConnectionRule.getConn(), responseMatcher.group("sessionId"));
        LOG.info(sessionLogNwsmtp);
        Matcher sessionIdMatcher = compile(SESSION_ID_PATTERN_IN_NWSMTP_LOG_2).matcher(sessionLogNwsmtp);
        String sessionIdPostfix;
        if (sessionIdMatcher.find()) {
            sessionIdPostfix = sessionIdMatcher.group("sessionId");
        } else {
            throw new RuntimeException("sessionId не нашли!");
        }
        String sessionLogPostfix = getInfoFromMaillog(sshConnectionRule.getConn(), sessionIdPostfix);
        LOG.info(sessionLogPostfix);

        sessionIdMatcher = compile(SESSION_ID_PATTERN_IN_NWSMTP_LOG).matcher(sessionLogPostfix);
        String sessionIdFastsrv;
        if (sessionIdMatcher.find()) {
            sessionIdFastsrv = sessionIdMatcher.group("sessionId");
        } else {
            throw new RuntimeException("sessionId не нашли!");
        }


        String debugLog = getInfoFromNsls(sshConnectionRule.getConn(), sessionIdFastsrv);
        LOG.info(debugLog);
        List<String> extractedHints = new ArrayList<>();

        Matcher hintMatcher = HINT_PATTERN.matcher(debugLog);
        while (hintMatcher.find()) {
            extractedHints.add(prepareHint(hintMatcher.group("hint")));
        }
        return extractedHints;
    }
}
