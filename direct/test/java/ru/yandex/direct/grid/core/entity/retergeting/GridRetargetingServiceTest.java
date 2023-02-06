package ru.yandex.direct.grid.core.entity.retergeting;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.retargeting.container.RetargetingSelection;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.showcondition.service.GridRetargetingService;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class GridRetargetingServiceTest {
    @Mock
    private RetargetingService retargetingService;

    @InjectMocks
    private GridRetargetingService gridRetargetingService;

    private static final Long OPERATOR_UID = 1L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);
    private static final long AD_GROUP_ID = 1L;
    private List<Long> adGroupIds;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        adGroupIds = singletonList(AD_GROUP_ID);
    }

    @Test
    public void getContainingOfRetargetingsForGroupIds_AdGroupHasRetargeting() {
        long retargetingId = 2L;
        long retargetingConditionId = 3L;

        List<TargetInterest> targetInterests = singletonList(new TargetInterest()
                .withId(retargetingId)
                .withRetargetingConditionId(retargetingConditionId)
                .withAdGroupId(AD_GROUP_ID));
        doReturn(targetInterests)
                .when(retargetingService)
                .getRetargetings(any(), eq(CLIENT_ID), eq(OPERATOR_UID), any());


        Map<Long, Boolean> containingOfRetargetingsByAdGroupId =
                gridRetargetingService.getContainingOfRetargetingsForAdGroupIds(CLIENT_ID, OPERATOR_UID, adGroupIds);

        Map<Long, Boolean> expectedRetargetingsByAdGroupId = new ImmutableMap.Builder<Long, Boolean>()
                .put(AD_GROUP_ID, true)
                .build();

        assertThat(containingOfRetargetingsByAdGroupId)
                .is(matchedBy(beanDiffer(expectedRetargetingsByAdGroupId)));
    }

    @Test
    public void getContainingOfRetargetingsForGroupIds_AdGroupHasNotRetargeting() {
        doReturn(emptyList())
                .when(retargetingService)
                .getRetargetings(eq(new RetargetingSelection().withAdGroupIds(adGroupIds)), eq(CLIENT_ID),
                        eq(OPERATOR_UID), any());


        Map<Long, Boolean> containingOfRetargetingsByAdGroupId =
                gridRetargetingService.getContainingOfRetargetingsForAdGroupIds(CLIENT_ID, OPERATOR_UID, adGroupIds);

        Map<Long, Boolean> expectedRetargetingsByAdGroupId = new ImmutableMap.Builder<Long, Boolean>()
                .put(AD_GROUP_ID, false)
                .build();

        assertThat(containingOfRetargetingsByAdGroupId)
                .is(matchedBy(beanDiffer(expectedRetargetingsByAdGroupId)));
    }
}
