package ru.yandex.market.markup3.yang;

import ru.yandex.toloka.client.staging.TolokaStagingClientFactory;
import ru.yandex.toloka.client.staging.adjuster.AdjusterClient;
import ru.yandex.toloka.client.staging.analytics.AnalyticsClient;
import ru.yandex.toloka.client.staging.task.TaskClient;
import ru.yandex.toloka.client.staging.trait.TraitClient;
import ru.yandex.toloka.client.staging.usertrait.UserTraitClient;

public class TolokaStagingClientFactoryMock implements TolokaStagingClientFactory {

    private TolokaClientMock clientMock;

    public TolokaStagingClientFactoryMock(TolokaClientFactoryImplMock tolokaClientFactoryImplMock) {
        this.clientMock = tolokaClientFactoryImplMock.getClientMock();
    }

    @Override
    public AdjusterClient getAdjusterClient() {
        return null;
    }

    @Override
    public AnalyticsClient getAnalyticsClient() {
        return null;
    }

    @Override
    public TaskClient getTaskClient() {
        return null;
    }

    @Override
    public TraitClient getTraitClient() {
        return clientMock;
    }

    @Override
    public UserTraitClient getUserTraitClient() {
        return null;
    }
}
