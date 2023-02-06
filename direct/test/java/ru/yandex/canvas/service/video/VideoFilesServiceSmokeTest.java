package ru.yandex.canvas.service.video;

import java.util.Date;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.config.VideoFilesServiceConfig;
import ru.yandex.canvas.model.video.VideoFiles;
import ru.yandex.canvas.model.video.files.AudioSource;
import ru.yandex.canvas.model.video.files.FileType;
import ru.yandex.canvas.model.video.files.Movie;
import ru.yandex.canvas.model.video.files.MovieAndVideoSourceFactory;
import ru.yandex.canvas.model.video.files.VideoSource;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthRequestParams;
import ru.yandex.canvas.service.DateTimeService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.video.files.StockMoviesService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@Import({ControllerTestConfiguration.class, VideoFilesServiceConfig.class})
public class VideoFilesServiceSmokeTest {
    public static final long COMMON_MOVIE_PRESET_ID = 1L;

    @Autowired
    private VideoFilesRepository videoFilesRepository;

    @Autowired
    private MovieServiceInterface commonMovieService;

    @Autowired
    private SessionParams sessionParams;

    @TestConfiguration
    public static class TestConf {
        @MockBean
        private VhService vhClient;

        @MockBean
        private VideoFileUploadServiceInterface fileUploadService;

        @MockBean
        private DirectService directService;

        @MockBean
        private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @MockBean
        private VideoGeometryService videoGeometryService;

        @Bean
        public VideoPresetsService videoPresetsService(VideoLimitsService videoLimitsService, AuthRequestParams authRequestParams) {
            return new VideoPresetsService(videoLimitsService, directService, authRequestParams);
        }

        @Bean
        public VideoLimitsService videoLimitsService(AuthRequestParams authRequestParams,
                                                     DirectService directService) {
            return new VideoLimitsService(authRequestParams, directService);
        }

        @Bean
        public MovieServiceInterface commonMovieService(VideoFilesRepository videoFilesRepository,
                                                        StockMoviesService stockMoviesService,
                                                        VideoFileUploadServiceInterface fileUploadService,
                                                        StillageService stillageService,
                                                        VideoLimitsService videoLimitsService,
                                                        CmsConversionStatusUpdateService cmsConversionStatusUpdateService,
                                                        VideoPresetsService videoPresetsService,
                                                        MovieAndVideoSourceFactory movieAndVideoSourceFactory,
                                                        VideoGeometryService videoGeometryService) {
            return new MovieService(videoFilesRepository, stockMoviesService, fileUploadService,
                    stillageService, videoLimitsService, new DateTimeService(), directService, videoPresetsService,
                    cmsConversionStatusUpdateService, movieAndVideoSourceFactory, videoGeometryService);
        }
    }


