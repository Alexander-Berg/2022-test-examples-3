package ru.yandex.direct.grid.processing.service.operator;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestUsers;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignStrategyName;
import ru.yandex.direct.grid.processing.model.client.GdClientFeatures;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class OperatorAccessServiceCanUseRoiStrategyTest
        extends OperatorAccessServiceBaseTest {

    private static final long OPERATOR_UID = RandomNumberUtils.nextPositiveLong();

    @Parameterized.Parameter(value = 0)
    public boolean isCrrStrategyAllowed;

    @Parameterized.Parameter(value = 1)
    public boolean hasRoiStrategy;

    @Parameterized.Parameter(value = 2)
    public boolean isArchived;

    @Parameterized.Parameter(value = 3)
    public boolean canUseRoiStrategy;

    @Parameterized.Parameters(name = "CRR_STRATEGY_ALLOWED = {0}, HAS_ROI_STRATEGY = {1}, IS_ARCHIVED = {2} Result = " +
            "{3}")
    public static Collection testData() {
        var data = new Object[][]{
                {true, true, true, false},
                {true, true, false, true},
                {true, false, true, false},
                {true, false, false, false},
                {false, true, true, true},
                {false, true, false, true},
                {false, false, true, true},
                {false, false, false, true},
        };

        return Arrays.asList(data);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCanUseRoiStrategy() {
        doReturn(ImmutableList.builder().add(createCampaign(hasRoiStrategy, isArchived)).build())
                .when(campaignInfoService).getAllBaseCampaigns(any(ClientId.class));
        when(clientDataService.getClientFeatures(any(ClientId.class), any(User.class), any()))
                .thenReturn(new GdClientFeatures()
                        .withIsCrrStrategyAllowed(isCrrStrategyAllowed)
                        .withIsInternalAdsAllowed(false)
                );
        var operator = TestUsers.defaultUser()
                .withUid(OPERATOR_UID)
                .withClientId(ClientId.fromLong(OPERATOR_UID))
                .withClientId(ClientId.fromLong(RandomNumberUtils.nextPositiveInteger()));

        GdClientInfo clientInfo = new GdClientInfo()
                .withId(1L)
                .withShard(RandomNumberUtils.nextPositiveInteger(22))
                .withChiefUserId(OPERATOR_UID)
                .withManagersInfo(List.of())
                .withCountryRegionId(RandomNumberUtils.nextPositiveLong());
        var result = operatorAccessService.getAccess(operator, clientInfo, defaultSubjectUser(),
                Instant.now());

        assertThat(result.getCanUseRoiStrategy()).isEqualTo(canUseRoiStrategy);
    }

    private static User defaultSubjectUser() {
        return TestUsers.defaultUser().withUid(123456L);
    }

    private GdiCampaign createCampaign(boolean hasRoiStrategy, boolean isArchived) {
        return new GdiCampaign()
                .withArchived(isArchived)
                .withStrategyName(
                        hasRoiStrategy ? GdiCampaignStrategyName.AUTOBUDGET_ROI : GdiCampaignStrategyName.AUTOBUDGET
                );
    }
}
