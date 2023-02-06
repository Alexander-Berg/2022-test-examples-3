package ru.yandex.market.crm.core.test.utils.report;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author apershukov
 */
public class ReportEntity {

    @JsonProperty("id")
    private String id;

    @JsonProperty("pictures")
    private List<Picture> pictures;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Picture> getPictures() {
        return pictures;
    }

    public void setPictures(List<Picture> pictures) {
        this.pictures = pictures;
    }
}
