package ru.yandex.autotests.direct.cmd.data.clients;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class SearchClientIDRequest  extends BasicDirectRequest {

    @SerializeKey("client_login")
    private String clientLogin;

    @SerializeKey("name")
    private String name;

    @SerializeKey("from")
    private String from;

    @SerializeKey("client_country")
    private Integer searchCountry;

    @SerializeKey("currency")
    private String searchCurrency;

    @SerializeKey("phone")
    private String phone;

    @SerializeKey("email")
    private String email;

    @SerializeKey("ClientID")
    private String clientId;

    @SerializeKey("submit_search")
    private String submitSearch;

    @SerializeKey("submit_create")
    private String submitCreate;

    public String getSubmitCreate() {
        return submitCreate;
    }

    public SearchClientIDRequest withSubmitCreate(String submitCreate) {
        this.submitCreate = submitCreate;
        return this;
    }

    public String getClientLogin() {
        return clientLogin;
    }

    public SearchClientIDRequest withClientLogin(String clientLogin) {
        this.clientLogin = clientLogin;
        return this;
    }

    public String getName() {
        return name;
    }

    public SearchClientIDRequest withName(String name) {
        this.name = name;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public SearchClientIDRequest withFrom(String from) {
        this.from = from;
        return this;
    }

    public Integer getSearchCountry() {
        return searchCountry;
    }

    public SearchClientIDRequest withSearchCountry(Integer searchCountry) {
        this.searchCountry = searchCountry;
        return this;
    }

    public String getSearchCurrency() {
        return searchCurrency;
    }

    public SearchClientIDRequest withSearchCurrency(String searchCurrency) {
        this.searchCurrency = searchCurrency;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public SearchClientIDRequest withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public SearchClientIDRequest withEmail(String email) {
        this.email = email;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public SearchClientIDRequest withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getSubmitSearch() {
        return submitSearch;
    }

    public SearchClientIDRequest withSubmitSearch(String submitSearch) {
        this.submitSearch = submitSearch;
        return this;
    }
}
