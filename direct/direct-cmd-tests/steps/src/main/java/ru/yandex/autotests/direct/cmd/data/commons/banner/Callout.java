package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;

public class Callout extends BannerAddition {

    @SerializedName("callout_text")
    private String calloutText;

    /**
     *
     * @return
     * The calloutText
     */
    public String getCalloutText() {
        return calloutText;
    }

    /**
     *
     * @param calloutText
     * The callout_text
     */
    public void setCalloutText(String calloutText) {
        this.calloutText = calloutText;
    }

    public Callout withCalloutText(String calloutText) {
        this.calloutText = calloutText;
        return this;
    }

}