package ru.yandex.autotests.direct.cmd.data.clients;

import com.google.gson.annotations.SerializedName;

public class ClientData {

    @SerializedName("country")
    private Long country;

    @SerializedName("email")
    private String email;

    @SerializedName("gdpr_agreement_accepted")
    private Integer gdprAgreementAccepted;

    @SerializedName("currency")
    private String currency;

    public Long getCountry() {
        return country;
    }

    public void setCountry(Long country) {
        this.country = country;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getGdprAgreementAccepted() {
        return gdprAgreementAccepted;
    }

    public void setGdprAgreementAccepted(Integer gdprAgreementAccepted) {
        this.gdprAgreementAccepted = gdprAgreementAccepted;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public ClientData withCountry(Long country) {
        this.country = country;
        return this;
    }

    public ClientData withEmail(String email) {
        this.email = email;
        return this;
    }

    public ClientData withGdprAgreementAccepted(Integer gdprAgreementAccepted) {
        this.gdprAgreementAccepted = gdprAgreementAccepted;
        return this;
    }

    public ClientData withCurrency(String currency) {
        this.currency = currency;
        return this;
    }
}
