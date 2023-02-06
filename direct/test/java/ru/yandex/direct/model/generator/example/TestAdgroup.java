package ru.yandex.direct.model.generator.example;

import ru.yandex.direct.model.Model;

public interface TestAdgroup extends Model {

    Long getCampaignId();

    void setCampaignId(Long campaignId);

    TestAdgroup withCampaignId(Long campaignId);

    byte[] getBinaryData();
    void setBinaryData(byte[] binaryData);
    TestAdgroup withBinaryData(byte[] binaryData);
}
