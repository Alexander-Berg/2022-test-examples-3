package ru.yandex.market.crm.campaign.domain.sending;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestEmail {

    @JsonProperty("selected")
    private Boolean selected;

    @JsonProperty("email")
    private String email;

    @SuppressWarnings("unused")
    public TestEmail() {
    }

    public TestEmail(String email, boolean selected) {
        this.email = email;
        this.selected = selected;
    }

    public TestEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (null == obj || !getClass().equals(obj.getClass())) {
            return false;
        }
        TestEmail other = (TestEmail) obj;
        return Objects.equals(this.email, other.email);
    }

    public String getEmail() {
        return email;
    }

    public TestEmail setEmail(String email) {
        this.email = email;
        return this;
    }

    public boolean isSelected() {
        return Boolean.TRUE.equals(selected);
    }

    public TestEmail setSelected(boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

}
