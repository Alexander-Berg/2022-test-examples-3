package ru.yandex.autotests.direct.cmd.steps.teasers;
//Task: 9250.

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.ajaxsetrecomedationsemail.AjaxSetRecomedationsEmailRequest;
import ru.yandex.autotests.direct.cmd.data.ajaxsetrecomedationsemail.AjaxSetRecomedationsEmailResponse;
import ru.yandex.autotests.direct.cmd.data.ajaxuseroptions.AjaxUserOptionsRequest;
import ru.yandex.autotests.direct.cmd.data.ajaxuseroptions.AjaxUserOptionsResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class AjaxSetRecomedationsEmailSteps extends DirectBackEndSteps {
    @Step("POST cmd = ajaxSetRecommendationsEmail (сохранение настроек пользователя)")
    public AjaxSetRecomedationsEmailResponse postAjaxSetRecomendationsEmail(AjaxSetRecomedationsEmailRequest request) {
        return post(CMD.AJAX_SET_RECOMMENDATIONS_EMAIL, request, AjaxSetRecomedationsEmailResponse.class);
    }

    @Step("Сохранение почты {1} для рекомендаций для пользователя {0}")
    public AjaxSetRecomedationsEmailResponse postAjaxSetRecomendationsEmail(String login, String email) {
        return post(
                CMD.AJAX_SET_RECOMMENDATIONS_EMAIL, new AjaxSetRecomedationsEmailRequest()
                .withRecommendationsEmail(email)
                .withUlogin(login),
                AjaxSetRecomedationsEmailResponse.class
        );
    }
}
