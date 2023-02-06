package ru.yandex.autotests.direct.cmd.steps.campaings;
//Task: TESTIRT-9409.

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.campaigns.AjaxCampOptionsRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.SetAutoPriceAjaxRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.irt.testutils.allure.AllureUtils.addJsonAttachment;

public class AjaxCampSteps extends DirectBackEndSteps {
    @Step("POST cmd=ajaxCampOptions (сохраняем свойства кампании)")
    public String postAjaxCampOptions(AjaxCampOptionsRequest request) {
        addJsonAttachment("Параметры кампании", JsonUtils.toStringLow(request));
        return post(CMD.AJAX_CAMP_OPTIONS, request, String.class);
    }

    @Step("POST cmd=setAutoPriceAjax (сохраняем свойства кампании)")
    public CommonResponse postSetAutoPriceAjax(SetAutoPriceAjaxRequest request) {
        addJsonAttachment("Параметры кампании", JsonUtils.toStringLow(request));
        return post(CMD.SET_AUTO_PRICE_AJAX, request, CommonResponse.class);
    }
}
