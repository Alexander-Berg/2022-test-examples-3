package ru.yandex.autotests.direct.httpclient.data.clients;

public class SwitchEasinessParametersBuilder {
    private String clientCountry;
    private String currency;
    private String interfaceType;

    public SwitchEasinessParametersBuilder setClientCountry(String clientCountry) {
        this.clientCountry = clientCountry;
        return this;
    }

    public SwitchEasinessParametersBuilder setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public SwitchEasinessParametersBuilder setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
        return this;
    }

    public SwitchEasinessParameters createSwitchEasinessParameters() {
        return new SwitchEasinessParameters(clientCountry, currency, interfaceType);
    }
}