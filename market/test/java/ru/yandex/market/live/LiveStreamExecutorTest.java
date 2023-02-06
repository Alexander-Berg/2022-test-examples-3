package ru.yandex.market.live;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.live.model.LiveStreamingPreview;
import ru.yandex.market.mbo.MboCmsApiClient;
import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.pers.author.live.LiveStreamService;
import ru.yandex.market.pers.author.tms.live.LiveStreamDbService;
import ru.yandex.market.pers.author.tms.live.LiveStreamExecutor;
import ru.yandex.market.pers.author.tms.live.client.FApiLiveClient;
import ru.yandex.market.pers.author.tms.live.client.ZenClient;
import ru.yandex.market.pers.author.tms.live.model.LiveStream;
import ru.yandex.market.pers.author.tms.live.model.LiveStreamData;
import ru.yandex.market.pers.author.tms.live.model.LiveStreamState;
import ru.yandex.market.pers.author.tms.live.model.ZenData;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.pers.author.PersAuthorMockFactory.generatePreviews;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.and;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withMethod;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withPath;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withQueryParam;

public class LiveStreamExecutorTest extends PersAuthorTest {

    public static final String CHAT_ID = "0/24/10083039-c025-4fa7-9b2a-2faee120f363";
    public static final String INVITE_HASH = "eaf2a9c7-0b9d-479e-bdba-4c8ceb885bd4";
    private static final String streamId = "1234";
    private static final String videoId = "123";
    private static final String publicationId = "12c3f456cd7e891f11011c1a";
    private final HttpClient httpClientFapi = mock(HttpClient.class);
    private final HttpClient httpClientCms = mock(HttpClient.class);
    private final HttpClient httpClientZen = mock(HttpClient.class);
    private final HttpClient httpClientTarantino = mock(HttpClient.class);
    @Autowired
    private LiveStreamDbService liveStreamDbService;
    @Autowired
    private ConfigurationService configurationService;
    @Value("${pers.cms.live.stream.props.path}")
    private String cmsPropertiesPath;
    @Value("${pers.zen.publisher.id:5943d1343c50f75ccde78633}")
    private String zenPublisherId;
    private LiveStreamExecutor executor;

