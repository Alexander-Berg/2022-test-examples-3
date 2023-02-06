package ru.yandex.market.tsup.service.pipeline.sample_classes;

import ru.yandex.market.tsup.core.pipeline.Pipeline;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineName;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineNameImpl;

public class SamplePipeline implements Pipeline<SamplePayloadData> {
    @Override
    public Class<SamplePayloadData> getPayloadClass() {
        return SamplePayloadData.class;
    }

    @Override
    public PipelineName getPipelineName() {
        return PipelineNameImpl.QUICK_TRIP_CREATOR;
    }
}
