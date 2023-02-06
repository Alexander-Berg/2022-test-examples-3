package ru.yandex.canvas.service.video;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
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
import ru.yandex.canvas.model.video.files.FileStatus;
import ru.yandex.canvas.model.video.files.Movie;
import ru.yandex.canvas.model.video.files.StreamFormat;
import ru.yandex.canvas.service.TankerKeySet;
import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.VideoPreset;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.canvas.steps.AdditionsSteps.defaultBodyElementOptions;
import static ru.yandex.canvas.steps.AdditionsSteps.defaultBodyElementOptionsWithText;
import static ru.yandex.canvas.steps.AdditionsSteps.defaultVideoId;
import static ru.yandex.canvas.steps.AdditionsSteps.leastAddition;
import static ru.yandex.canvas.steps.PresetDescriptionsSteps.leastPresetDescription;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@Import({ControllerTestConfiguration.class, VideoAdditionsTestConfiguration.class})
public class BodyValidateTest {
    private TankerKeySet keyset = TankerKeySet.VIDEO_VALIDATION_MESSAGES;

    private static final long CLIENT_ID = 12345L;
    private static final String VIDEO_ID = defaultVideoId();
    private static final long EDITABLE_BUTTON_PRESET_ID = 55L;
    private static final long NON_EDITABLE_BUTTON_PRESET_ID = 1L;

    @Autowired
    private VideoPresetsService videoPresetsService;

    @Autowired
    private VideoAdditionValidationService videoAdditionValidationService;

    @Autowired
    private MovieServiceInterface commonMovieService;

    @Before
    public void setupMocks() {
        Movie movie = mock(Movie.class);
        when(movie.getStatus()).thenReturn(FileStatus.READY);
        StreamFormat streamFormat = mock(StreamFormat.class);
        when(movie.getFormats()).thenReturn(Arrays.asList(streamFormat));
        when(movie.isReady()).thenReturn(true);

        Mockito.when(commonMovieService.lookupMovie(VIDEO_ID, null, CLIENT_ID, NON_EDITABLE_BUTTON_PRESET_ID))
                .thenReturn(movie);
        Mockito.when(commonMovieService.lookupMovie(VIDEO_ID, null, CLIENT_ID, EDITABLE_BUTTON_PRESET_ID))
                .thenReturn(movie);

        Mockito.when(videoPresetsService.getPreset(NON_EDITABLE_BUTTON_PRESET_ID))
                .thenReturn(nonEditableButtonVideoPreset(NON_EDITABLE_BUTTON_PRESET_ID));
        Mockito.when(videoPresetsService.getPreset(EDITABLE_BUTTON_PRESET_ID))
                .thenReturn(editableButtonVideoPreset(EDITABLE_BUTTON_PRESET_ID));
        Mockito.when(videoPresetsService.contains(NON_EDITABLE_BUTTON_PRESET_ID)).thenReturn(true);
        Mockito.when(videoPresetsService.contains(EDITABLE_BUTTON_PRESET_ID)).thenReturn(true);
    }

    @Test
    public void badColorTest() {
        Addition addition = additionWithBodyText();
        getBodyElement(addition).getOptions().setTextColor("GHRR");
        validateAndCheckForMessage(addition, "invalid_color_value");
    }

    @Test
    public void badBgColorTest() {
        Addition addition = additionWithBodyText();
        getBodyElement(addition).getOptions().setBackgroundColor("#0000001");
        validateAndCheckForMessage(addition, "invalid_color_value");
    }

    @Test
    public void nonEditableBodyWithTextTest() {
        Addition addition = additionWithoutBodyText();
        getBodyElement(addition).getOptions().setText("SomeText");
        validateAndCheckForMessage(addition, "field_is_not_null");
    }

    @Test
    public void nonEditableBodyPlaceholderTest() {
        Addition addition = additionWithoutBodyText();
        AdditionElement element = getBodyElement(addition);
        element.getOptions().setPlaceholder("SomeText");
        validateAndCheckForMessage(addition, "field_is_not_null");
    }

