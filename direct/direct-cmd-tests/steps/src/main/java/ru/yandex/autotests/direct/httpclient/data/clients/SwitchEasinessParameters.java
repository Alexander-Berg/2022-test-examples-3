package ru.yandex.autotests.direct.httpclient.data.clients;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class SwitchEasinessParameters extends BasicDirectFormParameters {
    @FormParameter("client_country")
    private String clientCountry;
    @FormParameter("currency")
    private String currency;
    @FormParameter("interface_type")
    private String interfaceType;

    public String getClientCountry() {
        return clientCountry;
    }

    public void setClientCountry(String clientCountry) {
        this.clientCountry = clientCountry;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public SwitchEasinessParameters() {
    }

    public SwitchEasinessParameters(String clientCountry, String currency, String interfaceType) {
        this.clientCountry = clientCountry;
        this.currency = currency;
        this.interfaceType = interfaceType;
    }
}
