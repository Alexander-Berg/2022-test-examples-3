package ru.yandex.autotests.direct.cmd.autocorrection;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка получения блока before_moderation")
@Stories(TestFeatures.Banners.AUTO_CORRECTION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SHOW_CAMP)
@Tag(ObjectTag.BANNER)
@Tag(TrunkTag.YES)
public class BeforeModerationTest extends BeforeModerationTestBase {

    public BeforeModerationTest(CampaignTypeEnum campaignType) {
        super(campaignType);
    }

    @Test
    @Description("Проверяем блок before_moderation")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9025")
    public void campaignParametersTest() {
        ShowCampResponse actualResponse = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId.toString());
        Banner bannerResponse = actualResponse.getGroups().stream()
                .filter(t -> t.getBid().equals(bannerId))
                .findFirst().orElse(null);
        assumeThat("получили баннер", bannerResponse, notNullValue());
        assumeThat("блок before_moderation существует", bannerResponse.getBeforeModeration(), notNullValue());
        assertThat("данные блока before_moderation соответствуют ожидаемым",
                bannerResponse.getBeforeModeration(),
                beanDiffer(expectedBean).useCompareStrategy(onlyExpectedFields()));
    }
}
