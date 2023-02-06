package ru.yandex.canvas.service.video;

import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import ru.yandex.canvas.controllers.video.wrappers.VideoFileWrapper;
import ru.yandex.canvas.model.video.VideoFiles;
import ru.yandex.canvas.model.video.files.FileType;
import ru.yandex.canvas.model.video.files.Movie;
import ru.yandex.canvas.model.video.files.MovieAndVideoSourceFactory;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.DateTimeService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.video.files.StockMoviesService;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@Import({ControllerTestConfiguration.class, VideoFilesServiceConfig.class})
public class FileSearchTestTest {

    @Autowired
    MovieService movieService;

    @Autowired
    VideoFilesRepository videoFilesRepository;

    @Autowired
    SessionParams sessionParams;

    @TestConfiguration
    public static class TestConf {
        @MockBean
        private VhService vhClient;

        @MockBean
        private DirectService directService;

        @MockBean
        private VideoPresetsService videoPresetsService;

        @MockBean
        private VideoLimitsService videoLimitsService;

        @MockBean
        private VideoFileUploadServiceInterface videoFileUploadServiceInterface;

        @MockBean
        private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @MockBean
        private VideoGeometryService videoGeometryService;

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

    /*
    {
        "_id" : ObjectId("5c545cbd3bbdd16d0335455d"),
        "client_id" : 103997791,
        "name" : "coffe",
        "stock_file_id" : "old_97",
        "type" : "video",
        "date" : ISODate("2019-02-01T17:50:37.809Z"),
        "archive" : false
}
     */

    @Test
    //TODO
    public void makeFromDbTest() throws JsonProcessingException {

        for (String id : Arrays.asList("old_111", "old_62", "old_97", "old_54", "old_98")) {
            VideoFiles record = new VideoFiles()
                    .setId("5c545cbd3bbdd16d0335455d")
                    .setArchive(false)
                    .setName("coffee")
                    .setDate(Date.from(Instant.now()))
                    .setType(FileType.VIDEO)
                    .setStockFileId(id)
                    .setClientId(103997791L);

            Movie movie = movieService.makeFromDb(record);
            VideoFileWrapper wrapper = new VideoFileWrapper(movie);
            String w = new ObjectMapper().writeValueAsString(wrapper);
            System.err.println(w);
        }
    }


}
