package ru.yandex.autotests.direct.httpclient.steps.banners;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxEditAdGroupDynamicConditionsParameters;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxUpdateShowConditionsParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by f1nal on 26.08.15.
 */
public class AjaxEditAdGroupDynamicConditionsSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера ajaxEditAdGroupDynamicConditions")
    public DirectResponse ajaxEditAdGroupDynamicConditions(CSRFToken token, AjaxEditAdGroupDynamicConditionsParameters params) {
        return execute(getRequestBuilder().post(CMD.AJAX_EDIT_ADGROUP_DYNAMIC_CONDITIONS, token, params));
    }
}