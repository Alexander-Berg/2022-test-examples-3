package ru.yandex.autotests.direct.web.api.tests.keyword;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.web.api.core.DirectRule;
import ru.yandex.autotests.direct.web.api.features.TestFeatures;
import ru.yandex.autotests.direct.web.api.features.tags.Tags;
import ru.yandex.autotests.direct.web.api.models.KeywordStatShowsResponse;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка получения статистики по фразе")
@Stories(TestFeatures.Keyword.STAT_SHOW)
@Features(TestFeatures.KEYWORD)
@Tag(TrunkTag.YES)
@Tag(Tags.KEYWORD)
public class KeywordStatShowTest {

    private static final String PHRASE = "купить слона";

    @Rule
    public DirectRule directRule = DirectRule.defaultClassRule().useAuth(Boolean.FALSE);

    @Test
    public void keywordStatShow() {
        KeywordStatShowsResponse response =
                directRule.webApiSteps().keywordStatSteps().getKeywordStatShows(Geo.RUSSIA.getGeo(), PHRASE);
        assertThat("Получен ответ на запрос", response.getSuccess(), equalTo(Boolean.TRUE));
    }

}
