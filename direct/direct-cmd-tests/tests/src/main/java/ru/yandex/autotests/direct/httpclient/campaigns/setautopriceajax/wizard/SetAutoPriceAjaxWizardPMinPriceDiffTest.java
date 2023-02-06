package ru.yandex.autotests.direct.httpclient.campaigns.setautopriceajax.wizard;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.campaigns.setautopriceajax.SetAutoPriceAjaxTestBase;
import ru.yandex.autotests.direct.httpclient.data.campaigns.setautopriceajax.PriceBase;
import ru.yandex.autotests.direct.httpclient.data.campaigns.setautopriceajax.SetAutoPriceAjaxRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
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
@Description("Изменение в мастере цен в процентах от цены входа в спецразмещение(SetAutoPriceAjax)")
@Stories(TestFeatures.Campaigns.SET_AUTO_PRICE_AJAX)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(OldTag.YES)
public class SetAutoPriceAjaxWizardPMinPriceDiffTest extends SetAutoPriceAjaxTestBase {

    public SetAutoPriceAjaxWizardPMinPriceDiffTest() {
        super(new PropertyLoader<>(SetAutoPriceAjaxRequestParameters.class).getHttpBean("wizardAutoPrice"));
    }

    @Test
    @Description("Изменение в мастере цен в процентах от цены входа в спецразмещение")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10399")
    public void changeWizardPMinPriceDiffTest() {
        requestParams.setWizardSearchPriceBase(PriceBase.PMIN);
        final String expectedPrice = "300";
        response = cmdRule.oldSteps().setAutoPriceAjaxSteps().setAutoPriceAjax(csrfToken, requestParams);
        waitAndCheckChangedPriceForBanners(equalTo(expectedPrice),
                bannersRule.getCurrentGroup().getPhrases().get(0).getPrice().toString());
    }
}
