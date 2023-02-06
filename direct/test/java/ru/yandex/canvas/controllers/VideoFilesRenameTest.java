package ru.yandex.canvas.controllers;

import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.controllers.video.VideoFilesController;
import ru.yandex.canvas.model.stillage.StillageInfoConverter;
import ru.yandex.canvas.model.video.VideoFiles;
import ru.yandex.canvas.model.video.files.FileType;
import ru.yandex.canvas.model.video.files.MovieAndVideoSourceFactory;
import ru.yandex.canvas.repository.video.AudioFilesRepository;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthService;
import ru.yandex.canvas.service.DateTimeService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.PackshotService;
import ru.yandex.canvas.service.SandBoxService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.video.AudioService;
import ru.yandex.canvas.service.video.AudioUploadService;
import ru.yandex.canvas.service.video.CmsConversionStatusUpdateService;
import ru.yandex.canvas.service.video.InBannerVideoFilesService;
import ru.yandex.canvas.service.video.MovieService;
import ru.yandex.canvas.service.video.MovieServiceInterface;
import ru.yandex.canvas.service.video.VhService;
import ru.yandex.canvas.service.video.VideoFileUploadServiceInterface;
import ru.yandex.canvas.service.video.VideoGeometryService;
import ru.yandex.canvas.service.video.VideoLimitsService;
import ru.yandex.canvas.service.video.VideoPresetsService;
import ru.yandex.canvas.service.video.VideoSoundTrackService;
import ru.yandex.canvas.service.video.files.StockMoviesService;
import ru.yandex.canvas.steps.ResourceHelpers;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(VideoFilesController.class)
public class VideoFilesRenameTest {
    public static final String RENAME_STOCK_VIDEO_EXPECTED_JSON =
            "/ru/yandex/canvas/controllers/videoFilesRenameTest/renameStockVideoExpected.json";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoOperations mongoOperations;

    @Autowired
    private DateTimeService dateTimeService;

    @Configuration
    public static class TestConf {
        @MockBean
        private VhService vhClient;

        @MockBean
        DirectService directService;

        @MockBean
        private VideoPresetsService videoPresetsService;

        @MockBean
        SandBoxService sandBoxService;

        @MockBean
        VideoLimitsService videoLimitsService;

        @MockBean
        PackshotService packshotService;

        @MockBean
        VideoFileUploadServiceInterface videoFileUploadServiceInterface;

        @MockBean
        MongoOperations mongoOperations;

        @MockBean
        AuthService authService;

        @MockBean
        DateTimeService dateTimeService;

        @MockBean
        AudioUploadService audioUploadService;

        @MockBean
        AudioFilesRepository audioFilesRepository;

        @MockBean
        InBannerVideoFilesService inBannerVideoFilesService;

        @MockBean
        private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @MockBean
        private VideoGeometryService videoGeometryService;

        @Bean(name = "main")
        @Primary
        public VideoFilesRepository videoFilesRepository(MongoOperations mongoOperations,
                                                         DateTimeService dateTimeService) {
            return new VideoFilesRepository(mongoOperations, dateTimeService);
        }

        @Bean
        public MovieAndVideoSourceFactory movieAndVideoSourceFactory() {
            return new MovieAndVideoSourceFactory(new StillageInfoConverter(new ObjectMapper()));
        }

        @Bean
        StockMoviesService stockMoviesService(MovieAndVideoSourceFactory movieAndVideoSourceFactory) {
            return new StockMoviesService(null, movieAndVideoSourceFactory);
        }

        @Bean
        public AudioService audioService(AudioFilesRepository audioFilesRepository, StillageService stillageService,
                                         AudioUploadService audioUploadService, SandBoxService sandBoxService,
                                         VideoLimitsService videoLimitsService) {
            return new AudioService(audioFilesRepository, stillageService, audioUploadService,
                    sandBoxService, videoLimitsService,"", "");
        }

        @Bean
        public VideoSoundTrackService videoSoundTrackService(StockMoviesService stockMoviesService,
                                                             SessionParams sessionParams,
                                                             VideoFilesRepository videoFilesRepository) {
            return new VideoSoundTrackService(stockMoviesService, sessionParams, videoFilesRepository);
        }

