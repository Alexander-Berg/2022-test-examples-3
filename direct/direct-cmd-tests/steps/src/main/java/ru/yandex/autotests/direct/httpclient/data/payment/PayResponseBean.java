package ru.yandex.autotests.direct.httpclient.data.payment;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.06.15
 */
public class PayResponseBean {

    @JsonPath(responsePath = "CONSTS/currencies/RUB/DIRECT_DEFAULT_PAY")
    private Float directDefaultPayRub;

    @JsonPath(responsePath = "client")
    private Client client;

    @JsonPath(responsePath = "topay")
    private Float toPay;

    @JsonPath(responsePath = "cid")
    private String campaignId;

    public Float getDirectDefaultPayRub() {
        return directDefaultPayRub;
    }

    public void setDirectDefaultPayRub(Float directDefaultPayRub) {
        this.directDefaultPayRub = directDefaultPayRub;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Float getToPay() {
        return toPay;
    }

    public void setToPay(Float toPay) {
        this.toPay = toPay;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }
}
