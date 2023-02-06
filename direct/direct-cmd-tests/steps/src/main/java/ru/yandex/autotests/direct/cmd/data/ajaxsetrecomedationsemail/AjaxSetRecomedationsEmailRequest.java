package ru.yandex.autotests.direct.cmd.data.ajaxsetrecomedationsemail;//Task: .

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AjaxSetRecomedationsEmailRequest extends BasicDirectRequest{
    @SerializeKey("recommendations_email")
    private String recommendationsEmail;

    public AjaxSetRecomedationsEmailRequest withRecommendationsEmail(String recommendationsEmail) {
        this.recommendationsEmail = recommendationsEmail;
        return this;
    }

    public String getRecommendationsEmail() {
        return recommendationsEmail;
    }
}
