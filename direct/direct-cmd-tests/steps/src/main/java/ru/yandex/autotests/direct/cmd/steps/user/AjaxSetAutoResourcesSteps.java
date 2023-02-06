package ru.yandex.autotests.direct.cmd.steps.user;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.ajaxSetAutoResources.AjaxSetAutoResourcesRequest;
import ru.yandex.autotests.direct.cmd.data.ajaxSetAutoResources.AutoVideoAction;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class AjaxSetAutoResourcesSteps extends DirectBackEndSteps {
    @Step("POST cmd = ajaxSetAutoResources (включение/отключение авто-видеорекламы)")
    public CommonResponse postAjaxSetAutoResources(AjaxSetAutoResourcesRequest request) {
        return post(CMD.AJAX_SET_AUTO_RESOURCSES, request, CommonResponse.class);
    }

    @Step("Включение/отключение авто-видеорекламы для кампании {0} клиента {2}")
    public CommonResponse postAjaxSetAutoResources(String cid, String action, String login) {
        AjaxSetAutoResourcesRequest request = new AjaxSetAutoResourcesRequest()
                .withCid(cid)
                .withAction(action)
                .withUlogin(login);
        return postAjaxSetAutoResources(request);
    }
}
