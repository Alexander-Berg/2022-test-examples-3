package ru.yandex.http.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.parser.config.ConfigException;

public class ExpectingServerTest {
    private static final int BUFFER_SIZE = 65536;
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String URI = "/";
    private static final String RFC = "rfc3261.txt";
    private static final Set<String> OPTIONS_POST =
        new HashSet<>(Arrays.asList("OPTIONS", "GET", "HEAD", "POST", "PUT"));

    public static String streamToString(final InputStream is)
        throws IOException
    {
        StringBuilder sb = new StringBuilder();
        char[] cbuf = new char[BUFFER_SIZE];
        try (InputStreamReader reader =
                new InputStreamReader(is, StandardCharsets.UTF_8))
        {
            int read;
            while ((read = reader.read(cbuf)) != -1) {
                sb.append(cbuf, 0, read);
            }
        }
        return sb.toString();
    }

    @Test
    public void test()
        throws ConfigException, IOException, InterruptedException
    {
        String body = streamToString(getClass().getResourceAsStream(RFC));
        InputStreamEntity entity =
            new InputStreamEntity(getClass().getResourceAsStream(RFC), -1);
        entity.setChunked(true);
        try (CloseableHttpClient client = HttpClients.createDefault();
            ExpectingServer server = new ExpectingServer(Configs.baseConfig()))
        {
            server.uri(URI).add(body);
            server.uri(URI).add(body);
            server.start();
            HttpPost post = new HttpPost(HTTP_LOCALHOST + server.port() + URI);
            post.setEntity(entity);
            HttpResponse response = client.execute(post);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            EntityUtils.consume(response.getEntity());
            post.setEntity(new StringEntity("Hello, world!"));
            response = client.execute(post);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_NOT_IMPLEMENTED,
                response);
            Assert.assertEquals(
                "For '/' expected entity is '[" + body
                + "]', but got '[Hello, world!]'",
                EntityUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testMismatch()
        throws ConfigException, IOException, InterruptedException
    {
        try (CloseableHttpClient client = HttpClients.createDefault();
            ExpectingServer server = new ExpectingServer(Configs.baseConfig()))
        {
            HttpPost post = new HttpPost(HTTP_LOCALHOST + server.port() + URI);
            post.setEntity(new StringEntity("Hi, world"));
            server.uri(URI).add("Hello world");
            server.uri(URI).add("Привет мир");
            server.uri(URI).add("перезвон");
            server.start();
            HttpResponse response = client.execute(post);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_NOT_IMPLEMENTED,
                response);
            Assert.assertEquals(
                "For '/' expected entity is 'H[ello] world'"
                + ", but got 'H[i,] world'",
                EntityUtils.toString(response.getEntity()));
            post.setEntity(
                new StringEntity("Привет мир!", StandardCharsets.UTF_8));
            response = client.execute(post);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_NOT_IMPLEMENTED,
                response);
            Assert.assertEquals(
                "For '/' expected entity is "
                + "'Привет мир[]', but got 'Привет мир[!]'",
                EntityUtils.toString(response.getEntity()));
            post.setEntity(
                new StringEntity(
                    "звон",
                    ContentType.create("text/plain", StandardCharsets.UTF_8)));
            response = client.execute(post);
            Assert.assertEquals(
                HttpStatus.SC_NOT_IMPLEMENTED,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(
                "For '/' expected entity is"
                + " '[пере]звон', but got '[]звон'",
                EntityUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testDelete()
        throws ConfigException, IOException, InterruptedException
    {
        try (CloseableHttpClient client = HttpClients.createDefault();
            ExpectingServer server = new ExpectingServer(Configs.baseConfig()))
        {
            server.start();
            HttpResponse response = client.execute(
                new HttpDelete(HTTP_LOCALHOST + server.port() + URI));
            HttpAssert.assertStatusCode(
                HttpStatus.SC_METHOD_NOT_ALLOWED,
                response);
            Assert.assertEquals(
                OPTIONS_POST,
                new HashSet<>(
                    Arrays.asList(
                        response.getFirstHeader(HttpHeaders.ALLOW).getValue()
                            .split(", "))));
            EntityUtils.consume(response.getEntity());
        }
    }

    @Test
    public void testOptions()
        throws ConfigException, IOException, InterruptedException
    {
        try (CloseableHttpClient client = HttpClients.createDefault();
            ExpectingServer server = new ExpectingServer(Configs.baseConfig()))
        {
            server.start();
            HttpOptions options =
                new HttpOptions(HTTP_LOCALHOST + server.port() + URI);
            HttpResponse response = client.execute(options);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            Assert.assertEquals(
                OPTIONS_POST,
                options.getAllowedMethods(response));
            EntityUtils.consume(response.getEntity());
        }
    }
}

