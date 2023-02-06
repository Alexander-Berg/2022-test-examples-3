package ru.yandex.canvas.service.video;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.model.video.addition.AdditionData;
import ru.yandex.canvas.model.video.addition.AdditionElement;
import ru.yandex.canvas.model.video.addition.options.AgeElementOptions;
import ru.yandex.canvas.model.video.addition.options.BodyElementOptions;
import ru.yandex.canvas.steps.ResourceHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.context.i18n.LocaleContextHolder.setLocale;

@RunWith(SpringJUnit4ClassRunner.class)
public class DCParamsTest {

    public static final String SMOKE_TEST_EXPECTED_JSON =
            "/ru/yandex/canvas/service/video/dcParamsTest/smokeTestExpected.json";

    @Test
    public void dcParamsSmokeTest() throws IOException {

        setLocale(Locale.ENGLISH);
        Locale.setDefault(Locale.ENGLISH);

        PreviewData previewData = new PreviewData()
                .setAge("18")
                .setBody("BODY")
                .setTitle("\u00AD\u0600-\u0604\u070F឴-\u200F\u2028- \u2060-\u206F\uFFF0-\uFFFF")
                .setUrl("http://yandex.ru")
                .setDomain("DIRECT.YANDEX.RU")
                .setLang("en")
                .setBannerFlags("medicine")
                .setSecondTitle("Second bloody title")
                .setImages(Arrays.asList("image1", "image2"));

        AdditionData additionData = new AdditionData();
        additionData.setBundle(null);
        additionData.setElements(ImmutableList.of(
                new AdditionElement(AdditionElement.ElementType.BODY)
                        .withOptions(new BodyElementOptions().setText("NEW BODY TEXT")),
                new AdditionElement(AdditionElement.ElementType.AGE)
                        .withOptions(new AgeElementOptions().setText("12"))
        ));

        String expected = ResourceHelpers.getResource(SMOKE_TEST_EXPECTED_JSON);

        String dcParams = new DCParams(previewData, additionData, false).toJSON();

        assertEquals("Dcparams created right",
                expected.replaceAll("\\s+", ""),
                dcParams.replaceAll("\\s+", ""));
    }

    @Test
    public void dcParamsEscapeTest() throws IOException {
        PreviewData previewData = new PreviewData()
                .setAge("1\"8")
                .setBody("BODY")
                .setTitle("TITLE")
                .setUrl("http://yandex.ru")
                .setDomain("DIRECT.YANDEX.RU");

        AdditionData additionData = new AdditionData();
        additionData.setBundle(null);
        additionData.setElements(ImmutableList.of(
                new AdditionElement(AdditionElement.ElementType.BODY)
                        .withOptions(new BodyElementOptions().setText("NEW BODY TEXT"))
        ));


        String dcParams = new DCParams(previewData, additionData, false).toJSON();

        ObjectMapper objectMapper = new ObjectMapper();

        boolean fail = false;
        Exception exception = null;

        try {
            objectMapper.readTree(dcParams);
        } catch (Exception e) {
            fail = true;
            exception = e;
        }

        assertFalse("Exception " + exception, fail);

        //assertEquals("Dcparams created right", expected, dcParams);
    }

}
