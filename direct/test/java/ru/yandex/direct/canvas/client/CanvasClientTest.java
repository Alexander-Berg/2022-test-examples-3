package ru.yandex.direct.canvas.client;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.JUnitSoftAssertions;
import org.asynchttpclient.Response;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.asynchttp.ErrorResponseWrapperException;
import ru.yandex.direct.asynchttp.ParallelFetcher;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.asynchttp.ParsableStringRequest;
import ru.yandex.direct.asynchttp.Result;
import ru.yandex.direct.canvas.client.model.exception.CanvasClientException;
import ru.yandex.direct.canvas.client.model.video.Creative;
import ru.yandex.direct.canvas.client.model.video.CreativeResponse;
import ru.yandex.direct.canvas.client.model.video.ModerateInfo;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoAspect;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoHtml;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoSound;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoText;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoVideo;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class CanvasClientTest {
    private static final String TEST_VIDEO_URL = "http://canvas-creatives-devtest.qart.yandex.ru:8081/direct";
    private static final String TEST_CANVAS_BACKEND_URL = "http://canvas.devtest.direct.yandex.ru:84";
    private static final String TEST_CANVAS_URL = "http://canvas-creatives-devtest.qart.yandex.ru:8081/direct";

    private static final String TEST_GET_VIDEO_ADDITION_TOKEN = "s3cr3t";
    private static final Long TEST_CLIENT_ID = 29085967L;
    private static final String TEST_CREATIVE_ID = "1076628100";
    public static final String ENCODING = "UTF-8";
    public static final String GET_VIDEO_ADDITIONS_JSON_EXAMPLE_JSON_PATH = "get_video_additions_json_example.json";
    public static final int CHUNK_SIZE = 50;

    private CanvasClient canvasClient;
    private Result<String> fetcherResponse;

    @Mock
    private ParallelFetcher<String> fetcher;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();


    @Before
    public void before() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);

        ParallelFetcherFactory parallelFetcherFactory = mock(ParallelFetcherFactory.class);
        when(parallelFetcherFactory.getParallelFetcherWithMetricRegistry(any())).thenAnswer(fs -> fetcher);

        CanvasClientConfiguration configuration =
                new CanvasClientConfiguration(TEST_VIDEO_URL, TEST_CANVAS_BACKEND_URL,
                        TEST_CANVAS_URL, null,
                        TEST_GET_VIDEO_ADDITION_TOKEN, CHUNK_SIZE);
        canvasClient = new CanvasClient(configuration, parallelFetcherFactory);

        fetcherResponse = new Result<>(ParsableStringRequest.DEFAULT_REQUEST_ID);
    }

    @Test
    public void testGetAuthTokenFromFileWithNewline() throws Exception {
        String tokenFile = Paths.get(this.getClass().getResource("/token_file_with_new_lines").toURI()).toFile()
                .getAbsolutePath();
        CanvasClientConfiguration configuration = new CanvasClientConfiguration(TEST_VIDEO_URL,
                TEST_CANVAS_BACKEND_URL, TEST_CANVAS_URL, tokenFile, null, CHUNK_SIZE);
        assertEquals("testtoken", configuration.getAuthToken());
    }

    @Test
    public void testSuccessfulRequest() throws Exception {
        fetcherResponse.setSuccess(getVideoAdditionReponseExample());
        Long clientId = TEST_CLIENT_ID;
        List<Long> creativeIds = List.of(1076628100L);
        doReturn(fetcherResponse).when(fetcher).execute(isA(ParsableStringRequest.class));

        List<CreativeResponse> videoAdditions = canvasClient.getVideoAdditions(clientId, creativeIds);
        BeanDifferMatcher<CreativeResponse> creativeResponseBeanDifferMatcher =
                beanDiffer(getCreativeResponseExample()).useCompareStrategy(
                        DefaultCompareStrategies.onlyExpectedFields());

        softly.assertThat(videoAdditions).hasSize(2);
        softly.assertThat(videoAdditions.get(0)).is(matchedBy(creativeResponseBeanDifferMatcher));
    }

    @Test
    public void testCanvasError_returnsVideoMessagesFromResponseBody() throws Exception {
        Long clientId = TEST_CLIENT_ID;
        String creativeId = TEST_CREATIVE_ID;
        ErrorResponseWrapperException responseWrapperException = mock(ErrorResponseWrapperException.class);
        Response response = mock(Response.class);
        String responseBody = "{\"message\":\"Could not upload video\",\"id\":\"123\",\"properties\":{}}";

        doReturn(true).when(response).hasResponseBody();
        doReturn(responseBody).when(response).getResponseBody();
        doReturn(response).when(responseWrapperException).getResponse();

        Result<String> result = new Result(clientId);
        result.addError(responseWrapperException);

        doReturn(result).when(fetcher).execute(isA(ParsableStringRequest.class));

        CanvasClientException exception = assertThrows(CanvasClientException.class, () -> canvasClient.getVideoById(clientId, creativeId));

        assertEquals(Arrays.asList("Could not upload video"), exception.getValidationErrors());
    }

    @Test
    public void testCanvasError_returnsHtml5MessagesFromResponseBody() throws Exception {
        Long clientId = TEST_CLIENT_ID;
        String creativeId = TEST_CREATIVE_ID;
        String[] messages = new String[] {"HTML file does not have meta tag", "Redirect not found"};
        ErrorResponseWrapperException responseWrapperException = mock(ErrorResponseWrapperException.class);
        Response response = mock(Response.class);
        String responseBody = String.format("{\"messages\":[\"%s\",\"%s\"]}", messages[0], messages[1]);

        doReturn(true).when(response).hasResponseBody();
        doReturn(responseBody).when(response).getResponseBody();
        doReturn(response).when(responseWrapperException).getResponse();

        Result<String> result = new Result(clientId);
        result.addError(responseWrapperException);

        doReturn(result).when(fetcher).execute(isA(ParsableStringRequest.class));

        CanvasClientException exception = assertThrows(CanvasClientException.class, () -> canvasClient.getVideoById(clientId, creativeId));

        assertEquals(Arrays.asList(messages), exception.getValidationErrors());
    }

    private String getVideoAdditionReponseExample() throws Exception {
        return FileUtils.readFileToString(getResourceAsFile(GET_VIDEO_ADDITIONS_JSON_EXAMPLE_JSON_PATH), ENCODING);
    }

    private static File getResourceAsFile(String resourceName) throws Exception {
        return new File(ClassLoader.getSystemResource(resourceName).toURI());
    }

    private static CreativeResponse getCreativeResponseExample() {
        return new CreativeResponse()
                .withCreative(new Creative()
                        .withCreativeId(1076628100L)
                        .withCreativeName("Автоматическое видеодополнение")
                        .withCreativeType(Creative.CreativeType.VIDEO_ADDITION)
                        .withStockCreativeId(1076628099L)
                        .withLivePreviewUrl(
                                "https://canvas-creatives-devtest.qart.yandex" +
                                        ".ru/video-additions/5a215e05e4511f000b865881/preview?compact=1")
                        .withModerationInfo(new ModerateInfo()
                                .withAspects(Arrays.asList(new ModerationInfoAspect()
                                        .withHeight(9)
                                        .withWidth(16)))
                                .withHtml(new ModerationInfoHtml()
                                        .withUrl(
                                                "https://canvas-creatives-devtest.qart.yandex" +
                                                        ".ru/video-additions/5a215e05e4511f000b865881/preview?compact" +
                                                        "=1"))
                                .withSounds(Arrays.asList(new ModerationInfoSound()
                                        .withStockId("3891307")
                                        .withUrl(
                                                "https://storage.mds.yandex" +
                                                        ".net/get-bstor/15200/6f84faa9-f3d2-4e31-be6b-7e1205aa46a4" +
                                                        ".wav")))
                                .withTexts(Arrays.asList(new ModerationInfoText()
                                        .withColor("#000000")
                                        .withText("Подробнее")
                                        .withType("button")))
                                .withVideos(Arrays.asList(new ModerationInfoVideo()
                                        .withStockId("new2_12_12-7.mov")
                                        .withUrl(
                                                "https://storage.mds.yandex" +
                                                        ".net/get-bstor/15200/fa1468e8-b413-4534-b2b8-d05238a7da8a" +
                                                        ".qt"))))
                        .withPreviewUrl(
                                "https://avatars.mds.yandex.net/get-media-adv-screenshooter/41244/7af73256-3663-49f4" +
                                        "-abc0-d77a9d5d10dd/orig"))
                .withCreativeId(1076628100L)
                .withOk(true);
    }
}
