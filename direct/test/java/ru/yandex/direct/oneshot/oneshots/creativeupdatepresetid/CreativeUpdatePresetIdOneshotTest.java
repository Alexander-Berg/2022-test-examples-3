package ru.yandex.direct.oneshot.oneshots.creativeupdatepresetid;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.CreativeSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.oneshot.configuration.OneshotTest;
import ru.yandex.direct.oneshot.oneshots.creativeupdatepresetid.repository.MongoVideoAddtionsRepository;
import ru.yandex.direct.oneshot.oneshots.creativeupdatepresetid.repository.PerfCreativesRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@OneshotTest
@RunWith(SpringRunner.class)
public class CreativeUpdatePresetIdOneshotTest {

    private static final Long CPC_VIDEO_PRESET_ID = 45L;

    @Autowired
    private CreativeRepository creativeRepository;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private CreativeSteps creativeSteps;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private PerfCreativesRepository perfCreativesRepository;

    private int shard;
    private ClientInfo clientInfo;
    private MongoVideoAddtionsRepository mongoVideoAddtionsRepository;
    private CreativeUpdatePresetIdOneshot oneshot;

    @Before
    public void setUp() {
        clientInfo = clientSteps.createDefaultClient();
        shard = clientInfo.getShard();
        mongoVideoAddtionsRepository = mock(MongoVideoAddtionsRepository.class);
        when(mongoVideoAddtionsRepository.updatePresetId(any(), any())).thenReturn(0L);
        oneshot = new CreativeUpdatePresetIdOneshot(shardHelper, mongoVideoAddtionsRepository, perfCreativesRepository);
    }

    @Test
    public void successUpdatePerfCreatives() {
        CreativeInfo creativeInfo = creativeSteps.addDefaultCpmVideoAdditionCreative(clientInfo,
                creativeSteps.getNextCreativeId());
        var creativeIds = Collections.singletonList(creativeInfo.getCreativeId());
        var inputData = new CreativeUpdatePresetIdOneshotData()
                .withCreativeIds(creativeIds)
                .withPresetId(CPC_VIDEO_PRESET_ID);

        oneshot.execute(inputData, null);

        var updatedCreative = creativeRepository.getCreatives(shard, creativeIds).get(0);
        assertThat(updatedCreative.getLayoutId()).isEqualTo(CPC_VIDEO_PRESET_ID);
    }

    @Test
    public void otherCreativesNotChanged() {
        CreativeInfo creativeForUpdateInfo = creativeSteps.addDefaultCpmVideoAdditionCreative(clientInfo,
                creativeSteps.getNextCreativeId());
        CreativeInfo anotherCreativeInfo = creativeSteps.addDefaultCpmVideoAdditionCreative(clientInfo,
                creativeSteps.getNextCreativeId());
        var creativeForUpdateIds = Collections.singletonList(creativeForUpdateInfo.getCreativeId());
        var inputData = new CreativeUpdatePresetIdOneshotData()
                .withCreativeIds(creativeForUpdateIds)
                .withPresetId(CPC_VIDEO_PRESET_ID);

        oneshot.execute(inputData, null);

        var anotherCreative = creativeRepository.getCreatives(shard,
                Collections.singletonList(anotherCreativeInfo.getCreativeId())).get(0);
        assertThat(anotherCreative.getLayoutId()).isEqualTo(6L);
    }
}
