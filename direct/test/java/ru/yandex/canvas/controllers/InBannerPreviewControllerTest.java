package ru.yandex.canvas.controllers;

import java.util.List;

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
import org.springframework.web.servlet.LocaleResolver;

import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.configs.CanvasAuthInterceptor;
import ru.yandex.canvas.configs.WebLocaleResolver;
import ru.yandex.canvas.configs.auth.AuthorizeBy;
import ru.yandex.canvas.configs.auth.BlackBoxAuthorizer;
import ru.yandex.canvas.configs.auth.DirectTokenAuthorizer;
import ru.yandex.canvas.configs.auth.QueryStringAuthorizer;
import ru.yandex.canvas.configs.auth.SandboxCallbackAuthorizer;
import ru.yandex.canvas.controllers.video.VideoAdditionsController;
import ru.yandex.canvas.model.video.files.MovieAndVideoSourceFactory;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthRequestParams;
import ru.yandex.canvas.service.AuthService;
import ru.yandex.canvas.service.DateTimeService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.video.CmsConversionStatusUpdateService;
import ru.yandex.canvas.service.video.MovieService;
import ru.yandex.canvas.service.video.MovieServiceInterface;
import ru.yandex.canvas.service.video.VhService;
import ru.yandex.canvas.service.video.VideoFileUploadServiceInterface;
import ru.yandex.canvas.service.video.VideoGeometryService;
import ru.yandex.canvas.service.video.VideoLimitsService;
import ru.yandex.canvas.service.video.VideoPresetsService;
import ru.yandex.canvas.service.video.files.StockMoviesService;
import ru.yandex.canvas.steps.ResourceHelpers;
import ru.yandex.direct.rotor.client.RotorClient;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.direct.web.auth.blackbox.BlackboxCookieAuthProvider;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.canvas.configs.auth.AuthorizeBy.AuthType.OAUTH;
import static ru.yandex.canvas.configs.auth.AuthorizeBy.AuthType.TRUSTED_QUERY_STRING;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.ROTOR_CLIENT;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(VideoAdditionsController.class)
@Import(ControllerTestConfiguration.class)
public class InBannerPreviewControllerTest {

    public static final String PREVIEW_REQUEST_JSON = "/ru/yandex/canvas/controllers/inBannerPreviewRequest.json";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @TestConfiguration
    public static class TestConf {
        @MockBean
        private VhService vhClient;

        @MockBean(name = ROTOR_CLIENT)
        private RotorClient rotorClient;

        @MockBean
        private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @MockBean
        private VideoGeometryService videoGeometryService;

        @Bean
        StockMoviesService stockMoviesService(MovieAndVideoSourceFactory movieAndVideoSourceFactory) {
            return new StockMoviesService(null, movieAndVideoSourceFactory);
        }

        @Bean
        MovieServiceInterface movieServiceInterface(VideoFilesRepository videoFilesRepository,
                                                    StockMoviesService stockMoviesService,
                                                    VideoFileUploadServiceInterface fileUploadService,
                                                    StillageService stillageService,
                                                    VideoLimitsService videoLimitsService,
                                                    DirectService directService,
                                                    MovieAndVideoSourceFactory movieAndVideoSourceFactory,
                                                    VideoPresetsService videoPresetsService) {
            return new MovieService(videoFilesRepository, stockMoviesService, fileUploadService,
                    stillageService, videoLimitsService, new DateTimeService(), directService, videoPresetsService,
                    cmsConversionStatusUpdateService, movieAndVideoSourceFactory, videoGeometryService);
        }

        @Bean
        public CanvasAuthInterceptor canvasAuthInterceptor(
                DirectService directService,
                BlackboxCookieAuthProvider blackboxCookieAuthProvider, AuthRequestParams authRequestParams,
                TvmIntegration tvmIntegration) {
            CanvasAuthInterceptor.Builder builder = CanvasAuthInterceptor.builder();

            return builder
                    .defaultAuth(List.of(TRUSTED_QUERY_STRING))
                    .register(AuthorizeBy.AuthType.BLACKBOX,
                            new BlackBoxAuthorizer(blackboxCookieAuthProvider, authRequestParams, tvmIntegration,
                                    TvmService.BLACKBOX_MIMINO))
                    .register(AuthorizeBy.AuthType.DIRECT_TOKEN, new DirectTokenAuthorizer(directService))
                    .register(TRUSTED_QUERY_STRING, new QueryStringAuthorizer(authRequestParams))
                    .register(OAUTH, new QueryStringAuthorizer(authRequestParams))
                    .register(AuthorizeBy.AuthType.TVM_TOKEN, new QueryStringAuthorizer(authRequestParams))
                    .register(AuthorizeBy.AuthType.SANDBOX_SECRET, new SandboxCallbackAuthorizer("hookSecret"))
                    .build();
        }

        @Bean
        public LocaleResolver localeResolver() {
            return new WebLocaleResolver();
        }
    }


    @Test
    public void testValidation() throws Exception {
        final String[] result = new String[1];

        mockMvc.perform(post("/creatives/in_banner")
                .param("client_id", "1")
                .param("user_id", "2")
                .header("Accept-Language", "en_US")
                .content(ResourceHelpers.getResource(PREVIEW_REQUEST_JSON))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(e -> System.err.println("!!"+  e.getResponse().getContentAsString()))
                .andDo(e -> result[0] = e.getResponse().getContentAsString())
                .andExpect(status().is(200));
    }

}
