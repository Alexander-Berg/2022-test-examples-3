package ru.yandex.canvas.controllers;

import java.util.Arrays;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonObjectId;
import org.bson.BsonValue;
import org.bson.types.ObjectId;
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
import org.springframework.dao.DuplicateKeyException;
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

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(VideoFilesController.class)
public class VideoFilesFromStockTest {
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
        AudioUploadService audioUploadService;

        @MockBean
        AudioFilesRepository audioFilesRepository;

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
        InBannerVideoFilesService inBannerVideoFilesService;

        @MockBean
        VideoGeometryService videoGeometryService;

        @MockBean
        CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @Bean
        public MovieAndVideoSourceFactory movieAndVideoSourceFactory() {
            return new MovieAndVideoSourceFactory(new StillageInfoConverter(new ObjectMapper()));
        }

        @Bean(name = "main")
        @Primary
        public VideoFilesRepository videoFilesRepository(MongoOperations mongoOperations,
                                                         DateTimeService dateTimeService) {
            return new VideoFilesRepository(mongoOperations, dateTimeService);
        }

        @Bean
        StockMoviesService stockMoviesService(MovieAndVideoSourceFactory movieAndVideoSourceFactory) {
            return new StockMoviesService(null, movieAndVideoSourceFactory);
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
                                                  DateTimeService dateTimeService,
                                                  MovieAndVideoSourceFactory movieAndVideoSourceFactory,
                                                  VideoGeometryService videoGeometryService) {
            return new MovieService(videoFilesRepository, stockMoviesService, fileUploadService,
                    stillageService, videoLimitsService, dateTimeService, directService, videoPresetsService,
                    cmsConversionStatusUpdateService, movieAndVideoSourceFactory, videoGeometryService);
        }

        @Bean
        public AudioService audioService(AudioFilesRepository audioFilesRepository, StillageService stillageService,
                                         AudioUploadService audioUploadService, SandBoxService sandBoxService,
                                         VideoLimitsService videoLimitsService) {
            return new AudioService(audioFilesRepository, stillageService, audioUploadService,
                    sandBoxService, videoLimitsService, "", "");
        }

        @Bean
        public VideoSoundTrackService videoSoundTrackService(StockMoviesService stockMoviesService,
                                                             SessionParams sessionParams,
                                                             VideoFilesRepository videoFilesRepository) {
            return new VideoSoundTrackService(stockMoviesService, sessionParams, videoFilesRepository);
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
                return false;
            }

            @Override
            public long getModifiedCount() {
                return 0;
            }

