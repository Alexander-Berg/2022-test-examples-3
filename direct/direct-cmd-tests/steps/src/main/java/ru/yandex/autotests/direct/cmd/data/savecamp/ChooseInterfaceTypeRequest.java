package ru.yandex.autotests.direct.cmd.data.savecamp;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class ChooseInterfaceTypeRequest extends BasicDirectRequest {
    @SerializeKey("interface_type")
    private String interfaceType;
    @SerializeKey("client_country")
    private String clientCountry;
    @SerializeKey("currency")
    private String currency;
    @SerializeKey("campaign_type")
    private String campaignType;
    @SerializeKey("gdpr_agreement_accepted")
    private String gdprAgreementAccepted;

    public String getInterfaceType() {
        return interfaceType;
    }

    public ChooseInterfaceTypeRequest withInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
        return this;
    }

    public String getClientCountry() {
        return clientCountry;
    }

    public ChooseInterfaceTypeRequest withClientCountry(String clientCountry) {
        this.clientCountry = clientCountry;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public ChooseInterfaceTypeRequest withCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getCampaignType() {
        return campaignType;
    }

    public ChooseInterfaceTypeRequest withCampaignType(String campaignType) {
        this.campaignType = campaignType;
        return this;
    }

    public String getGdprAgreementAccepted() {
        return gdprAgreementAccepted;
    }

    public ChooseInterfaceTypeRequest withGdprAgreementAccepted(String gdprAgreementAccepted) {
        this.gdprAgreementAccepted = gdprAgreementAccepted;
        return this;
    }
}
