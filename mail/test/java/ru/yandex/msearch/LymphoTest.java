package ru.yandex.msearch;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.test.ChainedHttpResource;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.io.StringBuilderWriter;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.lympho.AliveStatusProvier;
import ru.yandex.lympho.BasicLymphoContext;
import ru.yandex.lympho.LymphoKeysIterator;
import ru.yandex.lympho.LymphoNodeWorker;
import ru.yandex.lympho.LymphoWorkerTask;
import ru.yandex.lympho.TaskConfig;
import ru.yandex.lympho.TaskConfigBuilder;
import ru.yandex.lympho.TaskFields;
import ru.yandex.lympho.WorkerTaskConfig;
import ru.yandex.msearch.config.DatabaseConfig;
import ru.yandex.msearch.printkeys.PrintKeysParams;
import ru.yandex.parser.uri.CgiParams;
import ru.yandex.queryParser.QueryParserFactory;
import ru.yandex.queryParser.YandexQueryParserFactory;
import ru.yandex.search.prefix.StringPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class LymphoTest extends TestBase {
    @Test
    public void testExecuteRequest() throws Exception {
        File root = Files.createTempDirectory("testLymphoHttp").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
             CloseableHttpClient client = HttpClients.createDefault())
        {

            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":["
                + "{\"keyword\":\"lookup\",\"text\":\"text\",\"boolean\":true},"
                + "{\"keyword\":\"second\",\"text\":\"lookup\"},"
                + "{\"keyword\":\"shird\",\"text\":\"lookup\"}"
                + "]}",
                StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            String script = IOStreamUtils.consume(
                new InputStreamReader(
                    this.getClass().getResourceAsStream("execute_search_script.js"),
                    StandardCharsets.UTF_8))
                .toString();

            post = new HttpPost("http://localhost:"
                + daemon.httpSearchServer().port() + "/execute");
            post.setEntity(
                new StringEntity(script,
                StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(post)) {
                Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                String respText = CharsetUtils.toString(response.getEntity());
                System.out.println("Lympho " + respText);
            }

            Thread.sleep(1000);
        }
    }

    @Test
    public void testExecuteRequestHttpOut() throws Exception {
        File root = Files.createTempDirectory("testExecuteRequestHttpOut").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
             CloseableHttpClient client = HttpClients.createDefault();
             StaticServer server = new StaticServer(Configs.baseConfig()))
        {
            server.start();
            HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":["
                    + "{\"keyword\":\"lookup\",\"text\":\"text\",\"boolean\":true},"
                    + "{\"keyword\":\"second\",\"text\":\"lookup\",\"boolean\":true},"
                    + "{\"keyword\":\"third\",\"text\":\"lookup\"},"
                    + "{\"keyword\":\"forth\",\"text\":\"lookup\",\"boolean\":true},"
                    + "{\"keyword\":\"fifth\",\"text\":\"lookup\"},"
                    + "{\"keyword\":\"sixth\",\"text\":\"lookup\"}"
                    + "]}",
                    StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            String script = IOStreamUtils.consume(
                    new InputStreamReader(
                            this.getClass().getResourceAsStream("execute_script_httpout.js"),
                            StandardCharsets.UTF_8))
                    .toString();
            script = script.replaceAll("RCV_PORT", String.valueOf(server.port()));

            server.add("/sendBatch*", new ChainedHttpResource(
                    new ExpectingHttpItem(new JsonChecker("{" +
                            "    \"records\": [" +
                            "        {\"keyword\": \"lookup\"}," +
                            "        {\"keyword\": \"second\"}" +
                            "    ],\n" +
                            "    \"meta\": {\n" +
                            "        \"records_processed\": 2,\n" +
                            "        \"batch_num\": 0\n" +
                            "    }\n" +
                            "}")),
                    new ExpectingHttpItem(new JsonChecker("{\n" +
                            "    \"records\": [\n" +
                            "        {\n" +
                            "            \"keyword\": \"forth\"\n" +
                            "        }\n" +
                            "    ],\n" +
                            "    \"meta\": {\n" +
                            "        \"records_processed\": 3,\n" +
                            "        \"batch_num\": 1\n" +
                            "    }\n" +
                            "}"))));

            post = new HttpPost("http://localhost:"
                    + daemon.httpSearchServer().port() + "/execute");
            post.setEntity(
                    new StringEntity(script,
                            StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(post)) {
                Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    public void testHttpOutput() throws Exception {
        File root = Files.createTempDirectory("testLymphoWorker").toFile();
        try (Daemon daemon = new Daemon(SearchBackendTestBase.config(root));
             CloseableHttpClient client = HttpClients.createDefault();
             LymphoNodeWorker mrWorker = new LymphoNodeWorker(daemon);
             StaticServer server = new StaticServer(Configs.baseConfig()))
        {

            server.start();
            mrWorker.start();
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":["
                + "{\"keyword\":\"lookup\",\"text\":\"text\",\"boolean\":true},"
                + "{\"keyword\":\"second\",\"text\":\"lookup\"},"
                + "{\"keyword\":\"shird\",\"text\":\"lookup\"}"
                + "]}",
                StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            String script =
                Files.readString(Path.of(this.getClass().getResource("lympho_example_script.js").toURI()));

            script = script.replaceAll("RCV_PORT", String.valueOf(server.port()));

            StringBuilderWriter sbw = new StringBuilderWriter();
            try (JsonWriter writer = JsonType.NORMAL.create(sbw)) {
                writer.startObject();
                writer.key(TaskFields.TASK_ID.fieldName());
                writer.value("taskId1");
                writer.key(TaskFields.JOB_ID.fieldName());
                writer.value("jobId1");
                writer.key(TaskFields.MAX_RETRIES.fieldName());
                writer.value(1);
                writer.key(TaskFields.SCHEDULER_URI.fieldName());
                writer.value("http://localhost:10500/scheduler");
                writer.key(TaskFields.HEARTBEAT_TIMEOUT.fieldName());
                writer.value(100);
                writer.key(TaskFields.BACKEND_SHARDS.fieldName());
                writer.value(1);
                writer.key(TaskFields.CREATED_TIME.fieldName());
                writer.value(System.currentTimeMillis());
                writer.key("lympho_worker_add_ts");
                writer.value(System.currentTimeMillis());
                writer.key(TaskFields.DEADLINE.fieldName());
                writer.value(System.currentTimeMillis() + 60000);
                writer.key(TaskFields.SCRIPT_BODY.fieldName());
                writer.value(script);
                writer.endObject();
            }

            server.add("/sendBatch*",  new StaticHttpResource(new HttpRequestHandler() {
                @Override
                public void handle(
                    final HttpRequest httpRequest,
                    final HttpResponse httpResponse,
                    final HttpContext httpContext) throws HttpException, IOException
                {
                    System.out.println(CharsetUtils.toString(((HttpEntityEnclosingRequest) httpRequest).getEntity()));
                    httpResponse.setStatusCode(HttpStatus.SC_OK);
                }
            }));

            WorkerTaskConfig config =
                new WorkerTaskConfig(
                    TypesafeValueContentHandler.parse(sbw.toString()).asMap(),
                    daemon.index().config());
            LymphoWorkerTask task = new LymphoWorkerTask(config, daemon.httpSearchServer().logger(), 1);
            mrWorker.schedule(task);

            Thread.sleep(10000);
        }
    }

    @Test
    public void testLymphoKeys() throws Exception {
        File root = Files.createTempDirectory("testLymphoWorker").toFile();
        Config config = SearchBackendTestBase.config(root);
        try (Daemon daemon = new Daemon(config);
             CloseableHttpClient client = HttpClients.createDefault();
             StaticServer server = new StaticServer(Configs.baseConfig()))
        {

            server.start();
            HttpPost post = new HttpPost(
                "http://localhost:"
                    + daemon.jsonServerPort() + "/add");
            post.setEntity(
                new StringEntity(
                    "{\"prefix\":0,\"docs\":["
                        + "{\"keyword\":\"lookup\",\"text\":\"text\",\"boolean\":true},"
                        + "{\"keyword\":\"second\",\"text\":\"lookup\"},"
                        + "{\"keyword\":\"shird\",\"text\":\"lookup\"}"
                        + "]}",
                    StandardCharsets.UTF_8));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);
            BasicLymphoContext context =
                new BasicLymphoContext(daemon.index(), 0, daemon.httpSearchServer().logger(), new AliveStatusProvier() {
                    @Override
                    public boolean isAlive() {
                        return true;
                    }
                });

            Map<String, QueryParserFactory> queryParserFactory = new LinkedHashMap<>();
            for (Map.Entry<String, DatabaseConfig> entry: config.databasesConfigs().entrySet()) {
                queryParserFactory.put(entry.getKey(), new YandexQueryParserFactory(entry.getValue()));
            }
            //this.queryParserFactory = Collections.unmodifiableMap(queryParserFactory);

            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("nashorn");
            SimpleScriptContext scriptContext = new SimpleScriptContext();
            scriptContext.setAttribute(
                "context",
                context,
                ScriptContext.ENGINE_SCOPE);

            engine.eval(loadResourceAsString("test_printkeys.js"), scriptContext);
            //HttpAssert.assertStatusCode(HttpStatus.SC_OK, );
//            LymphoKeysIterator iterator = new LymphoKeysIterator(
//                context,
//                new PrintKeysParams(
//                    queryParserFactory,
//                    daemon.index(),
//                    new CgiParams(Collections.singletonMap("field", Collections.singletonList("keyword"))), null));
//            while (iterator.hasNext()) {
//                System.out.println(iterator.next());
//            }
        }
    }
}
