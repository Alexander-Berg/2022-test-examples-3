package ru.yandex.autotests.direct.httpclient.steps.campaigns;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.adjustment.rates.HierarchicalMultipliers;
import ru.yandex.autotests.direct.httpclient.data.campaigns.editcamp.EditCampParameters;
import ru.yandex.autotests.direct.httpclient.data.campaigns.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.BeanType;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 25.11.14
 */
public class EditCampSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера EditCamp для кампании {0} логина {1} ")
    public DirectResponse openEditCamp(String campaignId, String login) {
        EditCampParameters params = new EditCampParameters();
        params.setCampaignId(campaignId);
        params.setUlogin(login);
        return execute(getRequestBuilder().get(CMD.EDIT_CAMP, params));
    }

    public EditCampResponse getEditCampResponse(String campaignId, String login) {
        DirectResponse directResponse = openEditCamp(campaignId, login);
        return JsonPathJSONPopulater.eval(directResponse.getResponseContent().asString(), new EditCampResponse(),
                BeanType.RESPONSE);

    }

    public EditCampResponse getEditCampResponseByDirectResponse(DirectResponse directResponse) {
        return JsonPathJSONPopulater.eval(directResponse.getResponseContent().asString(), new EditCampResponse(),
                BeanType.RESPONSE);

    }

    @Step("Получаем ответ контроллера EditCamp для новой кампании логина {0} ")
    public DirectResponse openEditCamp(String login) {
        EditCampParameters params = new EditCampParameters();
        params.setNew_camp("1");
        params.setUlogin(login);
        return execute(getRequestBuilder().get(CMD.EDIT_CAMP, params));
    }

    public EditCampResponse getEditCampResponseForNewCamp(String login) {
        DirectResponse directResponse = openEditCamp(login);
        return JsonPathJSONPopulater.eval(directResponse.getResponseContent().asString(), new EditCampResponse(),
                BeanType.RESPONSE);

    }
}