    @Test
    public void bodyWithBorderColorTest() {
        Addition addition = additionWithBodyText();
        AdditionElement element = getBodyElement(addition);
        element.getOptions().setBorderColor("GHRR");
        validateAndCheckForMessage(addition, "field_is_not_null");
    }

    @Test
    public void editableBodyWithNoTextTest() {
        Addition addition = additionWithBodyText();
        getBodyElement(addition).getOptions().setText(null);
        validateAndCheckForMessage(addition, "org.hibernate.validator.constraints.NotBlank.message");
    }

    @Test
    public void editableBodyWithTextTest() {
        Addition addition = additionWithBodyText();
        getBodyElement(addition).getOptions().setText("SomeText");
        assertTrue("Addition with body.text passes validation",
                !videoAdditionValidationService.validate(addition).hasErrors());
    }

    @Test
    public void editableBodyWithEmptyStringTextTest() {
        Addition addition = additionWithBodyText();
        getBodyElement(addition).getOptions().setText("");
        assertTrue("Addition with body.text passes validation",
                !videoAdditionValidationService.validate(addition).hasErrors());    }

    @Test
    public void editableBodyWithBigTextTest() {
        Addition addition = additionWithBodyText();
        getBodyElement(addition).getOptions().setText(RandomStringUtils.randomAlphanumeric(100));
        validateAndCheckForMessage(addition, "javax.validation.constraints.Size.message", 0, 81);
    }

    @Test
    public void editableBodyPlaceholderPresetTest() {
        Addition addition = additionWithBodyText();
        AdditionElement element = getBodyElement(addition);
        element.getOptions().setPlaceholder("SomeText");
        assertTrue("Addition with placeholder passes validation if it's editable: true",
                !videoAdditionValidationService.validate(addition).hasErrors());
    }

    @Test
    public void editableBodyPlaceholderMissedTest() { // still ok, as placeholder never mandatory
        Addition addition = additionWithBodyText();
        assertTrue("Valid addition passes validation",
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

    private AdditionElement getBodyElement(Addition addition) {
        return addition.findElementByType(AdditionElement.ElementType.BODY);
    }

    private Addition additionWithBodyText() {
        Addition addition = leastAddition(CLIENT_ID, EDITABLE_BUTTON_PRESET_ID, VIDEO_ID);

        AdditionElement elementBody = new AdditionElement(AdditionElement.ElementType.BODY)
                .withOptions(defaultBodyElementOptionsWithText()).withAvailable(true);

        List<AdditionElement> elements = new ArrayList<>(addition.getData().getElements());
        elements.add(elementBody);

        addition.getData().setElements(elements);

        return addition;
    }

    private Addition additionWithoutBodyText() {
        Addition addition = leastAddition(CLIENT_ID, NON_EDITABLE_BUTTON_PRESET_ID, VIDEO_ID);

        AdditionElement elementBody = new AdditionElement(AdditionElement.ElementType.BODY)
                .withOptions(defaultBodyElementOptions()).withAvailable(true);

        List<AdditionElement> elements = new ArrayList<>(addition.getData().getElements());
        elements.add(elementBody);

        addition.getData().setElements(elements);


        return addition;
    }

    public static VideoPreset editableButtonVideoPreset(Long presetId) {
        PresetDescription description = leastBodyPresetDescription();
        description.setPresetId(presetId);
        description.setBodyEditable(true);
        return new VideoPreset(description);
    }

    public static VideoPreset nonEditableButtonVideoPreset(Long presetId) {
        PresetDescription description = leastBodyPresetDescription();
        description.setPresetId(presetId);
        description.setBodyEditable(false);
        return new VideoPreset(description);
    }

    public static PresetDescription leastBodyPresetDescription() {
        PresetDescription description = leastPresetDescription();
        description.setBodyPresent(true);
        return description;
    }
}
