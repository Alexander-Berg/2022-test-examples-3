package ru.yandex.direct.grid.processing.service.group;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.dataloader.DataLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupAccess;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroup;
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.service.group.loader.AdGroupsHasCalloutsDataLoader;
import ru.yandex.direct.grid.processing.service.group.loader.AdGroupsHasShowConditionsDataLoader;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdAdGroupAccess;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class AdGroupAccessForInternalActionTest {

    private GridGraphQLContext gridGraphQLContext;

    private User operator;

    private Long adGroupId1 = 13L;
    private Long adGroupId2 = 14L;

    @Mock
    private AdGroupsHasCalloutsDataLoader adGroupsHasCalloutsDataLoader;

    @Mock
    private AdGroupsHasShowConditionsDataLoader adGroupsHasShowConditionsDataLoader;

    @Mock
    private DataLoader<Long, Boolean> dataLoaderMock;

    @Mock
    private GridContextProvider gridContextProvider;

    @Mock
    private CampaignInfoService campaignInfoService;

    @InjectMocks
    private GroupDataService groupDataService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        gridGraphQLContext = ContextHelper.buildDefaultContext();
        fillContext(gridGraphQLContext, List.of(adGroupId1, adGroupId2));
        operator = gridGraphQLContext.getOperator();

        //noinspection ResultOfMethodCallIgnored
        doReturn(gridGraphQLContext)
                .when(gridContextProvider).getGridContext();

        doReturn(dataLoaderMock)
                .when(adGroupsHasCalloutsDataLoader).get();
        doReturn(dataLoaderMock)
                .when(adGroupsHasShowConditionsDataLoader).get();
    }

    private void fillContext(GridGraphQLContext context, List<Long> adGroupIds) {
        Long campaignId = 384L;
        List<GdAdGroup> gdAdGroups = adGroupIds.stream()
                .map(adGroupId -> new GdTextAdGroup()
                        .withId(adGroupId)
                        .withCampaignId(campaignId))
                .collect(toList());
        GdiCampaign campaign = new GdiCampaign()
                .withId(campaignId)
                .withType(CampaignType.TEXT);
        context.setGdAdGroups(gdAdGroups);
        context.setClientGdiCampaigns(ImmutableList.of(campaign));
    }

    @SuppressWarnings("unused")
    private Object[] accessFlagExtractors() {
        GdAdGroupAccess gdAdGroupAccess = defaultGdAdGroupAccess(adGroupId1)
                .withMainAdStatusModerate(BannerStatusModerate.READY);
        GdAdGroupAccess gdAdGroupAccessForSendToModerationAction = defaultGdAdGroupAccess(adGroupId2)
                .withMainAdStatusModerate(BannerStatusModerate.NEW);

        return new Object[][]{
                {"getCanBeSentToBSAdGroup",
                        (BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Boolean>>)
                                (service, gridGraphQLContext) ->
                                        service.getCanBeSentToBSAdGroup(gdAdGroupAccess)
                },
                {"getCanBeSentToModerationAdGroup",
                        (BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Boolean>>)
                                (service, gridGraphQLContext) ->
                                        service.getCanBeSentToModerationAdGroup(gdAdGroupAccessForSendToModerationAction)
                },
                {"getCanBeSentToRemoderationAdGroup",
                        (BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Boolean>>)
                                (service, gridGraphQLContext) ->
                                        service.getCanBeSentToRemoderationAdGroup(gdAdGroupAccess)
                },
                {"getCanAcceptModerationAdGroup",
                        (BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Boolean>>)
                                (service, gridGraphQLContext) ->
                                        service.getCanAcceptModerationAdGroup(gdAdGroupAccess)
                },
                {"getCanRemoderateAdsCallouts",
                        (BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Boolean>>)
                                (service, gridGraphQLContext) ->
                                        service.getCanRemoderateAdsCallouts(gdAdGroupAccess.getAdGroupId())
                },
                {"canAcceptAdsCalloutsModeration",
                        (BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Boolean>>)
                                (service, gridGraphQLContext) ->
                                        service.getCanAcceptAdsCalloutsModeration(gdAdGroupAccess.getAdGroupId())
                },
        };
    }

    @Test
    @Parameters(method = "accessFlagExtractors")
    @TestCaseName("{0} is true")
    public void checkGetAccessFlag(
            @SuppressWarnings("unused") String accessFlagName,
            BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Boolean>> accessFlagExtractor) {
        //для сапорта операция доступна
        operator.setRole(RbacRole.SUPPORT);
        doReturn(CompletableFuture.completedFuture(true))
                .when(dataLoaderMock).load(anyLong());

        CompletableFuture<Boolean> accessFlagValue = accessFlagExtractor.apply(groupDataService, gridGraphQLContext);
        assertThat(accessFlagValue.join())
                .isTrue();
        verify(dataLoaderMock).load(anyLong());
    }

    @Test
    @Parameters(method = "accessFlagExtractors")
    @TestCaseName("{0} is false when access denied for operator")
    public void checkGetAccessFlag_WhenAccessDeniedForOperator(
            @SuppressWarnings("unused") String accessFlagName,
            BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Boolean>> accessFlagExtractor) {
        fillContext(gridGraphQLContext, List.of(adGroupId1, adGroupId2));
        //для клиента операция недоступна
        operator.setRole(RbacRole.CLIENT);

        CompletableFuture<Boolean> accessFlagValue = accessFlagExtractor.apply(groupDataService, gridGraphQLContext);
        assertThat(accessFlagValue.join())
                .isFalse();
        verifyNoMoreInteractions(dataLoaderMock, adGroupsHasCalloutsDataLoader, adGroupsHasShowConditionsDataLoader);
    }


    @SuppressWarnings("unused")
    private Object[] accessFlagsCountExtractors() {
        List<GdAdGroupAccess> gdAdGroupAccesses = singletonList(defaultGdAdGroupAccess(adGroupId1)
                .withMainAdStatusModerate(BannerStatusModerate.READY));
        List<GdAdGroupAccess> gdAdGroupAccessesForSendToModerationAction =
                singletonList(defaultGdAdGroupAccess(adGroupId2).withMainAdStatusModerate(BannerStatusModerate.NEW));
        List<Long> adGroupIds = List.of(adGroupId1, adGroupId2);

        return new Object[][]{
                {
                        "getCanBeSentToBSAdGroupsCount",
                        (BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Integer>>)
                                (service, gridGraphQLContext) ->
                                        service.getCanBeSentToBSAdGroupsCount(gdAdGroupAccesses)
                },
                {
                        "getCanBeSentToModerationAdGroupsCount",
                        (BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Integer>>)
                                (service, gridGraphQLContext) ->
                                        service.getCanBeSentToModerationAdGroupsCount(gdAdGroupAccessesForSendToModerationAction)
                },
                {
                        "getCanBeSentToRemoderationAdGroupsCount",
                        (BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Integer>>)
                                (service, gridGraphQLContext) ->
                                        service.getCanBeSentToRemoderationAdGroupsCount(gdAdGroupAccesses)
                },
                {
                        "getCanAcceptModerationAdGroupsCount",
                        (BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Integer>>)
                                (service, gridGraphQLContext) ->
                                        service.getCanAcceptModerationAdGroupsCount(gdAdGroupAccesses)
                },
                {
                        "getCanRemoderateAdsCalloutsCount",
                        (BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Integer>>)
                                (service, gridGraphQLContext) ->
                                        service.getCanRemoderateAdsCalloutsCount(adGroupIds)
                },
                {
                        "canAcceptAdsCalloutsModerationCount",
                        (BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Integer>>)
                                (service, gridGraphQLContext) ->
                                        service.getCanAcceptAdsCalloutsModerationCount(adGroupIds)
                },
        };
    }

    @Test
    @Parameters(method = "accessFlagsCountExtractors")
    @TestCaseName("check {0}")
    public void checkGetAccessFlagsCount(
            @SuppressWarnings("unused") String accessFlagName,
            BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Integer>> accessFlagsCountExtractor) {
        fillContext(gridGraphQLContext, List.of(adGroupId1, adGroupId2));
        //для сапорта операция доступна
        operator.setRole(RbacRole.SUPPORT);
        doReturn(CompletableFuture.completedFuture(singletonList(true)))
                .when(dataLoaderMock).loadMany(anyList());

        CompletableFuture<Integer> accessFlagsCount = accessFlagsCountExtractor.apply(groupDataService,
                gridGraphQLContext);
        assertThat(accessFlagsCount.join())
                .isOne();
        verify(dataLoaderMock).loadMany(anyList());
    }

    @Test
    @Parameters(method = "accessFlagsCountExtractors")
    @TestCaseName("{0} is zero when access denied for operator")
    public void checkGetAccessFlagsCount_WhenAccessDeniedForOperator(
            @SuppressWarnings("unused") String accessFlagName,
            BiFunction<GroupDataService, GridGraphQLContext, CompletableFuture<Integer>> accessFlagsCountExtractor) {
        fillContext(gridGraphQLContext, List.of(adGroupId1, adGroupId2));
        //для клиента операция недоступна
        operator.setRole(RbacRole.CLIENT);

        CompletableFuture<Integer> accessFlagsCount = accessFlagsCountExtractor.apply(groupDataService,
                gridGraphQLContext);
        assertThat(accessFlagsCount.join())
                .isZero();
        verifyNoMoreInteractions(dataLoaderMock, adGroupsHasCalloutsDataLoader, adGroupsHasShowConditionsDataLoader);
    }

}
