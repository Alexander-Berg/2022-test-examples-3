package ru.yandex.direct.core.entity.campaign.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetCpaAlertRepository;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetHourlyAlertRepository;
import ru.yandex.direct.core.entity.autobudget.service.AlertsFreezeInfo;
import ru.yandex.direct.core.entity.balance.model.BalanceInfoQueueObjType;
import ru.yandex.direct.core.entity.balance.model.BalanceNotificationInfo;
import ru.yandex.direct.core.entity.balance.service.BalanceInfoQueueService;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncItem;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncPriority;
import ru.yandex.direct.core.entity.bs.resync.queue.repository.BsResyncQueueRepository;
import ru.yandex.direct.core.entity.campaign.container.CampaignAdditionalActionsContainer;
import ru.yandex.direct.core.entity.mailnotification.model.CampaignEvent;
import ru.yandex.direct.core.entity.mailnotification.model.GenericEvent;
import ru.yandex.direct.core.entity.mailnotification.service.MailNotificationEventService;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(MockitoJUnitRunner.class)
@ParametersAreNonnullByDefault
public class CampaignAdditionalActionsServiceTest {

    @Mock
    private BannerRelationsRepository bannerRelationsRepository;

    @Mock
    private BannerCommonRepository bannerCommonRepository;

    @Mock
    private BsResyncQueueRepository bsResyncQueueRepository;

    @Mock
    private BalanceInfoQueueService balanceInfoQueueService;

    @Mock
    private MailNotificationEventService mailNotificationEventService;

    @Mock
    private AdGroupRepository adGroupRepository;

    @Mock
    private AutobudgetHourlyAlertRepository autobudgetHourlyAlertRepository;

    @Mock
    private AutobudgetCpaAlertRepository autobudgetCpaAlertRepository;

    @Spy
    @InjectMocks
    private CampaignAdditionalActionsService campaignAdditionalActionsService;

    @Mock
    private DSLContext dslContext;
    @Mock
    private Configuration configuration;

    @Captor
    private ArgumentCaptor<List<BsResyncItem>> captor;

    private long operatorUid;
    private long campaignId;
    private CampaignAdditionalActionsContainer container;

    @Before
    public void initTestData() {
        operatorUid = RandomNumberUtils.nextPositiveLong();
        campaignId = RandomNumberUtils.nextPositiveLong();
        container = new CampaignAdditionalActionsContainer();
    }


    @Test
    public void checkCallResetBannersBsStatusSynced() {
        campaignAdditionalActionsService.processAdditionalActionsContainer(dslContext, operatorUid, container);

        verify(campaignAdditionalActionsService).resetBannersBsStatusSynced(dslContext, container);
    }

    @Test
    public void checkResetBannersBsStatusSynced() {
        doReturn(configuration)
                .when(dslContext).configuration();
        long secondCampaignId = RandomNumberUtils.nextPositiveLong();
        container.resetCampaignBannersStatusBsSynced(Set.of(campaignId));
        container.resetCampaignBannersStatusBsSyncedAndUpdateLastChange(Set.of(secondCampaignId));

        Set<Long> campaignIdsWhichBannersBsSyncedStatusWereReset =
                campaignAdditionalActionsService.resetBannersBsStatusSynced(dslContext, container);

        assertThat(campaignIdsWhichBannersBsSyncedStatusWereReset)
                .containsExactlyInAnyOrder(campaignId, secondCampaignId);
        verify(bannerCommonRepository).updateStatusBsSyncedByCampaignIds(configuration,
                container.getCampaignIdsForResetBannersStatusBsSynced(), false, StatusBsSynced.NO);
        verify(bannerCommonRepository).updateStatusBsSyncedByCampaignIds(configuration,
                container.getCampaignIdsForResetBannersStatusBsSyncedAndUpdateLastChange(), true, StatusBsSynced.NO);
    }

    @Test
    public void checkResetBannersBsStatusSynced_whenFilterCampaignIdsWhichBannersBsSyncedStatusWereReset() {
        doReturn(configuration)
                .when(dslContext).configuration();
        long secondCampaignId = RandomNumberUtils.nextPositiveLong();
        container.resetCampaignBannersStatusBsSynced(Set.of(campaignId, secondCampaignId));
        container.resetCampaignBannersStatusBsSyncedAndUpdateLastChange(Set.of(campaignId));

        Set<Long> campaignIdsWhichBannersBsSyncedStatusWereReset =
                campaignAdditionalActionsService.resetBannersBsStatusSynced(dslContext, container);

        assertThat(campaignIdsWhichBannersBsSyncedStatusWereReset)
                .containsExactlyInAnyOrder(campaignId, secondCampaignId);
        verify(bannerCommonRepository).updateStatusBsSyncedByCampaignIds(configuration,
                container.getCampaignIdsForResetBannersStatusBsSynced(), false, StatusBsSynced.NO);
        verify(bannerCommonRepository).updateStatusBsSyncedByCampaignIds(configuration,
                Collections.emptySet(), true, StatusBsSynced.NO);
    }

    @Test
    public void checkCallAddBannersToBsResyncQueue() {
        container.addCampaignBannersToBsResyncQueue(BsResyncPriority.CHANGE_CAMP_CONTENT_LANG, Set.of(campaignId));
        campaignAdditionalActionsService.processAdditionalActionsContainer(dslContext, operatorUid, container);

        Map<Long, BsResyncPriority> expectedMap = Map.of(campaignId, BsResyncPriority.CHANGE_CAMP_CONTENT_LANG);
        verify(campaignAdditionalActionsService).addBannersToBsResyncQueue(dslContext, expectedMap);
    }

