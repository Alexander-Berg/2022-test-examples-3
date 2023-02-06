package ru.yandex.autotests.direct.httpclient.campaigns.setautopriceajax.wizard;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.*;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.campaigns.setautopriceajax.SetAutoPriceAjaxTestBase;
import ru.yandex.autotests.direct.httpclient.data.campaigns.setautopriceajax.SetAutoPriceAjaxRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.SetAutoPriceAjaxErrors.OUT_OF_BOUND_MAX_PRICE_PARAMETER;
import static ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.SetAutoPriceAjaxErrors.WRONG_MAX_PRICE_PARAMETER;
import static ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.SetAutoPriceAjaxErrors.WRONG_POSITION_CTR_CORRECTION_PARAMETER;
import static ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.SetAutoPriceAjaxErrors.WRONG_PROC_PARAMETER;

/**
 * Created by shmykov on 11.06.15.
 * TESTIRT-5015
 */
@Aqua.Test
@Description("Проверка валидации при изменении цены кампании (SetAutoPriceAjax)")
@Stories(TestFeatures.Campaigns.SET_AUTO_PRICE_AJAX)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SET_AUTO_PRICE_AJAX)
@Tag(TrunkTag.YES)
@Tag(OldTag.YES)
public class SetAutoPriceAjaxWizardValidationTest extends SetAutoPriceAjaxTestBase {

    public SetAutoPriceAjaxWizardValidationTest() {
        super(new PropertyLoader<>(SetAutoPriceAjaxRequestParameters.class).getHttpBean("wizardAutoPrice"));
    }

    @Before
    public void setRequestParams() {
        requestParams.setWizardSearchPositionCtrCorrection("75");
    }

    @Test
    @Description("Вызов с неверным значением объёма трафика (wizard_search_position_ctr_correction)")
    @TestCaseId("11040")
    public void wrongPositionCtrCorrectionTest() {
        requestParams.setWizardSearchPositionCtrCorrection("abc");
        response = cmdRule.oldSteps().setAutoPriceAjaxSteps().setAutoPriceAjax(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(WRONG_POSITION_CTR_CORRECTION_PARAMETER.toString()));
    }

    @Test
    @Description("Вызов с неверным значением максимальной цены (wizard_search_max_price)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10402")
    public void wrongMaxPriceTest() {
        requestParams.setWizardSearchMaxPrice("abc");
        response = cmdRule.oldSteps().setAutoPriceAjaxSteps().setAutoPriceAjax(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, startsWith(WRONG_MAX_PRICE_PARAMETER.toString()));
    }

    @Test
    @Description("Вызов с неверным значением процентов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10403")
    public void wrongProcTest() {
        requestParams.setWizardSearchProc("abc");
        response = cmdRule.oldSteps().setAutoPriceAjaxSteps().setAutoPriceAjax(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(WRONG_PROC_PARAMETER.toString()));
    }

    @Test
    @Description("Вызов с процентами больше максимального")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10404")
    public void outOfBoundProcTest() {
        requestParams.setWizardSearchProc("1001");
        response = cmdRule.oldSteps().setAutoPriceAjaxSteps().setAutoPriceAjax(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(WRONG_PROC_PARAMETER.toString()));
    }

    @Test
    @Description("Вызов с слишком большой максимальной ценой")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10405")
    public void outOfBoundMaxPriceTest() {
        requestParams.setWizardSearchMaxPrice("25001");
        response = cmdRule.oldSteps().setAutoPriceAjaxSteps().setAutoPriceAjax(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, startsWith(OUT_OF_BOUND_MAX_PRICE_PARAMETER.toString()));
    }
}
