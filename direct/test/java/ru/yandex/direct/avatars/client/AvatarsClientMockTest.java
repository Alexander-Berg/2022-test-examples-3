package ru.yandex.direct.avatars.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;

import ru.yandex.direct.asynchttp.ParallelFetcher;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.asynchttp.ParsableRequest;
import ru.yandex.direct.asynchttp.Result;
import ru.yandex.direct.avatars.client.exception.AvatarsClientCommonException;
import ru.yandex.direct.avatars.client.model.AvatarId;
import ru.yandex.direct.avatars.client.model.AvatarInfo;
import ru.yandex.direct.avatars.client.model.answer.StatusCode;
import ru.yandex.direct.avatars.client.model.answer.UploadImageResponse;
import ru.yandex.direct.avatars.config.AvatarsConfig;
import ru.yandex.direct.avatars.config.ServerConfig;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.monlib.metrics.registry.MetricRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.HashingUtils.getMd5HashAsBase64YaStringWithoutPadding;

@SuppressWarnings("unchecked")
public class AvatarsClientMockTest {
    private static final int DELETE_IMAGE_GROUP_ID = 1111;
    private static final String DELETE_IMAGE_KEY = "delete_image_name";
    private static final int WRITE_SERVER_PORT = 13000;
    private static final String WRITE_AVATARS_TESTING_HOST = "avatars-int.mdst.yandex.net";
    private static final String READ_AVATARS_TESTING_HOST = "avatars.mdst.yandex.net";
    private static final String SERVER_SCHEMA = "http";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final String NAMESPACE = "direct-avatars";
    private static final String OPERATION_SEPARATOR = "-";
    private static final String URI_PATH_SEPARATOR = "/";
    private static final String UPLOAD_URL =
            SERVER_SCHEMA + "://" + WRITE_AVATARS_TESTING_HOST + ":" + Integer.toString(WRITE_SERVER_PORT)
                    + URI_PATH_SEPARATOR
                    + "put" + OPERATION_SEPARATOR + NAMESPACE;
    private static final AvatarId deleteAvatarid = new AvatarId(NAMESPACE, DELETE_IMAGE_GROUP_ID, DELETE_IMAGE_KEY);
    private static final String DELETE_URL =
            SERVER_SCHEMA + "://" + WRITE_AVATARS_TESTING_HOST + ":" + Integer.toString(WRITE_SERVER_PORT)
                    + URI_PATH_SEPARATOR
                    + "delete" + OPERATION_SEPARATOR + NAMESPACE + URI_PATH_SEPARATOR
                    + Integer
                    .toString(DELETE_IMAGE_GROUP_ID) + URI_PATH_SEPARATOR + DELETE_IMAGE_KEY;
    private static final String FILTERED_ANSWER_EXAMPLE_PATH = "avatars/server_answer_example_filtered.json";
    private static final String ANSWER_EXAMPLE_PATH = "avatars/server_answer_example.json";
    private final byte[] imageBody = new byte[1];
    private final AvatarsConfig conf = new AvatarsConfig("test config",
            new ServerConfig(READ_AVATARS_TESTING_HOST, 443, "https"),
            new ServerConfig(WRITE_AVATARS_TESTING_HOST, WRITE_SERVER_PORT, "http"),
            TIMEOUT,
            NAMESPACE,
            false);

