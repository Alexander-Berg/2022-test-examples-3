package ru.yandex.mail.so.logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.james.mime4j.stream.EntityState;
import org.apache.james.mime4j.stream.MimeTokenStream;
import org.junit.Assert;
import org.junit.ComparisonFailure;

import ru.yandex.base64.Base64;
import ru.yandex.data.compressor.DataCompressor;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.NotImplementedException;
import ru.yandex.http.util.YandexReasonPhraseCatalog;
import ru.yandex.mail.mime.DefaultMimeConfig;
import ru.yandex.mail.mime.OverwritingBodyDescriptorBuilder;
import ru.yandex.mail.mime.Utf8FieldBuilder;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class SpLoggerTestBase extends TestBase {
    protected static final int BUFFER_SIZE = 1024;
    protected static final long TIMEOUT = 2000L;
    protected static final int MDS_BATCH_SIZE_MIN = 5000;
    protected static final int MDS_BATCH_SIZE_MIDDLE = 40000;
    protected static final int MDS_BATCH_SIZE_EXCEEDED = 60000;
    protected static final String LOG_TYPE = "delivery";
    protected static final String ROUTE_IN = Route.IN.lowerName();
    protected static final String ROUTE_OUT = Route.OUT.lowerName();
    protected static final String ROUTE_CORP = Route.CORP.lowerName();
    protected static final String QUEUEID1 = "AgrGIn8qE5-Lk94t8Mp";
    protected static final String QUEUEID2 = "OI5H1Ubrg4-hSA8f4HG";
    protected static final int CODE1 = 127;
    protected static final int CODE2 = 4;
    protected static final String MSGID1 = "%3CED1031D3-92B8-11AB-0004-DC790017629A@promo.wildberries.ru%3E";
    protected static final String MSGID2 = "%3C20210212174329.hSA8f4HG@myt5-4fdc2b48514c.qloud-c.yandex.net%3E";
    protected static final String FROMADDR1 = "info@promo.wildberries.ru";
    protected static final String FROMADDR2 = "mailer-daemon@comcast.net";
    protected static final String LOCL1 = "vla2-8356-c95-vla-spdaemon-in-18274.gencfg-c.yandex.net";
    protected static final String LOCL2 = "myt1-1987-77b-msk-myt-spdaemon-in-18274.gencfg-c.yandex.net";
    protected static final String MX_FRONT = "mxfront";
    protected static final String RCPT_UIDS1 = "57133661";
    protected static final String RCPT_UIDS2 = "1130000050148320";
    protected static final String SOURCE_IP1 = "185.62.201.11";
    protected static final String SOURCE_IP2 = "2001%3A558%3Afe21%3A29%3A69%3A252%3A207%3A34";
    protected static final long TS1 = 1611843706L;
    protected static final long TS2 = 1613141009L;
    protected static final String STAT = "/stat";
    protected static final String URI_IN = '/' + LOG_TYPE + '/' + ROUTE_IN + '?';
    protected static final String URI_PUT = "/check/?" + SearchParam.ROUTE.paramName() + '=' + ROUTE_IN + '&';
    protected static final String URI_SEARCH_IN = DeliveryLogRecordsHandler.SEARCH_URI + SearchParam.ROUTE.paramName()
        + '=' + ROUTE_IN + '&';
    protected static final String URI_SEARCH_OUT = DeliveryLogRecordsHandler.SEARCH_URI + "&"
        + SearchParam.ROUTE.paramName() + '=' + ROUTE_OUT + '&';
    protected static final String URI_SEARCH_CORP = DeliveryLogRecordsHandler.SEARCH_URI + "&"
        + SearchParam.ROUTE.paramName() + '=' + ROUTE_CORP + '&';
    protected static final String URI_SEARCH_GETBYID = DeliveryLogRecordsHandler.SEARCH_GETBYID_URI
        + SearchParam.ROUTE.paramName() + '=' + ROUTE_IN + "&getbyid=1&";
    protected static final String URI_PUT_PARAMS1 = SearchParam.CODE.paramName() + '=' + CODE1 + '&'
        + SearchParam.FROMADDR.paramName() + '=' + FROMADDR1 + '&' + SearchParam.LOCL.paramName() + '=' + LOCL1 + '&'
        + SearchParam.MSGID.paramName() + '=' + MSGID1 + '&' + SearchParam.MX.paramName() + '=' + MX_FRONT + '&'
        + SearchParam.QUEUEID.paramName() +'=' + QUEUEID1 + '&' + SearchParam.RCPT_UIDS.paramName() + '=' + RCPT_UIDS1
        + '&' + SearchParam.SOURCE_IP.paramName() + '=' + SOURCE_IP1 + '&' + SearchParam.TS.paramName() + '=' + TS1
        + '&' + SearchParam.UID.paramName() + '=';
    protected static final String URI_PUT_PARAMS2 = SearchParam.CODE.paramName() + '=' + CODE2 + '&'
        + SearchParam.FROMADDR.paramName() + '=' + FROMADDR2 + '&' + SearchParam.LOCL.paramName() + '=' + LOCL2 + '&'
        + SearchParam.MSGID.paramName() + '=' + MSGID2 + '&' + SearchParam.MX.paramName() + '=' + MX_FRONT + '&'
        + SearchParam.QUEUEID.paramName() +'=' + QUEUEID2 + '&' + SearchParam.RCPT_UIDS.paramName() + '=' + RCPT_UIDS2
        + '&' + SearchParam.SOURCE_IP.paramName() + '=' + SOURCE_IP2 + '&' + SearchParam.TS.paramName() + '=' + TS2
        + '&' + SearchParam.UID.paramName() + '=';
    protected static final String URI_UNPREFIXED_SEARCH1 = SpLogger.UNPREFIXED_PARALLEL
        + DeliveryLogRecordsHandler.SEARCH_URI + "get=log_bytes_offset,log_bytes_size,log_code,log_fromaddr,"
        + "log_locl,log_msgid,log_mx,log_offset,log_queueid,log_rcpt_uid,log_route,log_size,log_source_ip,log_stid,"
        + "log_ts,log_type,log_uid&text=" + IndexField.ROUTE.fieldName() + ':' + ROUTE_IN + "+AND+";
    protected static final String DELIVERY_LOG1 = "delivery_log1.json";
    protected static final String DELIVERY_LOG2 = "delivery_log2.json";
    protected static final String STID1 = "5653669/b8dfefdaf45540acb8c5f9b41ab236d8";
    protected static final String STID2 = "4405727/032dca27b843417d8f59f07d01a8f263";

    protected void mockAuxiliaryStorage(final SpLoggerCluster cluster, final Route route, final String logBody) {
        cluster.auxiliaryStorage().add(AuxiliaryStorageCluster.STORE_DELIVERY + '/' + route.lowerName(), logBody);
    }

    protected void testOnePutRequest(
        final SpLoggerCluster cluster,
        final CloseableHttpClient client,
        final String uri,
        final String body,
        final boolean lzoCompressed)
        throws Exception
    {
        mockAuxiliaryStorage(cluster, Route.IN, body);
        HttpPost post = new HttpPost(cluster.spLogger().host() + uri);
        String payload = lzoCompressed ? DataCompressor.LZO.compressAndBase64(body, Base64.URL) : body;
        ContentType contentType = lzoCompressed ? ContentType.APPLICATION_FORM_URLENCODED : SpLogger.TEXT_PLAIN;
        post.setEntity(new StringEntity(payload, contentType));
        try (CloseableHttpResponse httpResponse = client.execute(post)) {
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, httpResponse);
        }
    }

    protected void testSearchRequest(
        final SpLoggerCluster cluster,
        final CloseableHttpClient client,
        final String getUri,
        final Set<String> expectedResponseBodies)
        throws Exception
    {
        HttpGet get = new HttpGet(cluster.spLogger().host() + getUri);
        logger.info("testSearchRequest: " + cluster.spLogger().host() + getUri);
        try (CloseableHttpResponse httpResponse = client.execute(get)) {
            int status = httpResponse.getStatusLine().getStatusCode();
            String response = CharsetUtils.toString(httpResponse.getEntity());
            logger.info("SpLoggerTest.testSearchRequest: GET response = '" + response + "', size="
                + response.length());
            if (expectedResponseBodies.isEmpty()) {
                assertStatusCode(HttpStatus.SC_NOT_FOUND, status);
            } else {
                assertStatusCode(HttpStatus.SC_OK, status);
                HashSet<String> actualBodies = new HashSet<>(Arrays.asList(response.split("\n")));
                assertJsonSetsEquals("Unequal response", expectedResponseBodies, actualBodies);
            }
        }
    }

    public static void assertStatusCode(final int expected, final int status)
    {
        if (status != expected) {
            String msg = "Expected " + expected + ' '
                + YandexReasonPhraseCatalog.INSTANCE.getReason(expected, Locale.ENGLISH) + " but received " + status
                + ' ' + YandexReasonPhraseCatalog.INSTANCE.getReason(status, Locale.ENGLISH);
            Assert.fail(msg);
        }
    }

    protected String readFilesLine(final String fileName, final int lineNo) throws Exception {
        Path path = Paths.get(getClass().getResource(fileName).toURI());
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            int i = 0;
            for (;;) {
                String line = reader.readLine();
                if (line == null) {
                    logger.info("readFilesLine: fileName=" + fileName + ", i=" + i + ", line=null");
                    break;
                } else if (++i == lineNo) {
                    return line;
                } else if (i > lineNo) {
                    logger.info("readFilesLine: fileName=" + fileName + ", i=" + i);
                    break;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    protected static class MultipartAssert implements HttpRequestHandler {
        private final List<JsonChecker> checkers;

        MultipartAssert(final JsonChecker... checkers) {
            this.checkers = Arrays.asList(checkers);
        }

        @Override
        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context)
                throws NotImplementedException
        {
            MimeTokenStream stream = new MimeTokenStream(
                    DefaultMimeConfig.INSTANCE,
                    null,
                    new Utf8FieldBuilder(),
                    new OverwritingBodyDescriptorBuilder());
            try {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                char[] buf = new char[BUFFER_SIZE];
                List<String> requests = new ArrayList<>();
                stream.parseHeadless(entity.getContent(), entity.getContentType().getValue());
                EntityState state = stream.getState();
                while (state != EntityState.T_END_OF_STREAM) {
                    switch (state) {
                        case T_BODY:
                            StringBuilder sb = new StringBuilder();
                            try (Reader reader = new InputStreamReader(
                                    stream.getDecodedInputStream(),
                                    StandardCharsets.UTF_8))
                            {
                                int read = reader.read(buf);
                                while (read != -1) {
                                    sb.append(buf, 0, read);
                                    read = reader.read(buf);
                                }
                            }
                            requests.add(new String(sb));
                            break;
                        default:
                            break;
                    }
                    state = stream.next();
                }
                Assert.assertEquals(checkers, requests);
            } catch (Throwable t) {
                throw new NotImplementedException(t);
            }
        }
    }

    public static void assertJsonSetsEquals(String message, Set<String> expecteds, Set<String> actuals) {
        if (actuals == null && expecteds == null) {
            return;
        }
        if (actuals == null || expecteds == null) {
            String expected = expecteds == null ? null : Arrays.toString(expecteds.toArray());
            String actual = actuals == null ? null : Arrays.toString(actuals.toArray());
            throw new ComparisonFailure(message, expected, actual);
        }
        if (actuals.size() != expecteds.size()) {
            throw new ComparisonFailure(message, "size=" + expecteds.size(), "size=" + actuals.size());
        }
        if (actuals.size() == 0) {
            return;
        }
        List<String> expecteds2 = expecteds.stream().sorted().collect(Collectors.toList());
        List<String> actuals2 = actuals.stream().sorted().collect(Collectors.toList());
        int i = 0;
        while (i < expecteds2.size()) {
            if (expecteds2.get(i).isEmpty() && actuals2.get(i).isEmpty()) {
                expecteds2.remove(i);
                actuals2.remove(i);
            } else if ((!expecteds2.get(i).isEmpty() && actuals2.get(i).isEmpty())
                    || (expecteds2.get(i).isEmpty() && !actuals2.get(i).isEmpty()))
            {
                throw new ComparisonFailure(message, "element " + i + "=" + expecteds2.get(i), actuals2.get(i));
            } else {
                i++;
            }
        }
        if (actuals2.size() == 0) {
            return;
        }
        ArrayList<JsonChecker> arrayExpecteds =
                expecteds2.stream().sorted().map(JsonChecker::new).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> arrayActuals = actuals2.stream().sorted().collect(Collectors.toCollection(ArrayList::new));
        for (i = 0; i < arrayExpecteds.size(); i++) {
            String compareResult = arrayExpecteds.get(i).check(arrayActuals.get(i));
            if (compareResult != null) {
                //throw new ComparisonFailure(message, "item=" + item, "<absent>");
                Assert.fail(compareResult);
            }
        }
    }

    public static void assertMapsEquals(String message, Map<String, ?> expecteds, Map<String, ?> actuals) {
        if (actuals == null && expecteds == null) {
            return;
        }
        if (actuals == null || expecteds == null) {
            String expected = expecteds == null ? null : Arrays.toString(expecteds.entrySet().toArray());
            String actual = actuals == null ? null : Arrays.toString(actuals.entrySet().toArray());
            throw new ComparisonFailure(message, expected, actual);
        }
        if (actuals.size() != expecteds.size()) {
            throw new ComparisonFailure(message, "size=" + expecteds.size(), "size=" + actuals.size());
        }
        if (actuals.size() == 0) {
            return;
        }
        List<String> expecteds2 =
                expecteds.entrySet().stream().map(x -> "{\"" + x.getKey() + "\":\"" + x.getValue() + "\"}").sorted()
                        .collect(Collectors.toList());
        List<String> actuals2 =
                actuals.entrySet().stream().map(x -> "{\"" + x.getKey() + "\":\"" + x.getValue() + "\"}").sorted()
                        .collect(Collectors.toList());
        int i = 0;
        while (i < expecteds2.size()) {
            if (expecteds2.get(i).isEmpty() && actuals2.get(i).isEmpty()) {
                expecteds2.remove(i);
                actuals2.remove(i);
            } else if ((!expecteds2.get(i).isEmpty() && actuals2.get(i).isEmpty())
                    || (expecteds2.get(i).isEmpty() && !actuals2.get(i).isEmpty()))
            {
                throw new ComparisonFailure(message, "element " + i + "=" + expecteds2.get(i), actuals2.get(i));
            } else {
                i++;
            }
        }
        if (actuals2.size() == 0) {
            return;
        }
        ArrayList<JsonChecker> arrayExpecteds =
                expecteds2.stream().sorted().map(JsonChecker::new).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> arrayActuals = actuals2.stream().sorted().collect(Collectors.toCollection(ArrayList::new));
        for (i = 0; i < arrayExpecteds.size(); i++) {
            String compareResult = arrayExpecteds.get(i).check(arrayActuals.get(i));
            if (compareResult != null) {
                //throw new ComparisonFailure(message, "item=" + item, "<absent>");
                Assert.fail(compareResult);
            }
        }
    }

    protected void testSignal(final SpLoggerCluster cluster, final String signal, final int count) throws IOException {
        //String stats = HttpAssert.stats(cluster.spLogger().port());
        //HttpAssert.assertStat(signal, Integer.valueOf(count).toString(), stats);
    }
}
