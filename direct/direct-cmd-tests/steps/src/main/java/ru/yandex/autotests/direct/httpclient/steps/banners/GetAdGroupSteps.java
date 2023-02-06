package ru.yandex.autotests.direct.httpclient.steps.banners;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.banners.getadgroup.GetAdGroupRequestParameters;
import ru.yandex.autotests.direct.httpclient.data.banners.getadgroup.GetAdGroupResponse;
import ru.yandex.autotests.direct.httpclient.data.banners.getadgroup.GetAdGroupResponseBuilder;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.BeanType;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 10.03.15
 */
public class GetAdGroupSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера getAdGroup")
    public DirectResponse getAdGroupDirectResponse(GetAdGroupRequestParameters params) {
        return execute(getRequestBuilder().get(CMD.GET_AD_GROUP, params));
    }

    @Step("Преобразуем ответ контроллера getAdGroup в GetAdGroupResponse")
    public GetAdGroupResponse getAdGroup(GetAdGroupRequestParameters params) {
        DirectResponse directResponse = getAdGroupDirectResponse(params);
        return JsonPathJSONPopulater.eval(
                directResponse.getResponseContent().asString(),
                new GetAdGroupResponseBuilder().createGetAdGroupResponse(), BeanType.RESPONSE);
    }

}
