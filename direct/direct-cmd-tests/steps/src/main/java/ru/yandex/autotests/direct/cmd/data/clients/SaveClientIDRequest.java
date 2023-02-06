package ru.yandex.autotests.direct.cmd.data.clients;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class SaveClientIDRequest extends BasicDirectRequest {

    @SerializeKey("client_login")
    private String clientLogin;

    @SerializeKey("client_id")
    private String clientId;

    @SerializeKey("name")
    private String name;

    @SerializeKey("client_country")
    private Integer clientCountry;

    @SerializeKey("currency")
    private String currency;

    @SerializeKey("phone")
    private String phone;

    @SerializeKey("email")
    private String email;

    @SerializeKey("create_without_wallet")
    private Integer createWithoutWallet;

    @SerializeKey("from")
    private String from;

    public String getClientLogin() {
        return clientLogin;
    }

    public void setClientLogin(String clientLogin) {
        this.clientLogin = clientLogin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public Integer getClientCountry() {
        return clientCountry;
    }

    public void setClientCountry(Integer clientCountry) {
        this.clientCountry = clientCountry;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public SaveClientIDRequest withClientLogin(String login) {
        this.clientLogin = login;
        return this;
    }

    public SaveClientIDRequest withClientCountry(Integer country) {
        this.clientCountry = country;
        return this;
    }

    public SaveClientIDRequest withCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public SaveClientIDRequest withFrom(String from) {
        this.from = from;
        return this;
    }

    public Integer getCreateWithoutWallet() {
        return createWithoutWallet;
    }

    public void setCreateWithoutWallet(Integer createWithoutWallet) {
        this.createWithoutWallet = createWithoutWallet;
    }
}
