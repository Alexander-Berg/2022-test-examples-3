package ru.yandex.market.tsup.core.pipeline.pipeline;

import org.springframework.stereotype.Component;

import ru.yandex.market.tsup.core.pipeline.Pipeline;
import ru.yandex.market.tsup.core.pipeline.data.StringIntPayload;
import ru.yandex.market.tsup.domain.entity.TestPipelineName;
import ru.yandex.market.tsup.domain.entity.pipeline.PipelineName;

@Component
public class TmpSimplePipeline implements Pipeline<StringIntPayload> {
    @Override
    public Class<StringIntPayload> getPayloadClass() {
        return StringIntPayload.class;
    }

    @Override
    public PipelineName getPipelineName() {
        return TestPipelineName.TEST_SIMPLE_PIPELINE;
    }
}
