package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;

public class TurboLanding {

    @SerializedName("id")
    private Long id;

    @SerializedName("href")
    private String href;

    @SerializedName("is_disabled")
    private Integer isDisabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public Integer getIsDisabled() {
        return isDisabled;
    }

    public void setIsDisabled(Integer isDisabled) {
        this.isDisabled = isDisabled;
    }

    public TurboLanding withId(Long id) {
        this.id = id;
        return this;
    }

    public TurboLanding withHref(String href) {
        this.href = href;
        return this;
    }

    public TurboLanding withIsDisabled(Integer isDisabled) {
        this.isDisabled = isDisabled;
        return this;
    }
}
