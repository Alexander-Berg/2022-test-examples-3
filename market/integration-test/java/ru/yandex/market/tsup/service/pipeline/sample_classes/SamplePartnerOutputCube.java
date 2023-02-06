package ru.yandex.market.tsup.service.pipeline.sample_classes;

import ru.yandex.market.tsup.core.pipeline.Cube;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeName;

public class SamplePartnerOutputCube implements Cube<OutCubeData, SamplePartnerData> {
    @Override
    public SamplePartnerData execute(OutCubeData input) {
        return null;
    }

    @Override
    public PipelineCubeName getCubeName() {
        return PipelineCubeName.CARRIER_TRANSPORT_CREATOR;
    }
}
