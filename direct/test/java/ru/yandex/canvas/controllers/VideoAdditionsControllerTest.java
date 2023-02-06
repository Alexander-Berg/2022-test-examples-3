package ru.yandex.canvas.controllers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
import ru.yandex.canvas.exceptions.InternalServerError;
import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.model.video.AudioFiles;
import ru.yandex.canvas.model.video.PythiaAdditionExtraParams;
import ru.yandex.canvas.model.video.PythiaParams;
import ru.yandex.canvas.model.video.addition.AdditionData;
import ru.yandex.canvas.model.video.addition.AdditionDataBundle;
import ru.yandex.canvas.model.video.addition.AdditionElement;
import ru.yandex.canvas.model.video.addition.options.BodyElementOptions;
import ru.yandex.canvas.model.video.files.MovieAndVideoSourceFactory;
import ru.yandex.canvas.repository.video.AudioFilesRepository;
import ru.yandex.canvas.repository.video.VideoAdditionsRepository;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthRequestParams;
import ru.yandex.canvas.service.AuthService;
import ru.yandex.canvas.service.DateTimeService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.ScreenshooterService;
import ru.yandex.canvas.service.SequenceService;
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
import ru.yandex.direct.screenshooter.client.model.ScreenShooterScreenshot;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.web.auth.blackbox.BlackboxCookieAuthProvider;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
public class VideoAdditionsControllerTest {

    public static final String CORRECT_VIDEO_ADDITION_VAST_XML =
            "/ru/yandex/canvas/controllers/correctVideoAdditionVast.xml";
    public static final String CORRECT_CPC_VAST_XML = "/ru/yandex/canvas/controllers/correctCpcVast.xml";
    public static final String CORRECT_AUDIO_VAST_XML = "/ru/yandex/canvas/controllers/correctAudioVast.xml";
    public static final String VIDEO_ADDITION_PREVIEW_JSON =
            "/ru/yandex/canvas/controllers/correctVideoAdditionPreview.json";
    public static final String CPC_PREVIEW_JSON =
            "/ru/yandex/canvas/controllers/correctCpcPreview.json";
    public static final String AUDIO_ADDITION_JSON = "/ru/yandex/canvas/controllers/correctAudioAddition.json";
    public static final String INVALID_ADDITION_JSON = "/ru/yandex/canvas/controllers/invalidAddition.json";
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

