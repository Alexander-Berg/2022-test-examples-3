package ru.yandex.autotests.httpclient.metabeanprocessor.beans;


import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 28.01.15
 */
public class SecondTestBean {
    @JsonPath(requestPath = "cid", responsePath = "cidResponse")
    private Integer cid;

    @JsonPath(requestPath = "fio")
    private List<String> fio;

    @JsonPath(responsePath = "nameResponse")
    private List<String> name;

    @JsonPath(requestPath = "banners_status/running_unmoderated")
    private String runningUnmoderated;

    private String unnecessaryField;

    public Integer getCid() {
        return cid;
    }

    public void setCid(Integer cid) {
        this.cid = cid;
    }

    public void setFio(List<String> fio) {
        this.fio = fio;
    }

    public void setRunningUnmoderated(String runningUnmoderated) {
        this.runningUnmoderated = runningUnmoderated;
    }

    public void setUnnecessaryField(String unnecessaryField) {
        this.unnecessaryField = unnecessaryField;
    }

    public void setName(List<String> name) {
        this.name = name;
    }

    public List<String> getFio() {
        return fio;
    }

    public List<String> getName() {
        return name;
    }

    public String getRunningUnmoderated() {
        return runningUnmoderated;
    }

    public String getUnnecessaryField() {
        return unnecessaryField;
    }

    public SecondTestBean(Integer cid, List<String> fio, List<String> name, String runningUnmoderated, String unnecessaryField) {
        this.cid = cid;
        this.fio = fio;
        this.name = name;
        this.runningUnmoderated = runningUnmoderated;
        this.unnecessaryField = unnecessaryField;
    }

    public SecondTestBean() {
    }
}
