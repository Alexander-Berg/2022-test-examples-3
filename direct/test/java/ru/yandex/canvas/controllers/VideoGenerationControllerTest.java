package ru.yandex.canvas.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.controllers.video.VideoDirectController;
import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.repository.video.StockVideoAdditionsRepository;
import ru.yandex.canvas.repository.video.VideoAdditionsRepository;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AvatarsService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.video.CreativesGenerationService;
import ru.yandex.canvas.service.video.VideoAdditionsService;
import ru.yandex.canvas.service.video.VideoCreativesService;
import ru.yandex.canvas.service.video.VideoPresetsService;
import ru.yandex.canvas.service.video.files.StockMoviesService;
import ru.yandex.direct.rotor.client.RotorClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.ROTOR_CLIENT;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(VideoDirectController.class)
@Import(ControllerTestConfiguration.class)
public class VideoGenerationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AvatarsService avatarsService;

    @Autowired
    private VideoFilesRepository videoFilesRepository;

    @Autowired
    private VideoAdditionsService videoAdditionsService;

    @Autowired
    private MongoOperations mongoOperations;

    @TestConfiguration
    public static class TestConf {
        @MockBean(name = ROTOR_CLIENT)
        private RotorClient rotorClient;

        @MockBean
        private VideoAdditionsService videoAdditionsService;
        @Bean
        public VideoAdditionsService videoAdditionsService() {
            VideoAdditionsService mock = mock(VideoAdditionsService.class);
            when(mock.worksOn()).thenReturn(Addition.class);
            return mock;
        }

        @MockBean
        DirectService directService;

        @Bean
        public CreativesGenerationService creativesGenerationService(StockMoviesService stockMoviesService,
                                                                     VideoPresetsService presetsService,
                                                                     VideoAdditionsService videoAdditionsService,
                                                                     VideoAdditionsRepository videoAdditionsRepository,
                                                                     StockVideoAdditionsRepository stockVideoAdditionsRepository,
                                                                     VideoCreativesService videoCreativesService) {
            return new CreativesGenerationService(stockMoviesService, presetsService, videoAdditionsService,
                    videoAdditionsRepository, stockVideoAdditionsRepository, videoCreativesService);
        }

    }


    @Test
    public void correctAnswerTest() throws Exception {
        BulkOperations bulkOperations = mock(BulkOperations.class);
        when(mongoOperations.bulkOps(eq(BulkOperations.BulkMode.UNORDERED), any(Class.class))).thenReturn(bulkOperations);

        //send: 'POST /video/direct/generate-creatives HTTP/1.1\r\nHost: backend:8080\r\nConnection:
        // keep-alive\r\nAccept-Encoding: gzip, deflate\r\nAccept: */*\r\nUser-Agent: python-requests/2.11
        // .1\r\nAuthorization: \r\nContent-Length: 100\r\nContent-Type: applica
        //tion/json\r\n\r\n{"conditions": [{"count": 1, "creative_type": "videoAddition", "category_ids": []}],
        // "client_id": 1}'

        mockMvc.perform(post("/video/direct/generate-creatives")
                .content("{\"client_id\":1, \"conditions\":[ {\"count\":2, \"category_ids\":[], \"creative_type\":\"videoAddition\", \"locale\":\"ru_RU\"}]}")
                .header(HttpHeaders.AUTHORIZATION, "direct-secret")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
        )
                //TODO check json
                .andExpect(e -> System.err.println("!!! " + e.getResponse().getContentAsString()))
                .andExpect(status().is(200));

    }
}
