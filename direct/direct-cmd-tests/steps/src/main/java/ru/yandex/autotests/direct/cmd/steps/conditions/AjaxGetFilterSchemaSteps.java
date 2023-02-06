package ru.yandex.autotests.direct.cmd.steps.conditions;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.conditions.AjaxGetFilterSchemaRequest;
import ru.yandex.autotests.direct.cmd.data.conditions.AjaxGetFilterSchemaResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class AjaxGetFilterSchemaSteps extends DirectBackEndSteps {

    @Step("POST cmd = ajaxGetFilterSchema (получение схемы фильтра)")
    public AjaxGetFilterSchemaResponse postAjaxGetFilterSchema(AjaxGetFilterSchemaRequest request) {
        return post(CMD.AJAX_GET_FILTER_SCHEMA, request, AjaxGetFilterSchemaResponse.class);
    }

    @Step("получение схемы фильтра с типом {0} для логина {1}")
    public AjaxGetFilterSchemaResponse getFilterSchema(String filterType) {
        return postAjaxGetFilterSchema(new AjaxGetFilterSchemaRequest().withFilterType(filterType));
    }
}
