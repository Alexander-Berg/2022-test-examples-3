package ru.yandex.canvas.service.video;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.model.direct.CreativeUploadData;
import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.model.video.addition.AdditionData;
import ru.yandex.canvas.model.video.addition.AdditionDataBundle;
import ru.yandex.canvas.model.video.addition.AdditionElement;
import ru.yandex.canvas.model.video.addition.options.AdditionElementOptions;
import ru.yandex.canvas.model.video.addition.options.BodyElementOptions;
import ru.yandex.canvas.model.video.addition.options.ButtonElementOptions;
import ru.yandex.canvas.model.video.files.AudioSource;
import ru.yandex.canvas.model.video.files.Movie;
import ru.yandex.canvas.model.video.files.StreamFormat;
import ru.yandex.canvas.model.video.files.VideoSource;
import ru.yandex.canvas.repository.video.StockVideoAdditionsRepository;
import ru.yandex.canvas.repository.video.VideoAdditionsRepository;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.RTBHostExportService;
import ru.yandex.canvas.service.direct.VideoAdditionDirectUploadHelper;
import ru.yandex.canvas.service.video.files.MergedFileRecord;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static ru.yandex.canvas.model.direct.CreativeUploadType.VIDEO_ADDITION;
import static ru.yandex.direct.feature.FeatureName.PERF_CREATIVES_VIDEO_SIZE;

@RunWith(SpringJUnit4ClassRunner.class)
public class ToDirectUploadFormatTest {

    @Autowired
    private VideoAdditionDirectUploadHelper videoAdditionDirectUploadHelper;

    @Autowired
    private VideoAdditionsService videoAdditionsService;


    @Autowired
    private MovieServiceInterface movieService;

    @Autowired
    private DirectService directService;

    @Autowired
    private VideoPreviewUrlBuilder videoPreviewUrlBuilder;

    @TestConfiguration
    public static class TestConf {
        @MockBean
        private VideoAdditionsService videoAdditionsService;

        @MockBean
        private VideoAdditionsRepository videoAdditionsRepository;

        @MockBean
        private DirectService directService;

        @MockBean
        private RTBHostExportService rtbHostExportService;

        @MockBean
        private StockVideoAdditionsRepository stockVideoAdditionsRepository;

        @MockBean
        private MovieService movieService;

        @MockBean
        private AudioService audioService;

        @MockBean
        private VideoPresetsService videoPresetsService;

        @MockBean
        private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @MockBean
        private VideoPreviewUrlBuilder videoPreviewUrlBuilder;

        @Bean
        public VideoAdditionDirectUploadHelper videoAdditionDirectUploadHelper() {
            return new VideoAdditionDirectUploadHelper(videoPreviewUrlBuilder, audioService, movieService, directService);
        }
    }

    private Addition dummyAddition(AdditionData data) {
        return new Addition()
                .setData(data)
                .setCreativeId(1L)
                .setName("AdditionName")
                .setClientId(12345L)
                .setScreenshotUrl("screenshot_url")
                .setId("abcde")
                .setPresetId(241L)
                .setDate(null)
                .setArchive(false)
                .setVast("<xml> big vast content</xml>");
    }

    private Addition makeAdditionWithElements(List<AdditionElement> elements) {
        AdditionData data = new AdditionData();
        AdditionDataBundle bundle = new AdditionDataBundle();
        bundle.setName("Bundle name");
        data.setBundle(bundle);

        data.setElements(elements);

        return dummyAddition(data);
    }

