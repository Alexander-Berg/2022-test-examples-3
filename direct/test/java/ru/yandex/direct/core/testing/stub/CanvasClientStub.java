package ru.yandex.direct.core.testing.stub;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import one.util.streamex.StreamEx;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.canvas.client.CanvasClient;
import ru.yandex.direct.canvas.client.model.ffmpegresolutions.FfmpegResolutionsRequestType;
import ru.yandex.direct.canvas.client.model.ffmpegresolutions.FfmpegResolutionsResponse;
import ru.yandex.direct.canvas.client.model.html5.Html5BatchResponse;
import ru.yandex.direct.canvas.client.model.html5.Html5SourceResponse;
import ru.yandex.direct.canvas.client.model.html5.Html5Tag;
import ru.yandex.direct.canvas.client.model.video.AdditionResponse;
import ru.yandex.direct.canvas.client.model.video.Creative;
import ru.yandex.direct.canvas.client.model.video.CreativeResponse;
import ru.yandex.direct.canvas.client.model.video.ModerateInfo;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoAspect;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoHtml;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoImage;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoSound;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoText;
import ru.yandex.direct.canvas.client.model.video.ModerationInfoVideo;
import ru.yandex.direct.canvas.client.model.video.UacVideoCreativeType;
import ru.yandex.direct.canvas.client.model.video.VideoUploadResponse;
import ru.yandex.direct.core.testing.data.TestFfmpegResolution;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static ru.yandex.direct.core.entity.creative.repository.CreativeConstants.TEXT_VIDEO_ADDITION_LAYOUT_IDS;

public class CanvasClientStub extends CanvasClient {
    private final HashMap<Long, CreativeResponse> creativeResponsesByIds = new HashMap<>();
    private final HashMap<String, VideoUploadResponse> videoUploadResponseById = new HashMap<>();
    private final HashMap<String, VideoUploadResponse> videoUploadResponseByUrl = new HashMap<>();
    private final HashMap<String, VideoUploadResponse> videoUploadResponseByFilename = new HashMap<>();
    private final HashMap<String, AdditionResponse> additionResponseByVideoId = new HashMap<>();
    private final HashMap<Long, Html5SourceResponse> html5SourceResponseByClientId = new HashMap<>();
    private final HashMap<Long, Html5BatchResponse> html5BatchResponseByClientId = new HashMap<>();
    private final HashMap<Long, List<Long>> uploadedCreativesByClientId = new HashMap<>();

    public CanvasClientStub() {
        super(null, (ParallelFetcherFactory) null);
    }

    public void addCreatives(List<Long> creativeIds) {
        Map<Long, CreativeResponse> creatives = StreamEx.of(creativeIds)
                .map(this::defaultCanvasCreative)
                .map(x -> new CreativeResponse().withCreative(x).withOk(true).withCreativeId(x.getCreativeId()))
                .mapToEntry(CreativeResponse::getCreativeId)
                .invert()
                .toMap();

        creativeResponsesByIds.putAll(creatives);
    }

    public void addCustomCreatives(List<Creative> creatives) {
        Map<Long, CreativeResponse> creativeResponses = StreamEx.of(creatives)
                .map(x -> new CreativeResponse().withCreative(x).withOk(true).withCreativeId(x.getCreativeId()))
                .mapToEntry(CreativeResponse::getCreativeId)
                .invert()
                .toMap();

        creativeResponsesByIds.putAll(creativeResponses);
    }

    @Override
    public List<FfmpegResolutionsResponse> getFfmpegResolutions(@Nonnull FfmpegResolutionsRequestType type) {
        checkNotNull(type);
        return Arrays.stream(TestFfmpegResolution.values())
                .map(x -> {
                    String[] wxh = x.getRatio().split(":");
                    int ratioWidth = Integer.parseInt(wxh[0]);
                    int ratioHeight = Integer.parseInt(wxh[1]);
                    return new FfmpegResolutionsResponse(
                            x.getSuffix(), ratioWidth, ratioHeight, x.getWidth(), x.getHeight());
                }).collect(toList());
    }

    @Override
    public List<CreativeResponse> getVideoAdditions(Long clientId, List<Long> creativeIds) {
        return StreamEx.of(creativeIds)
                .map(creativeResponsesByIds::get)
                .nonNull()
                .toList();
    }

    public Creative defaultCanvasCreative(Long creativeId) {
        return new Creative()
                .withCreativeId(creativeId)
                .withCreativeName("video_addition_creative")
                .withModerationInfo(defaultModerationInfo(creativeId))
                .withStockCreativeId(creativeId)
                .withCreativeType(Creative.CreativeType.VIDEO_ADDITION)
                .withPresetId(Long.valueOf(TEXT_VIDEO_ADDITION_LAYOUT_IDS.lowerEndpoint() + 1L).intValue())
                .withPreviewUrl("http://ya.ru")
                .withLivePreviewUrl("http://video.ya.ru")
                .withIsAdaptive(false)
                .withHasPackshot(true);
    }

