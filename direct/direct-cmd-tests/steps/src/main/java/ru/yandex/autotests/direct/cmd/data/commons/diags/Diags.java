package ru.yandex.autotests.direct.cmd.data.commons.diags;

import com.google.gson.annotations.SerializedName;

public class Diags {

    @SerializedName("allow_first_aid")
    private String allowFirstAid;

    @SerializedName("token")
    private String token;

    @SerializedName("bad_reason")
    private String badReason;

    @SerializedName("diag_id")
    private String diagId;

    @SerializedName("show_details_url")
    private String showDetailsUrl;

    @SerializedName("diag_text")
    private String diagText;

    public String getAllowFirstAid() {
        return allowFirstAid;
    }

    public Diags withAllowFirstAid(String allowFirstAid) {
        this.allowFirstAid = allowFirstAid;
        return this;
    }

    public String getToken() {
        return token;
    }

    public Diags withToken(String token) {
        this.token = token;
        return this;
    }

    public String getBadReason() {
        return badReason;
    }

    public Diags withBadReason(String badReason) {
        this.badReason = badReason;
        return this;
    }

    public String getDiagId() {
        return diagId;
    }

    public Diags withDiagId(String diagId) {
        this.diagId = diagId;
        return this;
    }

    public String getShowDetailsUrl() {
        return showDetailsUrl;
    }

    public Diags withShowDetailsUrl(String showDetailsUrl) {
        this.showDetailsUrl = showDetailsUrl;
        return this;
    }

    public String getDiagText() {
        return diagText;
    }

    public Diags withDiagText(String diagText) {
        this.diagText = diagText;
        return this;
    }
}
