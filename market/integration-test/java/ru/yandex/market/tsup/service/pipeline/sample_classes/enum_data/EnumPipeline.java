package ru.yandex.market.tsup.service.pipeline.sample_classes.enum_data;

import ru.yandex.market.tsup.core.pipeline.Pipeline;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineName;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineNameImpl;

public class EnumPipeline implements Pipeline<SampleEnumPayloadData> {
    @Override
    public Class<SampleEnumPayloadData> getPayloadClass() {
        return SampleEnumPayloadData.class;
    }

    @Override
    public PipelineName getPipelineName() {
        return PipelineNameImpl.QUICK_TRIP_CREATOR;
    }
}
