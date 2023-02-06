package ru.yandex.tikaite.server;

import java.io.File;
import java.util.Map;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tika.mime.MediaType;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.search.document.mail.MailMetaInfo;
import ru.yandex.test.util.TestBase;
import ru.yandex.tikaite.util.CommonFields;
import ru.yandex.tikaite.util.Json;

public class DmarcTest extends TestBase {
    private static final Long SUID = Long.valueOf(9000);
    private static final String MID = "100500";
    private static final String ONE_ONE = "1.1";
    private static final String ONE_ONE_ONE = "1.1.1";
    private static final String ONE_TWO = "1.2";
    private static final String GET = "/get/";
    private static final String ATTACHMENT = "attachment";
    private static final String GZ = "gz";
    private static final String PARSE_DMARC = "&parse-dmarc";
    private static final String TO = "dmarc_rep_co@yandex-team.ru";
    private static final String POLICY_PUBLISHED =
        "\"policy_published\":{"
        + "\"domain\":\"yandex-team.ru\","
        + "\"adkim\":\"r\","
        + "\"aspf\":\"r\","
        + "\"p\":\"none\","
        + "\"pct\":\"100\"},";
    private static final String FROM_YANDEX_TEAM =
        "\"header_from\":\"yandex-team.ru\",";
    private static final String COUNT_1 =
        "\"count\":\"1\",";
    private static final String POLICY_EVALUATED =
        "\"policy_evaluated\":{"
        + "\"disposition\":\"none\","
        + "\"dkim\":\"pass\","
        + "\"spf\":\"pass\"},";
    private static final String AUTH_RESULTS =
        "\"dkim\":["
        + "{\"dkim_domain\":\"yandex-team.ru\","
        + "\"dkim_result\":\"pass\"}],"
        + "\"spf\":["
        + "{\"spf_domain\":\"yandex-team.ru\","
        + "\"spf_result\":\"pass\"}]}";

    private void checkMail(
        final String name,
        final Json json,
        final String uriSuffix)
        throws Exception
    {
        try (CloseableHttpClient client = HttpClients.createDefault();
            StaticServer backend = new StaticServer(Configs.baseConfig());
            Server server = new Server(ServerTest.getConfig(backend.port())))
        {
            backend.add(
                GET + name + "?raw",
                new File(getClass().getResource(name).toURI()));
            backend.start();
            server.start();
            OnlineMailHandlerTest.checkMail(
                client,
                new HttpGet(
                    server.host() + GET + name + "?name=mail&mid=" + MID
                    + "&suid=" + SUID + uriSuffix),
                json);
        }
    }

