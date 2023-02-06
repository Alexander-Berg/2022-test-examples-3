package ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions;

import org.apache.commons.collections4.map.HashedMap;
import ru.yandex.autotests.direct.httpclient.util.requestbeantojson.RequestBeanToJsonProcessor;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

import java.util.*;
import java.util.stream.Stream;

public class AjaxUpdateShowConditionsObjects {
    @SerializeKey("edited")
    private Map<String, AjaxUpdateShowConditions> edited;

    @SerializeKey("deleted")
    private List<String> deleted;

    @SerializeKey("added")
    private List<AjaxUpdateShowConditions> added;

    public AjaxUpdateShowConditionsObjects withAdded(List<AjaxUpdateShowConditions> added) {
        this.added = added;
        return this;
    }

    public AjaxUpdateShowConditionsObjects withAdded(AjaxUpdateShowConditions... added) {
        this.added = Arrays.asList(added);
        return this;
    }

    public Map getEdited() {
        return edited;
    }

    public void setEdited(Map<String, AjaxUpdateShowConditions> phrases) {
        this.edited = new HashedMap<>();
        for (Map.Entry<String, AjaxUpdateShowConditions> phrase : phrases.entrySet()) {
            edited.put(phrase.getKey(), phrase.getValue());
        }
    }

    public List<String> getDeleted() {
        return deleted;
    }

    public void setDeleted(String... phraseIds) {
        this.deleted = new ArrayList<>();
        deleted.addAll(Arrays.asList(phraseIds));
    }

    public AjaxUpdateShowConditionsObjects withDeleted(String... phraseIds) {
        this.deleted = new ArrayList<>();
        deleted.addAll(Arrays.asList(phraseIds));
        return this;
    }

    public AjaxUpdateShowConditionsObjects withDeleted(List<String> phraseIds) {
        this.deleted = new ArrayList<>();
        deleted.addAll(phraseIds);
        return this;
    }

    public AjaxUpdateShowConditionsObjects withEdited(Map<String, AjaxUpdateShowConditions> edited) {
        this.edited = edited;
        return this;
    }

    public AjaxUpdateShowConditionsObjects withSuspended(String[] phrasesId, boolean isSuspended) {
        if (edited == null) edited = new HashMap<>();
        Stream.of(phrasesId).forEach(t ->
                edited.put(t, new AjaxUpdateShowConditions()
                        .withIsSuspended(isSuspended ? "1" : "0")));
        return this;
    }

    public AjaxUpdateShowConditionsObjects withEdited(String phraseId, AjaxUpdateShowConditions condition) {
        if (edited == null) edited = new HashMap<>();
        edited.put(phraseId, condition);
        return this;
    }

    public List<AjaxUpdateShowConditions> getAdded() {
        return added;
    }

    public String toJson() {
        return RequestBeanToJsonProcessor.toJson(this);
    }
}
