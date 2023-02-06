package ru.yandex.market.stat.dicts.services;

import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;

import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.apache.http.Consts;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.FileCopyUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.stat.dicts.config.TestConstants.TEST_DICT_NAME;

public class ReactorArtifactsSenderTest {
    private static final String REACTOR_URL = "";
    private static final String FAKE_PROJECT_ID = "777";
    private static final String FAKE_PREFIX = "/market/mstat";
    private static final String STAND = "testing";
    private static final String TOKEN = "";
    private static final String CLUSTER = "hahn";
    private static final String METADATA_TYPE = "/yandex.reactor.artifact.YtPathArtifactValueProto";

    private RetryTemplate retryTemplate;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private CloseableHttpResponse httpResponse;

    @Before
    public void setup() {
        retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1,
                ImmutableMap.of(IOException.class, true)));
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetNamespacePath() {
        String fullPath1 = "//home/market/testing/mstat/dictionaries/" + TEST_DICT_NAME + "/2021-01-01";
        String fullPath2 = "//home/market/testing/mstat/dictionaries/" + TEST_DICT_NAME + "/2021-01-01T01:00:00";
        String namespacePath = FAKE_PREFIX + "/" + CLUSTER
                + "/home/market/testing/mstat/dictionaries/" + TEST_DICT_NAME;

        ReactorArtifactsSender sender = new ReactorArtifactsSender(null, null,
                REACTOR_URL, FAKE_PROJECT_ID, FAKE_PREFIX, STAND, TOKEN);

        assertThat(sender.getNamespacePath(CLUSTER, fullPath1), is(namespacePath));
        assertThat(sender.getNamespacePath(CLUSTER, fullPath2), is(namespacePath));
    }

    @Test
    @SneakyThrows
    public void testCreateArtifact() {
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);

        String fullPath = "//home/market/testing/mstat/dictionaries/" + TEST_DICT_NAME + "/2021-01-01";

        ReactorArtifactsSender sender = new ReactorArtifactsSender(httpClient, retryTemplate,
                REACTOR_URL, FAKE_PROJECT_ID, FAKE_PREFIX, STAND, TOKEN);

        String namespacePath = sender.getNamespacePath(CLUSTER, fullPath);
        sender.createArtifact(TEST_DICT_NAME, namespacePath);

        verify(httpClient, times(1)).execute(captor.capture());

        HttpPost actualPost = captor.getValue();
        assertEquals(actualPost.toString(), "POST " + REACTOR_URL + "/api/v1/a/create HTTP/1.1");

        String expectedBody = "{\"artifactTypeIdentifier\":{\"artifactTypeKey\":\"YT_PATH\"}," +
                "\"artifactIdentifier\":{\"namespaceIdentifier\":{\"namespacePath\":\"" + namespacePath + "\"}}," +
                "\"projectIdentifier\":{\"projectId\":\"" + FAKE_PROJECT_ID +"\"}," +
                "\"permissions\":{\"roles\":{\"*\":\"READER\"}}," +
                "\"description\":\"Artifact for dictionary " + TEST_DICT_NAME +"\"," +
                "\"createParentNamespaces\":true,\"createIfNotExist\":true}";

        HttpEntity actualEntity = actualPost.getEntity();
        Reader reader = new InputStreamReader(actualEntity.getContent(), UTF_8);
        String body = FileCopyUtils.copyToString(reader);
        assertEquals(body, expectedBody);
    }

    @Test
    @SneakyThrows
    public void testCreateInstance() {
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);

        String fullPath = "//home/market/testing/mstat/dictionaries/" + TEST_DICT_NAME + "/2021-01-01";
        String timestamp = "2021-01-01T00:00:00";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("@type", METADATA_TYPE);
        metadata.put("cluster", CLUSTER);
        metadata.put("path", fullPath);
        metadata.put("scale", "default");
        metadata.put("stand", "prestable");
        metadata.put("event", "publish");

        ReactorArtifactsSender sender = new ReactorArtifactsSender(httpClient, retryTemplate,
                REACTOR_URL, FAKE_PROJECT_ID, FAKE_PREFIX, STAND, TOKEN);

        String namespacePath = sender.getNamespacePath(CLUSTER, fullPath);
        sender.createInstance(TEST_DICT_NAME, namespacePath, timestamp, metadata);

        verify(httpClient, times(1)).execute(captor.capture());

        HttpPost actualPost = captor.getValue();
        assertEquals(actualPost.toString(), "POST " + REACTOR_URL + "/api/v1/a/i/create HTTP/1.1");

        String expectedBody = "{\"artifactIdentifier\":{\"namespaceIdentifier\":{\"namespacePath\":\"" + namespacePath +
                "\"}},\"metadata\":{\"cluster\":\"" + CLUSTER + "\",\"path\":\"" + fullPath +
                "\",\"@type\":\"/yandex.reactor.artifact.YtPathArtifactValueProto\",\"scale\":\"default\"," +
                "\"stand\":\"prestable\",\"event\":\"publish\"},\"userTimestamp\":\"" + timestamp +
                "\",\"artifactCreateRequest\":{\"artifactTypeIdentifier\":{\"artifactTypeKey\":\"YT_PATH\"}," +
                "\"artifactIdentifier\":{\"namespaceIdentifier\":{\"namespacePath\":\"" + namespacePath +
                "\"}},\"description\":\"Artifact for dictionary " + TEST_DICT_NAME + "\"," +
                "\"permissions\":{\"roles\":{\"*\":\"READER\"}},\"createParentNamespaces\":true,\"createIfNotExist\":true}}";

        HttpEntity actualEntity = actualPost.getEntity();
        Reader reader = new InputStreamReader(actualEntity.getContent(), UTF_8);
        String body = FileCopyUtils.copyToString(reader);
        assertEquals(body, expectedBody);
    }

    @Test
    @SneakyThrows
    public void testDeprecateInstance() {
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);

        String fullPath = "//home/market/testing/mstat/dictionaries/" + TEST_DICT_NAME + "/2021-01-01";
        String timestamp = "2021-01-01T00:00:00";
        String timestampTo = "2021-01-01T00:00:01";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("@type", METADATA_TYPE);
        metadata.put("cluster", CLUSTER);
        metadata.put("path", fullPath);
        metadata.put("scale", "default");
        metadata.put("stand", "prestable");
        metadata.put("event", "publish");

        ReactorArtifactsSender sender = new ReactorArtifactsSender(httpClient, retryTemplate,
                REACTOR_URL, FAKE_PROJECT_ID, FAKE_PREFIX, STAND, TOKEN);

        String namespacePath = sender.getNamespacePath(CLUSTER, fullPath);
        sender.deprecateAndCreateInstance(TEST_DICT_NAME, namespacePath, timestamp, timestampTo, metadata);

        verify(httpClient, times(1)).execute(captor.capture());

        HttpPost actualPost = captor.getValue();
        assertEquals(actualPost.toString(), "POST " + REACTOR_URL + "/api/v1/a/i/deprecate HTTP/1.1");

        String expectedBody = "{\"artifactInstanceIdentifier\":{\"rangeSelector\":{\"artifactIdentifier\":" +
                "{\"namespaceIdentifier\":{\"namespacePath\":\"" + namespacePath +
                "\"}},\"timestampSelector\":{\"userTimestampRange\":{\"from\":\"" + timestamp +
                "\",\"to\":\"" + timestampTo + "\"}}," +
                "\"statuses\":[\"CREATED\",\"DEPRECATED\",\"DELETED\",\"ACTIVE\",\"REPLACING\"]}}," +
                "\"description\":\"Dictionary " + TEST_DICT_NAME + " rebuild\",\"createRequest\":" +
                "{\"artifactIdentifier\":{\"namespaceIdentifier\":{\"namespacePath\":\"" + namespacePath +
                "\"}},\"metadata\":{\"cluster\":\"" + CLUSTER + "\",\"path\":\"" + fullPath + "\"," +
                "\"@type\":\"/yandex.reactor.artifact.YtPathArtifactValueProto\",\"scale\":\"default\"," +
                "\"stand\":\"prestable\",\"event\":\"publish\"},\"userTimestamp\":\"" + timestamp + "\"," +
                "\"artifactCreateRequest\":{\"artifactTypeIdentifier\":{\"artifactTypeKey\":\"YT_PATH\"}," +
                "\"artifactIdentifier\":{\"namespaceIdentifier\":{\"namespacePath\":\"" + namespacePath + "\"}}," +
                "\"description\":\"Artifact for dictionary " + TEST_DICT_NAME + "\"," +
                "\"permissions\":{\"roles\":{\"*\":\"READER\"}},\"createParentNamespaces\":true,\"createIfNotExist\":true}}}";

        HttpEntity actualEntity = actualPost.getEntity();
        Reader reader = new InputStreamReader(actualEntity.getContent(), UTF_8);
        String body = FileCopyUtils.copyToString(reader);
        assertEquals(body, expectedBody);
    }
}
