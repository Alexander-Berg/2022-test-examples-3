package ru.yandex.direct.core.entity.addition.callout.service;

import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.addition.callout.model.CalloutsStatusModerate;
import ru.yandex.direct.core.entity.addition.callout.repository.CalloutRepository;
import ru.yandex.direct.core.entity.addition.model.AdditionsModerateType;
import ru.yandex.direct.core.entity.addition.model.ModerateAdditions;
import ru.yandex.direct.core.entity.moderation.repository.ModerationRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(MockitoJUnitRunner.class)
@ParametersAreNonnullByDefault
public class RemoderateCalloutsTest {

    private int shard;
    private ClientId clientId;
    private List<Long> calloutIds;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private CalloutRepository calloutRepository;

    @Mock
    private ModerationRepository moderationRepository;

    @InjectMocks
    private CalloutService calloutService;

    @Captor
    private ArgumentCaptor<Collection<ModerateAdditions>> moderateAdditionsCaptor;

    @Before
    public void initTestData() {
        shard = RandomUtils.nextInt(1, 20);
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        calloutIds = ImmutableList.of(RandomNumberUtils.nextPositiveLong(), RandomNumberUtils.nextPositiveLong());

        doReturn(shard)
                .when(shardHelper).getShardByClientIdStrictly(clientId);
    }


    @Test
    public void checkRemoderateCallouts() {
        calloutService.remoderateCallouts(clientId, calloutIds, true);

        check(AdditionsModerateType.AUTO);
    }

    @Test
    public void checkRemoderateCallouts_WhenModerateAcceptIsFalse() {
        calloutService.remoderateCallouts(clientId, calloutIds, false);

        check(AdditionsModerateType.PRE);
    }

    private void check(AdditionsModerateType expectedModerateType) {
        verify(calloutRepository).setStatusModerate(shard, calloutIds, CalloutsStatusModerate.READY);
        verify(moderationRepository).addModerateAdditions(eq(shard), moderateAdditionsCaptor.capture());

        List<ModerateAdditions> expectedModerateAdditions =
                mapList(calloutIds, id -> new ModerateAdditions()
                        .withId(id)
                        .withModerateType(expectedModerateType));
        assertThat(moderateAdditionsCaptor.getValue())
                .is(matchedBy(beanDiffer(expectedModerateAdditions)));
    }

}
