package ru.yandex.autotests.direct.cmd.steps.counters;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.counters.AjaxCheckUserCountersRequest;
import ru.yandex.autotests.direct.cmd.data.counters.AjaxCheckUserCountersResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class AjaxCheckUserCountersSteps extends DirectBackEndSteps {

    @Step("GET cmd = ajaxCheckUserCounters")
    public AjaxCheckUserCountersResponse getCheckUserCounters(AjaxCheckUserCountersRequest request) {
        return get(CMD.AJAX_CHECK_USER_COUNTERS, request, AjaxCheckUserCountersResponse.class);
    }

    @Step("Проверяем доступность счетчика метрики {0} у пользователя {1}")
    public AjaxCheckUserCountersResponse getCheckUserCounters(Long counter, String client) {
        AjaxCheckUserCountersRequest request = new AjaxCheckUserCountersRequest()
                .withCounter(counter)
                .withUlogin(client);
        return getCheckUserCounters(request);
    }

}
