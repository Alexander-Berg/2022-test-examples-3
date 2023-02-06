package ru.yandex.autotests.direct.httpclient.data.clients.chooseinterfacetype;


import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
* @author : Alex Samokhin (alex-samo@yandex-team.ru)
*         Date: 30.01.15
*/
public class ChooseInterfaceTypeResponse {

    @JsonPath(responsePath = "currency_choose_enabled")
    private String currencyChooseEnabled;

    @JsonPath(responsePath = "client_country")
    private String clientCountry;

    @JsonPath(responsePath = "client_country/region_id")
    private String regionId;

    @JsonPath(responsePath = "interface_choose_enabled")
    private String interfaceChooseEnabled;

    @JsonPath(responsePath = "client_role")
    private String clientRole;

    @JsonPath(responsePath = "is_new_client")
    private String isNewClient;

    @JsonPath(responsePath = "currency")
    private String currency;

    @JsonPath(responsePath = "user_fio")
    private String userFio;

    @JsonPath(responsePath = "to")
    private String to;

    @JsonPath(responsePath = "country_choose_enabled")
    private String countryChooseEnabled;

    @JsonPath(responsePath = "country_currency_choose_enabled")
    private String countryCurrencyChooseEnabled;

    @JsonPath(responsePath = "easy_props/is_easy_user")
    private String isEasyUser;

    @JsonPath(responsePath = "easy_props/should_choose_interface")
    private String shouldChooseInterface;

    @JsonPath(responsePath = "easy_props/problems_with_easiness")
    private String problemsWithEasiness;

    @JsonPath(responsePath = "correct_role")
    private String correctRole;

    @JsonPath(responsePath = "force_std_interface")
    private String forceStdInterface;

    @JsonPath(responsePath = "lang")
    private String lang;

    @JsonPath(responsePath = "uid")
    private String uid;

    @JsonPath(responsePath = "uname")
    private String uname;

    public String getCurrencyChooseEnabled() {
        return currencyChooseEnabled;
    }

    public void setCurrencyChooseEnabled(String currencyChooseEnabled) {
        this.currencyChooseEnabled = currencyChooseEnabled;
    }

    public String getClientCountry() {
        return clientCountry;
    }

    public void setClientCountry(String clientCountry) {
        this.clientCountry = clientCountry;
    }

    public String getInterfaceChooseEnabled() {
        return interfaceChooseEnabled;
    }

    public void setInterfaceChooseEnabled(String interfaceChooseEnabled) {
        this.interfaceChooseEnabled = interfaceChooseEnabled;
    }

    public String getClientRole() {
        return clientRole;
    }

    public void setClientRole(String clientRole) {
        this.clientRole = clientRole;
    }

    public String getIsNewClient() {
        return isNewClient;
    }

    public void setIsNewClient(String isNewClient) {
        this.isNewClient = isNewClient;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getUserFio() {
        return userFio;
    }

    public void setUserFio(String userFio) {
        this.userFio = userFio;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getCountryChooseEnabled() {
        return countryChooseEnabled;
    }

    public void setCountryChooseEnabled(String countryChooseEnabled) {
        this.countryChooseEnabled = countryChooseEnabled;
    }

    public String getCountryCurrencyChooseEnabled() {
        return countryCurrencyChooseEnabled;
    }

    public void setCountryCurrencyChooseEnabled(String countryCurrencyChooseEnabled) {
        this.countryCurrencyChooseEnabled = countryCurrencyChooseEnabled;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getIsEasyUser() {
        return isEasyUser;
    }

    public void setIsEasyUser(String isEasyUser) {
        this.isEasyUser = isEasyUser;
    }

    public String getShouldChooseInterface() {
        return shouldChooseInterface;
    }

    public void setShouldChooseInterface(String shouldChooseInterface) {
        this.shouldChooseInterface = shouldChooseInterface;
    }

    public String getProblemsWithEasiness() {
        return problemsWithEasiness;
    }

    public void setProblemsWithEasiness(String problemsWithEasiness) {
        this.problemsWithEasiness = problemsWithEasiness;
    }

    public String getCorrectRole() {
        return correctRole;
    }

    public void setCorrectRole(String correctRole) {
        this.correctRole = correctRole;
    }

    public String getForceStdInterface() {
        return forceStdInterface;
    }

    public void setForceStdInterface(String forceStdInterface) {
        this.forceStdInterface = forceStdInterface;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public ChooseInterfaceTypeResponse(String currencyChooseEnabled, String clientCountry, String regionId, String interfaceChooseEnabled, String clientRole, String isNewClient, String currency, String userFio, String to, String countryChooseEnabled, String countryCurrencyChooseEnabled, String isEasyUser, String shouldChooseInterface, String problemsWithEasiness, String correctRole, String forceStdInterface, String lang, String uid, String uname) {
        this.currencyChooseEnabled = currencyChooseEnabled;
        this.clientCountry = clientCountry;
        this.regionId = regionId;
        this.interfaceChooseEnabled = interfaceChooseEnabled;
        this.clientRole = clientRole;
        this.isNewClient = isNewClient;
        this.currency = currency;
        this.userFio = userFio;
        this.to = to;
        this.countryChooseEnabled = countryChooseEnabled;
        this.countryCurrencyChooseEnabled = countryCurrencyChooseEnabled;
        this.isEasyUser = isEasyUser;
        this.shouldChooseInterface = shouldChooseInterface;
        this.problemsWithEasiness = problemsWithEasiness;
        this.correctRole = correctRole;
        this.forceStdInterface = forceStdInterface;
        this.lang = lang;
        this.uid = uid;
        this.uname = uname;
    }

    public ChooseInterfaceTypeResponse() {
    }
}
