package ru.yandex.autotests.direct.httpclient.campaigns.setautopriceajax;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.campaigns.setautopriceajax.SetAutoPriceAjaxRequestParameters;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.SetAutoPriceAjaxErrors.WRONG_PHRASE_TYPE_PARAMETER;

/**
 * Created by shmykov on 11.06.15.
 * TESTIRT-5015
 */
@Aqua.Test
@Description("Проверка валидации при изменении цены кампании (SetAutoPriceAjax)")
@Stories(TestFeatures.Campaigns.SET_AUTO_PRICE_AJAX)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SET_AUTO_PRICE_AJAX)
@Tag(OldTag.YES)
public class SetAutoPriceAjaxValidationTest extends SetAutoPriceAjaxTestBase {

    public SetAutoPriceAjaxValidationTest() {
        super(new SetAutoPriceAjaxRequestParameters());
    }

    @Before
    public void setRequestParams() {
        requestParams.setTabSimple("1");
        requestParams.setSimplePlatform("search");
        requestParams.setSimplePrice(EXPECTED_PRICE);
    }

    @Test
    @Description("Проверка неверной платформы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10391")
    public void wrongPlatformTest() {
        requestParams.setSimplePlatform("abcd");
        response = cmdRule.oldSteps().setAutoPriceAjaxSteps().setAutoPriceAjax(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(WRONG_PHRASE_TYPE_PARAMETER.toString()));
    }

    @Test
    @Description("Вызов без параметра tab_simple")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10392")
    public void wrongTabSimpleTest() {
        requestParams.setTabSimple(null);
        response = cmdRule.oldSteps().setAutoPriceAjaxSteps().setAutoPriceAjax(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseError(response, equalTo(WRONG_PHRASE_TYPE_PARAMETER.toString()));
    }

    @Test
    @Description("Вызов с неверным cid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10390")
    public void wrongCidTest() {
        requestParams.setCid("123");
        response = cmdRule.oldSteps().setAutoPriceAjaxSteps().setAutoPriceAjax(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }

    @Test
    @Description("Вызов с неверным ulogin")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10389")
    public void wrongUloginTest() {
        requestParams.setUlogin(Logins.SUPER);
        response = cmdRule.oldSteps().setAutoPriceAjaxSteps().setAutoPriceAjax(csrfToken, requestParams);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }
}
