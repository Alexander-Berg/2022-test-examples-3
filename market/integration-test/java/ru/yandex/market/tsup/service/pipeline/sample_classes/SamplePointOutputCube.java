package ru.yandex.market.tsup.service.pipeline.sample_classes;

import ru.yandex.market.tsup.core.pipeline.Cube;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeName;

public class SamplePointOutputCube implements Cube<SamplePointData, SamplePointData> {
    @Override
    public SamplePointData execute(SamplePointData input) {
        return null;
    }

    @Override
    public PipelineCubeName getCubeName() {
        return PipelineCubeName.CARRIER_COURIER_CREATOR;
    }
}
