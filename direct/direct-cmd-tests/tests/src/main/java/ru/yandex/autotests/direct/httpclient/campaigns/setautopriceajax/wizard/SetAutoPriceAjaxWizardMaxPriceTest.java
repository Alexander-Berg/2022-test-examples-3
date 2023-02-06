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
@Description("Изменение в мастере цен при максимальном ограничении(SetAutoPriceAjax)")
@Stories(TestFeatures.Campaigns.SET_AUTO_PRICE_AJAX)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(OldTag.YES)
public class SetAutoPriceAjaxWizardMaxPriceTest extends SetAutoPriceAjaxTestBase {

    public SetAutoPriceAjaxWizardMaxPriceTest() {
        super(new PropertyLoader<>(SetAutoPriceAjaxRequestParameters.class).getHttpBean("wizardAutoPrice"));
    }

    @Test
    @Description("Цена не должна быть выше максимального ограничения")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10396")
    public void changeWizardMinPriceDiffTest() {
        requestParams.setWizardSearchPriceBase(PriceBase.PMAX);
        final String EXPECTED_PRICE = "5";
        requestParams.setWizardSearchMaxPrice(EXPECTED_PRICE);
        response = cmdRule.oldSteps().setAutoPriceAjaxSteps().setAutoPriceAjax(csrfToken, requestParams);
        waitAndCheckChangedPriceForBanners(equalTo(EXPECTED_PRICE),
                getConvertedPrice(bannersRule.getCurrentGroup().getPhrases().get(0).getPrice().floatValue()));
    }
}