        @Bean
        public VideoFilesController videoFilesController(StockMoviesService stockMoviesService,
                                                         AuthService authService,
                                                         @Qualifier("main") VideoFilesRepository videoFilesRepository,
                                                         MovieServiceInterface movieService,
                                                         PackshotService packshotService,
                                                         AudioService audioService,
                                                         VideoSoundTrackService videoSoundTrackService,
                                                         SessionParams sessionParams,
                                                         InBannerVideoFilesService inBannerVideoFilesService) {
            return new VideoFilesController(stockMoviesService, authService, videoFilesRepository,
                    movieService, packshotService, audioService, videoSoundTrackService, sessionParams,
                    inBannerVideoFilesService, directService);
        }

        @MockBean
        SessionParams sessionParams;

        @MockBean
        StillageService stillageService;

        @Bean
        public MovieServiceInterface movieService(VideoFilesRepository videoFilesRepository,
                                                  StockMoviesService stockMoviesService,
                                                  VideoFileUploadServiceInterface fileUploadService,
                                                  StillageService stillageService,
                                                  VideoLimitsService videoLimitsService,
                                                  MovieAndVideoSourceFactory movieAndVideoSourceFactory,
                                                  DateTimeService dateTimeService) {
            return new MovieService(videoFilesRepository, stockMoviesService, fileUploadService,
                    stillageService, videoLimitsService, dateTimeService, directService, videoPresetsService,
                    cmsConversionStatusUpdateService, movieAndVideoSourceFactory, videoGeometryService);
        }
    }

    public UpdateResult makeUpdateResult(String upsertedId) {
        return new UpdateResult() {
            @Override
            public boolean wasAcknowledged() {
                return true;
            }

            @Override
            public long getMatchedCount() {
                return 0;
            }

            @Override
            public boolean isModifiedCountAvailable() {
                return true;
            }

            @Override
            public long getModifiedCount() {
                return 1;
            }

            @Override
            public BsonValue getUpsertedId() {
                return null;
            }
        };
    }

    @Before
    public void setTime() {
        when(dateTimeService.getCurrentDate()).thenReturn(new Date());
    }

    @Test
    public void renameStockVideoTest() throws Exception {

        String expected = ResourceHelpers.getResource(RENAME_STOCK_VIDEO_EXPECTED_JSON);

        ArgumentCaptor<Query> updateQueryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Query> findCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        Query validQuery = new Query();
        validQuery
                .addCriteria(Criteria.where("client_id").is(12344L))
                .addCriteria(Criteria.where("archive").is(false))
                .addCriteria(Criteria.where("_id").is("5c7d3a7da9e08a5562d00073"));

        Update validUpdate = new Update();
        validUpdate.set("name", "Lion King 2019");

        UpdateResult updateResult = makeUpdateResult("5c7d3a7da9e08a5562d00073");

        when(mongoOperations.updateFirst(updateQueryCaptor.capture(), updateCaptor.capture(), eq(VideoFiles.class)))
                .thenReturn(updateResult);

        VideoFiles mark = new VideoFiles();
        mark.setId("5c7d3a7da9e08a5562d00073")
                .setClientId(12344L)
                .setName("Lion King 2019")
                .setArchive(false)
                .setStockFileId("new_0_0-077.mov")
                .setType(FileType.VIDEO);

        Query validFind = new Query();
        validFind
                .addCriteria(Criteria.where("_id").is("5c7d3a7da9e08a5562d00073"))
                .addCriteria(Criteria.where("client_id").is(12344L));

        when(mongoOperations.findOne(findCaptor.capture(), eq(VideoFiles.class))).thenReturn(mark);

        mockMvc.perform(put("/video/files/5c7d3a7da9e08a5562d00073")
                .content("{\"name\": \"Lion King 2019\"}")
                .param("user_id", "12")
                .param("client_id", "12344")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().isEqualTo(expected))
                .andExpect(status().is(200));

        assertThat(updateQueryCaptor.getValue().toString(), equalTo(validQuery.toString()));

        assertThat(findCaptor.getValue().toString(), equalTo(validFind.toString()));

        assertThat(updateCaptor.getValue().toString(), equalTo(validUpdate.toString()));
    }


}
