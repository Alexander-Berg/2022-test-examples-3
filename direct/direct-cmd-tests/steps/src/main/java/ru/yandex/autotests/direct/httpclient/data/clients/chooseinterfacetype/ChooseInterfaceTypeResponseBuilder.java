package ru.yandex.autotests.direct.httpclient.data.clients.chooseinterfacetype;

public class ChooseInterfaceTypeResponseBuilder {
    private String currencyChooseEnabled;
    private String clientCountry;
    private String regionId;
    private String interfaceChooseEnabled;
    private String clientRole;
    private String isNewClient;
    private String currency;
    private String userFio;
    private String to;
    private String countryChooseEnabled;
    private String countryCurrencyChooseEnabled;
    private String isEasyUser;
    private String shouldChooseInterface;
    private String problemsWithEasiness;
    private String correctRole;
    private String forceStdInterface;
    private String lang;
    private String uid;
    private String uname;

    public ChooseInterfaceTypeResponseBuilder setCurrencyChooseEnabled(String currencyChooseEnabled) {
        this.currencyChooseEnabled = currencyChooseEnabled;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setClientCountry(String clientCountry) {
        this.clientCountry = clientCountry;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setRegionId(String regionId) {
        this.regionId = regionId;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setInterfaceChooseEnabled(String interfaceChooseEnabled) {
        this.interfaceChooseEnabled = interfaceChooseEnabled;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setClientRole(String clientRole) {
        this.clientRole = clientRole;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setIsNewClient(String isNewClient) {
        this.isNewClient = isNewClient;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setUserFio(String userFio) {
        this.userFio = userFio;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setTo(String to) {
        this.to = to;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setCountryChooseEnabled(String countryChooseEnabled) {
        this.countryChooseEnabled = countryChooseEnabled;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setCountryCurrencyChooseEnabled(String countryCurrencyChooseEnabled) {
        this.countryCurrencyChooseEnabled = countryCurrencyChooseEnabled;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setIsEasyUser(String isEasyUser) {
        this.isEasyUser = isEasyUser;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setShouldChooseInterface(String shouldChooseInterface) {
        this.shouldChooseInterface = shouldChooseInterface;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setProblemsWithEasiness(String problemsWithEasiness) {
        this.problemsWithEasiness = problemsWithEasiness;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setCorrectRole(String correctRole) {
        this.correctRole = correctRole;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setForceStdInterface(String forceStdInterface) {
        this.forceStdInterface = forceStdInterface;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setLang(String lang) {
        this.lang = lang;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setUid(String uid) {
        this.uid = uid;
        return this;
    }

    public ChooseInterfaceTypeResponseBuilder setUname(String uname) {
        this.uname = uname;
        return this;
    }

    public ChooseInterfaceTypeResponse createChooseInterfaceTypeResponse() {
        return new ChooseInterfaceTypeResponse(currencyChooseEnabled, clientCountry, regionId, interfaceChooseEnabled, clientRole, isNewClient, currency, userFio, to, countryChooseEnabled, countryCurrencyChooseEnabled, isEasyUser, shouldChooseInterface, problemsWithEasiness, correctRole, forceStdInterface, lang, uid, uname);
    }
}