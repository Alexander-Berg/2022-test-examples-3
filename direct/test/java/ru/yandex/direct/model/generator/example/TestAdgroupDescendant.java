package ru.yandex.direct.model.generator.example;

public interface TestAdgroupDescendant extends TestAdgroup {

    @Override
    TestAdgroupDescendant withCampaignId(Long campaignId);

}
