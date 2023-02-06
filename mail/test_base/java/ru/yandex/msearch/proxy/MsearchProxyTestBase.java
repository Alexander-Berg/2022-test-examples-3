package ru.yandex.msearch.proxy;

import java.nio.charset.CharacterCodingException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import ru.yandex.erratum.ErratumResult;
import ru.yandex.function.StringVoidProcessor;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.parser.uri.PctEncoder;
import ru.yandex.parser.uri.PctEncodingRule;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.test.util.TestBase;

public class MsearchProxyTestBase extends TestBase {
    protected static final String ALL_DOLLARS = "&all&json-type=dollar";
    protected static final String SEARCH_LIMITS =
        "\"search-limits\":{\"offset\":0,\"length\":200}";

    public static String doc(
        final String mid,
        final String common,
        final String... parts)
    {
        return doc(0L, mid, common, parts);
    }

    public static String doc(
        final long uid,
        final String mid,
        final String common,
        final String... parts)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; ++i) {
            if (i != 0) {
                sb.append("},{");
            }
            sb.append("\"uid\":");
            sb.append(uid);
            sb.append(",\"mid\":\"");
            sb.append(mid);
            sb.append("\",\"url\":\"");
            sb.append(uid);
            sb.append('_');
            sb.append(mid);
            sb.append('/');
            sb.append(i);
            sb.append('"');
            if (!common.isEmpty()) {
                sb.append(',');
                sb.append(common);
            }
            String part = parts[i];
            boolean appendHid = true;
            if (!part.isEmpty()) {
                sb.append(',');
                sb.append(part);
                appendHid = part.indexOf("\"hid\"") == -1;
            }
            if (appendHid) {
                sb.append(",\"hid\":");
                sb.append(i);
            }
        }
        return new String(sb);
    }

    protected static String fixedDoc(
        final String mid,
        final String subject,
        final String body)
    {
        return doc(
            mid,
            "\"hdr_to_normalized\":\"vonidu@yandex-team.ru\""
            + ",\"received_date\":\"1234567890\""
            + ",\"hdr_from\":\"test@yandex.ru\""
            + ",\"hdr_subject\":\"" + subject + "\"",
            "\"pure_body\":\"" + body + "\"");
    }

    protected static String envelope(final String mid, final String... attrs) {
        StringBuilder sb = new StringBuilder("{\"mid\":\"");
        sb.append(mid);
        sb.append('"');
        for (String attr: attrs) {
            sb.append(',');
            sb.append(attr);
        }
        sb.append('}');
        return new String(sb);
    }

    protected static String serp(
        final String request,
        final boolean pure,
        final String... envelopes)
    {
        return serpRequestless(
            "\"request\":\"" + request + '"',
            pure,
            envelopes);
    }

    protected static String serpRequestless(
        final String request,
        final boolean pure,
        final String... envelopes)
    {
        StringBuilder sb = new StringBuilder(
            "\"details\":{\"crc32\":\"0\"," + SEARCH_LIMITS
                + ",\"total-found\":"
                + envelopes.length
                + ",\"search-options\":{");

        sb.append(request);
        if (pure) {
            sb.append(",\"pure\":true");
        }
        sb.append("}},");

        return envelopes(new String(sb), envelopes);
    }

    protected static String envelopes(
        final String prologue,
        final String... envelopes)
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(prologue);
        sb.append("\"envelopes\":[");
        for (int i = 0; i < envelopes.length; ++i) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(envelopes[i]);
        }
        sb.append("]}");
        return new String(sb);
    }

    protected static String threadLabels(
        final String... labels)
    {
        StringBuilder sb = new StringBuilder("\"threadLabels\": [");
        for (int i = 0; i < labels.length; ++i) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append('{');
            sb.append(labels[i]);
            sb.append('}');
        }

        sb.append("],");
        return sb.toString();
    }

    protected static String erratum(
        final MsearchProxyCluster cluster,
        final String request,
        final String erratum)
        throws Exception
    {
        QueryConstructor query =
            new QueryConstructor("/misspell.json/check?srv=mail-search");
        if (request != null) {
            query.append("options", "321");
            query.append("text", request);
        }

        StringBuilderWriter sb = new StringBuilderWriter();
        try (JsonWriter jsonWriter = new JsonWriter(sb)) {
            jsonWriter.startObject();
            jsonWriter.key("code");
            if (erratum == null) {
                jsonWriter.value(ErratumResult.CODE_NOT_CORRECTED);
            } else {
                jsonWriter.value(ErratumResult.CODE_CORRECTED);
                jsonWriter.key("lang");
                jsonWriter.value("ru,en");
                jsonWriter.key("rule");
                jsonWriter.value("Misspell");
                jsonWriter.key("flags");
                jsonWriter.value(0);
                jsonWriter.key("r");
                jsonWriter.value(8000);
                jsonWriter.key("text");
                jsonWriter.value(erratum);
            }

            jsonWriter.key("srcText");
            jsonWriter.value(request);
            jsonWriter.endObject();
        }

        String uri = query.toString();
        cluster.erratum().add(uri, sb.toString());
        return uri;
    }

    protected static void filterSearch(
        final MsearchProxyCluster cluster,
        final String uri,
        final String... mids)
    {
        String[] envelopes = new String[mids.length];

        StringBuilder sb = new StringBuilder(uri);
        for (int i = 0; i < mids.length; i++) {
            sb.append("&mids=");
            sb.append(mids[i]);
            envelopes[i] = envelope(mids[i]);
        }

        System.err.println("FSuri " + sb.toString());
        cluster.filterSearch().add(sb.toString(), envelopes("", envelopes));
    }

    protected static String searchOk(
        final MsearchProxyCluster cluster,
        final CloseableHttpClient client,
        final String params)
        throws Exception
    {
        String uri = cluster.proxy().host()
            + params;
        StringVoidProcessor<char[], CharacterCodingException> encoder =
            new StringVoidProcessor<>(new PctEncoder(PctEncodingRule.FRAGMENT));
        encoder.process(uri);
        HttpGet get = new HttpGet(encoder.toString());
        get.setHeader(HttpHeaders.CONNECTION, "close");
        try (CloseableHttpResponse response = client.execute(get)) {
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            return CharsetUtils.toString(response.getEntity());
        }
    }

    protected static String systemFolders() {
        return "\"1\":{\"name\":\"Inbox\",\"isUser\":false," +
            "\"isSystem\":true,\"type\":{\"code\":3," +
            "\"title\":\"system\"},\"symbolicName\":{\"code\":1," +
            "\"title\":\"inbox\"},\"bytes\":12720961," +
            "\"messagesCount\":161,\"newMessagesCount\":70," +
            "\"recentMessagesCount\":152,\"unvisited\":false," +
            "\"folderOptions\":{\"getPosition\":0},\"position\":0," +
            "\"parentId\":\"0\",\"pop3On\":\"0\",\"scn\":\"0\"," +
            "\"creationTime\":\"1379749025\",\"subscribed\":\"1\"," +
            "\"shared\":\"0\"},\"2\":{\"name\":\"Spam\"," +
            "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
            "\"title\":\"system\"},\"symbolicName\":{\"code\":4," +
            "\"title\":\"spam\"},\"bytes\":0,\"messagesCount\":0," +
            "\"newMessagesCount\":0,\"recentMessagesCount\":0," +
            "\"unvisited\":false," +
            "\"folderOptions\":{\"getPosition\":0},\"position\":0," +
            "\"parentId\":\"0\",\"pop3On\":\"0\",\"scn\":\"0\"," +
            "\"creationTime\":\"1379749025\",\"subscribed\":\"1\"," +
            "\"shared\":\"0\"},\"3\":{\"name\":\"Trash\"," +
            "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
            "\"title\":\"system\"},\"symbolicName\":{\"code\":3," +
            "\"title\":\"trash\"},\"bytes\":0,\"messagesCount\":0," +
            "\"newMessagesCount\":0,\"recentMessagesCount\":0," +
            "\"unvisited\":false," +
            "\"folderOptions\":{\"getPosition\":0},\"position\":0," +
            "\"parentId\":\"0\",\"pop3On\":\"0\",\"scn\":\"0\"," +
            "\"creationTime\":\"1379749025\",\"subscribed\":\"1\"," +
            "\"shared\":\"0\"}," +
            "\"4\":{\"name\":\"Sent\"," +
            "\"isUser\":false,\"isSystem\":true,\"type\":{\"code\":3," +
            "\"title\":\"system\"},\"symbolicName\":{\"code\":2," +
            "\"title\":\"sent\"},\"bytes\":24483112," +
            "\"messagesCount\":18,\"newMessagesCount\":0," +
            "\"recentMessagesCount\":18,\"unvisited\":false," +
            "\"folderOptions\":{\"getPosition\":0},\"position\":0," +
            "\"parentId\":\"0\",\"pop3On\":\"0\",\"scn\":\"0\"," +
            "\"creationTime\":\"1379749025\",\"subscribed\":\"1\"," +
            "\"shared\":\"0\"}";
    }
}