    @Before
    public void setMocks() {
        Mockito.when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPC);
    }

    @Test
    public void checkAudioStockFile() {

        Movie movie = commonMovieService.lookupMovie("old_112", "3172374", null, COMMON_MOVIE_PRESET_ID);

        assertThat("Stream found", movie, notNullValue());

        AudioSource audioSource = movie.getAudioSource();

        assertThat("File is ok", audioSource, allOf(
                Matchers.hasProperty("name", equalTo("Pachelbel Canon in D")),
                Matchers.hasProperty("sourceType", equalTo(FileType.AUDIO)),
                Matchers.hasProperty("stillageUrl",
                        equalTo("https://storage.mds.yandex.net/get-bstor/15932/7f4eed63-0970-47fb-bd72-2b5b3f20c68a"
                                + ".wav")),
                Matchers.hasProperty("id", Matchers.equalTo("3172374"))
        ));

    }

    @Test
    public void checkAudioStockFileWithNullId() {
        Movie movie = commonMovieService.lookupMovie("old_112", null, null, COMMON_MOVIE_PRESET_ID);

        assertThat("Stream found", movie, notNullValue());
        AudioSource audioSource = movie.getAudioSource();

        assertThat("File is ok", audioSource, allOf(
                Matchers.hasProperty("name", equalTo("Silence")),
                Matchers.hasProperty("sourceType", equalTo(FileType.AUDIO)),
                Matchers.hasProperty("stillageUrl",
                        equalTo("https://storage.mds.yandex.net/get-bstor/15200/fc8c714a-6f1f-497d-855d-d38ec1a52699"
                                + ".wav")),
                Matchers.hasProperty("id", Matchers.nullValue())
        ));

    }

    @Test
    public void checkVideoStockFile() {
        Movie movie = commonMovieService.lookupMovie("old_112", "3172374", null, COMMON_MOVIE_PRESET_ID);

        assertThat("Stream found", movie, notNullValue());
        VideoSource videoSource = movie.getVideoSource();

        assertThat("File is ok", videoSource, allOf(
                Matchers.hasProperty("name", equalTo("Video 1")),
                Matchers.hasProperty("sourceType", equalTo(FileType.VIDEO)),
                Matchers.hasProperty("overlayColor", Matchers.equalTo("#002BFF")),
                Matchers.hasProperty("categoryId", Matchers.equalTo("200063726")),
                Matchers.hasProperty("stillageUrl", Matchers.equalTo(
                        "https://storage.mds.yandex.net/get-bstor/21287/3365aa38-6177-4a36-a21f-99b9d64f88e2.mp4")),
                Matchers.hasProperty("thumbnail", Matchers.allOf(
                        Matchers.hasProperty("height", equalTo(720L)),
                        Matchers.hasProperty("width", equalTo(1280L)),
                        Matchers.hasProperty("url",
                                equalTo("https://avatars.mds.yandex"
                                        + ".net/get-canvas/145764/2a00000163a77795cf0d42c4723de619af10/orig")),
                        Matchers.hasProperty("preview", allOf(
                                Matchers.hasProperty("height", equalTo(479L)),
                                Matchers.hasProperty("width", equalTo(852L)),
                                Matchers.hasProperty("url", equalTo("https://avatars.mds.yandex"
                                        + ".net/get-canvas/145764/2a00000163a77795cf0d42c4723de619af10/preview480p")))
                        ))
                ),
//TODO                Matchers.hasProperty("archive", Matchers.equalTo(false)),
                Matchers.hasProperty("id", Matchers.equalTo("old_112"))
        ));

    }

    @Test
    public void videoStreamFromDatabaseTest() {
        VideoFiles videoFiles = new VideoFiles();
        videoFiles.setId("5c17c1e74144050acc46be03")
                .setClientId(103997791L)
                .setName("Video 581")
                .setStockFileId("new2_12_12-2.mov")
                .setType(FileType.VIDEO)
                .setUrl("https://storage.mds.yandex.net/get-bstor/38293/648b4c6c-1d1f-41fd-88e6-6ebf0c18a194.qt")
                .setArchive(false)
                .setDate(new Date());

        Mockito.when(videoFilesRepository.findByIdAndQuery(eq("5c17c1e74144050acc46be03"), any()))
                .thenReturn(videoFiles);

        Movie movie = commonMovieService.lookupMovie("5c17c1e74144050acc46be03", null, 103997791L,
                COMMON_MOVIE_PRESET_ID);

        assertThat("File is ok", movie, allOf(
                hasProperty("strmPrefix", equalTo("video_59f2e3110d98b3da35c2fbdb")),
                hasProperty("videoSource", allOf(
                        notNullValue(),
                        hasProperty("stillageUrl", is("https://storage.mds.yandex"
                                + ".net/get-bstor/38293/648b4c6c-1d1f-41fd-88e6-6ebf0c18a194.qt")),
                        hasProperty("id", is("5c17c1e74144050acc46be03")),
                        hasProperty("stockId", is("new2_12_12-2.mov"))

                )),
                hasProperty("audioSource", allOf(
                        notNullValue(),
                        hasProperty("id", nullValue()),
                        hasProperty("stillageUrl",
                                is("https://storage.mds.yandex.net/get-bstor/15200/fc8c714a-6f1f-497d-855d"
                                        + "-d38ec1a52699.wav"))
                )),
                hasProperty("duration", equalTo(15.0)),
                hasProperty("formats", hasSize(19))
        ));

    }


}
