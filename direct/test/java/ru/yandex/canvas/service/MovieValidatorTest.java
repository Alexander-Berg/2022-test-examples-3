package ru.yandex.canvas.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.config.VideoAdditionsTestConfiguration;
import ru.yandex.canvas.exceptions.ValidationErrorsException;
import ru.yandex.canvas.model.stillage.StillageFileInfo;
import ru.yandex.canvas.service.video.Geometry;
import ru.yandex.canvas.service.video.MovieValidator;
import ru.yandex.canvas.service.video.VideoCreativeType;
import ru.yandex.canvas.service.video.VideoGeometryService;
import ru.yandex.canvas.service.video.VideoLimitsService;
import ru.yandex.canvas.service.video.VideoMetaData;
import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.VideoPreset;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static ru.yandex.canvas.VideoConstants.CANVAS_RANGE_RATIO_FEATURE;
import static ru.yandex.canvas.VideoConstants.VIDEO_FILE_RATE_LIMIT;
import static ru.yandex.canvas.service.video.VideoLimitsService.BASE_CONF_PATH;
import static ru.yandex.canvas.service.video.VideoLimitsService.OUTDOOR_CONF_PATH;
import static ru.yandex.canvas.steps.PresetDescriptionsSteps.leastPresetDescription;
import static ru.yandex.direct.feature.FeatureName.SKIP_VIDEO_FILE_RATE_LIMIT;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@Import({ControllerTestConfiguration.class, VideoAdditionsTestConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class MovieValidatorTest {
    private Set<String> features;
    private Long clientConvertingFilesCount;
    private VideoPreset preset;

    @Autowired
    @Qualifier("Mock")
    VideoLimitsService videoLimitsService;

    @Autowired
    SessionParams sessionParams;

    @Autowired
    VideoGeometryService videoGeometryService;


    @Autowired
    @Qualifier("Original")
    VideoLimitsService videoLimitsServiceOriginal;


    @TestConfiguration
    public static class TestConf {
        @Autowired
        AuthRequestParams authRequestParams;

        @Autowired
        DirectService directService;

        @MockBean(name = "Mock")
        VideoLimitsService videoLimitsService;

        @Bean("Original")
        VideoLimitsService videoLimitsServiceOriginal() {
            return new VideoLimitsService(authRequestParams, directService);
        }

        @Bean
        VideoGeometryService videoGeometryService() {
            return new VideoGeometryService(videoLimitsService);
        }
    }

    private static final String stillageInfo = "{\n"
            + "        \"mimeType\" : \"video/mp4\",\n"
            + "        \"metadataInfo\" : {\n"
            + "            \"duration\" : 14.92,\n"
            + "            \"audioStreams\" : null,\n"
            + "            \"videoStreams\" : [\n"
            + "                {\n"
            + "                    \"profile\" : \"High\",\n"
            + "                    \"index\" : 0,\n"
            + "                    \"level\" : \"31\",\n"
            + "                    \"colorSpace\" : \"\",\n"
            + "                    \"frameRate\" : 25,\n"
            + "                    \"height\" : 540,\n"
            + "                    \"width\" : 960,\n"
            + "                    \"pixelFormat\" : \"yuv420p\",\n"
            + "                    \"codec\" : \"h264\",\n"
            + "                    \"duration\" : 14.92,\n"
            + "                    \"bitrate\" : 661085,\n"
            + "                    \"colorRange\" : \"\"\n"
            + "                }\n"
            + "            ],\n"
            + "            \"bitrate\" : 665426,\n"
            + "            \"audioStreams\" : [ ]\n"
            + "        },\n"
            + "        \"url\" : \"https://storage.mds.yandex.net/get-bstor/15200/a032c580-a2a4-4800-887e-6bfe7222addc"
            + ".mp4\",\n"
            + "        \"md5Hash\" : \"1BKiS1GkKKb/NzJ0RPHoAg==\",\n"
            + "        \"fileSize\" : 1241020,\n"
            + "        \"contentGroup\" : \"VIDEO\",\n"
            + "        \"id\" : 6464600\n"
            + "    }";

    private StillageFileInfo makeStillageFileInfo() {
        try {
            return new ObjectMapper().readValue(stillageInfo, StillageFileInfo.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private StillageFileInfo makeOutdoorStillageFileInfoWithAspectRatio21() {
        StillageFileInfo fileInfo = makeStillageFileInfo();
        setDuration(fileInfo, 5);
        setWidth(fileInfo, 1440);
        setHeight(fileInfo, 720);
        return fileInfo;
    }

    @SuppressWarnings("unchecked")
    private StillageFileInfo makeOutdoorStillageFileInfoWithAspectRatio31() {
        StillageFileInfo fileInfo = makeStillageFileInfo();
        setDuration(fileInfo, 7.5);
        setWidth(fileInfo, 1248);
        setHeight(fileInfo, 416);
        return fileInfo;
    }

    @SuppressWarnings("unchecked")
    private StillageFileInfo makeOutdoorStillageFileInfoWithAspectRatio2318() {
        StillageFileInfo fileInfo = makeStillageFileInfo();
        setDuration(fileInfo, 15);
        setWidth(fileInfo, 736);
        setHeight(fileInfo, 576);
        return fileInfo;
    }

    @SuppressWarnings("unchecked")
    private StillageFileInfo makeOutdoorStillageFileInfoWithAspectRatio7855() {
        StillageFileInfo fileInfo = makeStillageFileInfo();
        setDuration(fileInfo, 15);
        setWidth(fileInfo, 1248);
        setHeight(fileInfo, 880);
        return fileInfo;
    }

    @SuppressWarnings("unchecked")
    private StillageFileInfo makeOutdoorStillageFileInfoWithAspectRatio9425() {
        StillageFileInfo fileInfo = makeStillageFileInfo();
        setDuration(fileInfo, 15);
        setWidth(fileInfo, 1504);
        setHeight(fileInfo, 400);
        return fileInfo;
    }

    @SuppressWarnings("unchecked")
    private StillageFileInfo makeOutdoorStillageFileInfoWithAspectRatio103() {
        StillageFileInfo fileInfo = makeStillageFileInfo();
        setDuration(fileInfo, 15);
        setWidth(fileInfo, 1920);
        setHeight(fileInfo, 576);
        return fileInfo;
    }

    private void setDuration(StillageFileInfo fileInfo, double duration) {
        fileInfo.getMetadataInfo().put("duration", duration);
        extractVideoStreamInfo(fileInfo).put("duration", duration);
    }

    private void setWidth(StillageFileInfo fileInfo, int width) {
        fileInfo.getMetadataInfo().put("width", width);
        extractVideoStreamInfo(fileInfo).put("width", width);
    }

    private void setHeight(StillageFileInfo fileInfo, int height) {
        fileInfo.getMetadataInfo().put("height", height);
        extractVideoStreamInfo(fileInfo).put("height", height);
    }

    private void setCodec(StillageFileInfo fileInfo, String codec) {
        extractVideoStreamInfo(fileInfo).put("codec", codec);
    }

    private Map extractVideoStreamInfo(StillageFileInfo fileInfo) {
        return (Map) ((List) fileInfo.getMetadataInfo().get("videoStreams")).get(0);
    }

    private VideoLimits getOutdoorLimits() {
        Config baseConf = ConfigFactory.load(BASE_CONF_PATH).resolve();
        Config outdoorConf = ConfigFactory.load(OUTDOOR_CONF_PATH).withFallback(baseConf).resolve();
        return ConfigBeanFactory.create(outdoorConf, VideoLimits.class);
    }

    private List<String> runValidator(StillageFileInfo stillageFileInfo) {
        return runValidator(stillageFileInfo, videoLimitsServiceOriginal.getLimits(sessionParams.getCreativeType(),
                preset.getId()));
    }

    private List<String> runValidator(StillageFileInfo stillageFileInfo, VideoLimitsInterface videoLimits) {
        VideoMetaData data = new ObjectMapper().convertValue(stillageFileInfo.getMetadataInfo(), VideoMetaData.class);

        Mockito.when(videoLimitsService.getRatiosByVideoCreativeType(any(), any())).thenCallRealMethod();
        Mockito.when(videoLimitsService.getLimits(any(), any())).thenReturn(videoLimits);
        Mockito.when(videoLimitsService.getLimits(any(), anySet(), any())).thenReturn(videoLimits);

        MovieValidator movieValidator = new MovieValidator(data, stillageFileInfo, videoLimits,
                sessionParams.getCreativeType(), features, preset, videoGeometryService, clientConvertingFilesCount);

        try {
            movieValidator.validate();
        } catch (ValidationErrorsException e) {
            return e.getMessages();
        }

        return Collections.emptyList();
    }

    @Before
    public void setLocale() {
        Locale.setDefault(new Locale("en", "US"));
    }

    @Before
    public void setMocks() {
        Mockito.when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPC);
        features = emptySet();
        clientConvertingFilesCount = 0L;
        PresetDescription description = leastPresetDescription();
        preset = new VideoPreset(description);
    }

    @Test
    public void invalidMimeTest() {
        StillageFileInfo info = makeStillageFileInfo();
        info.setMimeType("application/unknown");

        assertThat(runValidator(info), Matchers.notNullValue());
    }

    @Test
    public void invalidDurationTest() {
        StillageFileInfo info = makeStillageFileInfo();
        setDuration(info, 666.0);

        assertThat(runValidator(info),
                contains("Total duration is out of limit. Duration is 666s. "
                        + "Permitted duration range is from 5 up to 60 seconds"));
    }

    @Test
    public void shortVideoTest() {
        StillageFileInfo info = makeStillageFileInfo();
        setDuration(info, 0.1);

        assertThat(runValidator(info),
                contains("Total duration is out of limit. Duration is 0.1s. "
                        + "Permitted duration range is from 5 up to 60 seconds"));
    }

    @Test
    public void correctDurationTest() {
        StillageFileInfo info = makeStillageFileInfo();
        setDuration(info, 20);

        assertThat(runValidator(info), empty());
    }

    @Test
    public void invalidCodec() {
        StillageFileInfo info = makeStillageFileInfo();
        setCodec(info, "unknown codec");

        assertThat(runValidator(info), contains("Unknown video codec. Supported codecs are theora, h264, vp6f, vp8"));
    }

    @Test
    public void validCodec() {
        StillageFileInfo info = makeStillageFileInfo();
        setCodec(info, "vp8");

        assertThat(runValidator(info), empty());
    }

    @Test
    public void tooSmallWidth() {
        StillageFileInfo info = makeStillageFileInfo();
        setWidth(info, 359);

        assertThat(runValidator(info), contains("Video width is 359. But permitted range is from 360 up to 3840"));
    }

    @Test
    public void tooBigWidth() {
        StillageFileInfo info = makeStillageFileInfo();
        setWidth(info, 3841);

        assertThat(runValidator(info), contains("Video width is 3841. But permitted range is from 360 up to 3840"));
    }

    @Test
    public void tooSmallHeight() {
        StillageFileInfo info = makeStillageFileInfo();
        setHeight(info, 359);

        assertThat(runValidator(info), contains("Video height is 359. But permitted range is from 360 up to 3840"));
    }

    @Test
    public void tooBigHeight() {
        StillageFileInfo info = makeStillageFileInfo();
        setHeight(info, 3841);

        assertThat(runValidator(info), contains("Video height is 3841. But permitted range is from 360 up to 3840"));
    }

    @Test
    public void invalidRatio() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio21();
        setWidth(info, 1439);
        List<String> errors = runValidator(info, getOutdoorLimits());
        assertThat(errors, contains("incompatible_video_ratio_list"));
    }

    @Test
    public void validLimitsByRatio21() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio21();
        List<String> errors = runValidator(info, getOutdoorLimits());
        assertThat(errors, empty());
    }

    @Test
    public void invalidMinDurationByRatio21() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio21();
        setDuration(info, 4.8);
        assertThat(runValidator(info, getOutdoorLimits()),
                contains("Incompatible duration for 2:1 video ratio. Supported durations: 5"));
    }

    @Test
    public void invalidMaxDurationByRatio21() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio21();
        setDuration(info, 5.2);
        assertThat(runValidator(info, getOutdoorLimits()),
                contains("Incompatible duration for 2:1 video ratio. Supported durations: 5"));
    }

    @Test
    public void invalidMinWidthByRatio21() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio21();
        setWidth(info, 1438);
        setHeight(info, 719);
        assertThat(runValidator(info, getOutdoorLimits()), contains("Incompatible width for 2:1 video ratio. " +
                "Supported width from 1440px"));
    }

    @Test
    public void validLimitsByRatio31() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio31();
        assertThat(runValidator(info, getOutdoorLimits()), empty());
    }

    @Test
    public void invalidMinDurationByRatio31() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio31();
        setDuration(info, 7.3);
        assertThat(runValidator(info, getOutdoorLimits()),
                contains("Incompatible duration for 3:1 video ratio. Supported durations: 5, 7.5, 10"));
    }

    @Test
    public void invalidMaxDurationByRatio31() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio31();
        setDuration(info, 7.7);
        assertThat(runValidator(info, getOutdoorLimits()),
                contains("Incompatible duration for 3:1 video ratio. Supported durations: 5, 7.5, 10"));
    }

    @Test
    public void validFirstDurationByRatio31() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio31();
        setDuration(info, 5.0);
        assertThat(runValidator(info, getOutdoorLimits()), empty());
    }

    @Test
    public void validSecondDurationByRatio31() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio31();
        setDuration(info, 7.5);
        assertThat(runValidator(info, getOutdoorLimits()), empty());
    }

    @Test
    public void validThirdDurationByRatio31() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio31();
        setDuration(info, 10.0);
        assertThat(runValidator(info, getOutdoorLimits()), empty());
    }

    @Test
    public void validFirstDurationByRatio2318() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio2318();
        setDuration(info, 15.0);
        assertThat(runValidator(info, getOutdoorLimits()), empty());
    }

    @Test
    public void validFirstDurationByRatio7855() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio7855();
        setDuration(info, 15.0);
        assertThat(runValidator(info, getOutdoorLimits()), empty());
    }

    @Test
    public void validFirstDurationByRatio9425() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio9425();
        setDuration(info, 15.0);
        assertThat(runValidator(info, getOutdoorLimits()), empty());
    }

    @Test
    public void validFirstDurationByRatio103() {
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio103();
        setDuration(info, 15.0);
        assertThat(runValidator(info, getOutdoorLimits()), empty());
    }

    @Test
    public void checkInterpolation() {
        Locale.setDefault(new Locale("en", "US"));
        StillageFileInfo info = makeOutdoorStillageFileInfoWithAspectRatio21();
        VideoLimits limits = getOutdoorLimits();
        limits.setDurationLimitsByRatio(ImmutableMap.of("2:1",
                ImmutableMap.of("durations", ImmutableList.of(3.0, 9.2495))));

        assertThat("Error message interpolated ok",
                runValidator(info, limits),
                contains("Incompatible duration for 2:1 video ratio. Supported durations: 3, 9.25"));
    }

    @Test
    public void checkRangeRatio11() {
        preset.getDescription().setGeometry(Geometry.UNIVERSAL);
        try {
            features = Set.of(CANVAS_RANGE_RATIO_FEATURE);
            StillageFileInfo fileInfo = makeStillageFileInfo();
            setWidth(fileInfo, 720);
            setHeight(fileInfo, 720);
            assertThat(runValidator(fileInfo), empty());
        } finally {
            preset.getDescription().setGeometry(Geometry.WIDE);
        }
    }

    @Test
    public void checkRangeRatio11_noFeature() {
        preset.getDescription().setGeometry(Geometry.UNIVERSAL);
        try {
            features = Set.of();
            StillageFileInfo fileInfo = makeStillageFileInfo();
            setWidth(fileInfo, 720);
            setHeight(fileInfo, 720);
            assertThat(runValidator(fileInfo), contains("incompatible_video_ratio_list"));
        } finally {
            preset.getDescription().setGeometry(Geometry.WIDE);
        }
    }

    @Test
    public void checkRangeRatio31() {
        features = Set.of("canvas_range_ratio");
        StillageFileInfo fileInfo = makeStillageFileInfo();
        setWidth(fileInfo, 1248);
        setHeight(fileInfo, 416);
        assertThat(runValidator(fileInfo), empty());
    }

    @Test
    public void checkRangeRatio13() {
        features = Set.of(CANVAS_RANGE_RATIO_FEATURE);
        StillageFileInfo fileInfo = makeStillageFileInfo();
        setWidth(fileInfo, 700);
        setHeight(fileInfo, 2100);
        List<String> errors = runValidator(fileInfo);
        assertThat(errors, contains("incorrect_video_ratio_vertical"));
        // translation
    }

    @Test
    public void checkRangeRatio41() {
        features = Set.of(CANVAS_RANGE_RATIO_FEATURE);
        StillageFileInfo fileInfo = makeStillageFileInfo();
        setWidth(fileInfo, 1664);
        setHeight(fileInfo, 416);
        List<String> errors = runValidator(fileInfo);
        assertThat(errors, contains("Incompatible video ratio 4:1. Use another template."));
    }

    @Test
    public void checkClientConvertingFilesCount() {
        //сейчас конвертируется 40 файлов, загрузить ещё один нельзя
        features = Set.of();
        clientConvertingFilesCount = VIDEO_FILE_RATE_LIMIT;
        StillageFileInfo fileInfo = makeStillageFileInfo();
        List<String> errors = runValidator(fileInfo);
        assertThat(errors, hasItem(containsString("The limit for the number of converting videos exceeded")));
    }

    @Test
    public void checkSkipClientConvertingFilesCount() {
        //сейчас конвертируется 40 файлов, есть фича skip_video_file_rate_limit, можно загрузить ещё
        features = Set.of(SKIP_VIDEO_FILE_RATE_LIMIT.getName());
        clientConvertingFilesCount = VIDEO_FILE_RATE_LIMIT;
        StillageFileInfo fileInfo = makeStillageFileInfo();
        List<String> errors = runValidator(fileInfo);
        assertThat(errors, empty());
    }
}
