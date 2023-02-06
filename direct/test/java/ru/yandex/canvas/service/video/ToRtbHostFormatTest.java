package ru.yandex.canvas.service.video;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.model.video.VideoFiles;
import ru.yandex.canvas.model.video.addition.AdditionData;
import ru.yandex.canvas.model.video.addition.AdditionDataBundle;
import ru.yandex.canvas.model.video.addition.AdditionElement;
import ru.yandex.canvas.model.video.addition.options.AdditionElementOptions;
import ru.yandex.canvas.model.video.addition.options.BodyElementOptions;
import ru.yandex.canvas.model.video.files.Movie;
import ru.yandex.canvas.model.video.files.StreamFormat;
import ru.yandex.canvas.model.video.files.VideoSource;
import ru.yandex.canvas.repository.video.StockVideoAdditionsRepository;
import ru.yandex.canvas.repository.video.VideoAdditionsRepository;
import ru.yandex.canvas.service.DateTimeService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.PackshotService;
import ru.yandex.canvas.service.RTBHostExportService;
import ru.yandex.canvas.service.direct.VideoAdditionDirectUploadHelper;
import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.VideoPreset;
import ru.yandex.direct.bs.dspcreative.model.DspCreativeExportEntry;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static ru.yandex.canvas.steps.PresetDescriptionsSteps.leastPresetDescription;

@RunWith(SpringJUnit4ClassRunner.class)
public class ToRtbHostFormatTest {
    public static final long COMMON_MOVIE_PRESET_ID = 1L;

    private Movie movie;

    @Autowired
    private MovieServiceInterface movieService;

    @Autowired
    private VideoCreativesService videoCreativesService;

    @Autowired
    private VideoAdditionsService videoAdditionsService;

    @Autowired
    private MovieServiceInterface commonMovieService;

    @Autowired
    private VideoPresetsService videoPresetsService;

    @TestConfiguration
    public static class TestConf {
        @MockBean
        private VideoAdditionsService videoAdditionsService;

        @MockBean
        private MovieServiceInterface movieService;

        @MockBean
        private AudioService audioService;

        @MockBean
        private PackshotService packshotService;

        @MockBean
        private VideoAdditionsRepository videoAdditionsRepository;

        @MockBean
        private DirectService directService;

        @MockBean
        private RTBHostExportService rtbHostExportService;

        @MockBean
        private StockVideoAdditionsRepository stockVideoAdditionsRepository;

        @MockBean
        private VideoPresetsService videoPresetsService;

        @MockBean
        private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @MockBean
        private VideoAdditionDirectUploadHelper videoAdditionDirectUploadHelper;

        @Bean
        public VideoCreativesService videoCreativesService(MovieServiceInterface movieService,
                                                           AudioService audioService,
                                                           PackshotService packshotService,
                                                           VideoAdditionsService videoAdditionsService,
                                                           VideoAdditionsRepository videoAdditionsRepository,
                                                           DirectService directService,
                                                           RTBHostExportService rtbHostExportService,
                                                           StockVideoAdditionsRepository stockVideoAdditionsRepository,
                                                           VideoPresetsService videoPresetsService) {
            return new VideoCreativesService(movieService, audioService, packshotService, videoAdditionsService,
                    videoAdditionsRepository, directService, rtbHostExportService, stockVideoAdditionsRepository,
                    cmsConversionStatusUpdateService, videoPresetsService, videoAdditionDirectUploadHelper,
                    new DateTimeService());
        }
    }