    @Test
    public void checkNotCallAddBannersToBsResyncQueue_whenBannersBsSyncedStatusWereReset() {
        container.addCampaignBannersToBsResyncQueue(BsResyncPriority.CHANGE_CAMP_CONTENT_LANG, Set.of(campaignId));
        doReturn(Set.of(campaignId))
                .when(campaignAdditionalActionsService).resetBannersBsStatusSynced(dslContext, container);
        campaignAdditionalActionsService.processAdditionalActionsContainer(dslContext, operatorUid, container);

        verify(campaignAdditionalActionsService, never())
                .addBannersToBsResyncQueue(dslContext, Collections.emptyMap());
    }

    @Test
    public void checkAddBannersToBsResyncQueue() {
        Map<Long, BsResyncPriority> campaignIdsWithPriority =
                Map.of(campaignId, BsResyncPriority.INTAPI_RESYNC_CAMPAIGNS);
        long bannerId1 = RandomNumberUtils.nextPositiveLong();
        long bannerId2 = bannerId1 + 3;
        List<Long> bannerIds = List.of(bannerId1, bannerId2);
        doReturn(Map.of(campaignId, bannerIds))
                .when(bannerRelationsRepository).getBannerIdsByCampaignIdsMap(dslContext, Set.of(campaignId));

        campaignAdditionalActionsService.addBannersToBsResyncQueue(dslContext, campaignIdsWithPriority);

        List<BsResyncItem> expectedBsResyncItems = mapList(bannerIds, bannerId ->
                new BsResyncItem(BsResyncPriority.INTAPI_RESYNC_CAMPAIGNS.value(), campaignId, bannerId, null));
        verify(bsResyncQueueRepository).addToResync(eq(dslContext), captor.capture());
        assertThat(captor.getValue())
                .is(matchedBy(beanDiffer(expectedBsResyncItems)));
    }

    @Test
    public void checkCallMailNotificationEventService() {
        CampaignEvent<String> campaignEvent =
                CampaignEvent.changedStartDateEvent(operatorUid, RandomNumberUtils.nextPositiveLong(),
                        RandomNumberUtils.nextPositiveLong(), LocalDate.now(), LocalDate.now().plusDays(1));
        List<GenericEvent> mailEvents = List.of(campaignEvent);
        container.addMailEventsToQueue(mailEvents);
        campaignAdditionalActionsService.processAdditionalActionsContainer(dslContext, operatorUid, container);

        verify(mailNotificationEventService).queueEvents(dslContext, operatorUid, mailEvents);
    }

    @Test
    public void checkNotCallMailNotificationEventService_whenMailEventsInContainerIsEmpty() {
        campaignAdditionalActionsService.processAdditionalActionsContainer(dslContext, operatorUid, container);

        verifyZeroInteractions(mailNotificationEventService);
    }

    @Test
    public void checkCallBalanceInfoQueueService() {
        BalanceNotificationInfo balanceNotificationInfo = new BalanceNotificationInfo()
                .withObjType(BalanceInfoQueueObjType.CID)
                .withCidOrUid(RandomNumberUtils.nextPositiveLong());
        container.addBalanceNotificationToQueue(balanceNotificationInfo);
        campaignAdditionalActionsService.processAdditionalActionsContainer(dslContext, operatorUid, container);

        verify(balanceInfoQueueService).addToBalanceInfoQueue(dslContext, List.of(balanceNotificationInfo));
    }

    @Test
    public void checkCallBalanceInfoQueueService_whenBalanceNotificationsInContainerIsEmpty() {
        campaignAdditionalActionsService.processAdditionalActionsContainer(dslContext, operatorUid, container);

        verifyZeroInteractions(mailNotificationEventService);
    }

    @Test
    public void checkCallUpdateStatusShowsForecast() {
        var campaignIds = Set.of(1L, 42L);
        container.addCampaignIdsForUpdateStatusShowsForecast(campaignIds);

        doReturn(configuration).when(dslContext).configuration();
        campaignAdditionalActionsService.processAdditionalActionsContainer(dslContext, operatorUid, container);

        verify(adGroupRepository)
                .updateStatusShowsForecastByCampaignIds(configuration, campaignIds, StatusShowsForecast.NEW);
    }

    @Test
    public void checkCallUpdateBannersLastChangeUpdate() {
        var campaignIds = Set.of(1L, 42L);
        container.addCampaignIdsForUpdateBannersLastChange(campaignIds);

        campaignAdditionalActionsService.processAdditionalActionsContainer(dslContext, operatorUid, container);

        verify(bannerCommonRepository).updateBannersLastChangeByCampaignIds(dslContext, campaignIds);
    }

    @Test
    public void checkFreezeAlerts() {
        long baseId = RandomNumberUtils.nextPositiveLong();
        Set<Long> idsToFreezeHourlyAlerts = Set.of(baseId, baseId + 1, baseId + 2);
        Set<Long> idsToFreezeCpaAlerts = Set.of(baseId + 3, baseId + 4, baseId + 5);
        AlertsFreezeInfo info = new AlertsFreezeInfo(idsToFreezeHourlyAlerts, idsToFreezeCpaAlerts);
        container.addCampaignsToFreezeAlerts(info);
        campaignAdditionalActionsService.processAdditionalActionsContainer(dslContext, operatorUid, container);

        verify(autobudgetHourlyAlertRepository).freezeAlerts(dslContext, idsToFreezeHourlyAlerts);
        verify(autobudgetCpaAlertRepository).freezeAlerts(dslContext, idsToFreezeCpaAlerts);
    }
}
