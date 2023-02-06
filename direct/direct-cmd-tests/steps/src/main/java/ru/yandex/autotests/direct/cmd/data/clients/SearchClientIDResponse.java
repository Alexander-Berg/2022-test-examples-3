package ru.yandex.autotests.direct.cmd.data.clients;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.List;

public class SearchClientIDResponse {

    @SerializedName("currency")
    private String currency;

    @SerializedName("client_country")
    private String clientСountry;

    @SerializedName("client_login")
    private String clientLogin;

    @SerializedName("client_uid")
    private String clientUid;

    @SerializedName("for_agency")
    private Integer forAgency;

    @SerializedName("countries_currencies")
    private HashMap<String, List<String>> countriesCurrencies;

    public String getClientLogin() {
        return clientLogin;
    }

    public SearchClientIDResponse withClientLogin(String clientLogin) {
        this.clientLogin = clientLogin;
        return this;
    }

    public String getClientUid() {
        return clientUid;
    }

    public SearchClientIDResponse withClientUid(String clientUid) {
        this.clientUid = clientUid;
        return this;
    }

    public Integer getForAgency() {
        return forAgency;
    }

    public SearchClientIDResponse withForAgency(Integer forAgency) {
        this.forAgency = forAgency;
        return this;
    }

    public HashMap<String, List<String>> getCountriesCurrencies() {
        return countriesCurrencies;
    }

    public SearchClientIDResponse withCountriesCurrencies(HashMap<String, List<String>> countriesCurrencies) {
        this.countriesCurrencies = countriesCurrencies;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public SearchClientIDResponse withCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getClientСountry() {
        return clientСountry;
    }

    public SearchClientIDResponse withClientСountry(String clientСountry) {
        this.clientСountry = clientСountry;
        return this;
    }
}
