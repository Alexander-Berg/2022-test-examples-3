package ru.yandex.autotests.direct.httpclient.steps.campaigns;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.copyCamp.CopyCampRequestBean;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 23.04.15
 */
public class CopyCampSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера CopyCamp")
    public DirectResponse openCopyCamp() {
        return execute(getRequestBuilder().get(CMD.COPY_CAMP));
    }

    @Step("Копируем кампании с помощью контроллера CopyCamp")
    public DirectResponse copyCamp(CSRFToken token, CopyCampRequestBean copyCampRequestBean) {
        return execute(getRequestBuilder().get(CMD.COPY_CAMP, token, copyCampRequestBean));
    }
}
