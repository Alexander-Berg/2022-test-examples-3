package ru.yandex.autotests.direct.httpclient.data.payment.payForAll;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.lang.reflect.Field;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.06.15
 */
public class CampaignSumRequestBean extends BasicDirectRequestParameters {

    private String campaignId;

    @JsonPath(requestPath = "sum")
    private String sum;


    @Override
    protected String getFormFieldName(Field field) {
        String name = field.getAnnotation(JsonPath.class).requestPath();
        return name + "_" + getCampaignId();
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getSum() {
        return sum;
    }

    public void setSum(String sum) {
        this.sum = sum;
    }
}
