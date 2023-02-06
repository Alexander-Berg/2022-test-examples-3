package ru.yandex.autotests.direct.httpclient.data.transfer;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.lang.reflect.Field;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.06.15
 */
public class CampaignToSumRequestBean extends BasicDirectRequestParameters {

    private String campaignId;

    @JsonPath(requestPath = "to")
    private Float campaignToSum;


    @Override
    protected String getFormFieldName(Field field) {
        String name = field.getAnnotation(JsonPath.class).requestPath();
        return name + "__" + getCampaignId();
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public Float getCampaignToSum() {
        return campaignToSum;
    }

    public void setCampaignToSum(Float campaignToSum) {
        this.campaignToSum = campaignToSum;
    }
}
