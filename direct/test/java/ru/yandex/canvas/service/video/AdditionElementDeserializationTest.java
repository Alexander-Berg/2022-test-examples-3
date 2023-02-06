package ru.yandex.canvas.service.video;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.model.video.addition.options.AdditionElementOptions;
import ru.yandex.canvas.model.video.addition.options.AgeElementOptions;
import ru.yandex.canvas.model.video.addition.options.BodyElementOptions;
import ru.yandex.canvas.model.video.addition.options.ButtonElementOptions;
import ru.yandex.canvas.model.video.addition.options.DisclaimerElementOptions;
import ru.yandex.canvas.model.video.addition.options.DomainElementOptions;
import ru.yandex.canvas.model.video.addition.options.LegalElementOptions;
import ru.yandex.canvas.model.video.addition.options.SubtitlesElementOptions;
import ru.yandex.canvas.model.video.addition.options.TitleElementOptions;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;


public class AdditionElementDeserializationTest {

    @Test
    public void deserializeTest() throws IOException {
        String json = "{\"data\":{\"bundle\":{\"name\":\"video-banner_theme_empty\"},"
                + "\"elements\":[{\"type\":\"addition\",\"available\":true,\"options\":{\"video_id\":\"new_0_0-077"
                + ".mov\",\"audio_id\":null}},{\"type\":\"title\",\"available\":true,"
                + "\"options\":{\"background_color\":\"#000000\",\"text_color\":\"#ffffff\",\"text\":\"13er32r\"}},"
                + "{\"type\":\"body\",\"available\":true,\"options\":{\"background_color\":\"#000000\","
                + "\"text_color\":\"#ffffff\",\"text\":\"Заголовок\"}},{\"type\":\"domain\",\"available\":true,"
                + "\"options\":{\"text_color\":\"#70D1FF\",\"text\":\"\"}},{\"type\":\"button\",\"available\":true,"
                + "\"options\":{\"color\":\"#FFDC00\",\"text_color\":\"#000000\",\"border_color\":\"#000000\"}},"
                + "{\"type\":\"disclaimer\",\"available\":true,\"options\":{\"background_color\":\"#000000\","
                + "\"text_color\":\"#ffffff\"}},{\"type\":\"age\",\"available\":true,"
                + "\"options\":{\"background_color\":\"#000000\",\"text_color\":\"#ffffff\",\"text\":\"18\"}},"
                + "{\"type\":\"legal\",\"available\":false,\"options\":{\"background_color\":\"#000000\","
                + "\"text\":\"Полная информация о рекламодателе в соответствии с законодательством\","
                + "\"text_color\":\"#ffffff\"}},{\"type\":\"subtitles\",\"available\":\"true\",\"options\":{\"text\":"
                + "\"WEBVTT - This file has cues.\\n\\n14\\n00:01:14.815 --> 00:01:18.114\\n- What?\\n- Where are we "
                + "now?\",\"background_color\":\"#000000\",\"text_color\":\"#ffffff\"}}]},\"preset_id\":\"55\"}}";


        Addition addition = new ObjectMapper().readValue(json, Addition.class);

        assertThat(addition.getData(), notNullValue());
        assertThat(addition.getData().getElements(), allOf(notNullValue(),
                contains(
                        hasProperty("options", instanceOf(AdditionElementOptions.class)),
                        hasProperty("options", instanceOf(TitleElementOptions.class)),
                        hasProperty("options", instanceOf(BodyElementOptions.class)),
                        hasProperty("options", instanceOf(DomainElementOptions.class)),
                        hasProperty("options", instanceOf(ButtonElementOptions.class)),
                        hasProperty("options", instanceOf(DisclaimerElementOptions.class)),
                        hasProperty("options", instanceOf(AgeElementOptions.class)),
                        hasProperty("options", instanceOf(LegalElementOptions.class)),
                        hasProperty("options", instanceOf(SubtitlesElementOptions.class))
                )
        ));
    }
}
