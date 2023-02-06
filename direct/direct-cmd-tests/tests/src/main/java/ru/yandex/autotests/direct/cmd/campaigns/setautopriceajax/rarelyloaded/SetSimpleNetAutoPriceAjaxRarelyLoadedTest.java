package ru.yandex.autotests.direct.cmd.campaigns.setautopriceajax.rarelyloaded;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.campaigns.SetAutoPriceAjaxRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


@Aqua.Test
@Description("Изменение единой цены кампании в сетях через setAutoPriceAjax для групп с флагом мало показов")
@Features(TestFeatures.Groups.RARELY_LOADED_FLAG)
@Stories(TestFeatures.GROUPS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SET_AUTO_PRICE_AJAX)
@RunWith(Parameterized.class)
public class SetSimpleNetAutoPriceAjaxRarelyLoadedTest extends SetAutoPriceAjaxRarelyLoadedTestBase {

    public SetSimpleNetAutoPriceAjaxRarelyLoadedTest(CampaignTypeEnum campaignType) {
        super(campaignType);
    }

    @Override
    protected CampaignStrategy getCampStrategy() {
        return CmdStrategyBeans.getStrategyBean(Strategies.SHOWS_DISABLED_MAX_COVERADGE);
    }

    @Test
    @Description("Изменение единой цены для всей кампании в сетях")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10679")
    public void changeSimplePriceNetTest() {
        autoPriceAjaxRequest = new SetAutoPriceAjaxRequest()
                .withCid(bannersRule.getCampaignId())
                .withTabSimple(1)
                .withWizardCtx(1)
                .withSinglePriceCtx(String.valueOf(EXPECTED_PRICE));

        setPrice();

        assertThat("ставка фраз группы с мало показов изменилась",
                getGroupPhrase(bannersRule.getGroupId()).getPriceContext(), equalTo(EXPECTED_PRICE));
    }

}