    @MockBean
    private DirectService directService;

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
    public void getAdditionPreview() throws Exception {
        List<AdditionElement> elementList = Arrays.asList(
                new AdditionElement(AdditionElement.ElementType.BUTTON)
                        .withAvailable(true)
                        .withOptions(
                                new BodyElementOptions().setText("Body text")
                                        .setBackgroundColor("#00ffaa")
                                        .setTextColor("#deadbb")
                                        .setPlaceholder("placeholder")
                        )
        );

        AdditionData additionData = new AdditionData()
                .setBundle(new AdditionDataBundle().setName("MyBloodyBundle"))
                .setElements(elementList);

        Addition addition = new Addition()
                .setClientId(1L)
                .setArchive(false)
                .setVast("<xml>${AUCTION_DC_PARAMS}</xml>")
                .setPresetId(1L)
                .setData(additionData);

        Mockito.when(videoAdditionsRepository.getAdditionByIdArchivedAlso("12")).thenReturn(addition);

        //TODO check json
        mockMvc.perform(post("/video/additions/12/preview")
                .content(
                        "{\"previewData\":{\"title\":\"1232\", \"body\": \"body1\", \"domain\": \"domain.ru\", "
                                + "\"url\":\"http://ya"
                                + ".ru\",\"images\":{\"bid\":12}}}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(e -> System.err.println("!!!" + e.getResponse().getContentAsString()))
                .andExpect(status().is(200));
    }

    @Test
    public void survey() throws Exception {
        Mockito.when(sequenceService.getNextCreativeIdsList(Mockito.anyInt())).thenReturn(Arrays.asList(12L));

        PythiaAdditionExtraParams pythiaAdditionExtraParams = new PythiaAdditionExtraParams()
                .setPythia(new PythiaParams()
                        .setBasePath("https://yandex.ru/poll/video")
                        .setExtra("from_display=1&flight=2&bl_adg=tst&saw_adv=1")
                        .setSlug("8Pz4Do9dqUhpRhBpNG6HQR"))
                .setSkipUrl("https://yandex.ru/poll/api/v0/survey/8Pz4Do9dqUhpRhBpNG6HQR/skipPixel")
                .setVpaidPcodeUrl("https://yastatic.net/pcode/media/vpaid-pythia-survey-embed.js")
                .setVideoId("new_0_0-077.mov");
        String content = JsonUtils.toJson(pythiaAdditionExtraParams);

        ScreenShooterScreenshot screenshot = new ScreenShooterScreenshot()
                .withUrl("http://example.screen.shot")
                .withIsDone(true);

        Mockito.when(screenshooterService.getScreenshotFromHtml(anyString(), anyLong(), anyLong()))
                .thenReturn(screenshot);

        String resp = mockMvc.perform(
                post("/video/additions/survey")
                        .param("client_id", "103997791")
                        .param("user_id", "103997791")
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(resp).contains("${AUCTION_DC_PARAMS}");
        assertThat(resp).contains("\"survey-config\":${SURVEY_CONFIG}");
        System.out.println(resp);
    }


    @Test
    public void getPreviewCpc() throws Exception {
        getPreview(CORRECT_CPC_VAST_XML, CPC_PREVIEW_JSON);
    }

    @Test
    public void getPreviewVideoAdditions() throws Exception {
        getPreview(CORRECT_VIDEO_ADDITION_VAST_XML, VIDEO_ADDITION_PREVIEW_JSON);
    }

    private void getPreview(String correctVastResource, String additionsPreviewJsonResource) throws Exception {
        Mockito.when(sequenceService.getNextCreativeIdsList(Mockito.anyInt())).thenReturn(Arrays.asList(12L));

        String correctVast = ResourceHelpers.getResource(correctVastResource);

        ScreenShooterScreenshot screenshot = new ScreenShooterScreenshot()
                .withUrl("http://example.screen.shot")
                .withIsDone(true);

        Mockito.when(screenshooterService.getScreenshotFromHtml(anyString(), anyLong(), anyLong()))
                .thenReturn(screenshot);

        final String[] result = new String[1];

        //TODO check json
        mockMvc.perform(post("/video/additions/preview")
                .param("client_id", "1")
                .param("user_id", "2")
                .content(ResourceHelpers.getResource(additionsPreviewJsonResource))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(e -> result[0] = e.getResponse().getContentAsString())
                //.andExpect( jsonPath("$.vast", equalTo(correctVast) ))
                .andExpect(status().is(200));

        Map<String, String> map = new ObjectMapper().readValue(result[0], Map.class);

        assertThat(map.containsKey("vast")).describedAs("Vast is present").isTrue();
        String expected = correctVast.replaceAll("\\s+", "");
        String actual = map.get("vast").replaceAll("\\s+", "");
        assertEquals("vast is correct", expected, actual);
    }


    @Test
    public void getAdditionPreviewWithUnknownIdTest() throws Exception {
        Mockito.when(videoAdditionsRepository.getAdditionByIdArchivedAlso("12")).thenReturn(null);

        mockMvc.perform(post("/video/additions/12/preview")
                .content(
                        "{\"previewData\":{\"title\":\"1232\", \"body\": \"body1\", \"domain\": \"domain.ru\", "
                                + "\"url\":\"http://ya"
                                + ".ru\",\"images\":{\"bid\":12}}}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(404));
    }

    @Test
    public void getAdditionWithDirect500() throws Exception {

        doAnswer(
                invocation -> {
                    throw new InternalServerError("Auth response is not 200 ()");
                }
        ).when(authService).requirePermission(any());

        Mockito.when(videoAdditionsRepository.getAdditionById("12")).thenReturn(null);

        mockMvc.perform(get("/video/additions/12")
                .param("client_id", "1")
                .param("user_id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(500));
    }

    @Test
    public void createAdditionAudio() throws Exception {
        Mockito.when(sequenceService.getNextCreativeIdsList(Mockito.anyInt())).thenReturn(Arrays.asList(12L));
        String correctAudioVast = ResourceHelpers.getResource(CORRECT_AUDIO_VAST_XML);

        AudioFiles files = new AudioFiles();
        files.setDuration(27.0);
        files.setId("5ce51b706b4f424cc0113b4b");
        files.setName("example15.mp3");
        Mockito.when(audioFilesRepository.findById(any(), any())).thenReturn(files);

        String resp = mockMvc.perform(post("/video/additions")
                .param("client_id", "1")
                .param("user_id", "2")
                .content(ResourceHelpers.getResource(AUDIO_ADDITION_JSON))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, String> map = new ObjectMapper().readValue(resp, Map.class);
        assertThat(map.containsKey("vast")).describedAs("Vast is present").isTrue();
        String expected = correctAudioVast.replaceAll("\\s+", "");
        String actual = map.get("vast").replaceAll("\\s+", "");
        assertEquals("vast is correct", expected, actual);
    }

    @Test
    public void testValidation() throws Exception {
        Mockito.when(sequenceService.getNextCreativeIdsList(Mockito.anyInt())).thenReturn(Arrays.asList(12L));
        AudioFiles files = new AudioFiles();
        files.setDuration(27.3);
        files.setId("5ce51b706b4f424cc0113b4b");
        files.setName("example15.mp3");
        Mockito.when(audioFilesRepository.findById(any(), any())).thenReturn(files);

        mockMvc.perform(post("/video/additions")
                .param("client_id", "1")
                .param("user_id", "2")
                .header("Accept-Language", "en_US")
                .content(ResourceHelpers.getResource(INVALID_ADDITION_JSON))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
    }

}
