package ru.yandex.autotests.direct.httpclient.data.phrases;

import com.google.gson.JsonObject;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.CmdBeanBuilder;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by shmykov on 28.05.15.
 */
public class AjaxUpdateShowConditionsGroupPhrasesBeanBuilder extends CmdBeanBuilder {

    @JsonPath(requestPath = "edited")
    private JsonObject edited;

    @JsonPath(requestPath = "deleted")
    private List<String> deleted;

    public JsonObject getEdited() {
        return edited;
    }

    public void setEdited(Map<String, AjaxUpdateShowConditionsBean> phrases) {
        this.edited = new JsonObject();
        for (Map.Entry<String, AjaxUpdateShowConditionsBean> phrase : phrases.entrySet()) {
            edited.add(phrase.getKey(), gson.toJsonTree(phrase.getValue()));
        }
    }

    public List<String> getDeleted() {
        return deleted;
    }

    public void setDeleted(String... phraseIds) {
        this.deleted = new ArrayList<>();
        deleted.addAll(Arrays.asList(phraseIds));
    }
}