            @Override
            public BsonValue getUpsertedId() {
                return new BsonObjectId(new ObjectId(upsertedId));
            }
        };
    }

    @Before
    public void setTime() {
        when(dateTimeService.getCurrentDate()).thenReturn(new Date());
    }

    @Test
    public void markVideoTest() throws Exception {

        String expected = "{\n"
                + "  \"archive\": false, \n"
                + "  \"conversion_task_id\": null, \n"
                + "  \"formats\": [\n"
                + "    {\n"
                + "      \"url\": \"https://storage.mds.yandex"
                + ".net/get-bstor/15932/d648fd93-a2cf-4ca9-9bc1-4b68c98fe8ca.qt\",\n"
                + "      \"type\": null,\n"
                + "      \"delivery\": null,\n"
                + "      \"bitrate\" : null\n"
                + "    }\n"
                + "  ], \n"
                + "  \"client_id\": 12344,\n"
                + "  \"date\": null,\n"
                + "  \"error_message\": null,\n"
                + "  \"id\": \"5c7d3a7da9e08a5562d00073\", \n"
                + "  \"name\": \"Video 469\", \n"
                + "  \"overlayColor\": \"#3994CA\", \n"
                + "  \"show_early_preview\": false, \n"
                + "  \"create_early_creative\": false, \n"
                + "  \"duration\": 15.0, \n"
                + "  \"width\": null, \n"
                + "  \"height\": null, \n"
                + "  \"mime_type\": null, \n"
                + "  \"status\": \"ready\", \n"
                + "   \"stillage_id\":\"4041748\", \n"
                + "  \"stock_file_id\": \"new_0_0-077.mov\", \n"
                + "  \"sub_categories\": [], \n"
                + "  \"thumbnail\": {\n"
                + "    \"height\": 1080, \n"
                + "    \"preview\": {\n"
                + "      \"height\": 479, \n"
                + "      \"url\": \"https://avatars.mds.yandex"
                + ".net/get-canvas/145764/2a00000163a7878b914152d99404d7f39aca/preview480p\", \n"
                + "      \"width\": 852\n"
                + "    }, \n"
                + "    \"url\": \"https://avatars.mds.yandex"
                + ".net/get-canvas/145764/2a00000163a7878b914152d99404d7f39aca/orig\", \n"
                + "    \"width\": 1920\n"
                + "  }, \n"
                + "  \"thumbnailUrl\": \"https://avatars.mds.yandex"
                + ".net/get-canvas/145764/2a00000163a7878b914152d99404d7f39aca/orig\", \n"
                + "  \"type\": \"video\", \n"
                + "  \"url\": \"https://storage.mds.yandex.net/get-bstor/15932/d648fd93-a2cf-4ca9-9bc1-4b68c98fe8ca"
                + ".qt\"\n"
                + "}\n";

        ArgumentCaptor<Query> updateQueryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Query> findCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        Query validQuery = new Query();
        validQuery
                .addCriteria(Criteria.where("client_id").is(12344L))
                .addCriteria(Criteria.where("stock_file_id").is("new_0_0-077.mov"))
                .addCriteria(Criteria.where("type").is(FileType.VIDEO))
                .addCriteria(Criteria.where("name").is("Video 469"));

        Update validUpdate = new Update();
        validUpdate.set("archive", false)
                .set("date", dateTimeService.getCurrentDate());

        UpdateResult updateResult = makeUpdateResult("5c7d3a7da9e08a5562d00073");

        when(mongoOperations.upsert(updateQueryCaptor.capture(), updateCaptor.capture(), eq(VideoFiles.class)))
                .thenReturn(updateResult);

        VideoFiles mark = new VideoFiles();
        mark.setId("5c7d3a7da9e08a5562d00073").setClientId(12344L)
                .setName("Video 469").setArchive(false)
                .setStockFileId("new_0_0-077.mov").setType(FileType.VIDEO);

        Query validFind = new Query();
        validFind
                .addCriteria(Criteria.where("_id").is("5c7d3a7da9e08a5562d00073"))
                .addCriteria(Criteria.where("client_id").is(12344L));

        when(mongoOperations.findOne(findCaptor.capture(), eq(VideoFiles.class))).thenReturn(mark);

        when(mongoOperations.updateFirst(any(), any(), eq(VideoFiles.class))).thenReturn(makeUpdateResult(""));

        mockMvc.perform(post("/video/files/from-stock/new_0_0-077.mov/used")
                .param("user_id", "12")
                .param("client_id", "12344")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().isEqualTo(expected))
                .andExpect(status().is(200));

        assertThat(updateQueryCaptor.getValue().toString(), equalTo(validQuery.toString()));

        assertThat(findCaptor.getValue().toString(), equalTo(validFind.toString()));

        assertThat(updateCaptor.getValue().toString(), equalTo(validUpdate.toString()));
    }

    @Test
    public void markVideoDuplicateTest() throws Exception {

        String expected = "{\n"
                + "  \"archive\": false, \n"
                + "  \"conversion_task_id\": null, \n"
                + "  \"formats\": [\n"
                + "    {\n"
                + "      \"url\": \"https://storage.mds.yandex"
                + ".net/get-bstor/15932/d648fd93-a2cf-4ca9-9bc1-4b68c98fe8ca.qt\",\n"
                + "      \"type\": null,\n"
                + "      \"delivery\": null,\n"
                + "      \"bitrate\" : null\n"
                + "    }\n"
                + "  ], \n"
                + "  \"client_id\": 12344,\n"
                + "  \"date\": null,\n"
                + "  \"error_message\": null,\n"
                + "  \"id\": \"5c7d3a7da9e08a5562d00073\", \n"
                + "  \"name\": \"Video 469\", \n"
                + "  \"overlayColor\": \"#3994CA\", \n"
                + "  \"status\": \"ready\", \n"
                + "  \"show_early_preview\": false, \n"
                + "  \"create_early_creative\": false, \n"
                + "  \"duration\": 15.0, \n"
                + "  \"width\": null, \n"
                + "  \"height\": null, \n"
                + "  \"mime_type\": null, \n"
                + "   \"stillage_id\":\"4041748\", \n"
                + "  \"stock_file_id\": \"new_0_0-077.mov\", \n"
                + "  \"sub_categories\": [], \n"
                + "  \"thumbnail\": {\n"
                + "    \"height\": 1080, \n"
                + "    \"preview\": {\n"
                + "      \"height\": 479, \n"
                + "      \"url\": \"https://avatars.mds.yandex"
                + ".net/get-canvas/145764/2a00000163a7878b914152d99404d7f39aca/preview480p\", \n"
                + "      \"width\": 852\n"
                + "    }, \n"
                + "    \"url\": \"https://avatars.mds.yandex"
                + ".net/get-canvas/145764/2a00000163a7878b914152d99404d7f39aca/orig\", \n"
                + "    \"width\": 1920\n"
                + "  }, \n"
                + "  \"thumbnailUrl\": \"https://avatars.mds.yandex"
                + ".net/get-canvas/145764/2a00000163a7878b914152d99404d7f39aca/orig\", \n"
                + "  \"type\": \"video\", \n"
                + "  \"url\": \"https://storage.mds.yandex.net/get-bstor/15932/d648fd93-a2cf-4ca9-9bc1-4b68c98fe8ca"
                + ".qt\"\n"
                + "}\n";

        ArgumentCaptor<Query> updateQueryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Query> findCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        Query validQuery = new Query();
        validQuery
                .addCriteria(Criteria.where("client_id").is(12344L))
                .addCriteria(Criteria.where("stock_file_id").is("new_0_0-077.mov"))
                .addCriteria(Criteria.where("type").is(FileType.VIDEO))
                .addCriteria(Criteria.where("name").is("Video 469"));

        Update validUpdate = new Update();
        validUpdate.set("archive", false)
                .set("date", dateTimeService.getCurrentDate());

        when(mongoOperations.upsert(updateQueryCaptor.capture(), updateCaptor.capture(), eq(VideoFiles.class)))
                .thenThrow(DuplicateKeyException.class);


        VideoFiles mark = new VideoFiles();
        mark.setId("5c7d3a7da9e08a5562d00073").setClientId(12344L)
                .setName("Video 469").setArchive(false)
                .setStockFileId("new_0_0-077.mov").setType(FileType.VIDEO);

        Query validFind = new Query();
        validFind
                .addCriteria(Criteria.where("_id").is("5c7d3a7da9e08a5562d00073"))
                .addCriteria(Criteria.where("client_id").is(12344L));

        when(mongoOperations.find(any(Query.class), eq(VideoFiles.class))).thenReturn(Arrays.asList(mark));
        when(mongoOperations.findOne(any(Query.class), eq(VideoFiles.class))).thenReturn(mark);

        mockMvc.perform(post("/video/files/from-stock/new_0_0-077.mov/used")
                .param("user_id", "12")
                .param("client_id", "12344")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().isEqualTo(expected))
                .andExpect(status().is(200));

        assertThat(updateQueryCaptor.getValue().toString(), equalTo(validQuery.toString()));

        // assertThat(findCaptor.getValue().toString(), equalTo(validFind.toString()));

        assertThat(updateCaptor.getValue().toString(), equalTo(validUpdate.toString()));
    }

    @Test
    public void markAudioTest() throws Exception {

        String expected = "{\n"
                + "  \"archive\": false, \n"
                + "  \"client_id\": 12344,\n"
                + "  \"date\": null,\n"
                + "  \"id\": \"5c7d3a7da9e08a5562d00073\", \n"
                + "  \"name\": \"Pachelbel Canon in D\", \n"
                + "  \"originalName\": \"Pachelbel Canon in D\", \n"
                + "  \"stock_file_id\": \"3172374\", \n"
                + "  \"sub_categories\": [], \n"
                + "  \"type\": \"audio\", \n"
                + "  \"duration\":173,\n"
                + "  \"url\": \"https://storage.mds.yandex.net/get-bstor/15932/7f4eed63-0970-47fb-bd72-2b5b3f20c68a"
                + ".wav\"\n"
                + "}\n";

        ArgumentCaptor<Query> updateQueryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Query> findCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        Query validQuery = new Query();
        validQuery
                .addCriteria(Criteria.where("client_id").is(12344L))
                .addCriteria(Criteria.where("stock_file_id").is("3172374"))
                .addCriteria(Criteria.where("type").is(FileType.AUDIO))
                .addCriteria(Criteria.where("name").is("Pachelbel Canon in D"));

        Update validUpdate = new Update();
        validUpdate.set("archive", false)
                .set("date", dateTimeService.getCurrentDate());

        UpdateResult updateResult = makeUpdateResult("5c7d3a7da9e08a5562d00073");

        when(mongoOperations.upsert(updateQueryCaptor.capture(), updateCaptor.capture(), eq(VideoFiles.class)))
                .thenReturn(updateResult);

        VideoFiles mark = new VideoFiles();
        mark.setId("5c7d3a7da9e08a5562d00073").setClientId(12344L)
                .setName("Pachelbel Canon in D").setArchive(false)
                .setStockFileId("3172374").setType(FileType.AUDIO).setDuration(15.1);

        Query validFind = new Query();
        validFind
                .addCriteria(Criteria.where("_id").is("5c7d3a7da9e08a5562d00073"))
                .addCriteria(Criteria.where("client_id").is(12344L));

        when(mongoOperations.findOne(findCaptor.capture(), eq(VideoFiles.class))).thenReturn(mark);

        mockMvc.perform(post("/video/files/from-stock/3172374/used")
                .param("user_id", "12")
                .param("client_id", "12344")
                .accept(MediaType.APPLICATION_JSON))
                //FIXME .andExpect(json().isEqualTo(expected))
                .andExpect(status().is(200));

        assertThat(updateQueryCaptor.getValue().toString(), equalTo(validQuery.toString()));

        assertThat(findCaptor.getValue().toString(), equalTo(validFind.toString()));

        assertThat(updateCaptor.getValue().toString(), equalTo(validUpdate.toString()));

    }

    @Test
    public void markUnknownTest() throws Exception {

        mockMvc.perform(post("/video/files/from-stock/317237412/used")
                .param("user_id", "12")
                .param("client_id", "12344")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(404));

    }

}
