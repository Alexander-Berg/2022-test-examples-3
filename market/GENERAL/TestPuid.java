package ru.yandex.market.crm.campaign.domain.promo.entities;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestPuid {

    @JsonProperty("selected")
    private Boolean selected;

    @JsonProperty("puid")
    private Long puid;

    @JsonProperty("name")
    private String name;

    public TestPuid() {
    }

    public TestPuid(Long puid) {
        this.puid = puid;
    }

    public TestPuid(Long puid, String name) {
        this.puid = puid;
        this.name = name;
    }

    public TestPuid(Boolean selected, Long puid, String name) {
        this.selected = selected;
        this.puid = puid;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestPuid testUid = (TestPuid) o;
        return Objects.equals(puid, testUid.puid) &&
                Objects.equals(name, testUid.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(puid, name);
    }

    public boolean isSelected() {
        return Boolean.TRUE.equals(selected);
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public Long getPuid() {
        return puid;
    }

    public void setPuid(Long puid) {
        this.puid = puid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
