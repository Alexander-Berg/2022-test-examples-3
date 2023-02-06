package ru.yandex.canvas.service.video;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.model.video.addition.AdditionData;
import ru.yandex.canvas.model.video.addition.AdditionDataBundle;
import ru.yandex.canvas.model.video.addition.AdditionElement;
import ru.yandex.canvas.service.AuthRequestParams;
import ru.yandex.canvas.service.DirectService;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasToString;
import static org.springframework.context.i18n.LocaleContextHolder.setLocale;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdditionValidationSmokeTest {

    private Validator validator;

    @Autowired
    private LocalValidatorFactoryBean localValidatorFactoryBean;

    @MockBean
    VideoLimitsService videoLimitsService;

    Addition dummyAddition(AdditionData data) {
        return new Addition()
                .setData(data)
                .setCreativeId(1L)
                .setName("")
                .setClientId(12345L)
                .setScreenshotUrl("screenshot_url")
                .setId("abcde")
                .setPresetId(1L)
                .setDate(null)
                .setArchive(null)
                .setVast(null);
    }

    @MockBean
    private DirectService directService;
    @MockBean
    private AuthRequestParams authRequestParams;

    VideoPresetsService videoPresetsService = new VideoPresetsService(videoLimitsService, directService, authRequestParams);

    @Before
    public void init() {
        setLocale(Locale.forLanguageTag("en"));
        validator = localValidatorFactoryBean.getValidator();
    }

    @Test
    public void noBundleTest() {
        AdditionData additionData = new AdditionData().setBundle(null).setElements(Collections.emptyList());
        Addition addition = dummyAddition(additionData);

        Set<ConstraintViolation<Addition>> results = validator.validate(addition);

        Assert.assertThat("Valid results", results, Matchers.hasSize(1));

        List<String> messages = results.stream().map(e -> e.getMessage()).collect(Collectors.toList());

        Assert.assertThat("Valid results", messages.get(0), equalTo("Value must be not empty"));
    }


    @Test
    public void additionWithNoDataTest() {
        Addition addition = dummyAddition(null);

        Set<ConstraintViolation<Addition>> violations = validator.validate(addition);

        assertThat("Data field is null", violations, Matchers.hasSize(1));

        ConstraintViolation<Addition> violation = violations.iterator().next();

        assertThat("PropertyPath valid", violation.getMessage(), equalTo("Value must be not empty"));
        assertThat("PropertyPath valid", violation.getPropertyPath().toString(), equalTo("data"));
    }

    @Test
    public void additionWithNoElementsTest() {
        AdditionData data = new AdditionData()
                .setBundle(new AdditionDataBundle().setName("something"))
                .setElements(Collections.emptyList());
        Addition addition = dummyAddition(data);

        Set<ConstraintViolation<Addition>> violations = validator.validate(addition);

        assertThat("Data field is null", violations, Matchers.hasSize(0));
    }

    @Test
    public void additionWithNoBadButton() {

        AdditionData data = new AdditionData()
                .setBundle(new AdditionDataBundle().setName("something"));

        data.setElements(Arrays.asList(
                new AdditionElement(AdditionElement.ElementType.BUTTON).withAvailable(true)
        ));

        Addition addition = dummyAddition(data);

        Set<ConstraintViolation<Addition>> violations = validator.validate(addition);

        assertThat("Data field is null", violations, Matchers.hasSize(1));

        ConstraintViolation<Addition> violation = violations.iterator().next();

        assertThat("PropertyPath valid", violation.getMessage(), equalTo("Value must be not empty"));
        assertThat("PropertyPath valid", violation.getPropertyPath().toString(), equalTo("data.elements[0].options"));
    }

    @Test
    public void additionWithNoBadButtonAndBody() {

        AdditionData data = new AdditionData()
                .setBundle(new AdditionDataBundle().setName("something"));

        data.setElements(Arrays.asList(
                new AdditionElement(AdditionElement.ElementType.BUTTON).withAvailable(true),
                new AdditionElement(AdditionElement.ElementType.BODY).withAvailable(true)
        ));

        Addition addition = dummyAddition(data);

        Set<ConstraintViolation<Addition>> violations = validator.validate(addition);

        assertThat("Data field is null", violations, Matchers.hasSize(2));


        assertThat("All errors detected", violations, Matchers.containsInAnyOrder(
                allOf(hasProperty("message", equalTo("Value must be not empty")),
                        hasProperty("propertyPath", hasToString("data.elements[0].options"))),
                allOf(hasProperty("message", equalTo("Value must be not empty")),
                        hasProperty("propertyPath", Matchers.hasToString("data.elements[1].options"))
                )));
    }
}
