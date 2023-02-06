package ru.yandex.autotests.direct.httpclient.steps.banners;

import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.banners.SaveAdGroupTagsParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shmykov on 12.03.15.
 */
public class AjaxSaveAdGroupTags extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера AjaxSaveAdGroupTags")
    public DirectResponse ajaxSaveAdGroupTags(SaveAdGroupTagsParameters params, CSRFToken token) {
        return execute(getRequestBuilder().post(CMD.SAVE_ADGROUP_TAGS, token, params));
    }

    @Step("Добавляем новые тэги для группы")
    public List<String> addNewTagsAndGetTagIds(SaveAdGroupTagsParameters params, CSRFToken token) {
        List<String> result = new ArrayList<>();
        String responseContent = ajaxSaveAdGroupTags(params, token).getResponseContent().asString();
        for (String tagName : params.getNewTags().split(",")) {
            JSONArray tagId = JsonPath.read(responseContent, "." + tagName + ".tag_id");
            result.add(tagId.get(0).toString());
        }
        return result;
    }
}