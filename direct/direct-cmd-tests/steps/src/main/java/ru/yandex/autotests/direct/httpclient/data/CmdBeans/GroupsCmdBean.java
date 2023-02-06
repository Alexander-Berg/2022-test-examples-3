package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shmykov on 29.04.15.
 */
public class GroupsCmdBean extends JsonStringTransformableCmdBean {

    private static final String GROUPS_PATH = "groups";

    @JsonPath(requestPath = GROUPS_PATH, responsePath = "/" + GROUPS_PATH)
    private List<GroupCmdBean> groups;

    @Override
    public String toJson() {
        String nameValuePair = super.toJson();
        if (groups == null) {
            return nameValuePair;
        }
        JsonElement jsonTree = new JsonParser().parse(nameValuePair);
        return jsonTree.getAsJsonObject().get(GROUPS_PATH).toString();
    }


    public List<GroupCmdBean> getGroups() {
        return groups;
    }

    public void addGroup(GroupCmdBean groupCmdBean) {
        if (groups == null) {
            groups = new ArrayList<>();
        }
        groups.add(groupCmdBean);
    }

    public void setGroups(List<GroupCmdBean> groups) {
        this.groups = groups;
    }
}