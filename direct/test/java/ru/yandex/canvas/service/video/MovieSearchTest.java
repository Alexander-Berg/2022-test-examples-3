package ru.yandex.canvas.service.video;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.config.VideoFilesServiceConfig;
import ru.yandex.canvas.model.video.files.FileType;
import ru.yandex.canvas.model.video.files.MovieAndVideoSourceFactory;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthRequestParams;
import ru.yandex.canvas.service.DateTimeService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.video.files.StockMoviesService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@Import({ControllerTestConfiguration.class, VideoFilesServiceConfig.class})
public class MovieSearchTest {

    @Autowired
    MovieService cpmMovieService;

    @Autowired
    VideoFilesRepository videoFilesRepository;

    @Autowired
    SessionParams sessionParams;

    @Autowired
    VideoGeometryService videoGeometryService;

    @TestConfiguration
    public static class TestConf {
        @MockBean
        private VhService vhClient;

        @MockBean
        private DirectService directService;

        @MockBean
        private VideoPresetsService videoPresetsService;

        @MockBean
        private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @Bean
        public VideoGeometryService videoGeometryService(VideoLimitsService limits) {
            return new VideoGeometryService(limits);
        }

        @Bean
        public VideoLimitsService videoLimitsService(AuthRequestParams authRequestParams,
                                                     DirectService directService) {
            return new VideoLimitsService(authRequestParams, directService);
        }

        @MockBean
        private VideoFileUploadServiceInterface videoFileUploadServiceInterface;

        @Bean
        public MovieService movieService(VideoFilesRepository videoFilesRepository,
                                         StockMoviesService stockMoviesService, VideoFileUploadServiceInterface fileUploadService,
                                         StillageService stillageService, VideoLimitsService videoLimitsService,
                                         MovieAndVideoSourceFactory movieAndVideoSourceFactory,
                                         VideoGeometryService videoGeometryService) {
            return new MovieService(videoFilesRepository, stockMoviesService, fileUploadService,
                    stillageService, videoLimitsService, new DateTimeService(), directService, videoPresetsService,
                    cmsConversionStatusUpdateService, movieAndVideoSourceFactory, videoGeometryService);
        }

    }

    @Test
    public void cpmTest() {
        ArgumentCaptor<VideoFilesRepository.QueryBuilder> argument = ArgumentCaptor.forClass(
                VideoFilesRepository.QueryBuilder.class);

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPM);
        when(sessionParams.isPresent()).thenReturn(true);

        when(videoFilesRepository.findByIdAndQuery(eq("1234"), argument.capture()))
                .thenReturn(null);

        cpmMovieService.getFileByIdForCreativeType("1234", FileType.VIDEO, 12344L, VideoCreativeType.CPM, null);

        Query validQuery = new Query();
        validQuery
                .addCriteria(Criteria.where("type").is(FileType.VIDEO))
                .addCriteria(Criteria.where("client_id").is(12344L))
                .addCriteria(new Criteria().andOperator(

                        new Criteria().orOperator(
                                new Criteria().andOperator(Criteria.where("duration").is(null)),
                                new Criteria().andOperator(Criteria.where("duration").gte(5.0).lte(60.0))
                        ),

                        Criteria.where("ratio").in(null, "16:9")
                ));


        assertThat(argument.getValue().build().toString(), equalTo(validQuery.toString()));
    }

    @Test
    public void cpcTest() {
        ArgumentCaptor<VideoFilesRepository.QueryBuilder> argument = ArgumentCaptor.forClass(
                VideoFilesRepository.QueryBuilder.class);

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPC);
        when(sessionParams.isPresent()).thenReturn(true);

        when(videoFilesRepository.findByIdAndQuery(eq("1234"), argument.capture()))
                .thenReturn(null);

        cpmMovieService.getFileByIdForCreativeType("1234", FileType.VIDEO, 12344L, VideoCreativeType.CPC, null);

        Query validQuery = new Query();
        validQuery
                .addCriteria(Criteria.where("type").is(FileType.VIDEO))
                .addCriteria(Criteria.where("client_id").is(12344L))
                .addCriteria(new Criteria().andOperator(

                        new Criteria().orOperator(
                                new Criteria().andOperator(Criteria.where("duration").is(null)),
                                new Criteria().andOperator(Criteria.where("duration").gte(5.0).lte(60.0))
                        ),

                        Criteria.where("ratio").in(null, "16:9")
                ));


        assertThat(argument.getValue().build().toString(), equalTo(validQuery.toString()));
    }

    @Test
    public void outdoorTest() {
        ArgumentCaptor<VideoFilesRepository.QueryBuilder> argument = ArgumentCaptor.forClass(
                VideoFilesRepository.QueryBuilder.class);

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPM_OUTDOOR);
        when(sessionParams.isPresent()).thenReturn(true);

        when(videoFilesRepository.findByIdAndQuery(eq("1234"), argument.capture()))
                .thenReturn(null);

        cpmMovieService.getFileByIdForCreativeType("1234", FileType.VIDEO, 12344L, VideoCreativeType.CPM_OUTDOOR, null);

        Query validQuery = new Query();
        validQuery
                .addCriteria(Criteria.where("type").is(FileType.VIDEO))
                .addCriteria(Criteria.where("client_id").is(12344L))
                .addCriteria(new Criteria().andOperator(

                        new Criteria().orOperator(
                                new Criteria().andOperator(Criteria.where("duration").is(null)),
                                new Criteria().andOperator(Criteria.where("duration").gte(5.0).lte(15.0))
                        ),

                        Criteria.where("ratio").in("2:1", "3:1", "4:3", "23:18", "78:55", "94:25", "10:3")
                        )

                );


        assertThat(argument.getValue().build().toString(), equalTo(validQuery.toString()));
    }


}
