package ru.yandex.canvas.service.video;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;
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
import ru.yandex.canvas.model.video.addition.options.ButtonElementOptions;
import ru.yandex.canvas.model.video.files.Movie;
import ru.yandex.canvas.model.video.files.StreamFormat;
import ru.yandex.canvas.service.TankerKeySet;
import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.PresetTheme;
import ru.yandex.canvas.service.video.presets.VideoPreset;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.context.i18n.LocaleContextHolder.setLocale;
import static ru.yandex.canvas.steps.AdditionsSteps.defaultButtonElementOptions;
import static ru.yandex.canvas.steps.AdditionsSteps.defaultVideoId;
import static ru.yandex.canvas.steps.AdditionsSteps.leastAddition;
import static ru.yandex.canvas.steps.PresetDescriptionsSteps.leastPresetDescription;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@Import({ControllerTestConfiguration.class, VideoAdditionsTestConfiguration.class})
public class ButtonValidateTest {
    private TankerKeySet keyset = TankerKeySet.VIDEO_VALIDATION_MESSAGES;

    public static final long PRESET_ID = 1000L;
    public static final long CLIENT_ID = 12341L;
    public static final long OTHER_CLIENT_ID = 56743L;
    public static final String VIDEO_ID = defaultVideoId();

    @Autowired
    private VideoPresetsService videoPresetsService;

    @Autowired
    private MovieServiceInterface movieService;

    @Autowired
    private VideoAdditionValidationService videoAdditionValidationService;

    @Before
    public void setupMocks() {
        setLocale(new Locale("en", "US"));

        PresetDescription presetDescription = leastPresetDescription(PRESET_ID);
        presetDescription.setButtonPresent(true);
        presetDescription.setButtonTextPresent(true);
        presetDescription.setPresetTheme(PresetTheme.LAKE);
        VideoPreset videoPreset = new VideoPreset(presetDescription);

        Movie movie = mock(Movie.class);
        StreamFormat streamFormat = mock(StreamFormat.class);
        when(movie.getFormats()).thenReturn(Arrays.asList(streamFormat));
        when(movie.isReady()).thenReturn(true);

        Mockito.when(movieService.lookupMovie(VIDEO_ID, null, CLIENT_ID, PRESET_ID))
                .thenReturn(movie);

        Mockito.when(videoPresetsService.getPreset(PRESET_ID)).thenReturn(videoPreset);
        Mockito.when(videoPresetsService.contains(PRESET_ID)).thenReturn(true);
    }

    protected Addition makeValidAddition() {
        Options buttonElementOptions = defaultButtonElementOptions();
        buttonElementOptions.setText("Некий текст на ");

        AdditionElement elementButton = new AdditionElement(AdditionElement.ElementType.BUTTON)
                .withAvailable(true)
                .withOptions(buttonElementOptions);

        Addition addition = leastAddition(CLIENT_ID, PRESET_ID, defaultVideoId(), elementButton);

        return addition;
    }

    @Test
    public void noBundleBadColorTest() {
        Addition addition = makeValidAddition();
        addition.getData().setBundle(null);
        addition.findElementByType(AdditionElement.ElementType.BUTTON).getOptions().setTextColor("GHRR");
        validateAndCheckForMessage(addition, "invalid_color_value");
    }

    @Test
    public void noBundleWithBgColorTest() {
        Addition addition = makeValidAddition();
        addition.getData().setBundle(null);
        addition.findElementByType(AdditionElement.ElementType.BUTTON).getOptions().setBackgroundColor("#000000");
        validateAndCheckForMessage(addition, "field_is_not_null");
    }

    @Test
    public void placeholderTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.BUTTON).getOptions().setPlaceholder("SomeText");
        videoAdditionValidationService.validate(addition);
        // ok
    }

    @Test
    public void badTextColorTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.BUTTON).getOptions().setTextColor("#0000001");
        validateAndCheckForMessage(addition, "invalid_color_value");
    }

    @Test
    public void noTextTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.BUTTON).getOptions().setText(null);
        validateAndCheckForMessage(addition, "org.hibernate.validator.constraints.NotBlank.message");
    }

    @Test
    public void shortTextTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.BUTTON).getOptions().setText("");
        validateAndCheckForMessage(addition, "javax.validation.constraints.Size.message", 1, 17);
    }

    @Test
    public void longTextTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.BUTTON).getOptions()
                .setText(StringUtils.repeat('Я', 100));
        validateAndCheckForMessage(addition, "javax.validation.constraints.Size.message", 1, 17);
    }

    @Test
    public void badBorderColorTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.BUTTON).getOptions().setBorderColor("#0000001");

        assertTrue("Bad border color ignored", !videoAdditionValidationService.validate(addition).hasErrors());
        // TODO: We are ignoring border color for some reasons maybe it aint right? Check what's getting stored in
        // python
    }

    @Test
    public void badColorTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.BUTTON).getOptions().setColor("#0000001");
        validateAndCheckForMessage(addition, "invalid_color_value");
    }

    @Test
    public void withVideoIdTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.BUTTON).getOptions().setVideoId("12432");
        validateAndCheckForMessage(addition, "field_is_not_null");
    }

    @Test
    public void buttonWithSameColorsTest() {
        AdditionElement elementButton = new AdditionElement(AdditionElement.ElementType.BUTTON).withOptions(
                new ButtonElementOptions().setText("abc")
                        .setTextColor("#0ff002")
                        .setColor("#0ff002")
                        .setBorderColor("#0ff002") // will be ignored
        ).withAvailable(true);

        Addition addition = leastAddition(CLIENT_ID, PRESET_ID, defaultVideoId(), elementButton);

        validateAndCheckForMessage("Цвета недостаточно различаются", addition,
                Matchers.contains("Colors are not different enough", "Colors are not different enough"));
    }

    private void validateAndCheckForMessage(Addition addition, String message, Object... messageArgs) {
        validateAndCheckForMessage(message, addition, Matchers.contains(MessageFormat.format(keyset.key(message), messageArgs)));
    }

    private void validateAndCheckForMessage(String message, Addition addition, Matcher matcher) {
        BindingResult validationResult = videoAdditionValidationService.validate(addition);
        assertThat("got " + message, vrMessages(validationResult),
                matcher);
    }

    private List<String> vrMessages(BindingResult validationResult) {
        return validationResult.getAllErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.toList());
    }
}
