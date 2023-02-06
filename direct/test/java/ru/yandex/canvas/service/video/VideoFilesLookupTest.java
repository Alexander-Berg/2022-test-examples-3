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
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.config.VideoFilesServiceConfig;
import ru.yandex.canvas.model.stillage.StillageFileInfo;
import ru.yandex.canvas.model.video.VideoFiles;
import ru.yandex.canvas.model.video.files.FileType;
import ru.yandex.canvas.model.video.files.Movie;
import ru.yandex.canvas.model.video.files.MovieAndVideoSourceFactory;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthRequestParams;
import ru.yandex.canvas.service.DateTimeService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.video.files.StockMoviesService;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@Import({ControllerTestConfiguration.class, VideoFilesServiceConfig.class})
public class VideoFilesLookupTest {
    public static final long COMMON_MOVIE_PRESET_ID = 1L;

    @Autowired
    private VideoFilesRepository videoFilesRepository;

    @Autowired
    private MovieService commonMovieService;

    @Autowired
    private SessionParams sessionParams;

    @TestConfiguration
    public static class TestConf {
        @MockBean
        private VhService vhClient;

        @MockBean
        private VideoFileUploadServiceInterface fileUploadService;

        @Bean
        public VideoFilesRepository videoFilesRepository(MongoOperations mongoOperations) {
            return new VideoFilesRepository(mongoOperations, new DateTimeService());
        }

        @MockBean
        private DirectService directService;

        @MockBean
        private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @MockBean
        private VideoGeometryService videoGeometryService;

        @Bean
        public VideoPresetsService videoPresetsService(VideoLimitsService videoLimitsService,
                                                       AuthRequestParams authRequestParams) {
            return new VideoPresetsService(videoLimitsService, directService, authRequestParams);
        }

        @Bean
        public VideoLimitsService videoLimitsService(AuthRequestParams authRequestParams,
                                                     DirectService directService) {
            return new VideoLimitsService(authRequestParams, directService);
        }

        @Bean
        public MovieService commonMovieService(VideoFilesRepository videoFilesRepository,
                                               StockMoviesService stockMoviesService,
                                               VideoFileUploadServiceInterface videoFileUploadService,
                                               StillageService stillageService, VideoLimitsService videoLimitsService,
                                               VideoPresetsService videoPresetsService,
                                               MovieAndVideoSourceFactory movieAndVideoSourceFactory,
                                               VideoGeometryService videoGeometryService) {
            return new MovieService(videoFilesRepository, stockMoviesService, videoFileUploadService,
                    stillageService, videoLimitsService, new DateTimeService(), directService, videoPresetsService,
                    cmsConversionStatusUpdateService, movieAndVideoSourceFactory, videoGeometryService);
        }
    }

