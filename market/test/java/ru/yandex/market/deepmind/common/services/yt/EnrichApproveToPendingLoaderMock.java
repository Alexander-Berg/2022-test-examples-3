package ru.yandex.market.deepmind.common.services.yt;

import java.util.List;

import ru.yandex.market.deepmind.common.services.yt.pojo.EnrichApproveToPendingYtInfo;
import ru.yandex.market.deepmind.common.services.yt.pojo.EnrichApproveToPendingYtLoadRequest;

public class EnrichApproveToPendingLoaderMock extends AbstractLoader<EnrichApproveToPendingYtInfo,
    EnrichApproveToPendingYtLoadRequest> {
    private final List<EnrichApproveToPendingYtInfo> result;

    public EnrichApproveToPendingLoaderMock(List<EnrichApproveToPendingYtInfo> result) {
        this.result = result;
    }

    @Override
    public String getQuery(EnrichApproveToPendingYtLoadRequest request) {
        return "";
    }

    @Override
    public List<EnrichApproveToPendingYtInfo> load(EnrichApproveToPendingYtLoadRequest request) {
        return result;
    }
}