    @Test
    public void convertionSmokeTest() {
        Mockito.when(directService.getFeatures(any(), any()))
                .thenReturn(Set.of(PERF_CREATIVES_VIDEO_SIZE.getName()));
        Mockito.when(videoPreviewUrlBuilder.getPreviewUrl(anyLong(), any(Addition.class), eq(true)))
                .thenReturn("http://live.preview/");

        VideoSource videoSource = mock(VideoSource.class);
        AudioSource audioSource = mock(AudioSource.class);

        Mockito.when(videoSource.getId()).thenReturn("OLD_VIDEO_ID");
        Mockito.when(videoSource.getStockId()).thenReturn("OLD_VIDEO");
        Mockito.when(videoSource.getStillageUrl()).thenReturn("http://video.yandex.ru/stream.mpg");

        Mockito.when(audioSource.getId()).thenReturn("NEW_AUDIO_ID");
        Mockito.when(audioSource.getStockId()).thenReturn("NEW_AUDIO");
        Mockito.when(audioSource.getStillageUrl()).thenReturn("http://by.ly/sdvdfdkk");

        MergedFileRecord.FormatDescription description = new MergedFileRecord.FormatDescription()
                .setBitrate(138000L)
                .setDelivery("yaEda")
                .setHeight("1080")
                .setWidth("768")
                .setId("video.ogv")
                .setUrl("http://youtu.be/someVideo.ogg")
                .setMimeType("video/ogg");

        StreamFormat format = new StreamFormat(description);

        Movie movie = mock(Movie.class);
        Mockito.when(movie.isStock()).thenReturn(true);
        Mockito.when(movie.getDuration()).thenReturn(120.0);
        Mockito.when(movie.getVideoSource()).thenReturn(videoSource);
        Mockito.when(movie.getAudioSource()).thenReturn(audioSource);
        Mockito.when(videoSource.getRatio()).thenReturn("3:2");
        Mockito.when(movie.getFormats()).thenReturn(Arrays.asList(format));

        Mockito.when(movieService.lookupMovie("test_video1", "test_audio1", 12345L, 241L)).thenReturn(movie);

        AdditionElement fileElement = new AdditionElement(AdditionElement.ElementType.ADDITION);
        fileElement.withAvailable(true);
        fileElement.withOptions(new AdditionElementOptions()
                .setAudioId("test_audio1")
                .setVideoId("test_video1")
                .setPackshotId("test_packshot"));

        AdditionElement bodyElement = new AdditionElement(AdditionElement.ElementType.BODY);
        bodyElement.withAvailable(true);
        bodyElement.withOptions(
                new BodyElementOptions().setText("текст кнопки").setTextColor("#0FCDFE").setBackgroundColor("#0000FF"));

        AdditionElement buttonElement = new AdditionElement(AdditionElement.ElementType.BUTTON);
        buttonElement.withAvailable(false);
        buttonElement.withOptions(new ButtonElementOptions().setText("Button text"));

        Addition addition = makeAdditionWithElements(Arrays.asList(fileElement, buttonElement, bodyElement));

        CreativeUploadData creativeUploadData =
                videoAdditionDirectUploadHelper.toCreativeUploadData(addition, addition.getClientId());

        assertThat("Object has correct direct format", creativeUploadData, allOf(
                hasProperty("creativeId", equalTo(addition.getCreativeId())),
                hasProperty("stockCreativeId", equalTo(addition.getStockCreativeId())),
                hasProperty("creativeType", equalTo(VIDEO_ADDITION)),
                hasProperty("creativeName", equalTo("AdditionName")),
                hasProperty("previewUrl", is("screenshot_url")),
                hasProperty("livePreviewUrl", is("http://live.preview/")),
                hasProperty("duration", is(120.0)),
                hasProperty("presetId", is(241)),
                hasProperty("moderationInfo", Matchers.notNullValue()),
                hasProperty("hasPackshot", is(true)),
                hasProperty("additionalData", allOf(
                        hasProperty("duration", is(120.0)),
                        hasProperty("formats", contains(
                                allOf(
                                        hasProperty("width", is(768)),
                                        hasProperty("height", is(1080)),
                                        hasProperty("url", is("http://youtu.be/someVideo.ogg")),
                                        hasProperty("type", is("video/ogg"))
                                ))
                        ))
                )
        ));

        assertThat("Moderation info correct", creativeUploadData.getModerationInfo().getHtml().getUrl(),
                is("http://live.preview/"));

        assertThat("Moderation texts has correct size", creativeUploadData.getModerationInfo().getTexts(), hasSize(1));

        assertThat("Moderation texts correct", creativeUploadData.getModerationInfo().getTexts(),
                Matchers.contains(
                        allOf(hasProperty("text", is("текст кнопки")),
                                hasProperty("color", is("#0FCDFE")),
                                hasProperty("type", is("body"))
                        )
                )
        );

        assertThat("Moderation videos are ok", creativeUploadData.getModerationInfo().getVideos(), hasSize(1));
        assertThat("Moderation videos are ok", creativeUploadData.getModerationInfo().getVideos(), contains(
                allOf(
                        hasProperty("stockId", is("OLD_VIDEO")),
                        hasProperty("url", is("http://video.yandex.ru/stream.mpg"))
                )
        ));

        assertThat("Moderation sounds are ok", creativeUploadData.getModerationInfo().getSounds(), hasSize(1));
        assertThat("Moderation sounde are ok", creativeUploadData.getModerationInfo().getSounds(), contains(
                allOf(
                        hasProperty("stockId", is("NEW_AUDIO")),
                        hasProperty("url", is("http://by.ly/sdvdfdkk"))
                )
        ));

        assertThat("Moderation aspects are ok", creativeUploadData.getModerationInfo().getAspects(), hasSize(1));
        assertThat("Moderation aspects are ok", creativeUploadData.getModerationInfo().getAspects(), contains(
                allOf(
                        hasProperty("width", is(3L)),
                        hasProperty("height", is(2L))
                )
        ));

        assertThat("size not empty", creativeUploadData.getWidth(), is(768));
    }

}
