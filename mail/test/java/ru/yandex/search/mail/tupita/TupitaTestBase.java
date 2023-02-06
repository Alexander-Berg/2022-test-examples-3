package ru.yandex.search.mail.tupita;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.message.BasicHeader;

import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.test.util.TestBase;

public class TupitaTestBase extends TestBase {
    // CSOFF: MultipleStringLiterals
    protected static final String HTTP_LOCALHOST = "http://localhost:";
    protected static final String CHECK = "/check?uid=";
    protected static final String FAT_CHECK = "/fat-check?uid=";
    protected static final String UID = "227356512";
    protected static final String TICKAITE_RSP =
        "{\"docs\":[{\"hid\":\"1\",\"body_text\":\"hello\"}]}";

    protected static final String STID =
        "320.mail:227356512.E402922:19768727190313626561762053100";
    protected static final String KEY_STID = "STID";
    protected static final String SRW_NS_MAIL = "MAIL";

    protected static final String TICKET =
        "2:31B:1509878052:107:CJ-JehCz_ZqRpdT-AQ:MHvE5";
    protected static final String TICKET_HEADER = "TVM " + TICKET;

    protected static final String TIKAITE_BASE_URI =
        "/mail/handler?json-type=dollar&fast-mode&stid=";

    protected static final String TIKAITE_URI =
        TIKAITE_BASE_URI + STID;

    protected String message(final String stid, final String extra) {
        return message(stid, extra, false);
    }

    protected String message(final String stid) {
        return message(stid, "", false);
    }

    protected String message(
        final String stid,
        final String extra,
        final boolean spam)
    {
        String result = ",\"message\": {\"subject\" : \"Hello dear friend test!@#%^&()+\",\n"
            + "         \"types\" : [4,52],"
            + "         \"uid\" : \"227356512\",\n"
            + "         \"to\" : [{\n"
            + "               \"domain\" : \"gmail.com\",\n"
            + "               \"local\" : \"vonidu\",\n"
            + "               \"displayName\" : \"Ivan Dudinov\"\n"
            + "            }],\n"
            + "         \"from\" : [{\n"
            + "               \"domain\" : \"yandex.ru\","
            + "               \"local\" : \"ivan.dudinov\","
            + "               \"displayName\" : \"Ivan Dudinov\"}],\n"
            + "         \"stid\" : \"" + stid + '\"';
        if (!extra.isEmpty()) {
            result += ',' + extra;
        }
        if (spam) {
            result += ",\"spam\":\"true\"}}";
        } else {
            result += "}}";
        }

        return result;
    }

    protected String query(
        final String id,
        final String text)
    {
        return query(id, text, false);
    }

    protected String query(
        final String id,
        final String text,
        final boolean stop)
    {
        return "{\"id\":\"" + id + "\",\"query\":\""
            + text + "\", \"stop\":\"" + stop + "\"}";
    }

    protected String queries(final String... queries) {
        return "{\"queries\": [" + String.join(",", queries) + ']';
    }

    protected String userQueries(final String uid, final String... queries) {
        return "{\"uid\":" + uid + ", \"queries\": ["
            + String.join(",", queries)
            + "]}";
    }

    protected String userQueries(
        final String uid,
        final boolean spam,
        final String... queries)
    {
        return "{\"uid\":" + uid + ", \"spam\":\"" + spam + "\", \"queries\": ["
            + String.join(",", queries)
            + "]}";
    }

    protected String request(final String message, final String... users) {
        return "{\"users\":["
            + String.join(",", users)
            + ']' + message;
    }

    protected StaticHttpResource tikaiteResp(
        final String stid)
        throws IOException
    {
        String resId = stid;
        if (stid.contains(":")) {
            resId = stid.split(":")[2] + ".tikaite";
        }

        return tikaiteResp(stid, resId);
    }

    protected StaticHttpResource tikaiteResp(
        final String stid,
        final String resource)
        throws IOException
    {
        String tikaiteData =
            new String(
                IOStreamUtils.consume(
                    getClass().getResourceAsStream(resource)).toByteArray(),
                StandardCharsets.UTF_8);

        return new StaticHttpResource(
            new ExpectingHeaderHttpItem(
                new StaticHttpItem(tikaiteData),
                new BasicHeader(
                    YandexHeaders.X_SRW_KEY,
                    stid),
                new BasicHeader(
                    YandexHeaders.X_SRW_NAMESPACE,
                    SRW_NS_MAIL),
                new BasicHeader(
                    YandexHeaders.X_SRW_KEY_TYPE,
                    KEY_STID)));
    }

    // CSON: MultipleStringLiterals
}
