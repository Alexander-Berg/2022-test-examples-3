package ru.yandex.market.tsup.service.pipeline.sample_classes.enum_data;

import ru.yandex.market.tsup.core.pipeline.Cube;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineCubeName;

public class SampleEnumCube implements Cube<SampleEnumInputData, SampleEnumInputData> {

    @Override
    public SampleEnumInputData execute(SampleEnumInputData input) {
        return input;
    }

    @Override
    public PipelineCubeName getCubeName() {
        return PipelineCubeName.ROUTE_SCHEDULE_CREATOR;
    }
}
