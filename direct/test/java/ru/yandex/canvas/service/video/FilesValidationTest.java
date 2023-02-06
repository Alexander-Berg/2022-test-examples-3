package ru.yandex.canvas.service.video;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.BindingResult;

import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.config.VideoAdditionsTestConfiguration;
import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.model.video.addition.AdditionElement;
import ru.yandex.canvas.model.video.addition.options.AdditionElementOptions;
import ru.yandex.canvas.model.video.files.AudioSource;
import ru.yandex.canvas.model.video.files.Movie;
import ru.yandex.canvas.model.video.files.StreamFormat;
import ru.yandex.canvas.model.video.files.VideoSource;
import ru.yandex.canvas.service.TankerKeySet;
import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.VideoPreset;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.canvas.steps.AdditionsSteps.leastAddition;
import static ru.yandex.canvas.steps.PresetDescriptionsSteps.leastPresetDescription;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@Import({ControllerTestConfiguration.class, VideoAdditionsTestConfiguration.class})
public class FilesValidationTest {
    private TankerKeySet keyset = TankerKeySet.VIDEO_VALIDATION_MESSAGES;

    private static final long CLIENT_ID = 12345L;
    private static final long PRESET_ID = 1L;
    private static final long NO_STOCK_PRESET_ID = 2L;

    @Autowired
    private VideoPresetsService videoPresetsService;

    @Autowired
    private VideoAdditionValidationService videoAdditionValidationService;

    @Autowired
    private MovieServiceInterface movieService;

    @Before
    public void prepare() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Before
    public void setupMocks() {

        PresetDescription description = leastPresetDescription(PRESET_ID);
        description.setAllowStockVideo(true);
        Mockito.when(videoPresetsService.getPreset(PRESET_ID)).thenReturn(new VideoPreset(
                description));
        Mockito.when(videoPresetsService.contains(PRESET_ID)).thenReturn(true);

        PresetDescription nonStockDescription = leastPresetDescription(NO_STOCK_PRESET_ID);
        Mockito.when(videoPresetsService.getPreset(NO_STOCK_PRESET_ID)).thenReturn(new VideoPreset(
                nonStockDescription));
        Mockito.when(videoPresetsService.contains(NO_STOCK_PRESET_ID)).thenReturn(true);
    }

    @Test
    public void unknownFileTest() {
        Addition addition = dummyAddition("video1", "audio1");
        Mockito.when(movieService.lookupMovie("video1", null, CLIENT_ID, PRESET_ID)).thenReturn(null);
        validateAndCheckForMessage(addition, "video_file_not_found");
    }

    @Test
    public void nonReadyFileTest() {
        Addition addition = dummyAddition("video1", "audio1");

        VideoSource videoSource = mock(VideoSource.class);
        AudioSource audioSource = mock(AudioSource.class);
        Movie movie = mock(Movie.class);

        Mockito.when(movieService.lookupMovie("video1", "audio1", CLIENT_ID, PRESET_ID)).thenReturn(movie);

        Mockito.when(movie.getAudioSource()).thenReturn(audioSource);
        Mockito.when(movie.getVideoSource()).thenReturn(videoSource);

        Mockito.when(movie.isReady()).thenReturn(false);
        Mockito.when(movie.isStock()).thenReturn(true);

        validateAndCheckForMessage(addition, "video_file_not_ready");
    }

    @Test
    public void nonAllowedStockFiles() {
        Addition addition = dummyAddition("video1", "audio1");
        addition.setPresetId(NO_STOCK_PRESET_ID);

        VideoSource videoSource = mock(VideoSource.class);
        AudioSource audioSource = mock(AudioSource.class);
        Movie movie = mock(Movie.class);

        Mockito.when(movieService.lookupMovie("video1", "audio1", CLIENT_ID, NO_STOCK_PRESET_ID)).thenReturn(movie);

        Mockito.when(movie.getAudioSource()).thenReturn(audioSource);
        Mockito.when(movie.getVideoSource()).thenReturn(videoSource);

        StreamFormat streamFormat = mock(StreamFormat.class);
        when(movie.getFormats()).thenReturn(Arrays.asList(streamFormat));
        Mockito.when(movie.isReady()).thenReturn(true);
        Mockito.when(movie.isStock()).thenReturn(true);
        validateAndCheckForMessage(addition, "stock_video_not_allowed");
    }

    @Test
    public void nonAllowedAudioFiles() {
        Addition addition = dummyAddition("video1", "audio1");

        Movie movie = mock(Movie.class);
        Mockito.when(movieService.lookupMovie("video1", "audio1", CLIENT_ID, PRESET_ID)).thenReturn(movie);

        StreamFormat streamFormat = mock(StreamFormat.class);
        when(movie.getFormats()).thenReturn(Arrays.asList(streamFormat));
        Mockito.when(movie.isReady()).thenReturn(true);

        Mockito.when(movie.isStock()).thenReturn(false);

        validateAndCheckForMessage(addition, "audio_should_be_empty");
    }

    @Test
    public void noAudioFile() {
        Addition addition = dummyAddition("video1", "audio1");

        VideoSource videoSource = mock(VideoSource.class);
        Movie movie = mock(Movie.class);

        Mockito.when(movieService.lookupMovie("video1", "audio1", CLIENT_ID, PRESET_ID)).thenReturn(movie);
        Mockito.when(movie.getVideoSource()).thenReturn(videoSource);
        Mockito.when(movie.getAudioSource()).thenReturn(null);

        StreamFormat streamFormat = mock(StreamFormat.class);
        when(movie.getFormats()).thenReturn(Arrays.asList(streamFormat));
        Mockito.when(movie.isReady()).thenReturn(true);
        Mockito.when(movie.isStock()).thenReturn(true);

        validateAndCheckForMessage(addition, "audio_file_not_found");
    }

    @Test
    public void ok() {

        Addition addition = dummyAddition("video1", "audio1");

        VideoSource videoSource = mock(VideoSource.class);
        AudioSource audioSource = mock(AudioSource.class);
        Movie movie = mock(Movie.class);

        Mockito.when(movieService.lookupMovie("video1", "audio1", CLIENT_ID, PRESET_ID)).thenReturn(movie);

        Mockito.when(movie.getAudioSource()).thenReturn(audioSource);
        Mockito.when(movie.getVideoSource()).thenReturn(videoSource);

        StreamFormat streamFormat = mock(StreamFormat.class);
        when(movie.getFormats()).thenReturn(Arrays.asList(streamFormat));

        Mockito.when(movie.isReady()).thenReturn(true);
        Mockito.when(movie.isStock()).thenReturn(true);

        assertTrue("Addition proper files passes validation",
                !videoAdditionValidationService.validate(addition).hasErrors());
    }


    private void validateAndCheckForMessage(Addition addition, String message, Object... messageArgs) {
        BindingResult validationResult = videoAdditionValidationService.validate(addition);
        assertThat("got " + message, vrMessages(validationResult),
                Matchers.contains(MessageFormat.format(keyset.key(message), messageArgs)));
    }

    private List<String> vrMessages(BindingResult validationResult) {
        return validationResult.getAllErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.toList());
    }

    Addition dummyAddition(String videoId, String audioId) {
        Addition addition = leastAddition(CLIENT_ID, PRESET_ID, videoId);

        AdditionElement element = new AdditionElement(AdditionElement.ElementType.ADDITION).withOptions(
                new AdditionElementOptions().setVideoId(videoId).setAudioId(audioId)
        ).withAvailable(true);

        addition.getData().setElements(Collections.singletonList(element));

        return addition;
    }
}
