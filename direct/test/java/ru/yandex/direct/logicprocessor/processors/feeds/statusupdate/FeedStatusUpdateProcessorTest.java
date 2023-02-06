package ru.yandex.direct.logicprocessor.processors.feeds.statusupdate;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.xiva.model.XivaPushesQueueItem;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.ess.logicobjects.feeds.statusupdate.FeedStatusUpdateObject;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.logicprocessor.common.EssLogicProcessorContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedStatusUpdateProcessorTest {
    private FeedStatusUpdateProcessor feedStatusUpdateProcessor;

    @BeforeEach
    public void init() {
        EssLogicProcessorContext essLogicProcessorContext = mock(EssLogicProcessorContext.class);
        FeatureService featureService = mock(FeatureService.class);
        when(featureService.isEnabledForClientId(any(ClientId.class), any(FeatureName.class))).thenReturn(true);
        feedStatusUpdateProcessor = new FeedStatusUpdateProcessor(
                essLogicProcessorContext, null, featureService);
    }

    @Test
    public void testPushesGenerating() {
        List<FeedStatusUpdateObject> logicObjects = new ArrayList<>(){};
        logicObjects.add(new FeedStatusUpdateObject(1L));
        logicObjects.add(new FeedStatusUpdateObject(2L));
        List<XivaPushesQueueItem> got = feedStatusUpdateProcessor.createPushesByLogicObjects(logicObjects);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(got).hasSize(2);
        soft.assertThat(got.get(0).getClientId()).isEqualTo(1L);
        soft.assertThat(got.get(1).getClientId()).isEqualTo(2L);
        soft.assertAll();
    }
}
