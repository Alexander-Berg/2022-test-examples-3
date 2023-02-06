package ru.yandex.canvas.controllers;

import java.util.Arrays;

import javax.annotation.Nullable;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.controllers.video.VideoAdditionsController;
import ru.yandex.canvas.model.direct.Privileges;
import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.model.video.files.MovieAndVideoSourceFactory;
import ru.yandex.canvas.repository.video.StockVideoAdditionsRepository;
import ru.yandex.canvas.repository.video.VideoAdditionsRepository;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthService;
import ru.yandex.canvas.service.DateTimeService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.PackshotService;
import ru.yandex.canvas.service.RTBHostExportService;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.direct.VideoAdditionDirectUploadHelper;
import ru.yandex.canvas.service.video.AudioService;
import ru.yandex.canvas.service.video.CmsConversionStatusUpdateService;
import ru.yandex.canvas.service.video.MovieService;
import ru.yandex.canvas.service.video.MovieServiceInterface;
import ru.yandex.canvas.service.video.VideoAdditionsService;
import ru.yandex.canvas.service.video.VideoCreativesService;
import ru.yandex.canvas.service.video.VideoFileUploadService;
import ru.yandex.canvas.service.video.VideoGeometryService;
import ru.yandex.canvas.service.video.VideoLimitsService;
import ru.yandex.canvas.service.video.VideoPresetsService;
import ru.yandex.canvas.service.video.files.StockMoviesService;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(VideoAdditionsController.class)
@Import(ControllerTestConfiguration.class)
public class VideoGetCampaignsTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VideoAdditionsRepository videoAdditionsRepository;

    @Autowired
    private MockWebServer mockDirectWebServer;

    public static final long UID = 12;
    public static final long CLIENT_ID = 12312;
    public static final long CREATIVE_ID = 88953;

    @TestConfiguration
    public static class TestConf {
        @MockBean
        private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @MockBean
        private VideoAdditionDirectUploadHelper videoAdditionDirectUploadHelper;

        @MockBean
        private PackshotService packshotService;

        @Bean
        public VideoAdditionsService videoAdditionsService() {
            VideoAdditionsService mock = mock(VideoAdditionsService.class);
            when(mock.worksOn()).thenReturn(Addition.class);
            return mock;
        }

        @MockBean
        private VideoPresetsService videoPresetsService;

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
        public DirectService directService(TvmIntegration tvmIntegration,
                                           AsyncHttpClient asyncHttpClient,
                                           MockWebServer mockWebServer) {
            return new DirectService("http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort(),
                    "authToken", tvmIntegration, TvmService.DIRECT_INTAPI_TEST, asyncHttpClient);
        }

        @Bean
        public MovieServiceInterface movieServiceInterface(VideoFilesRepository videoFilesRepository,
                                                           StockMoviesService stockMoviesService,
                                                           VideoFileUploadService fileUploadService,
                                                           StillageService stillageService,
                                                           VideoLimitsService videoLimitsService,
                                                           DirectService directService,
                                                           MovieAndVideoSourceFactory movieAndVideoSourceFactory,
                                                           VideoGeometryService videoGeometryService) {
            return new MovieService(videoFilesRepository, stockMoviesService, fileUploadService,
                    stillageService, videoLimitsService, new DateTimeService(), directService, videoPresetsService,
                    cmsConversionStatusUpdateService, movieAndVideoSourceFactory, videoGeometryService);
        }

        @Bean
        public VideoCreativesService videoCreativesService(MovieServiceInterface movieService,
                                                           AudioService audioService,
                                                           PackshotService packshotService,
                                                           VideoAdditionsService videoAdditionsService,
                                                           VideoAdditionsRepository videoAdditionsRepository,
                                                           DirectService directService,
                                                           RTBHostExportService rtbHostExportService,
                                                           StockVideoAdditionsRepository stockVideoAdditionsRepository,
                                                           VideoPresetsService videoPresetsService) {
            return new VideoCreativesService(movieService, audioService, packshotService, videoAdditionsService,
                    videoAdditionsRepository,
                    directService, rtbHostExportService, stockVideoAdditionsRepository,
                    cmsConversionStatusUpdateService, videoPresetsService, videoAdditionDirectUploadHelper,
                    new DateTimeService());
        }
    }

    @Before
    public void setUp() {
        when(authService.authenticate()).thenReturn(new Privileges(Arrays.asList(Privileges.Permission.values())));
        when(authService.getUserId()).thenReturn(UID);
    }

    public void setupDirectResponse(@Nullable String response) {
        mockDirectWebServer.setDispatcher(
                new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                        if (response == null) {
                            return new MockResponse().setResponseCode(HttpStatus.BAD_REQUEST.value());
                        }
                        if (request.getMethod().equals("POST") &&
                                request.getPath().equals("/DisplayCanvas/get_creatives_campaigns?client_id=" +
                                        CLIENT_ID + "&operator_uid=" + UID) &&
                                request.getBody().readUtf8().equals("{\"creativeIds\":[" + CREATIVE_ID + "]}")) {
                            return new MockResponse().setBody(response);
                        }
                        return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value());
                    }
                });
    }

    @Test
    public void correctAnswerTest() throws Exception {
        Addition addition = new Addition().setId("12").setCreativeId(CREATIVE_ID);
        when(videoAdditionsRepository.getAdditionById("12")).thenReturn(addition);

        String response = "{\"" + CREATIVE_ID + "\" : [{\"campaignId\": 800, \"name\": \"blabla\", \"url\": "
                + "\"https://direct.awpaps.bannerstorage.frankenstein/800\"}] }";

        setupDirectResponse(response);

        mockMvc.perform(get("/video/additions/12/campaigns")
                .param("client_id", CLIENT_ID + "")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().isEqualTo("{\"items\":[{\"campaignId\":800,\"name\":\"blabla\","
                        + "\"url\":\"https://direct.awpaps.bannerstorage.frankenstein/800\"}]}"))
                .andExpect(status().is(200));
    }

    @Test
    public void noCampaignsTest() throws Exception {
        Addition addition = new Addition().setId("12").setCreativeId(CREATIVE_ID);
        when(videoAdditionsRepository.getAdditionById("12")).thenReturn(addition);

        String response = "{\"" + CREATIVE_ID + "\" : [] }";

        setupDirectResponse(response);

        mockMvc.perform(get("/video/additions/12/campaigns")
                .param("client_id", CLIENT_ID + "")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().isEqualTo("{\"items\":[]}"))
                .andExpect(status().is(200));
    }

    @Test
    public void directFailureTest() throws Exception {
        Addition addition = new Addition().setId("12").setCreativeId(CREATIVE_ID);
        when(videoAdditionsRepository.getAdditionById("12")).thenReturn(addition);

        setupDirectResponse(null);

        mockMvc.perform(get("/video/additions/12/campaigns")
                .param("client_id", CLIENT_ID + "")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().isEqualTo("{\"items\":[]}"))
                .andExpect(status().is(200));
    }

    @Test
    public void additionNotFoundTest() throws Exception {
        when(videoAdditionsRepository.getAdditionById("12")).thenReturn(null);

        mockMvc.perform(get("/video/additions/12/campaigns")
                .param("client_id", CLIENT_ID + "")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().isEqualTo("{\"items\":[]}"))
                .andExpect(status().is(200));
    }
}
