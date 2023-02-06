package ru.yandex.canvas.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import ru.yandex.canvas.repository.video.VideoAdditionsRepository;
import ru.yandex.canvas.service.DateTimeService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.PackshotService;
import ru.yandex.canvas.service.SequenceService;
import ru.yandex.canvas.service.screenshooters.VideoAdditionScreenshooterHelperService;
import ru.yandex.canvas.service.video.AudioService;
import ru.yandex.canvas.service.video.MovieServiceInterface;
import ru.yandex.canvas.service.video.PackshotServiceInterface;
import ru.yandex.canvas.service.video.VideoAdditionValidationService;
import ru.yandex.canvas.service.video.VideoAdditionsService;
import ru.yandex.canvas.service.video.VideoPresetsService;
import ru.yandex.canvas.service.video.VideoPreviewUrlBuilder;
import ru.yandex.canvas.service.video.files.StockMoviesService;
import ru.yandex.canvas.service.video.overlay.OverlayService;

@TestConfiguration
public class VideoAdditionsTestConfiguration {

    @MockBean
    private VideoPresetsService videoPresetsService;

    @MockBean
    private StockMoviesService stockMoviesService;

    @MockBean
    private MovieServiceInterface movieServiceInterface;

    @MockBean
    private PackshotService packshotService;

    @MockBean
    private DirectService directService;

    @MockBean
    private VideoPreviewUrlBuilder videoPreviewUrlBuilder;

    @Bean
    public LocalValidatorFactoryBean localValidatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }

    @MockBean
    private AudioService audioService;

    @MockBean
    private OverlayService overlayService;

    @MockBean
    private VideoAdditionScreenshooterHelperService videoAdditionScreenshooterHelperService;

    @Bean
    public VideoAdditionsService videoAdditionsService(SequenceService sequenceService,
                                                       VideoAdditionsRepository videoAdditionsRepository, VideoPresetsService videoPresetsService,
                                                       MovieServiceInterface movieService,
                                                       PackshotService packshotService, DirectService directService,
                                                       AudioService audioService,
                                                       VideoAdditionScreenshooterHelperService videoAdditionScreenshooterHelperService) {
        return new VideoAdditionsService(sequenceService, videoAdditionsRepository, videoPresetsService,
                "canvas.preview.host", movieService, packshotService, directService, new DateTimeService(),
                audioService, videoAdditionScreenshooterHelperService, videoPreviewUrlBuilder);
    }

    @Bean
    public VideoAdditionValidationService videoAdditionValidationService(VideoPresetsService videoPresetsService,
                                                                         MovieServiceInterface movieService,
                                                                         PackshotServiceInterface packshotService,
                                                                         AudioService audioService,
                                                                         DirectService directService) {
        return new VideoAdditionValidationService(videoPresetsService, movieService, packshotService, audioService,
                directService);
    }
}
