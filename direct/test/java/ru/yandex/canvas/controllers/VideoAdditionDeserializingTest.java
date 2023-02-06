package ru.yandex.canvas.controllers;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.model.video.addition.AdditionElement;
import ru.yandex.canvas.model.video.addition.options.AdditionElementOptions;
import ru.yandex.canvas.model.video.addition.options.AgeElementOptions;
import ru.yandex.canvas.model.video.addition.options.ButtonElementOptions;
import ru.yandex.canvas.model.video.addition.options.DisclaimerElementOptions;
import ru.yandex.canvas.model.video.addition.options.LegalElementOptions;
import ru.yandex.canvas.model.video.addition.options.SubtitlesElementOptions;
import ru.yandex.canvas.steps.ResourceHelpers;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class VideoAdditionDeserializingTest {

    private static final String TEST_REQUEST =
            "/ru/yandex/canvas/controllers/videoAdditionDeserializingTest/request.json";
    private static String request;

    @BeforeClass
    public static void setUp() throws Exception {
        request = ResourceHelpers.getResource(TEST_REQUEST);
    }

    @Test
    public void deserializingTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Addition addition = objectMapper.readValue(request, Addition.class);

        assertThat(addition, allOf(
                hasProperty("presetId", is(6L)),
                hasProperty("data", allOf(
                        hasProperty("elements", allOf(
                                hasSize(6),
                                contains(
                                        allOf(
                                                hasProperty("options", instanceOf(ButtonElementOptions.class)),
                                                hasProperty("type", is(AdditionElement.ElementType.BUTTON))
                                        ),
                                        allOf(
                                                hasProperty("options", instanceOf(AdditionElementOptions.class)),
                                                hasProperty("type", is(AdditionElement.ElementType.ADDITION))
                                        ),
                                        allOf(
                                                hasProperty("options", instanceOf(DisclaimerElementOptions.class)),
                                                hasProperty("type", is(AdditionElement.ElementType.DISCLAIMER))
                                        ),
                                        allOf(
                                                hasProperty("options", instanceOf(LegalElementOptions.class)),
                                                hasProperty("type", is(AdditionElement.ElementType.LEGAL))
                                        ),
                                        allOf(
                                                hasProperty("options", instanceOf(AgeElementOptions.class)),
                                                hasProperty("type", is(AdditionElement.ElementType.AGE))
                                        ),
                                        allOf(
                                                hasProperty("options", instanceOf(SubtitlesElementOptions.class)),
                                                hasProperty("type", is(AdditionElement.ElementType.SUBTITLES))
                                        )
                                )
                        )))
                )));

    }

}
