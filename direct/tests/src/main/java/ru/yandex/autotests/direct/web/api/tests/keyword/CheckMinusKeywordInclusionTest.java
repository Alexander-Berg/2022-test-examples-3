package ru.yandex.autotests.direct.web.api.tests.keyword;

import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.web.api.core.DirectRule;
import ru.yandex.autotests.direct.web.api.features.TestFeatures;
import ru.yandex.autotests.direct.web.api.features.tags.Tags;
import ru.yandex.autotests.direct.web.api.models.CheckMinusKeywordsInclusionRequest;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка включения минус-фразы в существующие ключевые фразы")
@Stories(TestFeatures.MinusKeyword.CHECK_INCLUSION)
@Features(TestFeatures.MINUS_KEYWORD)
@Tag(TrunkTag.YES)
@Tag(Tags.MINUS_KEYWORD)
@Tag(Tags.CAMPAIGN)
@Tag(Tags.AD_GROUP)
public class CheckMinusKeywordInclusionTest {

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    @Rule
    public DirectRule directRule = DirectRule.defaultRule().as(Logins.DEFAULT_CLIENT);

    @Test
    public void canCheckKeywordInclusion() {
        Boolean result =
                directRule.webApiSteps().minusKeywordSteps().getGoals(new CheckMinusKeywordsInclusionRequest(), null);
        assertThat("Получен ответ на пустой запрос", result, Matchers.is(Matchers.equalTo(true)));
    }

}
