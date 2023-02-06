package ru.yandex.canvas.service.video;

import java.util.Arrays;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTvmStubConfiguration;
import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.configs.WebMvcConfig;
import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.model.video.addition.AdditionData;
import ru.yandex.canvas.model.video.addition.AdditionDataBundle;
import ru.yandex.canvas.model.video.addition.AdditionElement;
import ru.yandex.canvas.model.video.addition.options.ButtonElementOptions;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.direct.common.tracing.TraceContextFilter;

import static org.hamcrest.MatcherAssert.assertThat;
@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@Import({ControllerTestConfiguration.class, WebMvcConfig.class, CanvasTvmStubConfiguration.class})
public class AdditionValidationTest {
    private Validator validator;

    @MockBean
    private TraceContextFilter traceContextFilter;

    @MockBean
    private DirectService directService;

    @Before
    public void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    Addition dummyAddition(AdditionElement... additionElementList) {
        Addition addition = new Addition()
                .setCreativeId(1L)
                .setName("")
                .setClientId(12345L)
                .setScreenshotUrl("screenshot_url")
                .setId("abcde")
                .setPresetId(1L)
                .setDate(null)
                .setArchive(null)
                .setVast(null);

        AdditionData data = new AdditionData()
                .setBundle(new AdditionDataBundle().setName("something"));

        data.setElements(Arrays.asList(additionElementList));

        addition.setData(data);

        return addition;
    }

    @Test
    public void additionWithNoElementsTest() {

        Addition addition = dummyAddition();

        Set<ConstraintViolation<Addition>> violations = validator.validate(addition);

        assertThat("No elements", violations, Matchers.hasSize(0));
    }

    @Test
    public void additionWithButtonTest() {

        Addition addition = dummyAddition(
                new AdditionElement(AdditionElement.ElementType.BUTTON).withOptions(
                        new ButtonElementOptions().setText("abc")
                                .setTextColor("#ffBB92")
                                .setColor("#ff0001")
                                .setBorderColor("#ff00AA")
                )
                        .withAvailable(true)
        );

        Set<ConstraintViolation<Addition>> violations = validator.validate(addition);

        assertThat("No elements", violations, Matchers.hasSize(0));
    }
}
