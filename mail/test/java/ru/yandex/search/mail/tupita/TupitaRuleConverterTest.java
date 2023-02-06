package ru.yandex.search.mail.tupita;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.json.writer.JsonValue;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.json.writer.JsonWriterBase;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.YandexAssert;

public class TupitaRuleConverterTest extends TupitaTestBase {

    private String testOneCondition(final TupitaCluster cluster, final String condition) throws Exception {
        String request =
            "{\n" +
                "  \"conditions\": [\n" + condition + "]}";
                HttpPost post = new HttpPost(
            HTTP_LOCALHOST + cluster.tupita().port()
                +  "/api/mail/conditions/convert");
        post.setEntity(new StringEntity(request));
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post))
        {
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            return CharsetUtils.toString(response.getEntity());
        }
    }

    private String runOneCondition(final TupitaCluster cluster, final String condition) throws Exception {
        String request =
            "{\"conditions\": [" + condition + "]}";
        HttpPost post = new HttpPost(
            HTTP_LOCALHOST + cluster.tupita().port()
                +  "/api/mail/conditions/convert");
        post.setEntity(new StringEntity(request, StandardCharsets.UTF_8));
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post))
        {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

                final String result = CharsetUtils.toString(response.getEntity());
                return TypesafeValueContentHandler.parse(result).asMap().getList("conditions").get(0).asMap().getString("query");
            }

            throw new Exception(CharsetUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testCanonicRules() throws Exception {
        JsonList tests =
            TypesafeValueContentHandler.parse(loadResourceAsString("convert_canonic_results.json")).asList();

//        Field field = Paths.class.getDeclaredField("sourceRoot");
//        field.setAccessible(true);
//        field.set(null, "/home/vonidu/Projects/PS/repo/arc2/arcadia");

        StringBuilderWriter sbw = new StringBuilderWriter();
        JsonWriter writer = JsonType.HUMAN_READABLE.create(sbw);
        writer.startArray();
        boolean failed = false;
        List<String> diff = new ArrayList<>();
        try (TupitaCluster cluster = new TupitaCluster(this)) {
            for (JsonObject jo: tests) {
                JsonMap map = jo.asMap();
                String input = map.getString("input");
                String output = map.getString("output");
                boolean ok = map.getBoolean("ok");

                writer.startObject();
                writer.key("input");
                writer.value(input);
                writer.key("output");
                try {
                    String result = runOneCondition(cluster, input);
                    writer.value(result);
                    writer.key("ok");
                    writer.value(Boolean.TRUE);
                    if (!ok) {
                        failed = true;
                        String error = "Expected to be failed, but got ok " + input;
                        System.err.println(error);
                        diff.add(error);
                        writer.endObject();
                        continue;
                    }
                    if (!result.equals(output)) {
                        failed = true;
                        String error =
                            "Query " + input + " expected " + output
                                + " got " + result + " mismatch " + StringChecker.compare(output, result);
                        diff.add(error);
                        System.err.println(error);
                    }
                } catch (Exception e) {
                    writer.value("");
                    writer.key("ok");
                    writer.value(Boolean.FALSE);
                    if (ok) {
                        String error = "Expected to be ok " + input + " but got " + e.getMessage();
                        diff.add(error);
                        System.err.println(error);
                        failed = true;
                    }
                }
                writer.endObject();
            }
        }

        writer.endArray();
        writer.close();

        System.out.println("CannonicChanged " + sbw);

        Assert.assertFalse(diff.toString(), failed);
    }
    @Test
    public void test() throws Exception {
        Field field = Paths.class.getDeclaredField("sourceRoot");
        field.setAccessible(true);
        field.set(null, "/home/vonidu/Projects/PS/repo/arc2/arcadia");
        // PS-3098
        try (TupitaCluster cluster = new TupitaCluster(this)) {
            System.err.println(testOneCondition(
                cluster,
                "{ \"from\": \"hello@ya.ru\" }"));
        }
    }

    private class QueryWithResult implements JsonValue {
        private final String extQuery;
        private final String luceneQuery;
        private final boolean match;
        private final int id;

        public QueryWithResult(
            final TupitaCluster cluster,
            final String query,
            final boolean match,
            final int id)
            throws Exception
        {
            this.extQuery = query;
            this.luceneQuery = runOneCondition(cluster, query);
            this.match = match;
            this.id = id;
        }

        public String extQuery() {
            return extQuery;
        }

        public String query() {
            return luceneQuery;
        }

        public boolean match() {
            return match;
        }

        public int id() {
            return id;
        }

        @Override
        public void writeValue(final JsonWriterBase writer) throws IOException {
            writer.startObject();
            writer.key("id");
            writer.value(id);
            writer.key("query");
            writer.value(luceneQuery);
            writer.endObject();
        }
    }

    @Test
    public void testConditionsWithCheck() throws Exception {
        List<QueryWithResult> queries = new ArrayList<>();
        int id = 0;

        Field field = Paths.class.getDeclaredField("sourceRoot");
        field.setAccessible(true);
        field.set(null, "/home/vonidu/Projects/PS/repo/arc2/arcadia");

        try (TupitaCluster cluster = new TupitaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            queries.add(new QueryWithResult(cluster, "{ \"header:x-yandex-spam\": { \"$exists\": true } }", true, id++));
            queries.add(new QueryWithResult(cluster, "{ \"body\": \"амбруазович\" }", true, id++));
            queries.add(new QueryWithResult(cluster, "{\"attach:filename\": \"picture.png\"}", true, id++));
            queries.add(new QueryWithResult(cluster, "{\"subject\":{\"$eq\":{\"$any\":[\"hello\", \"dear\"]}}}", false, id++));
            queries.add(new QueryWithResult(cluster, "{\"attach:filename\": {\"$contains\": \"picture.png\"}}", true, id++));
            queries.add(new QueryWithResult(cluster, "{ \"subject\": { \"$contains\": \"test!@#%^&()+\" } }", true, id++));
            queries.add(new QueryWithResult(cluster, "{ \"subject\": { \"$contains\": \"test !@#%^&()+\" } }", false, id++));

            String stid = "320.mail:500.E600:234523463";

            String message = ",\"message\": " + loadResourceAsString("converter_tupita_message.json") + "}";
            String request =
                request(
                    message,
                    userQueries(
                        "227356512",
                        queries.stream().map(JsonType.NORMAL::toString)
                            .collect(Collectors.joining(","))));

            Map<String, Set<String>> expected1 = new LinkedHashMap<>();
            expected1.put(
                "227356512",
                queries.stream()
                    .filter(QueryWithResult::match)
                    .map((query) -> String.valueOf(query.id()))
                    .collect(Collectors.toSet()));

            cluster.tikaite().add(
                TIKAITE_BASE_URI + stid,
                tikaiteResp(stid, "many_parts.tikaite"));

            String uri = HTTP_LOCALHOST + cluster.tupita().port()
                             + CHECK + "227356512&debug=true&parse-error=skip";

            HttpPost post = new HttpPost(uri);
            post.setEntity(new StringEntity(request, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new TupitaTest.TupitaNewFormatChecker(expected1),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}