    private ModerateInfo defaultModerationInfo(Long creativeId) {
        String url = "ya.ru";
        int height = 100;
        int width = 200;
        return new ModerateInfo()
                .withVideos(Collections.singletonList(defaultModerationInfoVideo(creativeId, url)))
                .withAspects(Collections.singletonList(defaultModerationInfoAspect(height, width)))
                .withHtml(defaultModerationInfoHtml(url))
                .withSounds(Collections
                        .singletonList(defaultModerationInfoSound(creativeId, url)))
                .withTexts(Collections.singletonList(defaultModerationInfoText()))
                .withImages(Collections.singletonList(new ModerationInfoImage()));
    }

    private ModerationInfoText defaultModerationInfoText() {
        return new ModerationInfoText().withType("type").withColor("color").withText("text");
    }

    private ModerationInfoSound defaultModerationInfoSound(Long creativeId, String url) {
        return new ModerationInfoSound().withStockId(String.valueOf(creativeId)).withUrl(url);
    }

    private ModerationInfoHtml defaultModerationInfoHtml(String url) {
        return new ModerationInfoHtml().withUrl(url);
    }

    private ModerationInfoAspect defaultModerationInfoAspect(int height, int width) {
        return new ModerationInfoAspect().withHeight(height).withWidth(width);
    }

    private ModerationInfoVideo defaultModerationInfoVideo(Long creativeId, String url) {
        return new ModerationInfoVideo()
                .withStockId(String.valueOf(creativeId))
                .withUrl(url);
    }

    public void addCustomVideoUploadResponseWithId(VideoUploadResponse videoUploadResponse, String id) {
        videoUploadResponseById.put(id, videoUploadResponse);
    }

    @Override
    public VideoUploadResponse getVideoById(Long clientId, String videoId) {
        return videoUploadResponseById.get(videoId);
    }

    //Для разных тестов нужно использовать отличный url
    public void addCustomVideoUploadResponseWithUrl(VideoUploadResponse videoUploadResponse, String url) {
        videoUploadResponseByUrl.put(url, videoUploadResponse);
    }


    //Для разных тестов нужно использовать отличный filename
    public void addCustomVideoUploadReseponseWithFileName(VideoUploadResponse videoUploadResponse, String filename) {
        videoUploadResponseByFilename.put(filename, videoUploadResponse);
    }

    @Override
    public VideoUploadResponse createVideoFromFile(
            Long clientId, byte[] data, String name, @Nullable UacVideoCreativeType uacVideoCreativeType, @Nullable Locale locale
    ) {
        return videoUploadResponseByFilename.get(name);
    }

    @Override
    public VideoUploadResponse createVideoFromUrl(
            Long clientId, String url, @Nullable UacVideoCreativeType uacVideoCreativeType, @Nullable Locale locale
    ) {
        return videoUploadResponseByUrl.get(url);
    }

    //Для разных тестов нужно использовать отличный videoId
    public void addCustomAdditionResponseWithVideoId(AdditionResponse additionResponse, String videoId) {
        additionResponseByVideoId.put(videoId, additionResponse);
    }

    @Override
    public AdditionResponse createDefaultAddition(Long clientId, Long presetId, String videoId) {
        return additionResponseByVideoId.get(videoId);
    }

    public void addCustomHtml5SourceResponseWithClientId(Html5SourceResponse html5SourceResponse, Long clientId) {
        html5SourceResponseByClientId.put(clientId, html5SourceResponse);
    }

    @Override
    public Html5SourceResponse uploadHtml5(Long clientId, byte[] data, String name, Html5Tag html5Tag, @Nullable Locale locale) {
        return html5SourceResponseByClientId.get(clientId);
    }

    public void addCustomHtml5BatchResponseWithClientId(Html5BatchResponse html5BatchResponse, Long clientId) {
        html5BatchResponseByClientId.put(clientId, html5BatchResponse);
    }

    @Override
    public Html5BatchResponse createHtml5Batch(Long clientId, String name, String sourceId) {
        return html5BatchResponseByClientId.get(clientId);
    }

    public void addCustomUploadedCreativesByClientId(List<Long> creatives, Long clientId) {
        uploadedCreativesByClientId.put(clientId, creatives);
    }

    @Override
    public List<Long> uploadCreativeToDirect(Long clientId, Long userId, String batchId, Long creativeId) {
        return uploadedCreativesByClientId.get(clientId);
    }
}
