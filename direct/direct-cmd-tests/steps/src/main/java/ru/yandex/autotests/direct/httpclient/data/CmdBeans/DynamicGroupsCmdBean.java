package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;


public class DynamicGroupsCmdBean extends JsonStringTransformableCmdBean {

    private static final String GROUPS_PATH = "groups";

    @JsonPath(requestPath = GROUPS_PATH, responsePath = "/" + GROUPS_PATH)
    private List<DynamicGroupCmdBean> groups;

    @Override
    public String toJson() {
        String nameValuePair = super.toJson();
        if (groups == null) {
            return nameValuePair;
        }
        JsonElement jsonTree = new JsonParser().parse(nameValuePair);
        return jsonTree.getAsJsonObject().get(GROUPS_PATH).toString();
    }

    public List<DynamicGroupCmdBean> getGroups() {
        return groups;
    }

    public void setGroups(List<DynamicGroupCmdBean> groups) {
        this.groups = groups;
    }
}