package ru.yandex.market.pers.author;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.mockito.Mockito;
import org.springframework.util.StreamUtils;

import ru.yandex.market.cataloger.CatalogerClient;
import ru.yandex.market.cataloger.model.CatalogerResponseWrapper;
import ru.yandex.market.cataloger.model.VersionInfo;
import ru.yandex.market.cataloger.model.VersionInfoWrapper;
import ru.yandex.market.live.LiveStreamingTarantinoClient;
import ru.yandex.market.live.model.LiveStreamingData;
import ru.yandex.market.live.model.LiveStreamingGarsons;
import ru.yandex.market.live.model.LiveStreamingGarsonsParams;
import ru.yandex.market.live.model.LiveStreamingPresenter;
import ru.yandex.market.live.model.LiveStreamingPresenterImage;
import ru.yandex.market.live.model.LiveStreamingPreview;
import ru.yandex.market.live.model.LiveStreamingPreviewImage;
import ru.yandex.market.live.model.LiveStreamingPromoInfo;
import ru.yandex.market.live.model.LiveStreamingProperties;
import ru.yandex.market.live.model.LiveStreamingScrollbox;
import ru.yandex.market.live.model.LiveStreamingScrollboxResources;
import ru.yandex.market.mbo.MboCmsApiClient;
import ru.yandex.market.pers.author.tms.live.client.FApiLiveClient;
import ru.yandex.market.pers.author.tms.live.client.ZenClient;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.telegram.TelegramBotClient;
import ru.yandex.market.telegram.TelegramResponse;
import ru.yandex.market.util.FormatUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersAuthorMockFactory {

    public static TelegramBotClient telegramBotClientMock() {
        return PersTestMocksHolder.registerMock(TelegramBotClient.class, client -> {
            final TelegramResponse mockResponse = new TelegramResponse();
            mockResponse.setOk(true);
            when(client.sendBotMessage(any(), any())).thenReturn(mockResponse);
        });
    }

    public static ReportService reportServiceMock() {
        return PersTestMocksHolder.registerMock(ReportService.class, reportService ->
                when(reportService.getModelsByIds(anyList())).then(invocation -> Collections.emptyMap()));
    }

    public static LiveStreamingTarantinoClient liveStreamingTarantinoClientMock() {
        return PersTestMocksHolder.registerMock(LiveStreamingTarantinoClient.class, liveStreamingTarantinoClient -> {
            when(liveStreamingTarantinoClient.getScheduledLiveStreams()).then(invocation -> generatePreviews());
            when(liveStreamingTarantinoClient.getLiveStreamingInfo(anyLong())).thenReturn(generateLiveStreamingData());
        });
    }

    private static LiveStreamingData generateLiveStreamingData() {
        LiveStreamingPreviewImage liveStreamingImage = new LiveStreamingPreviewImage("//avatars.mds.yandex.net/get-marketcms/1779479/img-24555b8f-9ded-4b58-a902-c977d9062695.png/optimize");
        LiveStreamingGarsonsParams garsonsParams = new LiveStreamingGarsonsParams(Arrays.asList(1L, 2L, 3L));
        LiveStreamingGarsons garsons = new LiveStreamingGarsons("PrimeSearch", garsonsParams);
        LiveStreamingScrollboxResources resources = new LiveStreamingScrollboxResources(Collections.singletonList(garsons));
        LiveStreamingScrollbox liveStreamingScrollbox = new LiveStreamingScrollbox(resources);
        LiveStreamingPromoInfo promoInfo = new LiveStreamingPromoInfo("announcement");
        LiveStreamingPresenterImage image = new LiveStreamingPresenterImage("url");
        LiveStreamingPresenter presenter = new LiveStreamingPresenter("type", "name",
                "description", image, 12345L, 123456L);
        LiveStreamingProperties liveStreamingProperties = new LiveStreamingProperties(presenter, promoInfo,
                "description", "title", liveStreamingScrollbox, liveStreamingImage);
        return new LiveStreamingData(1234L, "live_stream", liveStreamingProperties);
    }

    public static List<LiveStreamingPreview> generatePreviews() {
        List<LiveStreamingPreview> previews = new ArrayList<>();

        previews.add(new LiveStreamingPreview(123, "first_live", "Первая лайв трансляция", "2021-05-19T17:00:00.000Z"
                , 60));
        previews.add(new LiveStreamingPreview(124, "second_live", "Вторая лайв трансляция", "2021-05-20T17:00:00" +
                ".000Z", 60));
        return previews;
    }

    public static FApiLiveClient fApiLiveClientMock() {
        return PersTestMocksHolder.registerMock(FApiLiveClient.class);
    }

    public static MboCmsApiClient mboCmsApiClientMock() {
        return PersTestMocksHolder.registerMock(MboCmsApiClient.class);
    }

    public static ZenClient zenClientMock() {
        return PersTestMocksHolder.registerMock(ZenClient.class);
    }

    public static CatalogerClient catalogerClientMock() {
        VersionInfo versionInfoMock = mock(VersionInfo.class);
        when(versionInfoMock.getVersion()).thenReturn("current");
        VersionInfoWrapper versionInfoWrapperMock = Mockito.mock(VersionInfoWrapper.class);
        Mockito.when(versionInfoWrapperMock.getVersionInfo()).thenReturn(versionInfoMock);
        return PersTestMocksHolder.registerMock(CatalogerClient.class, catalogerClient -> {
                when(catalogerClient.getCatalogerVersion()).thenReturn(Optional.of(versionInfoWrapperMock));
                when(catalogerClient.getNavigationTreeFromDepartment()).thenReturn(generateTestCatalogerResponse());
            });
    }

    private static Optional<CatalogerResponseWrapper> generateTestCatalogerResponse() {
        try {
            String content = StreamUtils.copyToString(PersAuthorMockFactory.class.getClassLoader().getResourceAsStream(
                "data/cataloger_navigation_tree.json"), StandardCharsets.UTF_8);
            return Optional.of(FormatUtils.fromJson(content, CatalogerResponseWrapper.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

}
