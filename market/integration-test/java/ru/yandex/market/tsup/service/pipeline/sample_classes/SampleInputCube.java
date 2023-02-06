package ru.yandex.market.tsup.service.pipeline.sample_classes;

import ru.yandex.market.tsup.core.pipeline.Cube;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeName;

public class SampleInputCube implements Cube<SampleTransportationData, OutCubeData> {

    @Override
    public OutCubeData execute(SampleTransportationData input) {
        return new OutCubeData(1L);
    }

    @Override
    public PipelineCubeName getCubeName() {
        return PipelineCubeName.ROUTE_SCHEDULE_CREATOR;
    }
}