    @Before
    public void setMocks() {
        Mockito.when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPC);
    }

    private void setStockFileInDb(String stockId, String id) {
        StillageFileInfo fileInfo = new StillageFileInfo();
        fileInfo.setUrl("http://test.stillage.ru/");

        VideoFiles videoFiles = new VideoFiles();
        videoFiles.setId(id)
                .setClientId(103997791L)
                .setName("Video 581")
                .setStockFileId(stockId)
                .setType(FileType.VIDEO)
                .setUrl("https://storage.mds.yandex.net/get-bstor/38293/648b4c6c-1d1f-41fd-88e6-6ebf0c18a194.qt")
                .setArchive(false)
                .setFormats(emptyList())
                .setThumbnail(new VideoFiles.VideoThumbnail()
                        .setUrl("http://thmb.ru")
                        .setPreview(new VideoFiles.VideoThumbnail.ThumbnailPreview())
                )
                .setStillageFileInfo(fileInfo)
                .setDate(new Date());

        Mockito.when(videoFilesRepository.findByIdAndQuery(eq(id), any())).thenReturn(videoFiles);

//        Mockito.when(commonMovieService.getFileById(id, FileType.VIDEO, 103997791L));
    }

    private void setStockAudioFileInDb(String stockId, String id) {
        VideoFiles videoFiles = new VideoFiles();
        videoFiles.setId(id)
                .setClientId(103997791L)
                .setName("Audio 112")
                .setStockFileId(stockId)
                .setType(FileType.AUDIO)
                .setArchive(false)
                .setDate(new Date());

        Mockito.when(videoFilesRepository.findByIdAndQuery(eq(id), any())).thenReturn(videoFiles);
    }

    @Test
    public void nullAudioAndCustomVideoTest() {
        setStockFileInDb(null, "xxxx1232");
        Movie movie = commonMovieService.lookupMovie("xxxx1232", null, 103997791L, COMMON_MOVIE_PRESET_ID);

        assertThat(movie.isStock(), Matchers.is(false));
        assertThat(movie.getVideoSource(), allOf(notNullValue(),
                hasProperty("id", is("xxxx1232"))));

        assertThat(movie.getAudioSource(), allOf(nullValue()));
    }

    @Test
    public void nullAudioAndStockDbVideoTest() {
        setStockFileInDb("new2_25_25-5.mov", "xxxx1232");
        Movie movie = commonMovieService.lookupMovie("xxxx1232", null, 103997791L, COMMON_MOVIE_PRESET_ID);

        assertThat(movie.isStock(), Matchers.is(true));
        assertThat(movie.getVideoSource(), allOf(notNullValue(),
                hasProperty("stockId", is("new2_25_25-5.mov")),
                hasProperty("id", is("xxxx1232"))));

        assertThat(movie.getAudioSource(), allOf(notNullValue(),
                hasProperty("id", nullValue())));
    }

    @Test
    public void nullAudioAndStockVideoTest() {
        Mockito.when(commonMovieService.getFileByIdForCreativeType("new2_25_25-5.mov", FileType.VIDEO, 103997791L,
                VideoCreativeType.TEXT, null))
                .thenReturn(null);

        Movie movie = commonMovieService.lookupMovie("new2_25_25-5.mov", null, 103997791L, COMMON_MOVIE_PRESET_ID);

        assertThat(movie.isStock(), Matchers.is(true));
        assertThat(movie.getVideoSource(), allOf(notNullValue(),
                hasProperty("id", is("new2_25_25-5.mov"))));

        assertThat(movie.getAudioSource(), allOf(notNullValue(),
                hasProperty("id", nullValue())));
    }

    @Test
    public void stockAudioAndStockVideoTest() {
        Mockito.when(commonMovieService.getFileByIdForCreativeType("new2_25_25-5.mov", FileType.VIDEO, 103997791L,
                VideoCreativeType.TEXT, null))
                .thenReturn(null);

        Movie movie = commonMovieService.lookupMovie("new2_25_25-5.mov", "3891307", 103997791L, COMMON_MOVIE_PRESET_ID);

        assertThat(movie.isStock(), Matchers.is(true));
        assertThat(movie.getVideoSource(), allOf(notNullValue(),
                hasProperty("id", is("new2_25_25-5.mov"))));

        assertThat(movie.getAudioSource(), allOf(notNullValue(),
                hasProperty("id", is("3891307"))));
    }

    @Test
    public void stockAudioAndStockDbVideoTest() {
        setStockFileInDb("new2_25_25-5.mov", "xxxx1232");
        Movie movie = commonMovieService.lookupMovie("xxxx1232", "3891307", 103997791L, COMMON_MOVIE_PRESET_ID);

        assertThat(movie.isStock(), Matchers.is(true));
        assertThat(movie.getVideoSource(), allOf(notNullValue(),
                hasProperty("id", is("xxxx1232")),
                hasProperty("stockId", is("new2_25_25-5.mov"))));

        assertThat(movie.getAudioSource(), allOf(notNullValue(),
                hasProperty("id", is("3891307")),
                hasProperty("stockId", is("3891307"))));
    }

    @Test
    public void stockAudioAndCustomVideoTest() {
        setStockFileInDb(null, "xxxx1232");
        Exception thrown = null;

        try {
            commonMovieService.lookupMovie("xxxx1232", "3891307", 103997791L, COMMON_MOVIE_PRESET_ID);
        } catch (Exception e) {
            thrown = e;
        }

        assertThat(thrown, notNullValue());
        assertThat(thrown, Matchers.instanceOf(IllegalArgumentException.class));
    }

    @Test
    public void dbStockAudioAndStockVideoTest() {
        setStockAudioFileInDb("3891307", "xxxx1232");
        Movie movie = commonMovieService.lookupMovie("new2_25_25-5.mov", "xxxx1232", 103997791L,
                COMMON_MOVIE_PRESET_ID);

        assertThat(movie.isStock(), Matchers.is(true));
        assertThat(movie.getVideoSource(), allOf(notNullValue(),
                hasProperty("id", is("new2_25_25-5.mov")),
                hasProperty("stockId", is("new2_25_25-5.mov"))));

        assertThat(movie.getAudioSource(), allOf(notNullValue(),
                hasProperty("stockId", is("3891307")),
                hasProperty("id", is("xxxx1232"))));
    }

    @Test
    public void dbStockAudioAndDbStockVideoTest() {
        setStockAudioFileInDb("3891307", "audioId");
        setStockFileInDb("new2_25_25-5.mov", "videoId");

        Movie movie = commonMovieService.lookupMovie("videoId", "audioId", 103997791L, COMMON_MOVIE_PRESET_ID);

        assertThat(movie.isStock(), Matchers.is(true));
        assertThat(movie.getVideoSource(), allOf(notNullValue(),
                hasProperty("id", is("videoId")),
                hasProperty("stockId", is("new2_25_25-5.mov"))
        ));

        assertThat(movie.getAudioSource(), allOf(notNullValue(),
                hasProperty("id", is("audioId")),
                hasProperty("stockId", is("3891307"))));
    }

    @Test
    public void unknownAudioAndCustomVideoTest() {
        setStockFileInDb(null, "xxxx1232");
        Exception thrown = null;

        try {
            commonMovieService.lookupMovie("xxxx1232", "unknownAudio", 103997791L, COMMON_MOVIE_PRESET_ID);
        } catch (Exception e) {
            thrown = e;
        }

        assertThat(thrown, notNullValue());
        assertThat(thrown, Matchers.instanceOf(IllegalArgumentException.class));
    }

    @Test
    public void unknownAudioAndStockVideoTest() {
        Movie movie = commonMovieService.lookupMovie("new2_25_25-5.mov", "unknownAudio", 103997791L,
                COMMON_MOVIE_PRESET_ID);
        assertThat(movie, nullValue());
    }

    @Test
    public void stockAudioAndUnknownVideoTest() {
        Movie movie = commonMovieService.lookupMovie("unknownVideo", "3891307", 103997791L, COMMON_MOVIE_PRESET_ID);
        assertThat(movie, nullValue());
    }

}
