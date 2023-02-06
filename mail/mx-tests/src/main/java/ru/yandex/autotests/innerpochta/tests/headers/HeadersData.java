package ru.yandex.autotests.innerpochta.tests.headers;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hamcrest.Matcher;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.util.InnerpochtaProjectProperties;
import ru.yandex.autotests.innerpochta.wmi.core.base.DocumentConverter;
import ru.yandex.autotests.innerpochta.wmi.core.base.XpathConverter;

import javax.mail.Header;
import javax.mail.MessagingException;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.cthul.matchers.CthulMatchers.matchesPattern;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.innerpochta.tests.headers.HeadersData.HeaderNames.*;

/**
 * User: alex89
 * Date: 23.04.13
 */
public class HeadersData {
    private static final Logger LOG = LogManager.getLogger(HeadersData.class);
    private static final InnerpochtaProjectProperties PROPS = new InnerpochtaProjectProperties();
    public static final String TCP_DAMP_COMMAND = "tcpdump -s 0 -A -nnp -i any port 2525 > ";

    public static final String SENDER_LOGIN;

    static {
        if (PROPS.isCorpServer()) {
            SENDER_LOGIN = "filter-01@mail.yandex-team.ru";
        } else {
            SENDER_LOGIN = "rpop.user@yandex.by";
        }
    }

    public static final String SENDER_NAME = "Tolstuha";
    private static final String SENDER_ENCODED =
            encodeBase64String(("\"" + SENDER_NAME + "\" <" + SENDER_LOGIN + ">").getBytes());

    public static final String RECEIVER_LOGIN;

    static {
        if (PROPS.isCorpServer()) {
            RECEIVER_LOGIN = "filter-09@mail.yandex-team.ru";
        } else {
            RECEIVER_LOGIN = "yantester@yandex.com";
        }
    }

    public static final String RECEIVER_NAME = "Криворучко Парамон";
    private static final String RECEIVER_ENCODED =
            encodeBase64String(("\"" + RECEIVER_NAME + "\" <" + RECEIVER_LOGIN + ">").getBytes());

    //Характеристики письма.
    public static final String SUBJECT = "Alice in the Wander Land";
    public static final String METHOD_ID = "test";
    public static final String IP_FROM = "77.88.2.71";
    public static final String SESSION_KEY = "fCvKpi4Q-fCvSurfe";
    public static final String IMAP_LABELS = "IMAP_LABEL";
    public static final String USER_LABEL_ID;

    static {
        if (PROPS.isCorpServer()) {
            USER_LABEL_ID = "2370000000020866963";
        } else {
            USER_LABEL_ID = "67";
        }
    }

    public static final String HINT_INFO =
            encodeBase64String((format("method_id=%s\nipfrom=%s\nsession_key=%s\nimaplabel=%s\nlid=%s",
                    METHOD_ID, IP_FROM, SESSION_KEY, IMAP_LABELS, USER_LABEL_ID)).getBytes());
    public static final String REFERENCES = "<11841A44F99C142F31E6BA80403BB422C@WINMAILM2.msbo.masterhost.ru>";
    public static final String IN_REPLY_TO = "<1783901343@web71h.yandex.ru>";

    private static final String DATE_PATTERN_YYYY_MM_DD_HH_MM_SS =
            "20[\\d]{2}-[\\d]{2}-[\\d]{2} [\\d]{2}:[\\d]{2}:[\\d]{2}";

