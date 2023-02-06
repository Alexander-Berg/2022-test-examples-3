package ru.yandex.canvas.controllers;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import ru.yandex.canvas.configs.GlobalExceptionHandler;
import ru.yandex.canvas.controllers.video.VideoFilesModifyingController;
import ru.yandex.canvas.model.stillage.StillageInfoConverter;
import ru.yandex.canvas.model.video.VideoFiles;
import ru.yandex.canvas.model.video.files.FileStatus;
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
import ru.yandex.canvas.service.video.AudioServiceInterface;
import ru.yandex.canvas.service.video.CmsConversionStatusUpdateService;
import ru.yandex.canvas.service.video.InBannerVideoFilesService;
import ru.yandex.canvas.service.video.MovieService;
import ru.yandex.canvas.service.video.MovieServiceInterface;
import ru.yandex.canvas.service.video.VhService;
import ru.yandex.canvas.service.video.VideoCreativeType;
import ru.yandex.canvas.service.video.VideoCreativesService;
import ru.yandex.canvas.service.video.VideoFileUploadService;
import ru.yandex.canvas.service.video.VideoFileUploadServiceInterface;
import ru.yandex.canvas.service.video.VideoGeometryService;
import ru.yandex.canvas.service.video.VideoLimitsService;
import ru.yandex.canvas.service.video.VideoPresetsService;
import ru.yandex.canvas.service.video.files.StockMoviesService;
import ru.yandex.canvas.service.video.overlay.OverlayService;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.canvas.utils.SandboxTestUtils.makeSandboxDispatcher;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(VideoFilesModifyingController.class)
@Import({GlobalExceptionHandler.class})
public class VideoCleanupTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionParams sessionParams;

    @Autowired
    private VideoFilesRepository videoFilesRepository;

    @Autowired
    MockWebServer mockWebServer;

    @Configuration
    public static class TestConf {
        @MockBean
        DirectService directService;

        @MockBean
        private VideoPresetsService videoPresetsService;

        @MockBean
        StockMoviesService stockMoviesService;

        @MockBean
        PackshotService packshotService;

        @MockBean
        VideoLimitsService videoLimitsService;

        @MockBean
        AuthService authService;

        @MockBean
        DateTimeService dateTimeService;

        @MockBean
        AudioService audioService;

        @MockBean
        OverlayService overlayService;

        @MockBean
        StillageService stillageService;

        @MockBean
        SessionParams sessionParams;

        @MockBean
        VideoFilesRepository videoFilesRepository;

        @MockBean
        AvatarsService avatarsService;

        @MockBean
        VideoCreativesService videoCreativesService;

        @MockBean
        private VhService vhClient;

        @MockBean
        InBannerVideoFilesService inBannerVideoFilesService;

        @MockBean
        private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @MockBean
        VideoGeometryService videoGeometryService;

        @Bean
        public StillageInfoConverter stillageInfoConverter() {
            return new StillageInfoConverter(new ObjectMapper());
        }

        @Bean
        public MovieAndVideoSourceFactory movieAndVideoSourceFactory(StillageInfoConverter stillageInfoConverter) {
            return new MovieAndVideoSourceFactory(stillageInfoConverter);
        }

        @Bean
        public AsyncHttpClient asyncHttpClient() {
            return new DefaultAsyncHttpClient();
        }

        @Bean
        public MockWebServer mockWebServer() throws Exception {
            MockWebServer mockWebServer = new MockWebServer();
            mockWebServer.start();

            return mockWebServer;
        }

        @Bean
        public SandBoxService sandBoxService(MockWebServer mockWebServer, AsyncHttpClient asyncHttpClient) {
            return new SandBoxService("sandboxTestToken",
                    "http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort(),
                    asyncHttpClient);
        }

        @Bean
        public VideoFileUploadServiceInterface videoFileUploadService(SandBoxService sandBoxService,
                                                                      VideoFilesRepository videoFilesRepository,
                                                                      VideoLimitsService limits,
                                                                      AvatarsService avatarsService,
                                                                      StillageInfoConverter stillageInfoConverter,
                                                                      VideoGeometryService videoGeometryService) {
            return new VideoFileUploadService(sandBoxService, "HookSecret", "http://canvas.base.url/",
                    videoFilesRepository, limits, avatarsService, directService, videoPresetsService, stillageService,
                    stillageInfoConverter, vhClient, videoGeometryService);
        }

        @Bean
        public VideoFilesModifyingController videoFilesMController(AuthService authService,
                                                                   MovieServiceInterface movieService,
                                                                   AudioServiceInterface audioService,
                                                                   PackshotService packshotService,
                                                                   SessionParams sessionParams) {
            return new VideoFilesModifyingController(authService, movieService, audioService, packshotService,
                    sessionParams, videoCreativesService, directService, inBannerVideoFilesService, videoPresetsService);
        }

        @Bean
        public MovieServiceInterface movieService(VideoFilesRepository videoFilesRepository,
                                                  StockMoviesService stockMoviesService,
                                                  VideoFileUploadServiceInterface videoFileUploadService,
                                                  StillageService stillageService,
                                                  VideoLimitsService videoLimitsService,
                                                  MovieAndVideoSourceFactory movieAndVideoSourceFactory,
                                                  VideoGeometryService videoGeometryService) {
            return new MovieService(videoFilesRepository, stockMoviesService, videoFileUploadService,
                    stillageService, videoLimitsService, new DateTimeService(), directService, videoPresetsService,
                    cmsConversionStatusUpdateService, movieAndVideoSourceFactory, videoGeometryService);
        }
    }

    @Test
    public void cleanupWithPreparingTaskSmokeTest() throws Exception {
        mockWebServer.setDispatcher(makeSandboxDispatcher(request -> {
            if (request.getMethod().equals("GET") && request.getPath().equals("/task/12345")) {
                return new MockResponse().setBody("{\"id\":12345, \"status\":\"ENQUEUED\"}");
            }
            return null;
        }));

        VideoFiles file =
                new VideoFiles().setConvertionTaskId(12345L).setId("deadbeef").setStatus(FileStatus.CONVERTING);

        when(videoFilesRepository.findByQuery(any(VideoFilesRepository.QueryBuilder.class)))
                .thenReturn(Arrays.asList(file));

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);

        mockMvc.perform(post("/video/files/tasks/cleanup")
                .param("token", "internalApiToken")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().isEqualTo("{\"OK\":true,\"missing_sandbox_tasks\": 0}"))
                .andExpect(status().is(200));
    }

    @Test
    public void cleanupWithExceptionTaskSmokeTest() throws Exception {
        mockWebServer.setDispatcher(makeSandboxDispatcher(request -> {
            if (request.getMethod().equals("GET") && request.getPath().equals("/task/12345")) {
                return new MockResponse().setBody("{\"id\":12345, \"status\":\"EXCEPTION\"}");
            }
            return null;
        }));

        VideoFiles file =
                new VideoFiles().setConvertionTaskId(12345L).setId("deadbeef").setStatus(FileStatus.CONVERTING);

        when(videoFilesRepository.findByQuery(any(VideoFilesRepository.QueryBuilder.class)))
                .thenReturn(Arrays.asList(file));

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);

        mockMvc.perform(post("/video/files/tasks/cleanup")
                .param("token", "internalApiToken")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().isEqualTo("{\"OK\":true,\"missing_sandbox_tasks\": 0}"))
                .andExpect(status().is(200));
    }

    @Test
    public void cleanupWithNoOutputTest() throws Exception {
        mockWebServer.setDispatcher(makeSandboxDispatcher(request -> {
            if (request.getMethod().equals("GET") && request.getPath().equals("/task/12345")) {
                return new MockResponse().setBody("{\"id\":12345, \"status\":\"SUCCESS\"}");
            }
            return null;
        }));

        VideoFiles file =
                new VideoFiles().setConvertionTaskId(12345L).setId("deadbeef").setStatus(FileStatus.CONVERTING);

        when(videoFilesRepository.findByQuery(any(VideoFilesRepository.QueryBuilder.class)))
                .thenReturn(Arrays.asList(file));

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);

        mockMvc.perform(post("/video/files/tasks/cleanup")
                .param("token", "internalApiToken")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().isEqualTo("{\"OK\":true,\"missing_sandbox_tasks\": 0}"))
                .andExpect(status().is(200));
    }
}
