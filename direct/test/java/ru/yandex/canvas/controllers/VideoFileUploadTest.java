package ru.yandex.canvas.controllers;

import java.net.URL;
import java.util.Arrays;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;

import ru.yandex.canvas.configs.GlobalExceptionHandler;
import ru.yandex.canvas.controllers.video.VideoFilesModifyingController;
import ru.yandex.canvas.model.direct.Privileges;
import ru.yandex.canvas.model.stillage.StillageFileInfo;
import ru.yandex.canvas.model.stillage.StillageInfoConverter;
import ru.yandex.canvas.model.video.VideoFiles;
import ru.yandex.canvas.model.video.files.FileStatus;
import ru.yandex.canvas.model.video.files.MovieAndVideoSourceFactory;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthRequestParams;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.canvas.utils.SandboxTestUtils.makeSandboxDispatcher;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(VideoFilesModifyingController.class)
@Import({GlobalExceptionHandler.class})
public class VideoFileUploadTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StillageService stillageService;

    @Autowired
    private SessionParams sessionParams;

    @Autowired
    private VideoFilesRepository videoFilesRepository;

    @Autowired
    private VideoLimitsService videoLimitsService;

    @Autowired
    private MockWebServer mockSandboxWebServer;

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
        AuthRequestParams authRequestParams;

        @Bean
        public VideoLimitsService videoLimitsService(AuthRequestParams authRequestParams,
                                                     DirectService directService) {
            return new VideoLimitsService(authRequestParams, directService);
        }

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

    private static final String stillageInfo = "{\n"
            + "        \"mimeType\" : \"video/mp4\",\n"
            + "        \"metadataInfo\" : {\n"
            + "            \"duration\" : 14.92,\n"
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

    @Test
    public void fileUploadSmokeTest() throws Exception {
        mockSandboxWebServer.setDispatcher(makeSandboxDispatcher(request -> {
            if (request.getPath().equals("/task") && request.getMethod().equals("POST")) {
                return new MockResponse().setBody("{\"id\":7123553}");
            } else if (request.getPath().equals("/batch/tasks/start") && request.getMethod().equals("PUT")) {
                return new MockResponse().setBody("[{\"id\":\"7123553\",\"status\":\"SUCCESS\"}]");
            }
            return null;
        }));

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);

        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        MockMultipartFile mockMultipartFile =
                new MockMultipartFile("file", "giraffe.mov", "", "FAKE_MOV_FILE".getBytes());

        ObjectMapper mapper = new ObjectMapper();

        when(stillageService.uploadFile(anyString(), any(byte[].class))).thenReturn(mapper.readValue(stillageInfo,
                StillageFileInfo.class));

        when(videoFilesRepository.save(any(VideoFiles.class))).thenAnswer((Answer<VideoFiles>) invocation -> {
            Object[] args = invocation.getArguments();
            VideoFiles record = (VideoFiles) args[0];

            record.setStatus(FileStatus.NEW);
            record.setId("1234");

            return record;
        });

        mockMvc.perform(MockMvcRequestBuilders.multipart("/video/files").file(mockMultipartFile)
                .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(e -> System.err.println("!!! " + e.getResponse().getContentAsString()))
                // .andExpect(json().isEqualTo(expected))
                .andExpect(status().is(201));
    }


    @Test
    public void fileUploadByUrlSmokeTest() throws Exception {
        mockSandboxWebServer.setDispatcher(makeSandboxDispatcher(request -> {
            if (request.getPath().equals("/task") && request.getMethod().equals("POST")) {
                return new MockResponse().setBody("{\"id\":7123553}");
            } else if (request.getPath().equals("/batch/tasks/start") && request.getMethod().equals("PUT")) {
                return new MockResponse().setBody("[{\"id\":\"7123553\",\"status\":\"SUCCESS\"}]");
            }
            return null;
        }));

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);

        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        ObjectMapper mapper = new ObjectMapper();

        when(stillageService.uploadFile(anyString(), any(URL.class))).thenReturn(mapper.readValue(stillageInfo,
                StillageFileInfo.class));

        when(videoFilesRepository.save(any(VideoFiles.class))).thenAnswer((Answer<VideoFiles>) invocation -> {
            Object[] args = invocation.getArguments();
            VideoFiles record = (VideoFiles) args[0];

            record.setStatus(FileStatus.NEW);
            record.setId("1234");

            return record;
        });

        mockMvc.perform(MockMvcRequestBuilders.post("/video/files/url")
                .content("{\"url\":\"http://test.ru/film.avi\"}")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(e -> System.err.println("!!! " + e.getResponse().getContentAsString()))
                // .andExpect(json().isEqualTo(expected))
                .andExpect(status().is(201));
    }

    @Test
    public void invalidUrlUploadTest() throws Exception {
        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        mockMvc.perform(MockMvcRequestBuilders.post("/video/files/url")
                .content("{\"url\":\"Not an url\"}")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().node("message").isEqualTo("invalid: not an url"))
                .andExpect(json().node("properties.url[0]").isEqualTo("must be a valid URL"))
                .andExpect(status().is(400));
    }

    @Test
    public void fileUploadSandboxFailToCreateTaskTest() throws Exception {
        mockSandboxWebServer.setDispatcher(makeSandboxDispatcher(request -> {
            if (request.getPath().equals("/task") && request.getMethod().equals("POST")) {
                return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
            return null;
        }));

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);

        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        MockMultipartFile mockMultipartFile =
                new MockMultipartFile("file", "giraffe.mov", "", "FAKE_MOV_FILE".getBytes());

        ObjectMapper mapper = new ObjectMapper();

        when(stillageService.uploadFile(anyString(), any(byte[].class))).thenReturn(mapper.readValue(stillageInfo,
                StillageFileInfo.class));

        when(videoFilesRepository.save(any(VideoFiles.class))).thenAnswer((Answer<VideoFiles>) invocation -> {
            Object[] args = invocation.getArguments();
            VideoFiles record = (VideoFiles) args[0];

            record.setStatus(FileStatus.NEW);
            record.setId("1234");

            return record;
        });

        mockMvc.perform(MockMvcRequestBuilders.multipart("/video/files").file(mockMultipartFile)
                .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().isEqualTo("{\"message\":[\"Failed to convert file\"],\"file_id\":\"1234\"}"))
                .andExpect(status().is(500));
    }

    @Test
    public void fileUploadSandboxFailToStartTaskTest() throws Exception {
        mockSandboxWebServer.setDispatcher(makeSandboxDispatcher(request -> {
            if (request.getPath().equals("/task") && request.getMethod().equals("POST")) {
                return new MockResponse().setBody("{\"id\":7123553}");
            } else if (request.getPath().equals("/batch/tasks/start") && request.getMethod().equals("PUT")) {
                return new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
            return null;
        }));

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);

        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        MockMultipartFile mockMultipartFile =
                new MockMultipartFile("file", "giraffe.mov", "", "FAKE_MOV_FILE".getBytes());

        ObjectMapper mapper = new ObjectMapper();

        when(stillageService.uploadFile(anyString(), any(byte[].class))).thenReturn(mapper.readValue(stillageInfo,
                StillageFileInfo.class));

        when(videoFilesRepository.save(any(VideoFiles.class))).thenAnswer((Answer<VideoFiles>) invocation -> {
            Object[] args = invocation.getArguments();
            VideoFiles record = (VideoFiles) args[0];

            record.setStatus(FileStatus.NEW);
            record.setId("1234");

            return record;
        });

        mockMvc.perform(MockMvcRequestBuilders.multipart("/video/files").file(mockMultipartFile)
                .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType("application/json"))
                .andExpect(json().isEqualTo("{\"message\":[\"Failed to convert file\"],\"file_id\":\"1234\"}"))
                .andExpect(status().is(500));
    }

    @Test
    public void fileUploadSandboxFailToStartTaskButReturnOkTest() throws Exception {
        mockSandboxWebServer.setDispatcher(makeSandboxDispatcher(request -> {
            if (request.getPath().equals("/task") && request.getMethod().equals("POST")) {
                return new MockResponse().setBody("{\"id\":7123553}");
            } else if (request.getPath().equals("/batch/tasks/start") && request.getMethod().equals("PUT")) {
                return new MockResponse().setBody("[{\"id\":7123553, status:\"FAILED\", \"message\":null}]");
            }
            return null;
        }));

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);

        when(videoFilesRepository.save(any(VideoFiles.class))).thenAnswer((Answer<VideoFiles>) invocation -> {
            Object[] args = invocation.getArguments();
            VideoFiles record = (VideoFiles) args[0];

            record.setStatus(FileStatus.NEW);
            record.setId("1234");

            return record;
        });

        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        MockMultipartFile mockMultipartFile =
                new MockMultipartFile("file", "giraffe.mov", "", "FAKE_MOV_FILE".getBytes());

        ObjectMapper mapper = new ObjectMapper();

        when(stillageService.uploadFile(anyString(), any(byte[].class))).thenReturn(mapper.readValue(stillageInfo,
                StillageFileInfo.class));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/video/files").file(mockMultipartFile)
                .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType("application/json"))
                .andExpect(json().isEqualTo("{\"message\":[\"Failed to convert file\"],\"file_id\":\"1234\"}"))
                .andExpect(status().is(500));
    }


    private static final String shortStillageInfo = "{\n"
            + "        \"mimeType\" : \"video/mp4\",\n"
            + "        \"metadataInfo\" : {\n"
            + "            \"duration\" : 3.92,\n"
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
            + "                    \"duration\" : 4.92,\n"
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

    @Test
    public void shortFileUploadSmokeTest() throws Exception {

        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPM);
        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));

        MockMultipartFile mockMultipartFile =
                new MockMultipartFile("file", "giraffe.mov", "", "FAKE_MOV_FILE".getBytes());

        ObjectMapper mapper = new ObjectMapper();

        when(stillageService.uploadFile(anyString(), any(byte[].class))).thenReturn(mapper.readValue(shortStillageInfo,
                StillageFileInfo.class));

        when(videoFilesRepository.save(any(VideoFiles.class))).thenAnswer((Answer<VideoFiles>) invocation -> {
            Object[] args = invocation.getArguments();
            VideoFiles record = (VideoFiles) args[0];

            record.setStatus(FileStatus.NEW);
            record.setId("1234");

            return record;
        });

        mockMvc.perform(MockMvcRequestBuilders.multipart("/video/files").file(mockMultipartFile)
                .locale(Locale.forLanguageTag("en"))
                .param("shared_data",
                        "{\"isCompact\":true,\"clientId\":\"103997791\",\"cpm\":1,\"creativeType\":\"videoAddition\"}")
                .param("client_id", String.valueOf(1L)).param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                //TODO Numbers format is different locally and in ci
                // .andExpect(json().isEqualTo("{\"message\":\"Total duration is out of limit. Duration is 4,92s. "
                //      + "Permitted duration range is from 5,5 up to 60,5 seconds\",\"properties\":{}}"))
                .andExpect(status().is(400));

    }


}