    // CSOFF: MagicNumber
    private void testDmarcGzip(final String body, final String uriSuffix)
        throws Exception
    {
        Json json = new Json(null, MID, SUID);

        Map<String, Object> doc = json.createDoc(ONE_ONE_ONE);
        doc.put(MailMetaInfo.HEADERS, Json.ANY_VALUE);
        String smtpId = "SqJ4rLg0SP-5kxe9K1p";
        String receivedDate = "1475885147";
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "Find attached the DMARC Aggregate Report.");
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, Json.ANY_VALUE);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, TO);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            TO + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            TO + '\n');
        String from = "MAILER-DAEMON@bemtal10.swift.com";
        String fromNormalized = "mailer-daemon@bemtal10.swift.com";
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            from + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromNormalized + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SENDER, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SENDER + MailMetaInfo.EMAIL,
            from + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SENDER + MailMetaInfo.NORMALIZED,
            fromNormalized + '\n');
        String subject =
            "Report Domain: yandex-team.ru Submitter: bemtal10.swift.com "
            + "Report-ID: "
            + "<a86bbf$264b0c1=ee02bba313e46551@bemtal10.swift.com>";
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);

        doc = json.createDoc(ONE_TWO);
        doc.put(MailMetaInfo.HEADERS, Json.ANY_VALUE);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        if (body.isEmpty()) {
            doc.put(CommonFields.PARSED, false);
        } else {
            doc.put(CommonFields.PARSED, true);
            doc.put(CommonFields.BODY_TEXT, body);
            doc.put(
                CommonFields.META,
                "Content-Type:application/x-dmarc-rua+gzip");
            doc.put(
                MailMetaInfo.X_URLS,
                "http://bemtal10.swift.com\nhttp://yandex-team.ru\n");
        }
        doc.put(CommonFields.MIMETYPE, "application/x-dmarc-rua+gzip");
        doc.put(MailMetaInfo.CONTENT_TYPE, "application/gzip");
        doc.put(MailMetaInfo.ATTACHTYPE, GZ);
        doc.put(MailMetaInfo.ATTACHSIZE, 601L);
        doc.put(
            MailMetaInfo.ATTACHNAME,
            "bemtal10.swift.com!yandex-team.ru!1475798403!1475884804.xml.gz");
        doc.put(MailMetaInfo.MD5, "588A06CAF70109E14E2DD8AC7152AED2");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, TO);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            TO + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            TO + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            from + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromNormalized + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SENDER, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SENDER + MailMetaInfo.EMAIL,
            from + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SENDER + MailMetaInfo.NORMALIZED,
            fromNormalized + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);

        checkMail("dmarc-gzip.eml", json, uriSuffix);
    }

    @Test
    public void testDisabledDmarcGzip() throws Exception {
        testDmarcGzip("", "");
    }

    @Test
    public void testEnabledDmarcGzip() throws Exception {
        testDmarcGzip(
            "{\"org_name\":\"bemtal10.swift.com\","
            + "\"email\":\"MAILER-DAEMON@bemtal10.swift.com\","
            + "\"report_id\":\"a86bbf$264b0c1=ee02bba313e46551@bemtal10"
            + ".swift.com\","
            + "\"date_range_begin\":\"1475798403\","
            + "\"date_range_end\":\"1475884804\","
            + POLICY_PUBLISHED
            + FROM_YANDEX_TEAM
            + "\"source_ip\":\"5.255.216.106\","
            + COUNT_1
            + POLICY_EVALUATED
            + AUTH_RESULTS,
            PARSE_DMARC);
    }

    @Test
    public void testDmarcZip() throws Exception {
        Json json = new Json(null, MID, SUID);

        String gatewayReceivedDate = "1505174603";
        String smtpId = "bMXpFcwHxM-3MROvImP";
        String receivedDate = "1505174602";
        String from = "postmaster@bigcom.ru";
        String subject =
            "Report Domain: yandex-team.ru Submitter: bigcom.ru "
            + "Report-ID: 47467f52e12445ef815405e157e485a5";

        Map<String, Object> doc = json.createDoc(ONE_ONE);
        doc.put(MailMetaInfo.HEADERS, Json.ANY_VALUE);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, gatewayReceivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(CommonFields.PARSED, true);
        doc.put(CommonFields.BODY_TEXT, "");
        doc.put(
            MailMetaInfo.PURE_BODY,
            "This is an aggregate DMARC report from bigcom.ru\n"
            + "Report domain: yandex-team.ru\n"
            + "Submitter: bigcom.ru\n"
            + "Report-ID: 47467f52e12445ef815405e157e485a5\n"
            + ": Message contains [1] file attachments");
        doc.put(
            CommonFields.MIMETYPE,
            MediaType.TEXT_PLAIN.getBaseType().toString());
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(
            CommonFields.META,
            "Content-Type:text/plain; charset=windows-1251");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, TO);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            TO + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            TO + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            from + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            from + '\n');
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL, from + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            from + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(
            MailMetaInfo.X_URLS,
            "http://bigcom.ru\nhttp://yandex-team.ru\n");

        doc = json.createDoc(ONE_TWO);
        doc.put(MailMetaInfo.HEADERS, Json.ANY_VALUE);
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, gatewayReceivedDate);
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(CommonFields.PARSED, true);
        String prefix =
            "{\"org_name\":\"bigcom.ru\","
            + "\"email\":\"postmaster@bigcom.ru\","
            + "\"report_id\":\"47467f52e12445ef815405e157e485a5\","
            + "\"date_range_begin\":\"1505088000\","
            + "\"date_range_end\":\"1505174399\","
            + POLICY_PUBLISHED
            + FROM_YANDEX_TEAM;
        doc.put(
            CommonFields.BODY_TEXT,
            prefix
            + "\"source_ip\":\"37.9.109.162\","
            + COUNT_1
            + POLICY_EVALUATED
            + AUTH_RESULTS
            + '\n'
            + prefix
            + "\"source_ip\":\"5.255.227.192\","
            + COUNT_1
            + POLICY_EVALUATED
            + AUTH_RESULTS
            + '\n'
            + prefix
            + "\"source_ip\":\"5.255.227.202\","
            + COUNT_1
            + POLICY_EVALUATED
            + AUTH_RESULTS);
        doc.put(
            MailMetaInfo.X_URLS,
            "http://bigcom.ru\nhttp://yandex-team.ru\n");
        doc.put(
            CommonFields.META,
            "Content-Type:application/x-dmarc-rua+zip");
        doc.put(CommonFields.MIMETYPE, "application/x-dmarc-rua+zip");
        doc.put(MailMetaInfo.CONTENT_TYPE, "application/zip");
        doc.put(MailMetaInfo.ATTACHTYPE, "zip");
        doc.put(MailMetaInfo.ATTACHSIZE, 1079L);
        doc.put(
            MailMetaInfo.ATTACHNAME,
            "bigcom.ru!yandex-team.ru!1505088000!1505174399!80.zip");
        doc.put(MailMetaInfo.MD5, "3A570978C8E3E599A0A0A2103AE57388");
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, TO);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            TO + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            TO + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            from + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            from + '\n');
        doc.put(MailMetaInfo.REPLY_TO_FIELD, from);
        doc.put(MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.EMAIL, from + '\n');
        doc.put(
            MailMetaInfo.REPLY_TO_FIELD + MailMetaInfo.NORMALIZED,
            from + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);

        checkMail("dmarc-zip.eml", json, PARSE_DMARC);
    }

    @Test
    public void testBadDmarc() throws Exception {
        Json json = new Json(null, MID, SUID);

        Map<String, Object> doc = json.createDoc(Integer.toString(1));
        doc.put(MailMetaInfo.HEADERS, Json.ANY_VALUE);
        String receivedDate = "1475885148";
        doc.put(MailMetaInfo.GATEWAY_RECEIVED_DATE, receivedDate);
        String smtpId = "SqJ4rLg0SP-5kxe9K1p";
        doc.put(MailMetaInfo.SMTP_ID, smtpId);
        doc.put(MailMetaInfo.ALL_SMTP_IDS, smtpId);
        doc.put(MailMetaInfo.RECEIVED_DATE, receivedDate);
        doc.put(CommonFields.PARSED, true);
        doc.put(
            CommonFields.BODY_TEXT,
            "1.0\nbemtal10.swift.com\nMAILER-DAEMON@bemtal10.swift.com\n"
            + "a86bbf$264b0c1=ee02bba313e46551@bemtal10.swift.com\n1475798403"
            + "\n1475884804\nyandex-team.ru\nr\nr\nnone\n100\n5.255.216.106\n1"
            + "\nnone\npass\npass\nyandex-team.ru\nyandex-team.ru\n"
            + "yandex-team.ru\ndefault\npass\nyandex-team.ru\nmfrom\npass");
        doc.put(CommonFields.MIMETYPE, "application/gzip");
        doc.put(MailMetaInfo.CONTENT_TYPE, doc.get(CommonFields.MIMETYPE));
        doc.put(CommonFields.META, Json.ANY_VALUE);
        doc.put(MailMetaInfo.ATTACHTYPE, GZ);
        doc.put(MailMetaInfo.ATTACHSIZE, 543L);
        doc.put(
            MailMetaInfo.ATTACHNAME,
            "bad-dmarc.xml.gz");
        doc.put(MailMetaInfo.MD5, "F141236ACFB3A1E3C45DEB9F50D80B92");
        doc.put(MailMetaInfo.DISPOSITION_TYPE, ATTACHMENT);
        doc.put(MailMetaInfo.HDR + MailMetaInfo.TO, TO);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.EMAIL,
            TO + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.TO + MailMetaInfo.NORMALIZED,
            TO + '\n');
        String from = "MAILER-DAEMON@bemtal1.swift.com";
        String fromNormalized = "mailer-daemon@bemtal1.swift.com";
        doc.put(MailMetaInfo.HDR + MailMetaInfo.FROM, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.EMAIL,
            from + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.FROM + MailMetaInfo.NORMALIZED,
            fromNormalized + '\n');
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SENDER, from);
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SENDER + MailMetaInfo.EMAIL,
            from + '\n');
        doc.put(
            MailMetaInfo.HDR + MailMetaInfo.SENDER + MailMetaInfo.NORMALIZED,
            fromNormalized + '\n');
        String subject =
            "Report Domain: yandex-team.ru Submitter: bemtal1.swift.com "
            + "Report-ID:"
            + " <a86bbf$264b0c1=ee02bba313e46551@bemtal1.swift.com>";
        doc.put(MailMetaInfo.HDR + MailMetaInfo.SUBJECT, subject);
        doc.put(
            MailMetaInfo.X_URLS,
            "http://bemtal10.swift.com\nhttp://yandex-team.ru\n");
        checkMail("dmarc-bad-gzip.eml", json, PARSE_DMARC);
    }
    // CSON: MagicNumber
}