    public static final Map<HeaderNames, Matcher> META_HEADERS_VALUES_MAP = new HashMap<HeaderNames, Matcher>() {{
        put(X_YANDEX_META_FOLDER_NAME, equalTo("0JLRhdC+0LTRj9GJ0LjQtQ=="));
        put(X_YANDEX_META_HDR_SUBJECT, equalTo(encodeBase64String(SUBJECT.getBytes())));
        put(X_YANDEX_META_HDR_FROM, equalTo(SENDER_ENCODED));
        put(X_YANDEX_META_HDR_TO, equalTo(RECEIVER_ENCODED));
        put(X_YANDEX_META_HDR_CC, equalTo(""));
        put(X_YANDEX_META_HDR_BCC, equalTo(""));
        put(X_YANDEX_META_HDR_DATE, matchesPattern(DATE_PATTERN_YYYY_MM_DD_HH_MM_SS));
        put(X_YANDEX_META_HDR_MESSAGE_ID, containsString("JavaMail"));
        put(X_YANDEX_META_HDR_UIDL, matchesPattern("[0-9a-z]+"));
        put(X_YANDEX_META_HDR_IN_REPLY_TO, equalTo(IN_REPLY_TO));
        put(X_YANDEX_HDR_REFERENCES, equalTo(REFERENCES));
        put(X_YANDEX_META_MIXED, equalTo("0"));
        put(X_YANDEX_META_LID, equalTo(USER_LABEL_ID));
        put(X_YANDEX_META_FIRSTLINE, equalTo("SGkhIEhhbnMhIFJvYm90aWNzIGlzIHRoZSBicmFuY2ggb2YgdGVjaG5vbG9" +
                "neSB0aGF0IGRlYWxzIHdpdGggdGhlIGRlc2lnbiwgY29uc3RydWN0aW9uLCBvcGVyYXRpb24sIGFuZCBhcHBsaWNhdG" +
                "lvbiBvZiByb2JvdHMsWzFdIGFzIHdlbGwgYXMgY29tcHV0ZXIgc3lzdGVtcyBmb3IgdGhlaXIgY29udHJvbCwgc2Vuc" +
                "29yeSBmZWVkYmFjaywgYW5kIGluZm9ybWF0aW9uIHByb2Nlc3NpbmcuIFRoZXNlIHRlY2hub2xvZ2llcyBkZWFsIHdp" +
                "dGggYXV0b21hdGVkIG1hY2hpbmVzIHRoYXQgY2FuIHRha2UgdGhlIHBsYWNlIG9mIGh1bWFucyBpbiBkYW5nZXJvdXMg" +
                "ZW52aXJvbm1lbnRzIG9yIG1hbnVmYWN0dXJpbmcgcHJvY2Vzc2VzLCBvciByZXNlbWJsZSBodW1hbnMgaW4="));
        put(X_YANDEX_META_ATTACH, equalTo("MS4yCmltYWdlL2pwZWcKRXJyb3JzIC0yLmpwZwo1NTM2Cg=="));
        put(X_YANDEX_META_SIZE, matchesPattern("11[0-9]{3}"));
        put(X_YANDEX_META_REPLY_TO, equalTo(SENDER_LOGIN));
        put(X_YANDEX_META_REPLY_TO_ALL, equalTo(RECEIVER_LOGIN));
        put(X_YANDEX_META_METHOD_ID, equalTo(METHOD_ID));
        put(X_YANDEX_META_IP_FROM, equalTo(IP_FROM));
        put(X_YANDEX_META_SESSION_KEY, equalTo(SESSION_KEY));
        put(X_YANDEX_META_IMAP_LABELS, equalTo("1"));
    }};

    public static final String RECEIVED_HEADER_EXAMPLE = "from rpop1g.mail.yandex.net (rpop1g.mail.yandex.net " +
            "[95.108.252.64])\n\tby mxback%s.mail.yandex.net (nwsmtp/Yandex) with ESMTP id KS9zmjQoOv-Mu2q0NKs;\n" +
            "\tWed, 17 Jul 2013 17:22:56 +0400";


    public static String findHeaderInLog(String headerName, String tcpDump) {
        Pattern headerPattern = Pattern.compile(headerName + ": (.*)");
        java.util.regex.Matcher headerMatcher = headerPattern.matcher(tcpDump);
        assertThat("Не удалось найти " + headerName + " в ТСП-дампе!", headerMatcher.find(), is(true));
        return headerMatcher.group(1);
    }

    public static List<String> getAllHeaders(TestMessage msg) throws MessagingException {
        List<String> headers = new ArrayList<String>();
        for (Enumeration e = msg.getAllHeaders(); e.hasMoreElements(); ) {
            Header h = (Header) e.nextElement();
            System.out.println(h.getName() + "--" + h.getValue());
            headers.add(h.getName() + "--" + h.getValue());
        }
        return headers;
    }

