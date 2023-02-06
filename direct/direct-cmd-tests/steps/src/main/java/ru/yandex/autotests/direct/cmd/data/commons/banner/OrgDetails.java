package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;

public class OrgDetails {
    @SerializedName("org_details_id")
    Long orgDetailsId;

    @SerializedName("ogrn")
    String OGRN;

    public OrgDetails() {}

    public OrgDetails(Long orgDetailsId, String ogrn) {
        this.orgDetailsId = orgDetailsId;
        this.setOGRN(ogrn);
    }

    public String getOGRN() {
        return OGRN;
    }

    public void setOGRN(String OGRN) {
        this.OGRN = OGRN;
    }

    public Long getOrgDetailsId() {
        return orgDetailsId;
    }

    public void setOrgDetailsId(Long orgDetailsId) {
        this.orgDetailsId = orgDetailsId;
    }

    public OrgDetails withOrgDetailsId(Long orgDetailsId) {
        this.orgDetailsId = orgDetailsId;
        return this;
    }

    public OrgDetails withOGRN(String OGRN) {
        this.OGRN = OGRN;
        return this;
    }
}
