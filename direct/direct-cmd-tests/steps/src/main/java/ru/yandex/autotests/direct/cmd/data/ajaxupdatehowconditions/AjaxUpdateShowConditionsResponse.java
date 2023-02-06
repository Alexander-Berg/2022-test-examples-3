package ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AjaxUpdateShowConditionsResponse extends LinkedHashMap<String, AjaxUpdateShowConditionsGroup> {
    public List<AjaxUpdateShowConditionsGroup> getGroups() {
        return this.values().stream().collect(Collectors.toList());
    }

    private void setGroups(Map<String, AjaxUpdateShowConditionsGroup> groups) {
        this.putAll(groups);
    }

    public AjaxUpdateShowConditionsResponse withGroups(Map<String, AjaxUpdateShowConditionsGroup> groups) {
        this.putAll(groups);
        return this;
    }
}
