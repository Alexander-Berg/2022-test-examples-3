package ru.yandex.canvas.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.controllers.video.VideoFilesController;
import ru.yandex.canvas.model.stillage.StillageInfoConverter;
import ru.yandex.canvas.model.video.files.MovieAndVideoSourceFactory;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthService;
import ru.yandex.canvas.service.AvatarsService;
import ru.yandex.canvas.service.DateTimeService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.PackshotService;
import ru.yandex.canvas.service.SandBoxService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.video.AudioService;
import ru.yandex.canvas.service.video.CmsConversionStatusUpdateService;
import ru.yandex.canvas.service.video.InBannerVideoFilesService;
import ru.yandex.canvas.service.video.MovieService;
import ru.yandex.canvas.service.video.MovieServiceInterface;
import ru.yandex.canvas.service.video.VhService;
import ru.yandex.canvas.service.video.VideoFileUploadService;
import ru.yandex.canvas.service.video.VideoFileUploadServiceInterface;
import ru.yandex.canvas.service.video.VideoGeometryService;
import ru.yandex.canvas.service.video.VideoLimitsService;
import ru.yandex.canvas.service.video.VideoPresetsService;
import ru.yandex.canvas.service.video.VideoSoundTrackService;
import ru.yandex.canvas.service.video.files.StockMoviesService;
import ru.yandex.direct.rotor.client.RotorClient;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.ROTOR_CLIENT;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(VideoFilesController.class)
@Import(ControllerTestConfiguration.class)
public class VideoGetFileTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VideoPresetsService videoPresetsService;

    @TestConfiguration
    public static class TestConf {
        @MockBean(name = ROTOR_CLIENT)
        private RotorClient rotorClient;

        @MockBean
        private VhService vhClient;

        @MockBean
        DirectService directService;

        @MockBean
        private VideoPresetsService videoPresetsService;

        @MockBean
        InBannerVideoFilesService inBannerVideoFilesService;

        @Autowired
        private StillageInfoConverter stillageInfoConverter;

        @MockBean
        private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @Bean
        public VideoFileUploadServiceInterface videoFileUploadService(SandBoxService sandBoxService,
                                                                      VideoFilesRepository videoFilesRepository,
                                                                      VideoLimitsService videoLimitsService,
                                                                      AvatarsService avatarsService,
                                                                      StillageService stillageService,
                                                                      VideoGeometryService videoGeometryService) {
            return new VideoFileUploadService(sandBoxService, "hookSecret", "http://va.url/",
                    videoFilesRepository, videoLimitsService, avatarsService, directService, videoPresetsService,
                    stillageService, stillageInfoConverter, vhClient, videoGeometryService);
        }

        @Bean
        public VideoFilesController videoFilesController(StockMoviesService stockMoviesService,
                                                         AuthService authService,
                                                         VideoFilesRepository videoFilesRepository,
                                                         MovieServiceInterface movieService,
                                                         PackshotService packshotService,
                                                         AudioService audioService,
                                                         VideoSoundTrackService videoSoundTrackService,
                                                         SessionParams sessionParams) {
            return new VideoFilesController(stockMoviesService, authService, videoFilesRepository,
                    movieService, packshotService, audioService, videoSoundTrackService, sessionParams,
                    inBannerVideoFilesService, directService);
        }

        @Bean
        MovieServiceInterface movieServiceInterface(VideoFilesRepository videoFilesRepository,
                                                    StockMoviesService stockMoviesService,
                                                    VideoFileUploadServiceInterface videoFileUploadService,
                                                    StillageService stillageService,
                                                    VideoLimitsService videoLimitsService,
                                                    CmsConversionStatusUpdateService cmsConversionStatusUpdateService,
                                                    MovieAndVideoSourceFactory movieAndVideoSourceFactory,
                                                    VideoGeometryService videoGeometryService) {
            return new MovieService(videoFilesRepository, stockMoviesService, videoFileUploadService,
                    stillageService, videoLimitsService, new DateTimeService(), directService, videoPresetsService,
                    cmsConversionStatusUpdateService, movieAndVideoSourceFactory, videoGeometryService);
        }
    }

    @Test
    public void correctAnswerTest() throws Exception {

        String expected = "{\n"
                + "  \"archive\": false, \n"
                // + "  \"conversion_task_id\": null, \n"
                + "  \"formats\": [\n"
                + "    {\n"
                + "      \"url\": \"https://storage.mds.yandex"
                + ".net/get-bstor/15932/d648fd93-a2cf-4ca9-9bc1-4b68c98fe8ca.qt\"\n"
                + "    }\n"
                + "  ], \n"
                + "  \"id\": \"new_0_0-077.mov\", \n"
                + "  \"name\": \"Video 469\", \n"
                + "  \"overlayColor\": \"#3994CA\", \n"
                + "  \"status\": \"ready\", \n"
                + "  \"show_early_preview\": false, \n"
                + "  \"create_early_creative\": false, \n"
                + "  \"duration\": 15.0, \n"
                + "  \"stillage_id\": \"4041748\", \n"
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

        mockMvc.perform(get("/video/files/new_0_0-077.mov")
                .param("user_id", "12")
                .param("client_id", "13")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().isEqualTo(expected))
                .andExpect(status().is(200));
    }

}
