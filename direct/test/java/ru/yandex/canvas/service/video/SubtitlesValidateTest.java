package ru.yandex.canvas.service.video;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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
import ru.yandex.canvas.model.video.addition.Options;
import ru.yandex.canvas.model.video.addition.options.SubtitlesElementOptions;
import ru.yandex.canvas.model.video.files.Movie;
import ru.yandex.canvas.model.video.files.StreamFormat;
import ru.yandex.canvas.service.TankerKeySet;
import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.VideoPreset;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.context.i18n.LocaleContextHolder.setLocale;
import static ru.yandex.canvas.steps.AdditionsSteps.defaultVideoId;
import static ru.yandex.canvas.steps.AdditionsSteps.leastAddition;
import static ru.yandex.canvas.steps.PresetDescriptionsSteps.leastPresetDescription;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@Import({ControllerTestConfiguration.class, VideoAdditionsTestConfiguration.class})
public class SubtitlesValidateTest {
    private final TankerKeySet keyset = TankerKeySet.VIDEO_VALIDATION_MESSAGES;

    public static final long PRESET_ID = 6L;
    public static final long CLIENT_ID = 12689L;
    public static final String VIDEO_ID = defaultVideoId();

    @Autowired
    private VideoPresetsService videoPresetsService;

    @Autowired
    private MovieServiceInterface movieService;

    @Autowired
    private VideoAdditionValidationService videoAdditionValidationService;

    @Before
    public void prepare() {
        Locale.setDefault(Locale.forLanguageTag("en"));
        setLocale(Locale.forLanguageTag("en"));
    }

    @Before
    public void setupMocks() {
        PresetDescription presetDescription = leastPresetDescription(PRESET_ID);
        presetDescription.setSubtitlesPresent(true);
        VideoPreset videoPreset = new VideoPreset(presetDescription);

        Movie movie = mock(Movie.class);
        StreamFormat streamFormat = mock(StreamFormat.class);

        when(movie.getFormats()).thenReturn(Collections.singletonList(streamFormat));
        when(movie.isReady()).thenReturn(true);

        Mockito.when(movieService.lookupMovie(VIDEO_ID, null, CLIENT_ID, PRESET_ID))
                .thenReturn(movie);

        Mockito.when(videoPresetsService.getPreset(PRESET_ID)).thenReturn(videoPreset);
        Mockito.when(videoPresetsService.contains(PRESET_ID)).thenReturn(true);
    }

    @Test
    public void noBundleBadColorTest() {
        Addition addition = makeValidAddition();
        addition.getData().setBundle(null);
        addition.findElementByType(AdditionElement.ElementType.SUBTITLES).getOptions().setTextColor("GHRR");
        validateAndCheckForMessage(addition, "invalid_color_value");
    }

    @Test
    public void noTextColorTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.SUBTITLES).getOptions().setTextColor(null);
        BindingResult validationResult = videoAdditionValidationService.validate(addition);
        assertFalse("Valid addition passes validation", validationResult.hasErrors());
    }

    @Test
    public void badTextColorTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.SUBTITLES).getOptions().setTextColor("#0000001");
        validateAndCheckForMessage(addition, "invalid_color_value");
    }

    @Test
    public void noBgColorTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.SUBTITLES).getOptions().setBackgroundColor(null);
        BindingResult validationResult = videoAdditionValidationService.validate(addition);
        assertFalse("Valid addition passes validation", validationResult.hasErrors());
    }

    @Test
    public void badBgColorTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.SUBTITLES).getOptions().setBackgroundColor("121234");
        validateAndCheckForMessage(addition, "invalid_color_value");
    }

    @Test
    public void placeholderTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.SUBTITLES).getOptions().setPlaceholder("SomeText");
        videoAdditionValidationService.validate(addition);
    }

    @Test
    public void withTextTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.SUBTITLES).getOptions()
                .setText("WEBVTT\n" +
                        "\n" +
                        "00:01.000 --> 00:02.000\n" +
                        "Test1\n" +
                        "\n" +
                        "00:02.000 --> 00:03.000\n" +
                        "Test2");
        BindingResult validationResult = videoAdditionValidationService.validate(addition);
        assertFalse("Valid addition passes validation", validationResult.hasErrors());
    }

    @Test
    public void withTextTestUnfinished() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.SUBTITLES).getOptions()
                .setText("WEBVTT\n" +
                        "\n" +
                        "00:01.000 --> 00:02.000\n" +
                        "Test1\n" +
                        "\n" +
                        "00:02.000 --> 00:03.000");
        validateAndCheckForMessage(addition, "invalid_subtitles_format");
    }

    @Test
    public void withTextTestNoTimestamps() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.SUBTITLES).getOptions()
                .setText("WEBVTT\n" +
                        "\n" +
                        "Test1");
        validateAndCheckForMessage(addition, "invalid_subtitles_format");
    }

    @Test
    public void withTextTestBadTimestamps() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.SUBTITLES).getOptions()
                .setText("WEBVTT\n" +
                        "\n" +
                        "00:01.000 -> 00:002.000\n" +
                        "Test1");
        validateAndCheckForMessage(addition, "invalid_subtitles_format");
    }

    @Test
    public void noTextTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.SUBTITLES).getOptions().setText(null);
        BindingResult validationResult = videoAdditionValidationService.validate(addition);
        assertFalse("Valid addition passes validation", validationResult.hasErrors());
    }

    @Test
    public void longTextTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.SUBTITLES).getOptions()
                .setText("WEBVTT" + StringUtils.repeat("\n\n00:01.000 --> 00:02.000\nTest1", 10001));
        validateAndCheckForMessage(addition, "javax.validation.constraints.Size.message", 1, 10000);
    }

    @Test
    public void withVideoIdTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.SUBTITLES).getOptions().setVideoId("12432");
        validateAndCheckForMessage(addition, "field_is_not_null");
    }

    private void validateAndCheckForMessage(Addition addition, String message, Object... messageArgs) {
        BindingResult validationResult = videoAdditionValidationService.validate(addition);
        assertThat("got " + message, vrMessages(validationResult),
                Matchers.contains(MessageFormat.format(keyset.key(message), messageArgs)));
    }

    private List<String> vrMessages(BindingResult validationResult) {
        return validationResult.getAllErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.toList());
    }

    private Addition makeValidAddition() {
        Options subtitlesElementOptions = new SubtitlesElementOptions()
                .setTextColor("#1F00FF")
                .setBackgroundColor("#FF00DD");

        AdditionElement subtitlesElement = new AdditionElement(AdditionElement.ElementType.SUBTITLES)
                .withAvailable(true)
                .withOptions(subtitlesElementOptions);

        return leastAddition(CLIENT_ID, PRESET_ID, VIDEO_ID, subtitlesElement);
    }
}
