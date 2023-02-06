package ru.yandex.canvas.service.video;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import ru.yandex.canvas.model.video.addition.options.AgeElementOptions;
import ru.yandex.canvas.model.video.addition.options.DomainElementOptions;
import ru.yandex.canvas.model.video.addition.options.TitleElementOptions;
import ru.yandex.canvas.model.video.files.FileStatus;
import ru.yandex.canvas.model.video.files.Movie;
import ru.yandex.canvas.model.video.files.StreamFormat;
import ru.yandex.canvas.service.TankerKeySet;
import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.VideoPreset;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.canvas.steps.AdditionsSteps.defaultBodyElementOptionsWithText;
import static ru.yandex.canvas.steps.AdditionsSteps.defaultButtonElementOptions;
import static ru.yandex.canvas.steps.AdditionsSteps.defaultVideoId;
import static ru.yandex.canvas.steps.AdditionsSteps.leastAddition;
import static ru.yandex.canvas.steps.PresetDescriptionsSteps.leastPresetDescription;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@Import({ControllerTestConfiguration.class, VideoAdditionsTestConfiguration.class})
public class NotPresentElementsValidateTest {
    private TankerKeySet keyset = TankerKeySet.VIDEO_VALIDATION_MESSAGES;

    private static final long CLIENT_ID = 12345L;
    private static final String VIDEO_ID = defaultVideoId();
    private static final long PRESET_ID = 101L;

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

        Mockito.when(commonMovieService.lookupMovie(VIDEO_ID, null, CLIENT_ID, PRESET_ID))
                .thenReturn(movie);

        Mockito.when(videoPresetsService.getPreset(PRESET_ID))
                .thenReturn(getVideoPresetWithoutBody());

        Mockito.when(videoPresetsService.contains(PRESET_ID)).thenReturn(true);
    }

    @Test
    public void unexpectedBodyTest() {
        Addition addition = additionWithBodyText();
        validateAndCheckForMessage(addition, "unexpected_element");
    }

    @Test
    public void unexpectedButtonTest() {
        Addition addition = additionWithButton();
        validateAndCheckForMessage(addition, "unexpected_element");
    }

    @Test
    public void unexpectedTitleTest() {
        Addition addition = additionWithTitle();
        validateAndCheckForMessage(addition, "unexpected_element");
    }

    @Test
    public void unexpectedDomainAndAgeTest() {
        Addition addition = additionWithDomainAndAge();
        validateAndCheckForMessage(addition, "unexpected_element");
    }

    private void validateAndCheckForMessage(Addition addition, String message, Object... messageArgs) {
        BindingResult validationResult = videoAdditionValidationService.validate(addition);
        assertThat("got " + message + " " + vrMessages(validationResult), vrMessages(validationResult).get(0),
                is(MessageFormat.format(keyset.key(message), messageArgs)));
    }

    private List<String> vrMessages(BindingResult validationResult) {
        return validationResult.getAllErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.toList());
    }


    private Addition additionWithDomainAndAge() {
        Addition addition = leastAddition(CLIENT_ID, PRESET_ID, VIDEO_ID);

        DomainElementOptions options = new DomainElementOptions()
                .setColor("#FF00FF")
                .setTextColor("#454543");

        AgeElementOptions ageElementOptions = new AgeElementOptions()
                .setText("18");

        AdditionElement elementButton = new AdditionElement(AdditionElement.ElementType.DOMAIN)
                .withOptions(options).withAvailable(true);

        AdditionElement elementAge = new AdditionElement(AdditionElement.ElementType.AGE)
                .withOptions(options).withAvailable(true);

        List<AdditionElement> elements = new ArrayList<>(addition.getData().getElements());
        elements.add(elementButton);
        elements.add(elementAge);

        addition.getData().setElements(elements);

        return addition;
    }

    private Addition additionWithTitle() {
        Addition addition = leastAddition(CLIENT_ID, PRESET_ID, VIDEO_ID);

        TitleElementOptions options = new TitleElementOptions()
                .setColor("#FF00FF")
                .setBorderColor("#CCFFDD")
                .setTextColor("#454543");

        AdditionElement elementButton = new AdditionElement(AdditionElement.ElementType.TITLE)
                .withOptions(options).withAvailable(true);

        List<AdditionElement> elements = new ArrayList<>(addition.getData().getElements());
        elements.add(elementButton);

        addition.getData().setElements(elements);

        return addition;
    }

    private Addition additionWithButton() {
        Addition addition = leastAddition(CLIENT_ID, PRESET_ID, VIDEO_ID);

        AdditionElement elementButton = new AdditionElement(AdditionElement.ElementType.BUTTON)
                .withOptions(defaultButtonElementOptions()).withAvailable(true);

        List<AdditionElement> elements = new ArrayList<>(addition.getData().getElements());
        elements.add(elementButton);

        addition.getData().setElements(elements);

        return addition;
    }

    private Addition additionWithBodyText() {
        Addition addition = leastAddition(CLIENT_ID, PRESET_ID, VIDEO_ID);

        AdditionElement elementBody = new AdditionElement(AdditionElement.ElementType.BODY)
                .withOptions(defaultBodyElementOptionsWithText()).withAvailable(true);

        List<AdditionElement> elements = new ArrayList<>(addition.getData().getElements());
        elements.add(elementBody);

        addition.getData().setElements(elements);

        return addition;
    }


    public static VideoPreset getVideoPresetWithoutBody() {
        PresetDescription description = leastPresetDescription(101L);
        description.setPresetId(101L);
        return new VideoPreset(description);
    }

}