    @Test
    public void upload_CheckServerRequest() throws IOException, InterruptedException {
        String answerExample = getFilteredAnswerExample();
        UploadImageResponse uploadImageResponse = JsonUtils.fromJson(answerExample, UploadImageResponse.class);
        Response response = mock(Response.class);
        when(response.getResponseBody()).thenReturn(answerExample);
        when(response.getResponseBody(any(Charset.class))).thenReturn(answerExample);
        Result<UploadImageResponse> result = mock(Result.class);
        when(result.getErrors()).thenReturn(null);
        when(result.getSuccess()).thenReturn(uploadImageResponse);
        ParallelFetcher<UploadImageResponse> parallelFetcher = mock(ParallelFetcher.class);
        when(parallelFetcher.execute(any(ParsableRequest.class))).thenReturn(result);
        ParallelFetcherFactory parallelFetcherFactory = mock(ParallelFetcherFactory.class);
        when(parallelFetcherFactory.getParallelFetcherWithMetricRegistry(any(MetricRegistry.class))).thenReturn((ParallelFetcher) parallelFetcher);
        AvatarsClient avatarsClient = new AvatarsClient(conf, parallelFetcherFactory, null, null);
        avatarsClient.upload(imageBody);
        ArgumentCaptor<ParsableRequest> captor = ArgumentCaptor.forClass(ParsableRequest.class);
        verify(parallelFetcher, times(1)).execute(captor.capture());
        ParsableRequest<UploadImageResponse> parsableRequest = captor.getValue();
        Request request = parsableRequest.getAHCRequest();
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(request.getMethod()).isEqualTo("POST");
        sa.assertThat(request.getUrl()).startsWith(UPLOAD_URL);
        sa.assertAll();
    }

    @Test
    public void upload_CheckServerResponseParsing() throws IOException, InterruptedException {
        String answerExample = getFilteredAnswerExample();
        ParallelFetcher<UploadImageResponse> parallelFetcher = getParallelFetcher(answerExample);
        ParallelFetcherFactory parallelFetcherFactory = mock(ParallelFetcherFactory.class);
        when(parallelFetcherFactory.getParallelFetcherWithMetricRegistry(any(MetricRegistry.class))).thenReturn((ParallelFetcher) parallelFetcher);
        AvatarsClient avatarsClient = new AvatarsClient(conf, parallelFetcherFactory, null, null);
        AvatarId avatarId = avatarsClient.upload(imageBody);
        UploadImageResponse uploadImageResponse = JsonUtils.fromJson(answerExample, UploadImageResponse.class);
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(avatarId.getNamespace()).isEqualTo(NAMESPACE);
        sa.assertThat(avatarId.getGroupId()).isEqualTo(uploadImageResponse.getGroupId());
        sa.assertThat(avatarId.getKey()).isEqualTo(uploadImageResponse.getImageName());
        sa.assertAll();
    }

    @Test
    public void uploadWithKey_CheckServerResponseParsing() throws IOException, InterruptedException {
        String filteredAnswerExample = getFilteredAnswerExample();
        String answerExample = getAnswerExample();
        ParallelFetcher<UploadImageResponse>
                parallelFetcher = getParallelFetcher(answerExample);
        ParallelFetcherFactory parallelFetcherFactory = mock(ParallelFetcherFactory.class);
        when(parallelFetcherFactory.getParallelFetcherWithMetricRegistry(any(MetricRegistry.class))).thenReturn((ParallelFetcher) parallelFetcher);
        String md5HashAsBase64YaStringWithoutPadding = getMd5HashAsBase64YaStringWithoutPadding(imageBody);
        AvatarsClient avatarsClient = new AvatarsClient(conf, parallelFetcherFactory, null, null);
        AvatarInfo avatarInfo = avatarsClient.upload(md5HashAsBase64YaStringWithoutPadding, imageBody);
        UploadImageResponse uploadImageResponse = JsonUtils.fromJson(filteredAnswerExample, UploadImageResponse.class);
        uploadImageResponse.setMeta(filteredAnswerExample);
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(avatarInfo.getNamespace()).isEqualTo(NAMESPACE);
        sa.assertThat(avatarInfo.getGroupId()).isEqualTo(uploadImageResponse.getGroupId());
        sa.assertThat(avatarInfo.getKey()).isEqualTo(uploadImageResponse.getImageName());
        sa.assertThat(JsonUtils.fromJson(avatarInfo.getMeta())).isEqualTo(JsonUtils.fromJson(uploadImageResponse.getMeta()));
        sa.assertThat(avatarInfo.getSizes()).is(matchedBy(beanDiffer(uploadImageResponse.getSizes())));

        sa.assertAll();
    }

