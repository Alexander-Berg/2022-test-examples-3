package ru.yandex.canvas.controllers;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import ru.yandex.canvas.configs.GlobalExceptionHandler;
import ru.yandex.canvas.controllers.video.VideoFilesController;
import ru.yandex.canvas.model.direct.Privileges;
import ru.yandex.canvas.model.stillage.StillageInfoConverter;
import ru.yandex.canvas.model.video.VideoFiles;
import ru.yandex.canvas.model.video.files.FileStatus;
import ru.yandex.canvas.model.video.files.FileType;
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
import ru.yandex.canvas.service.video.VideoCreativeType;
import ru.yandex.canvas.service.video.VideoFileUploadService;
import ru.yandex.canvas.service.video.VideoFileUploadServiceInterface;
import ru.yandex.canvas.service.video.VideoGeometryService;
import ru.yandex.canvas.service.video.VideoLimitsService;
import ru.yandex.canvas.service.video.VideoPresetsService;
import ru.yandex.canvas.service.video.VideoSoundTrackService;
import ru.yandex.canvas.service.video.files.StockMoviesService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.canvas.utils.SandboxTestUtils.makeSandboxDispatcher;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(VideoFilesController.class)
@Import({GlobalExceptionHandler.class})
public class VideoDeleteMovieTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionParams sessionParams;

    @Autowired
    private VideoFilesRepository videoFilesRepository;

    @Autowired
    private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

    @Autowired
    private MockWebServer mockWebServer;

    @Configuration
    public static class TestConf {

        @MockBean
        private VhService vhClient;

        @MockBean
        DirectService directService;

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
        VideoSoundTrackService videoSoundTrackService;

        @MockBean
        StillageService stillageService;

        @MockBean
        SessionParams sessionParams;

        @MockBean
        VideoFilesRepository videoFilesRepository;

        @MockBean
        private VideoPresetsService videoPresetsService;

        @MockBean
        AvatarsService avatarsService;

        @MockBean
        InBannerVideoFilesService inBannerVideoFilesService;

        @MockBean
        private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @MockBean
        private VideoGeometryService videoGeometryService;

        @Bean
        public StillageInfoConverter stillageInfoConverter() {
            return new StillageInfoConverter(new ObjectMapper());
        }

        @Bean
        public MovieAndVideoSourceFactory movieAndVideoSourceFactory(StillageInfoConverter stillageInfoConverter) {
            return new MovieAndVideoSourceFactory(stillageInfoConverter);
        }

        @Bean
        public RestTemplate restTemplate() {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().removeIf(
                    m -> m.getClass().getName().equals(MappingJackson2XmlHttpMessageConverter.class.getName()));
            return restTemplate;
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
                                                                      StillageInfoConverter stillageInfoConverter) {
            return new VideoFileUploadService(sandBoxService, "HookSecret", "http://canvas.base.url/",
                    videoFilesRepository, limits, avatarsService, directService, videoPresetsService, stillageService,
                    stillageInfoConverter, vhClient, videoGeometryService);
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
        public MovieServiceInterface movieService(VideoFilesRepository videoFilesRepository,
                                                  StockMoviesService stockMoviesService,
                                                  VideoFileUploadService videoFileUploadService,
                                                  StillageService stillageService,
                                                  MovieAndVideoSourceFactory movieAndVideoSourceFactory,
                                                  VideoLimitsService videoLimitsService) {
            return new MovieService(videoFilesRepository, stockMoviesService, videoFileUploadService,
                    stillageService, videoLimitsService, new DateTimeService(), directService, videoPresetsService,
                    cmsConversionStatusUpdateService, movieAndVideoSourceFactory, videoGeometryService);
        }
    }

    @Before
    public void before() {
        when(cmsConversionStatusUpdateService.updateStatus(any(VideoFiles.class))).then(
                e -> e.getArgument(0)
        );
    }

    @Test
    public void fileDeleteSmokeTest() throws Exception {
        mockWebServer.setDispatcher(makeSandboxDispatcher(request -> {
            if (request.getMethod().equals("GET") && request.getPath().equals("/task/7123553")) {
                return new MockResponse().setBody("{\"id\":7123553, \"status\":\"EXECUTING\"}");
            } if (request.getMethod().equals("PUT") && request.getPath().equals("/batch/tasks/stop")) {
                return new MockResponse().setBody("[{\"id\":\"7123553\",\"status\":\"SUCCESS\"}]");
            }
            return null;
        }));

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);

        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        VideoFiles record = mock(VideoFiles.class);
        when(record.getId()).thenReturn("cafebabe");
        when(record.getConvertionTaskId()).thenReturn(7123553L);
        when(record.getType()).thenReturn(FileType.VIDEO);
        when(record.getStatus()).thenReturn(FileStatus.CONVERTING);

        when(videoFilesRepository.findByIdAndQuery(eq("cafebabe"), any(VideoFilesRepository.QueryBuilder.class))).thenReturn(record);
        when(videoFilesRepository.deleteFile(eq("cafebabe"), eq(1L), eq(FileType.VIDEO))).thenReturn(true);

        mockMvc.perform(delete("/video/files/cafebabe")
                .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(204));
    }

    @Test
    public void fileDeleteConvertedSmokeTest() throws Exception {
        mockWebServer.setDispatcher(makeSandboxDispatcher(request -> null));

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);

        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        VideoFiles record = mock(VideoFiles.class);
        when(record.getId()).thenReturn("cafebabe");
        when(record.getConvertionTaskId()).thenReturn(7123553L);
        when(record.getType()).thenReturn(FileType.VIDEO);
        when(record.getStatus()).thenReturn(FileStatus.READY);

        when(videoFilesRepository.findByIdAndQuery(eq("cafebabe"), any(VideoFilesRepository.QueryBuilder.class))).thenReturn(record);
        when(videoFilesRepository.deleteFile(eq("cafebabe"), eq(1L), eq(FileType.VIDEO))).thenReturn(true);

        mockMvc.perform(delete("/video/files/cafebabe")
                .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(204));
    }

    @Test
    public void fileDeleteWithStoppedTaskTest() throws Exception {
        mockWebServer.setDispatcher(makeSandboxDispatcher(request -> {
                if (request.getMethod().equals("GET") && request.getPath().equals("/task/7123553")) {
                    return new MockResponse().setBody("{\"id\":7123553, \"status\":\"STOPPED\"}");
                }
                return null;
        }));

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);

        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        VideoFiles record = mock(VideoFiles.class);
        when(record.getId()).thenReturn("cafebabe");
        when(record.getConvertionTaskId()).thenReturn(7123553L);
        when(record.getType()).thenReturn(FileType.VIDEO);
        when(record.getStatus()).thenReturn(FileStatus.CONVERTING);

        when(videoFilesRepository.findByIdAndQuery(eq("cafebabe"), any(VideoFilesRepository.QueryBuilder.class))).thenReturn(record);
        when(videoFilesRepository.deleteFile(eq("cafebabe"), eq(1L), eq(FileType.VIDEO))).thenReturn(true);

        mockMvc.perform(delete("/video/files/cafebabe")
                .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(204));
    }

    @Test
    public void fileDeleteWithPreparingTaskTest() throws Exception {
        mockWebServer.setDispatcher(makeSandboxDispatcher(request -> {
                if (request.getMethod().equals("GET") && request.getPath().equals("/task/7123553")) {
                    return new MockResponse().setBody("{\"id\":7123553, \"status\":\"PREPARING\"}");
                } else if (request.getMethod().equals("PUT") && request.getPath().equals("/batch/tasks/stop")) {
                    return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
                return null;
        }));

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);

        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        VideoFiles record = mock(VideoFiles.class);
        when(record.getId()).thenReturn("cafebabe");
        when(record.getConvertionTaskId()).thenReturn(7123553L);
        when(record.getType()).thenReturn(FileType.VIDEO);
        when(record.getStatus()).thenReturn(FileStatus.CONVERTING);

        when(videoFilesRepository.findByIdAndQuery(eq("cafebabe"), any(VideoFilesRepository.QueryBuilder.class))).thenReturn(record);
        when(videoFilesRepository.deleteFile(eq("cafebabe"), eq(1L), eq(FileType.VIDEO))).thenReturn(true);

        mockMvc.perform(delete("/video/files/cafebabe")
                .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(204));
    }
}
