package ru.yandex.market.load.admin.clients;

import java.util.List;

import org.mockito.Mockito;

import ru.yandex.market.load.admin.client.SimplifiedTsumApiClient;
import ru.yandex.mj.generated.client.tsum.model.ReleaseLaunchInfo;
import ru.yandex.mj.generated.client.tsum.model.Resource;

public class SimplifiedTsumApiClientTestImpl implements SimplifiedTsumApiClient {
    private int pipeline = 0;
    @Override
    public String getTicket(String releaseId) {
        return "MARKETLOAD-TEST";
    }

    @Override
    public ReleaseLaunchInfo getPipelineInfo(String releaseId) {
        return Mockito.mock(ReleaseLaunchInfo.class);
    }

    @Override
    public String cancelPipeline(String releaseId, String message) {
        return releaseId;
    }

    @Override
    public String launchPipeline(String pipeId, List<Resource> resources) {
        return "releaseId-" + (pipeline++);
    }
}
