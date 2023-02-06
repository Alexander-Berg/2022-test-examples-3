package ru.yandex.autotests.direct.httpclient.steps.campaigns;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.SaveCampParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 25.11.14
 */
public class SaveCampEasySteps extends DirectBackEndSteps {

    @Step("Сохраняем кампанию в легком интерфейсе")
    public DirectResponse saveCampEasy(CSRFToken token, SaveCampParameters params) {
        return execute(getRequestBuilder().post(CMD.SAVE_CAMP_EASY, token, params));
    }
}
