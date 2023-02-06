package ru.yandex.autotests.direct.cmd.campaigns.setautopriceajax.rarelyloaded;

import org.junit.*;
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
@Description("Изменение единой цены кампании на поиске через setAutoPriceAjax для групп с флагом мало показов")
@Features(TestFeatures.Groups.RARELY_LOADED_FLAG)
@Stories(TestFeatures.GROUPS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SET_AUTO_PRICE_AJAX)
@RunWith(Parameterized.class)
public class SetSimpleSearchAutoPriceAjaxRarelyLoadedTest extends SetAutoPriceAjaxRarelyLoadedTestBase {

    public SetSimpleSearchAutoPriceAjaxRarelyLoadedTest(CampaignTypeEnum campaignType) {
        super(campaignType);
    }

    @Override
    protected CampaignStrategy getCampStrategy() {
        return CmdStrategyBeans.getStrategyBean(Strategies.HIGHEST_POSITION_DEFAULT);
    }

    @Test
    @Description("Изменение единой цены для всей кампании на поиске")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10680")
    public void changeSimplePriceSearchTest() {
        autoPriceAjaxRequest = new SetAutoPriceAjaxRequest()
                .withCid(bannersRule.getCampaignId())
                .withTabSimple(1)
                .withSimplePlatform("search")
                .withSimplePrice(String.valueOf(EXPECTED_PRICE));

        setPrice();

        assertThat("ставка фраз группы с мало показов изменилась",
                getGroupPhrase(bannersRule.getGroupId()).getPrice(), equalTo(EXPECTED_PRICE));
    }
}
