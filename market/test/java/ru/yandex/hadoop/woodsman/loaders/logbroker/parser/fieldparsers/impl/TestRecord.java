package ru.yandex.hadoop.woodsman.loaders.logbroker.parser.fieldparsers.impl;

import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.entities.fieldmarkers.HasPpPof;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.entities.fieldmarkers.HasRequest;

import java.util.Date;

/**
 * @author aostrikov
 */
class TestRecord implements HasPpPof, HasRequest {

    private String request;
    private String pof_raw;
    private Integer pp;

    @Override
    public String getRequest() {
        return request;
    }

    @Override
    public void setRequest(String request) {
        this.request = request;
    }

    @Override
    public Date getEventtime() {
        return null;
    }

    @Override
    public Integer getPp() {
        return pp;
    }

    @Override
    public void setPp(Integer pp) {
        this.pp = pp;
    }

    @Override
    public Integer getPof() {
        return null;
    }

    @Override
    public void setPof(Integer pof) {

    }

    @Override
    public String getPof_raw() {
        return pof_raw;
    }

    @Override
    public void setPof_raw(String pof_raw) {
        this.pof_raw = pof_raw;
    }
}
