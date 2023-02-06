package ru.yandex.autotests.direct.cmd.data.dynamicconditions;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;

import java.util.HashMap;
import java.util.List;

public class DynamicConditionMap {

    @SerializedName("edited")
    private HashMap<String, DynamicCondition> dynamicConditionMap;

    @SerializedName("deleted")
    private List<Long> deleteIds;

    public HashMap<String, DynamicCondition> getDynamicConditionMap() {
        return dynamicConditionMap;
    }

    public void setDynamicConditionMap(HashMap<String, DynamicCondition> dynamicConditionMap) {
        this.dynamicConditionMap = dynamicConditionMap;
    }

    public List<Long> getDeleteIds() {
        return deleteIds;
    }

    public void setDeleteIds(List<Long> deleteIds) {
        this.deleteIds = deleteIds;
    }

    public DynamicConditionMap withDeleteIds(List<Long> deleteIds) {
        this.deleteIds = deleteIds;
        return this;
    }

    public void addDynamicCondition(String conditionId, DynamicCondition condition) {
        if (dynamicConditionMap == null) {
            dynamicConditionMap = new HashMap<>();
        }
        dynamicConditionMap.put(conditionId, condition);
    }
}
