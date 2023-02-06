package ru.yandex.canvas.service.video;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import ru.yandex.canvas.model.video.addition.Options;
import ru.yandex.canvas.model.video.addition.options.AgeElementOptions;
import ru.yandex.canvas.model.video.files.Movie;
import ru.yandex.canvas.model.video.files.StreamFormat;
import ru.yandex.canvas.service.TankerKeySet;
import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.VideoPreset;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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
public class AgeValidateTest {
    private TankerKeySet keyset = TankerKeySet.VIDEO_VALIDATION_MESSAGES;

    private static final String VIDEO_ID = defaultVideoId();
    private static final long CLIENT_ID = 12345L;
    public static final long PRESET_ID = 1000L;

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
        Movie movie = mock(Movie.class);
        StreamFormat streamFormat = mock(StreamFormat.class);

        when(movie.getFormats()).thenReturn(Arrays.asList(streamFormat));
        when(movie.isReady()).thenReturn(true);

        Mockito.when(movieService.lookupMovie(VIDEO_ID, null, CLIENT_ID, PRESET_ID))
                .thenReturn(movie);

        Mockito.when(videoPresetsService.getPreset(PRESET_ID)).thenReturn(selectableAgeVideoPreset(PRESET_ID));
        Mockito.when(videoPresetsService.contains(PRESET_ID)).thenReturn(true);
    }

    @Test
    public void noBundleBadColorTest() {
        Addition addition = makeValidAddition();
        addition.getData().setBundle(null);
        addition.findElementByType(AdditionElement.ElementType.AGE).getOptions().setTextColor("GHRR");
        validateAndCheckForMessage(addition, "invalid_color_value");
    }

    @Test
    public void noTextColorTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.AGE).getOptions().setTextColor(null);
        BindingResult validationResult = videoAdditionValidationService.validate(addition);
        assertTrue("Valid addition passes validation", !validationResult.hasErrors());
    }

    @Test
    public void noBgColorTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.AGE).getOptions().setBackgroundColor(null);
        BindingResult validationResult = videoAdditionValidationService.validate(addition);
        assertTrue("Valid addition passes validation", !validationResult.hasErrors());
    }

    @Test
    public void badBgColorTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.AGE).getOptions().setBackgroundColor("121234");
        validateAndCheckForMessage(addition, "invalid_color_value");
    }

    @Test
    public void withTextTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.AGE).getOptions().setText("Привет мир");
        BindingResult validationResult = videoAdditionValidationService.validate(addition);
        assertTrue("Valid addition passes validation", !validationResult.hasErrors());
    }

    @Test
    public void withVideoIdTest() {
        Addition addition = makeValidAddition();
        addition.findElementByType(AdditionElement.ElementType.AGE).getOptions().setVideoId("12432");
        validateAndCheckForMessage(addition, "field_is_not_null");
    }

    private void validateAndCheckForMessage(Addition addition, String message) {
        BindingResult validationResult = videoAdditionValidationService.validate(addition);
        assertThat("got " + message, vrMessages(validationResult),
                Matchers.contains(MessageFormat.format(keyset.key(message), new Object[0])));
    }

    private List<String> vrMessages(BindingResult validationResult) {
        return validationResult.getAllErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.toList());
    }

    private static VideoPreset selectableAgeVideoPreset(Long presetId) {
        PresetDescription description = leastAgePresetDescription();
        description.setPresetId(presetId);
        description.setAgeSelectable(true);
        return new VideoPreset(description);
    }

    private static PresetDescription leastAgePresetDescription() {
        PresetDescription description = leastPresetDescription();
        description.setAgePresent(true);
        return description;
    }

    private Addition makeValidAddition() {
        Addition addition = leastAddition(CLIENT_ID, PRESET_ID, VIDEO_ID);

        Options ageElementOptions = new AgeElementOptions()
                .setTextColor("#1F00FF")
                .setBackgroundColor("#FF00DD");

        AdditionElement ageElement = new AdditionElement(AdditionElement.ElementType.AGE)
                .withAvailable(true)
                .withOptions(ageElementOptions);

        List<AdditionElement> elements = new ArrayList<>(addition.getData().getElements());
        elements.add(ageElement);
        addition.getData().setElements(elements);

        return addition;
    }

}
