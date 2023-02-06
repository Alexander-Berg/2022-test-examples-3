package ru.yandex.autotests.direct.httpclient.data.CmdBeans.tags;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by shmykov on 27.03.15.
 */
public class TagsCmdBean {

    @JsonPath(responsePath = "campaign/tags")
    private List<BannerTagCmdBean> tags;

    @JsonPath(responsePath = "campaign")
    private String camp;

    public List<BannerTagCmdBean> getTags() {
        return tags;
    }

    public void setTags(List<BannerTagCmdBean> tags) {
        this.tags = tags;
    }

    public String getCamp() {
        return camp;
    }

    public void setCamp(String camp) {
        this.camp = camp;
    }
}