    @BeforeEach
    public void init() {
        super.setUp();
        FApiLiveClient fApiLiveClient = new FApiLiveClient(
                "http://target:90",
                "http://target:90",
                new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClientFapi)),
                "oauth_token"
        );

        MboCmsApiClient mboCmsApiClient = new MboCmsApiClient(
                new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClientCms)),
                "http://localhost12345:8080"
        );

        ZenClient zenClient = new ZenClient("http://target:90",
                new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClientZen)), "");

        LiveStreamingTarantinoClient tarantinoClient = new LiveStreamingTarantinoClient("http://target:90",
                new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClientTarantino)));

        LiveStreamService service = new LiveStreamService(cmsPropertiesPath, fApiLiveClient,
                configurationService, mboCmsApiClient, liveStreamDbService, zenClient, tarantinoClient);
        executor = new LiveStreamExecutor(service);
    }

    @Test
    public void testWorkWithStreams() throws IOException {
        mockFapi();
        mockCms();
        mockZen();
        mockTarantino();

        List<LiveStreamingPreview> previews = generatePreviews();
        liveStreamDbService.saveScheduledLiveStream(previews);
        previews.forEach(liveStreamingPreview -> liveStreamDbService.updateLiveStreamDate(liveStreamingPreview.getCmsPageId()));
        List<Long> ids = liveStreamDbService.getScheduledLiveStreams().stream()
                .map(LiveStream::getId).collect(Collectors.toList());

        //generateGarbage();

        executor.updateLiveStreamStatuses();

        List<LiveStream> streams = liveStreamDbService.getStreams(ids);
        assertTrue(streams.stream().allMatch(it ->
                CHAT_ID.equals(it.getData().getChat().getChatId())
                        && INVITE_HASH.equals(it.getData().getChat().getChatInviteHash())));

        executor.startLiveStreams();
        int cmsCalls = ids.size() * 4; // create draft, chat (hash+id): one call for new migration
        // and 1 call for run
        verify(httpClientCms, times(cmsCalls))
                .execute(any(HttpUriRequest.class), any(HttpContext.class));
        streams = liveStreamDbService.getStreams(ids);
        assertTrue(streams.stream().allMatch(it -> it.getData() != null && it.getData().getZenData() != null));


        assertTrue(streams.stream().allMatch(it ->
                CHAT_ID.equals(it.getData().getChat().getChatId())
                        && INVITE_HASH.equals(it.getData().getChat().getChatInviteHash())));

        assertTrue(streams.stream().allMatch(it ->
                streamId.equals(it.getData().getZenData().getStreamId()) &&
                        publicationId.equals(it.getData().getZenData().getPublicationId()) &&
                        videoId.equals(it.getData().getZenData().getTranslationId())));

        assertTrue(streams.stream().allMatch(it -> it.getState() == LiveStreamState.PREPARED));

        liveStreamDbService.updateLiveStreamsState(ids, LiveStreamState.READY_TO_CLOSE);
        executor.closeLiveStreams();
        streams = liveStreamDbService.getStreams(ids);
        assertTrue(streams.stream().allMatch(it -> it.getState() == LiveStreamState.CLOSED));
        cmsCalls += ids.size() * 4; // remove chatId, inviteHash : one call for new migration and 1 call for run
        verify(httpClientCms, times(cmsCalls))
                .execute(any(HttpUriRequest.class), any(HttpContext.class));
    }

    private void mockZen() {
        HttpClientMockUtils.mockResponseWithFile(httpClientZen, "/live/zen/draft_live.json",
                withPath("/internal-api/add-live"));

        HttpClientMockUtils.mockResponseWithFile(httpClientZen, "/live/zen/add_image.json",
                withPath("/internal-api/add-image-from-url"));

        HttpClientMockUtils.mockResponseWithFile(httpClientZen, "/live/zen/add_image.json",
                withPath("/internal-api/add-image-from-url"));

        HttpClientMockUtils.mockResponseWithFile(httpClientZen, "/live/zen/stream_status_ok.json",
                withPath("/internal-api/publisher/" + zenPublisherId + "/publication/" + publicationId + "/products"));

        HttpClientMockUtils.mockResponseWithFile(httpClientZen, "/live/zen/stream_status_ok.json",
                withPath("/internal-api/start-live-stream-and-publish"));
//                withQueryParam("publisherId", publisherId),
//                withQueryParam("publicationId", publicationId)));

        HttpClientMockUtils.mockResponseWithFile(httpClientZen, "/live/zen/stream_status_ready.json",
                withPath("/internal-api/live-status"));
//                withQueryParam("streamId", streamId)));
    }

    private void mockCms() {
        HttpClientMockUtils.mockResponseWithFile(httpClientCms, "/live/cms/create_migration.json",
                and(
                        withQueryParam("userId", MboCmsApiClient.USER_ID),
                        withPath("/migration/new"),
                        withMethod(HttpMethod.POST)));

        HttpClientMockUtils.mockResponseWithFile(httpClientCms, "/live/cms/run_migration.json",
                and(
                        withPath("/migration/runMigrationSync"),
                        withQueryParam("userId", MboCmsApiClient.USER_ID),
                        withQueryParam("id", 100500),
                        withMethod(HttpMethod.POST)));
    }

    private void mockFapi() {
        HttpClientMockUtils.mockResponseWithFile(httpClientFapi, "/live/fapi/create_chat_answer.json",
                withQueryParam("name", "resolveCreateChat"));
        HttpClientMockUtils.mockResponseWithFile(httpClientFapi, "/live/fapi/remove_chat_members.json",
                withQueryParam("name", "removeChatMembers"));
    }

    private void mockTarantino() {
        HttpClientMockUtils.mockResponseWithFile(httpClientTarantino, "/live/tarantino_blogger_response.json",
                withQueryParam("page_id", 123));

        HttpClientMockUtils.mockResponseWithFile(httpClientTarantino, "/live/tarantino_blogger_response.json",
                withQueryParam("page_id", 124));
    }

    private void generateGarbage() {
        List<LiveStreamingPreview> previews = new ArrayList<>();
        for (LiveStreamState state : LiveStreamState.values()) {
            if (state != LiveStreamState.READY_TO_PREPARE && state != LiveStreamState.READY_TO_CLOSE) {
                int value = state.getValue();
                int id = 100 + value;
                previews.add(new LiveStreamingPreview(
                        id,
                        value + "_live", "Kайв трансляция номер " + value,
                        "2021-05-20T17:00:00.000Z", 60 + value
                ));
                liveStreamDbService.saveScheduledLiveStream(previews);
                liveStreamDbService.updateLiveStreamDate(id);
                liveStreamDbService.updateLiveStreamState(id, state);
                liveStreamDbService.updateLiveStreamData(id, generateZenData());
            }
        }
    }

    private LiveStreamData generateZenData() {
        LiveStreamData data = new LiveStreamData();
        ZenData zenData = new ZenData("translationId", "streamId", "publicationId", "imageId");
        data.setVideoData(zenData);
        return data;
    }
}
