package ru.yandex.autotests.direct.cmd.data.dynamicconditions;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

import java.util.HashMap;

public class AjaxEditDynamicConditionsRequest extends BasicDirectRequest {

    public static AjaxEditDynamicConditionsRequest fromDynamicCondition(DynamicCondition condition) {
        DynamicConditionMap dynamicConditionMap = new DynamicConditionMap();
        dynamicConditionMap.addDynamicCondition(condition.getDynId(), condition);
        return new AjaxEditDynamicConditionsRequest()
                .withCondition(condition.getAdGroupId(), dynamicConditionMap);
    }

    @SerializeKey("cid")
    private String cid;

    @SerializeKey("json_adgroup_dynamic_conditions")
    @SerializeBy(ValueToJsonSerializer.class)
    private HashMap<Long, DynamicConditionMap> jsonAdgroupDynamicConditions;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public HashMap<Long, DynamicConditionMap> getJsonAdgroupDynamicConditions() {
        return jsonAdgroupDynamicConditions;
    }

    public void setJsonAdgroupDynamicConditions(HashMap<Long, DynamicConditionMap> jsonAdgroupDynamicConditions) {
        this.jsonAdgroupDynamicConditions = jsonAdgroupDynamicConditions;
    }

    public void addCondition(Long adGroupId, DynamicConditionMap conditionMap) {
        if (jsonAdgroupDynamicConditions == null) {
            jsonAdgroupDynamicConditions = new HashMap<>();
        }
        jsonAdgroupDynamicConditions.put(adGroupId, conditionMap);
    }

    public AjaxEditDynamicConditionsRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public AjaxEditDynamicConditionsRequest withCondition(Long adGroupId, DynamicConditionMap conditionMap) {
        addCondition(adGroupId, conditionMap);
        return this;
    }
}
