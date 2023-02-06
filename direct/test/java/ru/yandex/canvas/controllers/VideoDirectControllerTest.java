package ru.yandex.canvas.controllers;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.asynchttpclient.AsyncHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.controllers.video.VideoAdditionsController;
import ru.yandex.canvas.model.direct.DirectUploadResult;
import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.repository.video.AudioFilesRepository;
import ru.yandex.canvas.repository.video.VideoAdditionsRepository;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.ScreenshooterService;
import ru.yandex.canvas.service.SequenceService;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.video.VideoFileUploadServiceInterface;
import ru.yandex.canvas.service.video.VideoPresetsService;
import ru.yandex.canvas.steps.ResourceHelpers;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.direct.utils.JsonUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(VideoAdditionsController.class)
@Import(ControllerTestConfiguration.class)
public class VideoDirectControllerTest {
    public static final String ADDITION_JSON = "/ru/yandex/canvas/controllers/videoDirectControllerTestAddition.json";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VideoAdditionsRepository videoAdditionsRepository;

    @Autowired
    private VideoFilesRepository videoFilesRepository;

    @Autowired
    private AudioFilesRepository audioFilesRepository;

    @Autowired
    private SequenceService sequenceService;

    @Autowired
    private StillageService stillageService;

    @Autowired
    private ScreenshooterService screenshooterService;

    @Autowired
    private VideoPresetsService videoPresetsService;

    @MockBean
    private VideoFileUploadServiceInterface videoFileUploadServiceInterface;

    @Autowired
    private MockWebServer mockDirectWebServer;

    @Autowired
    private AuthService authService;

    @TestConfiguration
    public static class TestConf {
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
                    "12161216", tvmIntegration, TvmService.DIRECT_INTAPI_TEST, asyncHttpClient);
        }
    }

    @Test
    public void additionDirectControllerTest() throws Exception {
        DirectUploadResult rc = new DirectUploadResult();

        DirectUploadResult.DirectUploadCreativeResult b1 = new DirectUploadResult.DirectUploadCreativeResult();
        b1.setCreativeId(12L);
        b1.setStatus("OK");

        rc.setUploadResults(List.of(b1));

        mockDirectWebServer.setDispatcher(
                new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                        if (request.getMethod().equals("POST") &&
                                request.getPath().startsWith("/DisplayCanvas/upload_creatives")) {
                            return new MockResponse().setBody(JsonUtils.toJson(rc));
                        } else if (request.getMethod().equals("POST") &&
                                request.getPath().startsWith("/feature_dev/access")) {
                            return new MockResponse().setBody("{\"result\": {\"client_ids_features\": {}}}");
                        }
                        return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value());
                    }
                });

        Addition addition =  new ObjectMapper().readValue(ResourceHelpers.getResource(ADDITION_JSON), Addition.class);

        Mockito.when(videoAdditionsRepository.findByQuery(any())).thenReturn(List.of(addition));

        String resp = mockMvc.perform(post("/video/direct/additions")
                .param("client_id", "1")
                .param("user_id", "2")
                .content("{\"additions\": [{\"id\": 12}]}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

    }

}