    public static class XYandexNotifyMsgHeader {
        private static final Pattern NOTIFY_HEADER_PATTERN = Pattern.compile("X-Yandex-NotifyMsg: (.*)");
        private static final Pattern FROM_PATTERN = Pattern.compile("<from>([^<]*)</from>");
        /*
<?xml version="1.0" encoding="utf-8"?>
<doc>
<notify>
<uid>107880417</uid>
<mid>2170000000017362658</mid>
<fid>2170000800000014828</fid>
<tid>2170000000013455401</tid>
<channel>lastmail</channel>
<login>meta.user@yandex.ru</login>
<data>
<subject>Elina responded to your Formspring question</subject>
<from>&quot;Formspring&quot; &lt;noreply@formspring.me&gt;</from>
<date>1377156925</date>
<servicename>formspring</servicename>
</data>
</notify>
</doc>
         */
        private String uid;
        private String mid;
        private String fid;
        private String tid;
        private String channel;
        private String login;
        private String subject;
        private String from;
        private String date;
        private String content;
        private String servicename;

        public XYandexNotifyMsgHeader(String base64Value) {
            LOG.info("Base64 encoded string:\n" + base64Value);
            content = new String(decodeBase64(base64Value));
            LOG.info("Decoded string:\n" + content);

            parseXML(content);
        }

        private void parseXML(String xml) {
            XpathConverter xpathConverter = DocumentConverter.from(xml);
            uid = xpathConverter.byXpath("//doc/notify/uid").asString();
            mid = xpathConverter.byXpath("//doc/notify/mid").asString();
            fid = xpathConverter.byXpath("//doc/notify/fid").asString();
            tid = xpathConverter.byXpath("//doc/notify/tid").asString();
            channel = xpathConverter.byXpath("//doc/notify/channel").asString();
            login = xpathConverter.byXpath("//doc/notify/login").asString();
            subject = xpathConverter.byXpath("//doc/notify/data/subject").asString();
            from = xpathConverter.byXpath("//doc/notify/data/from").asString();
            date = xpathConverter.byXpath("//doc/notify/data/date").asString();
            servicename = xpathConverter.byXpath("//doc/notify/data/servicename").asString();
        }


        public static String findNotifyMessageInLog(String tcpDump) {
            java.util.regex.Matcher notifyHeaderMatcher = NOTIFY_HEADER_PATTERN.matcher(tcpDump);
            assertThat("Не удалось найти X-Yandex-NotifyMsg в ТСП-дампе!", notifyHeaderMatcher.find(), is(true));
            return notifyHeaderMatcher.group(1);
        }


        public String getUid() {
            return uid;
        }

        public String getMid() {
            return mid;
        }

        public String getFid() {
            return fid;
        }

        public String getTid() {
            return tid;
        }

        public String getChannel() {
            return channel;
        }

        public String getLogin() {
            return login;
        }

        public String getSubject() {
            return subject;
        }

        public String getFrom() {
            return from;
        }

        public String getFromAsItIs() {
            java.util.regex.Matcher fromMatcher = FROM_PATTERN.matcher(content);
            assertThat("Не далось вытащить <from>, как он есть.", fromMatcher.find(), is(true));
            return fromMatcher.group(1);
        }

        public String getDate() {
            return date;
        }

        public String getContent() {
            return content;
        }

        public String getServicename() {
            return servicename;
        }
    }

    public static enum HeaderNames {
        X_YANDEX_STID("X-Yandex-Stid"),
        X_YANDEX_MDB("X-Yandex-Mdb"),
        X_YANDEX_UID("X-Yandex-Uid"),
        X_YANDEX_SUID("X-Yandex-Suid"),
        X_YANDEX_FID("X-Yandex-Fid"),
        X_YANDEX_LOGIN("X-Yandex-Login"),
        X_YANDEX_SSUID("X-Yandex-Ssuid"),
        X_YANDEX_RECEIVED("X-Yandex-Received"),
        X_YANDEX_THREAD_ID("X-Yandex-ThreadID"),
        X_YANDEX_NOTIFY_MSG("X-Yandex-NotifyMsg"),
        X_YANDEX_MID("X-Yandex-Mid"),

