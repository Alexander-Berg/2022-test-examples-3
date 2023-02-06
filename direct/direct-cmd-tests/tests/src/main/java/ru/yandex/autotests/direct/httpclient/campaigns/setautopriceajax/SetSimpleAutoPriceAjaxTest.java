package ru.yandex.autotests.direct.httpclient.campaigns.setautopriceajax;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.campaigns.setautopriceajax.SetAutoPriceAjaxRequestParameters;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;

/**
 * Created by shmykov on 11.06.15.
 * TESTIRT-5015
 */
@Aqua.Test
@Description("Изменение единой цены кампании (SetAutoPriceAjax)")
@Stories(TestFeatures.Campaigns.SET_AUTO_PRICE_AJAX)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SET_AUTO_PRICE_AJAX)
@Tag(OldTag.YES)
public class SetSimpleAutoPriceAjaxTest extends SetAutoPriceAjaxTestBase {

    public SetSimpleAutoPriceAjaxTest() {
        super(new SetAutoPriceAjaxRequestParameters());
    }

    @Before
    public void setRequestParams() {
        requestParams.setTabSimple("1");
        requestParams.setSimplePlatform("search");
        requestParams.setSimplePrice(EXPECTED_PRICE);
    }

    @Test
    @Description("Изменение единой цены для всей кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10393")
    public void changeSimplePriceTest() {
        response = cmdRule.oldSteps().setAutoPriceAjaxSteps().setAutoPriceAjax(csrfToken, requestParams);
        Double actualPrice = cmdRule.cmdSteps().groupsSteps()
                .getPhrases(CLIENT_LOGIN, bannersRule.getCampaignId(), bannersRule.getGroupId()).get(0).getPrice();
        waitAndCheckChangedPriceForBanners(equalTo(EXPECTED_PRICE), actualPrice.toString());
    }
}
