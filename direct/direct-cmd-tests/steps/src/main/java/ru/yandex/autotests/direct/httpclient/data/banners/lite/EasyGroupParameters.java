package ru.yandex.autotests.direct.httpclient.data.banners.lite;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.LightGroupCmdBean;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

public class EasyGroupParameters extends BasicDirectRequestParameters {


    public EasyGroupParameters() {
        ignoreEmptyParameters(true);
    }

    @JsonPath(requestPath = "bid")
    private String bid;
    @JsonPath(requestPath = "cid")
    private String cid;
    @JsonPath(requestPath = "adgroup_id")
    private String adgroupId;
    @JsonPath(requestPath = "json_groups")
    private LightGroupCmdBean jsonGroups;
    @JsonPath(requestPath = "oferta")
    private String oferta;
    @JsonPath(requestPath = "fio")
    private String fio;
    @JsonPath(requestPath = "email")
    private String email;
    @JsonPath(requestPath = "currency")
    private String currecy;
    @JsonPath(requestPath = "client_country")
    private String clientCountry;
    @JsonPath(requestPath = "timetarget")
    private String timeTarget;
    @JsonPath(requestPath = "timeTargetMode")
    private String timeTargetMode;
    @JsonPath(requestPath = "time_target_holiday")
    private String time_target_holiday;
    @JsonPath(requestPath = "time_target_holiday_coef")
    private String time_target_holiday_coef;
    @JsonPath(requestPath = "time_target_holiday_dont_show")
    private String time_target_holiday_dont_show;
    @JsonPath(requestPath = "time_target_holiday_from")
    private String time_target_holiday_from;
    @JsonPath(requestPath = "time_target_holiday_to")
    private String time_target_holiday_to;
    @JsonPath(requestPath = "time_target_preset")
    private String time_target_preset;
    @JsonPath(requestPath = "time_target_working_holiday")
    private String time_target_working_holiday;
    @JsonPath(requestPath = "timezone_id")
    private String timezone_id;
    @JsonPath(requestPath = "timezone_text")
    private String timezone_text;
    @JsonPath(requestPath = "interface_type")
    private String interfaceType;
    @JsonPath(requestPath = "notnew")
    private String notNew;

    public String getClientCountry() {
        return clientCountry;
    }

    public void setClientCountry(String clientCountry) {
        this.clientCountry = clientCountry;
    }

    public String getTimeTarget() {
        return timeTarget;
    }

    public void setTimeTarget(String timeTarget) {
        this.timeTarget = timeTarget;
    }

    public String getTimeTargetMode() {
        return timeTargetMode;
    }

    public void setTimeTargetMode(String timeTargetMode) {
        this.timeTargetMode = timeTargetMode;
    }

    public String getTime_target_holiday() {
        return time_target_holiday;
    }

    public void setTime_target_holiday(String time_target_holiday) {
        this.time_target_holiday = time_target_holiday;
    }

    public String getTime_target_holiday_coef() {
        return time_target_holiday_coef;
    }

    public void setTime_target_holiday_coef(String time_target_holiday_coef) {
        this.time_target_holiday_coef = time_target_holiday_coef;
    }

    public String getTime_target_holiday_dont_show() {
        return time_target_holiday_dont_show;
    }

    public void setTime_target_holiday_dont_show(String time_target_holiday_dont_show) {
        this.time_target_holiday_dont_show = time_target_holiday_dont_show;
    }

    public String getTime_target_holiday_from() {
        return time_target_holiday_from;
    }

    public void setTime_target_holiday_from(String time_target_holiday_from) {
        this.time_target_holiday_from = time_target_holiday_from;
    }

    public String getTime_target_holiday_to() {
        return time_target_holiday_to;
    }

    public void setTime_target_holiday_to(String time_target_holiday_to) {
        this.time_target_holiday_to = time_target_holiday_to;
    }

    public String getTime_target_preset() {
        return time_target_preset;
    }

    public void setTime_target_preset(String time_target_preset) {
        this.time_target_preset = time_target_preset;
    }

    public String getTime_target_working_holiday() {
        return time_target_working_holiday;
    }

    public void setTime_target_working_holiday(String time_target_working_holiday) {
        this.time_target_working_holiday = time_target_working_holiday;
    }

    public String getTimezone_id() {
        return timezone_id;
    }

    public void setTimezone_id(String timezone_id) {
        this.timezone_id = timezone_id;
    }

    public String getTimezone_text() {
        return timezone_text;
    }

    public void setTimezone_text(String timezone_text) {
        this.timezone_text = timezone_text;
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getAdgroupId() {
        return adgroupId;
    }

    public void setAdgroupId(String adgroupId) {
        this.adgroupId = adgroupId;
    }

    public LightGroupCmdBean getJsonGroups() {
        return jsonGroups;
    }

    public void setJsonGroups(LightGroupCmdBean jsonGroups) {
        this.jsonGroups = jsonGroups;
    }

    public String getOferta() {
        return oferta;
    }

    public void setOferta(String oferta) {
        this.oferta = oferta;
    }

    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCurrecy() {
        return currecy;
    }

    public void setCurrecy(String currecy) {
        this.currecy = currecy;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getNotNew() {
        return notNew;
    }

    public void setNotNew(String notNew) {
        this.notNew = notNew;
    }
}