        X_YANDEX_META_FOLDER_NAME("X-Yandex-Meta-FolderName"),
        X_YANDEX_META_HDR_SUBJECT("X-Yandex-Meta-HdrSubject"),
        X_YANDEX_META_HDR_FROM("X-Yandex-Meta-HdrFrom"),
        X_YANDEX_META_HDR_TO("X-Yandex-Meta-HdrTo"),
        X_YANDEX_META_HDR_CC("X-Yandex-Meta-HdrCc"),
        X_YANDEX_META_HDR_BCC("X-Yandex-Meta-HdrBcc"),
        X_YANDEX_META_HDR_DATE("X-Yandex-Meta-HdrDate"),
        X_YANDEX_META_HDR_MESSAGE_ID("X-Yandex-Meta-HdrMessageId"),
        X_YANDEX_META_HDR_UIDL("X-Yandex-Meta-HdrUidl"),
        X_YANDEX_META_HDR_IN_REPLY_TO("X-Yandex-Meta-HdrInReplyTo"),
        X_YANDEX_HDR_REFERENCES("X-Yandex-HdrReferences"),
        X_YANDEX_META_MIXED("X-Yandex-Meta-Mixed"),
        X_YANDEX_META_LID("X-Yandex-Meta-Lid"),
        X_YANDEX_META_FIRSTLINE("X-Yandex-Meta-Firstline"),
        X_YANDEX_META_SIZE("X-Yandex-Meta-Size"),
        X_YANDEX_META_REPLY_TO("X-Yandex-Meta-ReplyTo"),
        X_YANDEX_META_REPLY_TO_ALL("X-Yandex-Meta-ReplyToAll"),
        X_YANDEX_META_METHOD_ID("X-Yandex-Meta-MethodID"),
        X_YANDEX_META_IP_FROM("X-Yandex-Meta-IpFrom"),
        X_YANDEX_META_SESSION_KEY("X-Yandex-Meta-SessionKey"),
        X_YANDEX_META_ATTACH("X-Yandex-Meta-Attach"),
        X_YANDEX_META_IMAP_LABELS("X-Yandex-Meta-ImapLabels"),

        X_YANDEX_KARMA_STATUS("X-Yandex-Karma-Status"),
        X_YANDEX_KARMA("X-Yandex-Karma"),
        X_BORN_DATE("X-BornDate"),
        RECEIVED_SPF("Received-SPF"),
        X_YANDEX_QUEUE_ID("X-Yandex-QueueID"),
        AUTH_RESULTS("Authentication-Results"),
        DKIM_SIGNATURE("DKIM-Signature"),
        X_YANDEX_UNIQ("X-Yandex-Uniq"),

        X_YANDEX_FILTER("X-Yandex-Filter"),
        X_YANDEX_SPAM("X-Yandex-Spam"),
        X_SPAM_FLAG("X-Spam-Flag"),
        RECEIVED("Received"),

        X_YANDEX_HINT("X-Yandex-Hint"),
        X_YANDEX_INSIGHT("X-Yandex-Insight"),
        X_YANDEX_FOLDER("X-Yandex-Folder"),
        X_YANDEX_SERVICE("X-Yandex-Service"),
        X_YANDEX_GREYLISTING("X-Yandex-Greylisting"),
        X_YANDEX_RCPT_SUID("X-Yandex-Rcpt-Suid"),
        X_YANDEX_SENDER_UID("X-Yandex-Sender-Uid"),
        X_YANDEX_TIMEMARK("X-Yandex-TimeMark"),
        RETURN_PATH("Return-Path"),

