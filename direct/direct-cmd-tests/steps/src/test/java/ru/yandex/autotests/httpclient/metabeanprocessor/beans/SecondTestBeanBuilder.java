package ru.yandex.autotests.httpclient.metabeanprocessor.beans;

import java.util.List;

public class SecondTestBeanBuilder {
    private Integer cid;
    private List<String> fio;
    private List<String> name;
    private String runningUnmoderated;
    private String unnecessaryField;

    public SecondTestBeanBuilder setCid(Integer cid) {
        this.cid = cid;
        return this;
    }

    public SecondTestBeanBuilder setFio(List<String> fio) {
        this.fio = fio;
        return this;
    }

    public SecondTestBeanBuilder setName(List<String> name) {
        this.name = name;
        return this;
    }

    public SecondTestBeanBuilder setRunningUnmoderated(String runningUnmoderated) {
        this.runningUnmoderated = runningUnmoderated;
        return this;
    }

    public SecondTestBeanBuilder setUnnecessaryField(String unnecessaryField) {
        this.unnecessaryField = unnecessaryField;
        return this;
    }

    public SecondTestBean createSecondTestBean() {
        return new SecondTestBean(cid, fio, name, runningUnmoderated, unnecessaryField);
    }
}