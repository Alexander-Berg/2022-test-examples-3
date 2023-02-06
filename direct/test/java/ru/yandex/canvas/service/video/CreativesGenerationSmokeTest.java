package ru.yandex.canvas.service.video;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.config.VideoFilesServiceConfig;
import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.model.video.files.MovieAndVideoSourceFactory;
import ru.yandex.canvas.repository.video.StockVideoAdditionsRepository;
import ru.yandex.canvas.repository.video.VideoAdditionsRepository;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthRequestParams;
import ru.yandex.canvas.service.DateTimeService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.PackshotService;
import ru.yandex.canvas.service.ScreenshooterService;
import ru.yandex.canvas.service.SequenceService;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.screenshooters.VideoAdditionScreenshooterHelperService;
import ru.yandex.canvas.service.video.files.StockMoviesService;
import ru.yandex.canvas.service.video.overlay.OverlayService;
import ru.yandex.canvas.service.video.presets.PresetTag;
import ru.yandex.direct.screenshooter.client.model.ScreenShooterScreenshot;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@Import({ControllerTestConfiguration.class, VideoFilesServiceConfig.class})
public class CreativesGenerationSmokeTest {

    @Autowired
    CreativesGenerationService creativesGenerationService;

    @Autowired
    private VideoAdditionsService videoAdditionsService;

    @Autowired
    private SequenceService sequenceService;

    @Autowired
    private ScreenshooterService screenshooterService;

    @Autowired
    private VideoAdditionScreenshooterHelperService videoAdditionScreenshooterHelperService;

    @TestConfiguration
    public static class TestConf {
        @MockBean
        private VhService vhClient;

        @MockBean
        private StockVideoAdditionsRepository stockVideoAdditionsRepository;

        @MockBean
        private VideoCreativesService videoCreativesService;

        @MockBean
        public VideoFileUploadServiceInterface videoFileUploadServiceInterface;

        @MockBean
        private PackshotService packshotService;

        @MockBean
        private DirectService directService;

        @MockBean
        private VideoLimitsService videoLimitsService;

        @MockBean
        private AudioService audioService;

        @MockBean
        private OverlayService overlayService;

        @Autowired
        private AuthRequestParams authRequestParams;

        @MockBean
        private CmsConversionStatusUpdateService cmsConversionStatusUpdateService;

        @MockBean
        private VideoPreviewUrlBuilder videoPreviewUrlBuilder;

        @MockBean
        private VideoAdditionScreenshooterHelperService videoAdditionScreenshooterHelperService;

        @MockBean
        private VideoGeometryService videoGeometryService;

        @Bean
        public VideoPresetsService videoPresetsService(VideoLimitsService videoLimitsService) {
            return new VideoPresetsService(videoLimitsService, directService, authRequestParams);
        }

        @Bean
        public CreativesGenerationService creativesGenerationService(StockMoviesService stockMoviesService,
                                                                     VideoPresetsService presetsService,
                                                                     VideoAdditionsService videoAdditionsService,
                                                                     VideoAdditionsRepository videoAdditionsRepository,
                                                                     StockVideoAdditionsRepository stockVideoAdditionsRepository,
                                                                     VideoCreativesService videoCreativesService) {
            return new CreativesGenerationService(stockMoviesService,
                    presetsService,
                    videoAdditionsService,
                    videoAdditionsRepository,
                    stockVideoAdditionsRepository,
                    videoCreativesService);
        }


        @Bean
        public MovieServiceInterface movieService(VideoFilesRepository videoFilesRepository,
                                                  StockMoviesService stockMoviesService,
                                                  VideoFileUploadServiceInterface fileUploadService,
                                                  StillageService stillageService,
                                                  VideoLimitsService videoLimitsService,
                                                  VideoPresetsService presetsService,
                                                  CmsConversionStatusUpdateService cmsConversionStatusUpdateService,
                                                  MovieAndVideoSourceFactory movieAndVideoSourceFactory,
                                                  VideoGeometryService videoGeometryService) {
            return new MovieService(videoFilesRepository, stockMoviesService, fileUploadService,
                    stillageService, videoLimitsService, new DateTimeService(), directService, presetsService,
                    cmsConversionStatusUpdateService, movieAndVideoSourceFactory, videoGeometryService);
        }

        @Bean
        public VideoAdditionsService videoAdditionsService(SequenceService sequenceService,
                                                           VideoAdditionsRepository videoAdditionsRepository,
                                                           VideoPresetsService videoPresetsService,
                                                           MovieServiceInterface movieService,
                                                           PackshotService packshotService, DirectService directService,
                                                           AudioService audioService,
                                                           VideoAdditionScreenshooterHelperService videoAdditionScreenshooterHelperService) {
            return new VideoAdditionsService(sequenceService, videoAdditionsRepository, videoPresetsService,
                    "canvas.preview.host", movieService, packshotService, directService,
                    new DateTimeService(), audioService, videoAdditionScreenshooterHelperService,
                    videoPreviewUrlBuilder);
        }
    }

    void mockScreenShooter() {
        ScreenShooterScreenshot screenshot = new ScreenShooterScreenshot()
                .withUrl("https://my.screen.shot/1?a=12")
                .withIsDone(true);
        when(videoAdditionScreenshooterHelperService.getScreenshot(Mockito.any(Addition.class), Mockito.isNull(),
                anyLong()))
                .thenReturn(screenshot);
    }

    @Test
    public void checkGeneration2() throws IOException, URISyntaxException {
        CreativesGenerationService.GenerateCondition generateCondition =
                new CreativesGenerationService.GenerateCondition(
                        PresetTag.COMMON, Arrays.asList("200063733"), Arrays.asList("ru_RU"), 20, true);

        CreativesGenerationService.GenerateCondition noVideoCondition =
                new CreativesGenerationService.GenerateCondition(
                        PresetTag.CPM, Arrays.asList("not_existing_id"), Arrays.asList("ru_RU"), 20, true);

        CreativesGenerationService.GenerateCondition zeroCategory = new CreativesGenerationService.GenerateCondition(
                PresetTag.CPM, Arrays.asList("0"), Arrays.asList("en_US"), 1000, true);

        mockScreenShooter();

        Mockito.when(sequenceService.getNextCreativeIdsList(Mockito.anyInt())).thenReturn(Arrays.asList(12L));

        List<List<Addition>> result = creativesGenerationService
                .generateAdditions(Arrays.asList(generateCondition, noVideoCondition, zeroCategory), 12L);

        //assertThat("generated")

        assertThat("Rows count is correct", result, hasSize(3));
        assertThat("First row size", result.get(0), hasSize(20));
        assertThat("Second row size", result.get(1), hasSize(20));
        assertThat("Third row size", result.get(2), hasSize(1000));

    }

}