        X_YANDEX_FORWARD("X-Yandex-Forward"),
        X_YANDEX_SMS_DELIVERY("X-YandexSms-Delivery"),
        X_YANDEX_SMS_HOST("X-YandexSms-Host"),
        X_YANDEX_SMS_PHONE("X-YandexSms-Phone"),
        X_YANDEX_SMS_FROM("X-YandexSms-From"),
        X_YANDEX_LIVEMAIL("X-Yandex-Livemail"),
        X_YANDEX_CATCH_ALL("X-Yandex-Catch-All"),
        X_YANDEX_MX_CODE("X-Yandex-MXCode"),
        X_YANDEX_FOREIGN_MX("X-Yandex-ForeignMX"),
        X_YANDEX_RPOP_FOLDERNAME("X-yandex-rpop-foldername"),
        X_YANDEX_AVIR("X-Yandex-Avir"),
        X_YANDEX_CAPTCHA_ENTERED("X-Yandex-Captcha-Entered"),
        SUBJECT("Subject"),
        FROM("From"),
        TO("To"),
        CC("Cc"),
        BCC("BCC"),
        MIME_VERSION("MIME-Version"),
        DATE("Date"),
        MESSAGE_ID("Message-Id"),
        X_MAILER("X-Mailer"),
        CONTENT_TRANSFER_ENCODING("Content-Transfer-Encoding"),
        CONTENT_TYPE("Content-Type");

        private String name;

        private HeaderNames(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }


        public String findHeaderValueInLog(String tcpDump) {
            Pattern headerPattern = Pattern.compile(name + ": (.*)");
            java.util.regex.Matcher headerMatcher = headerPattern.matcher(tcpDump);
            assertThat("Не удалось найти " + name + " в ТСП-дампе!", headerMatcher.find(), is(true));
            return headerMatcher.group(1);
        }

        public List<String> findHeaderValuesInLog(String tcpDump) {
            Pattern headerPattern = Pattern.compile(name + ": (.*)");
            List<String> values = new ArrayList<String>();
            java.util.regex.Matcher headerMatcher = headerPattern.matcher(tcpDump);
            while (headerMatcher.find()) {
                values.add(headerMatcher.group(1));
            }
            return values;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final List<String> HEADERS_TO_REMOVE_FRONT_AND_SMTP = new ArrayList<String>() {{
        add(BCC.getName());
        add(X_YANDEX_SPAM.getName());
        add(X_YANDEX_INSIGHT.getName());
        add(X_YANDEX_FOLDER.getName());
        add(X_YANDEX_HINT.getName());
        add(X_YANDEX_SERVICE.getName());
        add(X_YANDEX_SUID.getName());
        add(X_YANDEX_GREYLISTING.getName());
        add(X_YANDEX_RCPT_SUID.getName());
        add(X_YANDEX_TIMEMARK.getName());
        add(AUTH_RESULTS.getName());
        add(RETURN_PATH.getName());
        add(X_YANDEX_LIVEMAIL.getName());
        add(X_YANDEX_RPOP_FOLDERNAME.getName());
        add(X_YANDEX_AVIR.getName());
        add(X_SPAM_FLAG.getName());
        add(X_YANDEX_CAPTCHA_ENTERED.getName());
    }};

    public static final Map<String, List<String>> HEADERS_TO_REMOVE_LISTS = new HashMap<String, List<String>>() {{
        put("mxfront-qa.cmail.yandex.net", HEADERS_TO_REMOVE_FRONT_AND_SMTP);
        put("mxback-qa.cmail.yandex.net",
                asList(X_YANDEX_RCPT_SUID.getName(), X_YANDEX_RPOP_FOLDERNAME.getName(),
                        BCC.getName(), X_YANDEX_CAPTCHA_ENTERED.getName()));
        put("yaback-qa.cmail.yandex.net", asList(X_YANDEX_RCPT_SUID.getName(),
                X_YANDEX_CAPTCHA_ENTERED.getName()));
        put("smtp-qa.cmail.yandex.net", HEADERS_TO_REMOVE_FRONT_AND_SMTP);
        put("mxfront-qa2.cmail.yandex.net", HEADERS_TO_REMOVE_FRONT_AND_SMTP);
        put("mxback-qa.mail.yandex.net",
                asList(X_YANDEX_RCPT_SUID.getName(), X_YANDEX_RPOP_FOLDERNAME.getName(),
                        BCC.getName(), X_YANDEX_CAPTCHA_ENTERED.getName()));
        put("yaback-qa2.cmail.yandex.net", asList(X_YANDEX_RCPT_SUID.getName(),
                X_YANDEX_CAPTCHA_ENTERED.getName()));
        put("smtp-qa2.cmail.yandex.net", HEADERS_TO_REMOVE_FRONT_AND_SMTP);
    }};
}