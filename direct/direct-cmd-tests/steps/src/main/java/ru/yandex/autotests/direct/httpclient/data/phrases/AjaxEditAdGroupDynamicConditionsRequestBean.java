package ru.yandex.autotests.direct.httpclient.data.phrases;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.CmdBeanBuilder;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 28.05.15.
 */
public class AjaxEditAdGroupDynamicConditionsRequestBean extends CmdBeanBuilder {

    private static final String JSON_PHRASES_PATH = "json_adgroup_dynamic_conditions";

    @JsonPath(requestPath = JSON_PHRASES_PATH)
    private JsonObject jsonPhrases;

    public void setGroupPhrases(String adgroupId, AjaxUpdateShowConditionsGroupPhrasesBeanBuilder bannerPhrases) {
        jsonPhrases = new JsonObject();
        addGroupPhrases(adgroupId, gson.toJsonTree(bannerPhrases));
    }

    public void addGroupPhrases(String adgroupId, Object bannerPhrases) {
        jsonPhrases.add(adgroupId, gson.toJsonTree(bannerPhrases));
    }


    @Override
    public String toJson() {
        String nameValuePair = super.toJson();
        if (jsonPhrases == null) {
            return nameValuePair;
        }
        JsonElement jsonTree = new JsonParser().parse(nameValuePair);
        return jsonTree.getAsJsonObject().get(JSON_PHRASES_PATH).toString();
    }

}