    private ParallelFetcher<UploadImageResponse> getParallelFetcher(String answerExample) throws InterruptedException {
        ParallelFetcher<UploadImageResponse> parallelFetcher = mock(ParallelFetcher.class);
        when(parallelFetcher.execute(any(ParsableRequest.class)))
                .thenAnswer((InvocationOnMock invocation) -> {
                    ParsableRequest<UploadImageResponse> parsableRequest = invocation.getArgument(0);
                    Request request = parsableRequest.getAHCRequest();
                    Function<Response, UploadImageResponse> parseFunction = parsableRequest.getParseFunction();
                    Response response = mock(Response.class);
                    when(response.getResponseBody()).thenReturn(answerExample);
                    when(response.getResponseBody(any(Charset.class))).thenReturn(answerExample);
                    Result<UploadImageResponse> result = mock(Result.class);
                    when(result.getErrors()).thenReturn(null);
                    UploadImageResponse groupidImagename = parseFunction.apply(response);
                    when(result.getSuccess()).thenReturn(groupidImagename);
                    return result;
                });
        return parallelFetcher;
    }


    private String getFilteredAnswerExample() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = classloader.getResourceAsStream(FILTERED_ANSWER_EXAMPLE_PATH)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    private String getAnswerExample() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = classloader.getResourceAsStream(ANSWER_EXAMPLE_PATH)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    @Test
    public void delete_CheckServerRequest() throws InterruptedException, NoSuchMethodException {
        Result<StatusCode> result = mock(Result.class);
        when(result.getErrors()).thenReturn(null);
        when(result.getSuccess()).thenReturn(new StatusCode(200));
        ParallelFetcher<StatusCode> parallelFetcher = mock(ParallelFetcher.class);
        when(parallelFetcher.execute(any(ParsableRequest.class))).thenReturn(result);
        ParallelFetcherFactory parallelFetcherFactory = mock(ParallelFetcherFactory.class);
        when(parallelFetcherFactory.getParallelFetcherWithMetricRegistry(any(MetricRegistry.class))).thenReturn((ParallelFetcher) parallelFetcher);
        AvatarsClient avatarsClient = new AvatarsClient(conf, parallelFetcherFactory, null, null);
        avatarsClient.delete(deleteAvatarid);
        ArgumentCaptor<ParsableRequest> captor = ArgumentCaptor.forClass(ParsableRequest.class);
        verify(parallelFetcher, times(1)).execute(captor.capture());
        ParsableRequest<StatusCode> parsableRequest = captor.getValue();
        Request request = parsableRequest.getAHCRequest();
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(request.getMethod()).isEqualTo("DELETE");
        sa.assertThat(request.getUrl()).startsWith(DELETE_URL);
        sa.assertAll();
    }

    @Test
    public void delete_CheckServerResponse() throws InterruptedException {
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(prepareAvatarsClient(202).delete(deleteAvatarid)).isFalse();
        sa.assertThat(prepareAvatarsClient(200).delete(deleteAvatarid)).isTrue();
        sa.assertThat(prepareAvatarsClient(404).delete(deleteAvatarid)).isTrue();
        sa.assertThatThrownBy(() -> prepareAvatarsClient(500).delete(deleteAvatarid))
                .isInstanceOf(AvatarsClientCommonException.class);
        sa.assertAll();
    }

    private AvatarsClient prepareAvatarsClient(int serverAnswerCode) throws InterruptedException {
        Result<StatusCode> result = mock(Result.class);
        when(result.getErrors()).thenReturn(null);
        when(result.getSuccess()).thenReturn(new StatusCode(serverAnswerCode));
        ParallelFetcher<StatusCode> parallelFetcher = mock(ParallelFetcher.class);
        when(parallelFetcher.execute(any(ParsableRequest.class))).thenReturn(result);
        ParallelFetcherFactory parallelFetcherFactory = mock(ParallelFetcherFactory.class);
        when(parallelFetcherFactory.getParallelFetcherWithMetricRegistry(any(MetricRegistry.class))).thenReturn((ParallelFetcher) parallelFetcher);
        return new AvatarsClient(conf, parallelFetcherFactory, null, null);
    }
}
