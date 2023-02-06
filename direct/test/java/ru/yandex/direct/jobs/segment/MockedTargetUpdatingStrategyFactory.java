package ru.yandex.direct.jobs.segment;

import ru.yandex.direct.core.entity.adgroup.model.ExternalAudienceStatus;
import ru.yandex.direct.jobs.segment.common.result.SegmentUploadResult;
import ru.yandex.direct.jobs.segment.common.target.SegmentTargetUpdatingStrategy;
import ru.yandex.direct.jobs.segment.common.target.SegmentTargetUpdatingStrategyFactory;
import ru.yandex.direct.jobs.segment.common.target.YaAudienceSegmentUploadStrategy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockedTargetUpdatingStrategyFactory extends SegmentTargetUpdatingStrategyFactory {

    private final Long audienceId;

    public MockedTargetUpdatingStrategyFactory(Long audienceId) {
        super(null, null, null, null);
        this.audienceId = audienceId;
    }

    @Override
    public SegmentTargetUpdatingStrategy createUpdatingStrategyForUpdateSegments() {
        return getMockedStrategy();
    }

    @Override
    public SegmentTargetUpdatingStrategy createUpdatingStrategyForCreateSegments() {
        return getMockedStrategy();
    }

    private SegmentTargetUpdatingStrategy getMockedStrategy() {
        YaAudienceSegmentUploadStrategy uploadStrategy = mock(YaAudienceSegmentUploadStrategy.class);
        try {
            when(uploadStrategy.upload(any(), any(), any()))
                    .thenReturn(new SegmentUploadResult(123L, audienceId, ExternalAudienceStatus.IS_PROCESSED));
        } catch (Exception e) {
            // ignore
        }
        return new SegmentTargetUpdatingStrategy(uploadStrategy);
    }
}
