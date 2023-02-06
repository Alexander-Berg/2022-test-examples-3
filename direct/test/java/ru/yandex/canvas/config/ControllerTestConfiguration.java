package ru.yandex.canvas.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.web.filter.CharacterEncodingFilter;

import ru.yandex.canvas.configs.CanvasAuthInterceptor;
import ru.yandex.canvas.configs.auth.AuthorizeBy;
import ru.yandex.canvas.configs.auth.BlackBoxAuthorizer;
import ru.yandex.canvas.configs.auth.DirectTokenAuthorizer;
import ru.yandex.canvas.configs.auth.QueryStringAuthorizer;
import ru.yandex.canvas.configs.auth.SandboxCallbackAuthorizer;
import ru.yandex.canvas.configs.auth.SandboxCleanupHandleAuthorizer;
import ru.yandex.canvas.controllers.video.PythonRedirect;
import ru.yandex.canvas.model.CreativeDocument;
import ru.yandex.canvas.repository.html5.BatchesRepository;
import ru.yandex.canvas.repository.video.VideoAdditionsRepository;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthRequestParams;
import ru.yandex.canvas.service.AuthService;
import ru.yandex.canvas.service.AvatarsService;
import ru.yandex.canvas.service.CreativesService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.MDSService;
import ru.yandex.canvas.service.RTBHostExportService;
import ru.yandex.canvas.service.ScreenshooterService;
import ru.yandex.canvas.service.SequenceService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.TvmIntegrationService;
import ru.yandex.canvas.service.scraper.ScraperService;
import ru.yandex.direct.tracing.TraceHelper;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmIntegrationStub;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.direct.web.auth.blackbox.BlackboxCookieAuthProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.canvas.configs.auth.AuthorizeBy.AuthType.OAUTH;
import static ru.yandex.canvas.configs.auth.AuthorizeBy.AuthType.TRUSTED_QUERY_STRING;

@TestConfiguration
@Import(VideoFilesServiceConfig.class)
public class ControllerTestConfiguration {

    @MockBean
    private BlackboxCookieAuthProvider blackboxCookieAuthProvider;

    @MockBean
    private TraceHelper traceHelper;

    @MockBean
    private StillageService stillageService;

    @MockBean
    private MDSService mdsService;

    @MockBean
    private ScreenshooterService screenshooterService;

    @MockBean
    private AuthService authService;

    @MockBean
    private ScraperService scraperService;

    @MockBean
    private AvatarsService avatarsService;

    @MockBean
    private RTBHostExportService rtbHostExportService;

    @Bean
    public CreativesService creativesService() {
        CreativesService mock = mock(CreativesService.class);
        when(mock.worksOn()).thenReturn(CreativeDocument.class);
        return mock;
    }

    @MockBean
    private BatchesRepository batchesRepository;

    @MockBean
    private SequenceService sequenceService;

    @MockBean
    private VideoAdditionsRepository videoAdditionsRepository;

    @MockBean
    private VideoFilesRepository videoFilesRepository;

    @MockBean
    private PythonRedirect pythonRedirect;

    @MockBean
    private SessionParams sessionParams;

    @MockBean
    private AuthRequestParams authRequestParams;

    @Bean
    public MongoOperations mongoOperations() {
        MongoOperations mongoOperations = mock(MongoOperations.class);
        when(mongoOperations.getConverter()).thenReturn(mock(MongoConverter.class));
        return mongoOperations;
    }

    @Bean
    public TvmIntegration tvmIntegration() {
        return new TvmIntegrationStub();
    }

    @Bean
    public TvmIntegrationService tvmIntegrationServiceStub(TvmIntegration tvmIntegration) {
        return new TvmIntegrationService(tvmIntegration, TvmService.DIRECT_INTAPI_TEST.getId());
    }

    @Bean
    @Primary
    public CanvasAuthInterceptor canvasAuthInterceptor(
            DirectService directService,
            BlackboxCookieAuthProvider blackboxCookieAuthProvider, AuthRequestParams authRequestParams,
            TvmIntegration tvmIntegration)
    {
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
                .register(AuthorizeBy.AuthType.SANDBOX_CLEANUP_TOKEN, new SandboxCleanupHandleAuthorizer("internalToken"))
                .build();
    }


    @Bean
    public MockMvcBuilderCustomizer defaultCharacterEncodingMockMvcBuilderCustomizer() {
        return builder -> builder.addFilters(
                new CharacterEncodingFilter(StandardCharsets.UTF_8.name(), true, true));
    }

}