    private Addition dummyAddition(AdditionData data) {
        return new Addition()
                .setData(data)
                .setCreativeId(1L)
                .setName("")
                .setClientId(12345L)
                .setScreenshotUrl("screenshot_url")
                .setId("abcde")
                .setPresetId(1L)
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

    @Before
    public void setUp() {
        VideoSource videoSource = mock(VideoSource.class);
        Mockito.when(videoSource.getId()).thenReturn("test_video1");
        Mockito.when(videoSource.getVideoMetaId()).thenReturn(BigInteger.valueOf(8569468391257906944L));
        Mockito.when(videoSource.getPlayerId()).thenReturn("testPlayerId");

        movie = mock(Movie.class);
        Mockito.when(movie.getDuration()).thenReturn(12.0);
        Mockito.when(movie.getVideoSource()).thenReturn(videoSource);

        Mockito.when(commonMovieService.lookupMovie("test_video1", "test_audio1", 12345L, COMMON_MOVIE_PRESET_ID))
                .thenReturn(movie);

        PresetDescription description = leastPresetDescription(COMMON_MOVIE_PRESET_ID);
        description.setControlsAllowed(true);
        Mockito.when(videoPresetsService.getPreset(COMMON_MOVIE_PRESET_ID)).thenReturn(new VideoPreset(description));
        Mockito.when(videoPresetsService.contains(COMMON_MOVIE_PRESET_ID)).thenReturn(true);
    }

    @Test
    public void convertionSmokeTest() {
        AdditionElement fileElement = new AdditionElement(AdditionElement.ElementType.ADDITION);
        fileElement.withAvailable(true);
        fileElement.withOptions(new AdditionElementOptions().setAudioId("test_audio1").setVideoId("test_video1"));

        Addition addition = makeAdditionWithElements(Arrays.asList(fileElement));

        ObjectMapper objectMapper = new ObjectMapper();

        DspCreativeExportEntry entry = videoCreativesService.toImportDspCreativeEntry(addition, objectMapper);

        assertThat("Object has correct rtb format", entry, allOf(
                hasProperty("creativeId", equalTo(1L)),
                hasProperty("creativeVersionId", equalTo(1L)),
                hasProperty("data", equalTo("<xml> big vast content</xml>")),
                hasProperty("staticData", equalTo("{\"creative_id\":\"1\"}")),
                hasProperty("constructorData", Matchers.not(empty())),
                hasProperty("isEnabled", is(true)),
                hasProperty("isVideo", is(true)),
                hasProperty("postmoderated", is(true)),
                hasProperty("isStatic", is(false)),
                hasProperty("tag", is("yabs")),
                hasProperty("width", is(0)),
                hasProperty("height", is(0))
        ));
    }

    @Test
    public void constructorDataJsonTest() {
        VideoFiles.VideoFormat videoFormat = new VideoFiles.VideoFormat()
                .setUrl("http://video.url.ru/1234/klk?13.mp4")
                .setHeight("1024")
                .setWidth("768")
                .setMimeType("video/mpeg")
                .setBitrate(13441100L);

        StreamFormat format = new StreamFormat(videoFormat);

        Mockito.when(movie.getFormats()).thenReturn(Arrays.asList(format));

        AdditionElement fileElement = new AdditionElement(AdditionElement.ElementType.ADDITION);
        fileElement.withAvailable(true);
        fileElement.withOptions(new AdditionElementOptions().setAudioId("test_audio1").setVideoId("test_video1"));

        AdditionElement bodyElement = new AdditionElement(AdditionElement.ElementType.BODY)
                .withAvailable(true)
                .withOptions(new BodyElementOptions().setText("body text").setBackgroundColor("#00FFAACC")
                        .setTextColor("#00000C"));

        Addition addition = makeAdditionWithElements(Arrays.asList(fileElement, bodyElement));

        ObjectMapper objectMapper = new ObjectMapper();

        DspCreativeExportEntry entry = videoCreativesService.toImportDspCreativeEntry(addition, objectMapper);

        String json = entry.getConstructorData();

        assertThat("Valid json", json, is("{\"template\":\"Bundle name\",\"underlayerID\":648102816,\"duration\":12,"
                + "\"isStock\":false,\"hasPackshot\":false,\"playbackParameters\":{\"showSkipButton\":true,\"skipDelay\":5},"
                + "\"elements\":[{\"type\":\"addition\"},{\"type\":\"body\",\"text\":\"body text\","
                + "\"backgroundColor\":\"#00FFAACC\",\"textColor\":\"#00000C\"}],\"formats\":[{\"height\":\"1024\","
                + "\"width\":\"768\",\"type\":\"video/mpeg\",\"url\":\"http://video.url"
                + ".ru/1234/klk?13.mp4\"}],\"creative_parameters\":{\"Video\":{\"Theme\":\"Bundle name\",\"Duration\":12.0,"
                + "\"MediaFiles\":[{\"Width\":768,\"Height\":1024,\"Url\":\"http://video.url.ru/1234/klk?13.mp4\","
                + "\"MimeType\":\"video/mpeg\",\"Bitrate\":\"13441100\"}],\"HasAbuseButton\":true,"
                + "\"SocialAdvertisement\":false,\"PlaybackParameters\":{\"ShowSkipButton\":true,\"SkipDelay\":\"5\"},"
                + "\"UseTrackingEvents\":false,\"IsStock\":false,\"AdditionElements\":[{\"Type\":\"BODY\","
                + "\"Options\":{\"BackgroundColor\":\"#00FFAACC\",\"TextColor\":\"#00000C\",\"Text\":\"body text\"}}],"
                + "\"InteractiveVpaid\":false,\"AddPixelImpression\":false,\"CreativeId\":\"1\",\"ShowVpaid\":true,"
                + "\"ShowVideoClicks\":true,\"SoundbtnLayout\":\"1\",\"AdlabelLayout\":\"1\",\"CountdownLayout\":\"1\","
                + "\"UseVpaidImpressions\":false,\"AdSystem\":\"Yabs Ad Server\",\"VideoMetaId" +
                "\":\"8569468391257906944\",\"PlayerId\":\"testPlayerId\"}}}"));
    }

}
