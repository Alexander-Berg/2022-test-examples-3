package ru.yandex.direct.core.entity.creative.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import junitparams.JUnitParamsRunner;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.creative.model.ModerationInfo;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoAspect;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoHtml;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoImage;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoSound;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoText;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoVideo;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class CreativeMappingsTest {
    private static final String FULL_VALID_JSON_FILENAME = "fullValidModerationInfo.json";

    private ModerationInfo fullFilledModel = new ModerationInfo()
            .withContentId(123213L)
            .withHtml(new ModerationInfoHtml().withUrl("https://html.url.ru"))
            .withImages(asList(
                    new ModerationInfoImage()
                            .withOriginalFileId("originalFileId1")
                            .withUrl("https://images.url.ru/store/1.png")
                            .withAlt("some text"),
                    new ModerationInfoImage()
                            .withOriginalFileId("originalFileId2")
                            .withUrl("https://images.url.ru/store/2.png")
                            .withType("logo")
                            .withAlt("another text")
            ))
            .withTexts(asList(
                    new ModerationInfoText()
                            .withText("Яндекс Поиск")
                            .withType("headline")
                            .withColor("#FAFAFA"),
                    new ModerationInfoText()
                            .withText("0")
                            .withType("ageRestriction")
                            .withColor("#E8E6E6")
            ))
            .withVideos(asList(
                    new ModerationInfoVideo()
                            .withStockId("v_01")
                            .withUrl("https://www.video.url/video1"),
                    new ModerationInfoVideo()
                            .withStockId("v_02")
                            .withUrl("https://www.video.url/video2")
            ))
            .withSounds(asList(
                    new ModerationInfoSound()
                            .withStockId("s_01")
                            .withUrl("https://www.sound.url/sound1"),
                    new ModerationInfoSound()
                            .withStockId("s_02")
                            .withUrl("https://www.sound.url/sound2")
            ))
            .withAspects(asList(
                    new ModerationInfoAspect()
                            .withWidth(123L)
                            .withHeight(666L),
                    new ModerationInfoAspect()
                            .withWidth(100L)
                            .withHeight(200L)
            ));

    @Test
    public void moderationInfoFromDb_ParsingFullValidModelJson_AllFieldsNotEmptyAndHasCorrectType() throws IOException {
        String json =
                IOUtils.toString(this.getClass().getResourceAsStream(FULL_VALID_JSON_FILENAME), StandardCharsets.UTF_8);

        ModerationInfo actual = CreativeMappings.moderationInfoFromDb(json);
        assertThat(actual, beanDiffer(fullFilledModel));
    }

    @Test
    public void moderationInfoToDb_DeserializeFullValidModel_GetCorrectJson() throws IOException {
        String json =
                IOUtils.toString(this.getClass().getResourceAsStream(FULL_VALID_JSON_FILENAME), StandardCharsets.UTF_8);

        String serializedModel = CreativeMappings.moderationInfoToDb(fullFilledModel);

        ObjectMapper parser = new ObjectMapper();
        JsonNode expected = parser.readTree(json);
        JsonNode actual = parser.readTree(serializedModel);

        assertThat(actual, is(expected));
    }
}
