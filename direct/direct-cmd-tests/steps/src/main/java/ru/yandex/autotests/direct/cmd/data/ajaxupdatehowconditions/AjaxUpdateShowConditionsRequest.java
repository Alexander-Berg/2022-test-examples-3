package ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

import java.util.HashMap;
import java.util.Map;

public class AjaxUpdateShowConditionsRequest extends BasicDirectRequest {
    @SerializeKey("cid")
    private String cid;
    @SerializeKey("copy_group_if_oversized")
    private String copyGroupIfOversized;
    @SerializeKey("json_phrases")
    @SerializeBy(ValueToJsonSerializer.class)
    private Map<String, AjaxUpdateShowConditionsObjects> jsonPhrases;
    @SerializeKey("json_adgroup_retargetings")
    @SerializeBy(ValueToJsonSerializer.class)
    private Map<String, AjaxUpdateShowConditionsObjects> jsonRetargetings;

    @SerializeKey("json_adgroup_target_interests")
    @SerializeBy(ValueToJsonSerializer.class)
    private Map<String, AjaxUpdateShowConditionsObjects> jsonInterests;

    @SerializeKey("json_relevance_match")
    @SerializeBy(ValueToJsonSerializer.class)
    private Map<Long, AjaxUpdateShowConditionsObjects> jsonRelevanceMatch;

    public AjaxUpdateShowConditionsRequest() {
        jsonPhrases = new HashMap<>();
        jsonRetargetings = new HashMap<>();
        jsonInterests = new HashMap<>();
        jsonRelevanceMatch = new HashMap<>();
    }

    public Map<String, AjaxUpdateShowConditionsObjects> getJsonInterests() {
        return jsonInterests;
    }

    public AjaxUpdateShowConditionsRequest withJsonInterests(Map<String, AjaxUpdateShowConditionsObjects> jsonInterests) {
        this.jsonInterests = jsonInterests;
        return this;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public AjaxUpdateShowConditionsRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public Map<String, AjaxUpdateShowConditionsObjects> getJsonPhrases() {
        return jsonPhrases;
    }

    public AjaxUpdateShowConditionsRequest withJsonPhrases(Map<String, AjaxUpdateShowConditionsObjects> jsonPhrases) {
        this.jsonPhrases = jsonPhrases;
        return this;
    }

    public AjaxUpdateShowConditionsRequest withJsonRetargetings(Map<String,
            AjaxUpdateShowConditionsObjects> jsonRetargetings)
    {
        this.jsonRetargetings = jsonRetargetings;
        return this;
    }

    public void setGroupPhrases(String adgroupId, AjaxUpdateShowConditionsObjects bannerPhrases) {
        addGroupPhrases(adgroupId, bannerPhrases);
    }

    public AjaxUpdateShowConditionsRequest withGroupPhrases(String adgroupId,
                                                            AjaxUpdateShowConditionsObjects bannerPhrases)
    {
        addGroupPhrases(adgroupId, bannerPhrases);
        return this;
    }

    public void addGroupPhrases(String adgroupId, AjaxUpdateShowConditionsObjects bannerPhrases) {
        jsonPhrases.put(adgroupId, bannerPhrases);
    }

    public void addInterest(String adgroupId, AjaxUpdateShowConditionsObjects interest) {
        jsonInterests.put(adgroupId, interest);
    }

    public Map<String, AjaxUpdateShowConditionsObjects> getJsonRetargetings() {
        return jsonRetargetings;
    }

    public AjaxUpdateShowConditionsRequest withRetargetings(String adgroupId,
                                                            AjaxUpdateShowConditionsObjects bannerRetargetings)
    {
        jsonRetargetings.put(adgroupId, bannerRetargetings);
        return this;
    }

    public AjaxUpdateShowConditionsRequest withInterest(String adgroupId,
                                                        AjaxUpdateShowConditionsObjects interest)
    {
        jsonInterests.put(adgroupId, interest);
        return this;
    }

    public Map<Long, AjaxUpdateShowConditionsObjects> getJsonRelevanceMatch() {
        return jsonRelevanceMatch;
    }

    public AjaxUpdateShowConditionsRequest withJsonRelevanceMatch(Map<Long, AjaxUpdateShowConditionsObjects> jsonRelevanceMatch) {
        this.jsonRelevanceMatch = jsonRelevanceMatch;
        return this;
    }

    public AjaxUpdateShowConditionsRequest withRelevanceMatch(Long adgroupId, AjaxUpdateShowConditionsObjects relevanceMatch) {
        jsonRelevanceMatch.put(adgroupId, relevanceMatch);
        return this;
    }

    public AjaxUpdateShowConditionsRequest withCopyGroupIfOversized(String copyGroupIfOversized) {
        this.copyGroupIfOversized = copyGroupIfOversized;
        return this;
    }
}
