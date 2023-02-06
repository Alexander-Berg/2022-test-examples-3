package ru.yandex.autotests.direct.httpclient.data.banners.lite;

import ru.yandex.autotests.direct.httpclient.data.CmdBeans.LightGroupCmdBean;
import ru.yandex.autotests.direct.httpclient.data.timetargeting.TimeTargetInfoCmd;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by shmykov on 14.04.15.
 */
public class EditBannerEasyResponseBean {

    @JsonPath(responsePath = "campaign/groups", requestPath = "groups")
    private List<LightGroupCmdBean> groups;

    @JsonPath(responsePath = "easy_direct")
    private String easyDirect;

    @JsonPath(responsePath = "campaign/budget_strategy")
    private String budgetStrategy;

    @JsonPath(responsePath = "campaign/timetargeting")
    private TimeTargetInfoCmd timetargeting;

    @JsonPath(responsePath = "new_banner")
    private Integer newBanner;

    @JsonPath(responsePath = "need_oferta")
    private Integer needOferta;

    @JsonPath(responsePath = "show_timetarget")
    private Integer showTimeTarget;


    public List<LightGroupCmdBean> getGroups() {
        return groups;
    }

    public void setGroups(List<LightGroupCmdBean> groups) {
        this.groups = groups;
    }

    public String getEasyDirect() {
        return easyDirect;
    }

    public void setEasyDirect(String easyDirect) {
        this.easyDirect = easyDirect;
    }

    public String getBudgetStrategy() {
        return budgetStrategy;
    }

    public void setBudgetStrategy(String budgetStrategy) {
        this.budgetStrategy = budgetStrategy;
    }

    public TimeTargetInfoCmd getTimetargeting() {
        return timetargeting;
    }

    public void setTimetargeting(TimeTargetInfoCmd timetargeting) {
        this.timetargeting = timetargeting;
    }

    public Integer getNewBanner() {
        return newBanner;
    }

    public void setNewBanner(Integer newBanner) {
        this.newBanner = newBanner;
    }

    public Integer getNeedOferta() {
        return needOferta;
    }

    public void setNeedOferta(Integer needOferta) {
        this.needOferta = needOferta;
    }

    public Integer getShowTimeTarget() {
        return showTimeTarget;
    }

    public void setShowTimeTarget(Integer showTimeTarget) {
        this.showTimeTarget = showTimeTarget;
    }
}
