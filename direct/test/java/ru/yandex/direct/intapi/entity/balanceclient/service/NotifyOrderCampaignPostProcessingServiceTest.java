package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncItem;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncPriority;
import ru.yandex.direct.core.entity.bs.resync.queue.repository.BsResyncQueueRepository;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignForNotifyOrder;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampActivizationRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.campaign.service.WhenMoneyOnCampWasEvents;
import ru.yandex.direct.core.entity.client.repository.AgencyClientRelationRepository;
import ru.yandex.direct.core.entity.statistics.service.OrderStatService;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.entity.balanceclient.container.CampaignDataForNotifyOrder;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters;
import ru.yandex.direct.intapi.entity.balanceclient.repository.NotifyOrderRepository;
import ru.yandex.direct.intapi.entity.balanceclient.service.migration.MigrationSchema;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderCampaignPostProcessingService.BS_RESOURCE_ARC_PERIOD;

public class NotifyOrderCampaignPostProcessingServiceTest {
    private static final int TEST_SHARD = 123;
    private static final ClientId TEST_CLIENT_ID = ClientId.fromLong(31);
    private static final long TEST_USER_ID = 987;
    private static final long TEST_CAMPAIGN_ID = 1234;
    private static final long TEST_ORDER_ID = 5678;

    @Mock
    private BsResyncQueueRepository bsResyncQueueRepository;
    @Mock
    private CampActivizationRepository campActivizationRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private NotifyOrderRepository notifyOrderRepository;
    @Mock
    private OrderStatService orderStatService;
    @Mock
    private CampaignService campaignService;
    @Mock
    private NotifyOrderNotificationService notifyOrderNotificationService;
    @Mock
    private AgencyClientRelationRepository agencyClientRelationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PpcProperty<Boolean> checkCashbackOnlyProperty = mock(PpcProperty.class);
    @Mock
    private PpcProperty<Boolean> sendToBsProperty = mock(PpcProperty.class);
    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    private NotifyOrderCampaignPostProcessingService service;
    private CampaignDataForNotifyOrder dbCampaignData;

    @Before
    @SuppressWarnings("unchecked")
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(Boolean.TRUE)
                .when(checkCashbackOnlyProperty)
                .getOrDefault(any());

        doReturn(checkCashbackOnlyProperty)
                .when(ppcPropertiesSupport)
                .get(eq(PpcPropertyNames.CHECK_CASHBACK_ONLY_ON_NEW_MONEY), any());

        doReturn(Boolean.TRUE)
                .when(sendToBsProperty)
                .getOrDefault(any());

        doReturn(sendToBsProperty)
                .when(ppcPropertiesSupport)
                .get(eq(PpcPropertyNames.AUTOBUDGET_RESTART_SEND_TO_BS_ON_NEW_MONEY), any());

        service = spy(new NotifyOrderCampaignPostProcessingService(
                notifyOrderRepository,
                campaignRepository,
                campaignService,
                notifyOrderNotificationService,
                bsResyncQueueRepository,
                agencyClientRelationRepository,
                userRepository,
                campActivizationRepository,
                orderStatService,
                ppcPropertiesSupport));
        dbCampaignData = new CampaignDataForNotifyOrder()
                .withUid(TEST_USER_ID)
                .withClientId(TEST_CLIENT_ID.asLong())
                .withCampaignId(TEST_CAMPAIGN_ID)
                .withOrderId(TEST_ORDER_ID)
                .withType(CampaignType.TEXT)
                .withSource(CampaignSource.DIRECT);
    }

    @Test
    public void testProcessUnchangedCampaign() {
        service.processUnchangedCampaign(TEST_SHARD, TEST_CAMPAIGN_ID);

        ArgumentCaptor<List<BsResyncItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(bsResyncQueueRepository).addToResync(eq(TEST_SHARD), captor.capture());

        List<BsResyncItem> resyncItems = captor.getValue();
        assertThat("Получили список из одного элемента", resyncItems.size(), equalTo(1));
        assertThat("Передали на синхронизацию именно кампанию", resyncItems.get(0), allOf(
                hasProperty("priority", equalTo(BsResyncPriority.MULTICURRENCY_SUMS_UPDATED.value())),
                hasProperty("campaignId", equalTo(TEST_CAMPAIGN_ID)),
                hasProperty("adgroupId", equalTo(0L)),
                hasProperty("bannerId", equalTo(0L))
        ));
    }

    @Test
    public void testProcessMoneyRefillFromZero_Wallet() {
        doNothing().when(service).resyncCampaignsUnderWalletInBannerSystem(anyInt(), any(), any());
        doNothing().when(service).unarchiveOldCampaignResources(anyInt(), any(), any());

        dbCampaignData.withType(CampaignType.WALLET);
        List<CampaignForNotifyOrder> campsInWallet = Collections.singletonList(new Campaign());
        service.processMoneyRefillFromZero(TEST_SHARD, dbCampaignData, campsInWallet);

        verify(service).resyncCampaignsUnderWalletInBannerSystem(eq(TEST_SHARD), eq(dbCampaignData), eq(campsInWallet));
        verify(service).unarchiveOldCampaignResources(eq(TEST_SHARD), eq(dbCampaignData), eq(campsInWallet));
    }

    @Test
    public void testProcessMoneyRefillFromZero_EmptyWallet() {
        doNothing().when(service).resyncCampaignsUnderWalletInBannerSystem(anyInt(), any(), any());
        doNothing().when(service).unarchiveOldCampaignResources(anyInt(), any(), any());

        dbCampaignData.withType(CampaignType.WALLET);
        List<CampaignForNotifyOrder> campsInWallet = Collections.emptyList();
        service.processMoneyRefillFromZero(TEST_SHARD, dbCampaignData, campsInWallet);

        verify(service, never()).resyncCampaignsUnderWalletInBannerSystem(anyInt(), any(), any());
        verify(service).unarchiveOldCampaignResources(eq(TEST_SHARD), eq(dbCampaignData), eq(campsInWallet));
    }

    @Test
    public void testProcessMoneyRefillFromZero_NotWallet() {
        doNothing().when(service).resyncCampaignsUnderWalletInBannerSystem(anyInt(), any(), any());
        doNothing().when(service).unarchiveOldCampaignResources(anyInt(), any(), any());

        List<CampaignForNotifyOrder> campsInWallet = Collections.emptyList();
        service.processMoneyRefillFromZero(TEST_SHARD, dbCampaignData, campsInWallet);

        verify(service, never()).resyncCampaignsUnderWalletInBannerSystem(anyInt(), any(), any());
        verify(service).unarchiveOldCampaignResources(eq(TEST_SHARD), eq(dbCampaignData), eq(campsInWallet));
    }

    @Test
    public void testResyncCampaignsUnderWalletInBannerSystem() {
        doReturn(Boolean.FALSE).when(sendToBsProperty).getOrDefault(any());

        ArgumentCaptor<Collection<Long>> captor = resyncCampaignsInternals();

        // Проверяем что из кампаний, которых нет в БК, отправилась нужная
        verify(campaignRepository).resetBannerSystemSyncStatus(eq(TEST_SHARD), captor.capture());
        List<Long> newResendCampaignIds = new ArrayList<>(captor.getValue());
        assertThat("Получили список из одного элемента", newResendCampaignIds.size(), equalTo(1));
        assertThat("На отправку в БК поставлена ожидаемая кампания", newResendCampaignIds.get(0), equalTo(2L));

        // Проверяем что из кампаний, которые есть в БК, отправилась нужная
        captor = ArgumentCaptor.forClass(Collection.class);
        verify(campActivizationRepository).addCampsForActivization(eq(TEST_SHARD), captor.capture());
        List<Long> oldResendCampaignIds = new ArrayList<>(captor.getValue());
        assertThat("Получили список из одного элемента", oldResendCampaignIds.size(), equalTo(1));
        assertThat("На отправку в БК поставлена ожидаемая кампания", oldResendCampaignIds.get(0), equalTo(5L));
    }

    @Test
    public void testResyncCampaignsUnderWalletInBannerSystemWithProperty() {
        doReturn(Boolean.TRUE).when(sendToBsProperty).getOrDefault(any());

        ArgumentCaptor<Collection<Long>> captor = resyncCampaignsInternals();

        // Проверяем что из кампаний, которых нет в БК, отправилась нужная
        verify(campaignRepository).resetBannerSystemSyncStatus(eq(TEST_SHARD), captor.capture());
        List<Long> newResendCampaignIds = new ArrayList<>(captor.getValue());
        assertThat("Отправились все кампании", newResendCampaignIds.size(), equalTo(4));
    }

    @NotNull
    private ArgumentCaptor<Collection<Long>> resyncCampaignsInternals() {
        doReturn(1).when(campaignRepository).resetBannerSystemSyncStatus(anyInt(), any());
        doNothing().when(campActivizationRepository).addCampsForActivization(anyInt(), any());

        List<CampaignForNotifyOrder> campsInWallet = Arrays.asList(
                new Campaign()
                        .withId(2L)
                        .withOrderId(0L)
                        .withStatusModerate(CampaignStatusModerate.YES)
                        .withStatusShow(false),
                new Campaign()
                        .withId(3L)
                        .withOrderId(0L)
                        .withStatusModerate(CampaignStatusModerate.NEW)
                        .withStatusShow(false),
                new Campaign()
                        .withId(4L)
                        .withOrderId(4L)
                        .withStatusModerate(CampaignStatusModerate.YES)
                        .withStatusShow(false),
                new Campaign()
                        .withId(5L)
                        .withOrderId(5L)
                        .withStatusModerate(CampaignStatusModerate.YES)
                        .withStatusShow(true)
        );

        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        service.resyncCampaignsUnderWalletInBannerSystem(TEST_SHARD, dbCampaignData, campsInWallet);
        return captor;
    }

    /**
     * Если кампания - не кошелек и у нее нет OrderId - ничего не делаем.
     */
    @Test
    public void testUnarchiveOldCampaignResources_NotWallet_NoOrderId() {
        service.unarchiveOldCampaignResources(TEST_SHARD, dbCampaignData.withOrderId(0L), Collections.emptyList());

        verify(notifyOrderRepository, never()).fetchCampaignItemsForBsResync(anyInt(), any(), any());
        verify(bsResyncQueueRepository, never()).addToResync(anyInt(), any());
    }

    /**
     * Если кампания - не кошелек и у нее есть OrderId, но последний показ был недавно - не переотправляем.
     */
    @Test
    public void testUnarchiveOldCampaignResources_NotWallet_WithOrderId_WithRecentShow() {
        doReturn(Collections.singletonMap(TEST_ORDER_ID, LocalDate.now())).when(orderStatService)
                .getLastDayOfCampaigns(any());

        service.unarchiveOldCampaignResources(TEST_SHARD, dbCampaignData, Collections.emptyList());

        verify(notifyOrderRepository, never()).fetchCampaignItemsForBsResync(anyInt(), any(), any());
        verify(bsResyncQueueRepository, never()).addToResync(anyInt(), any());
    }

    /**
     * Если кампания - не кошелек и у нее есть OrderId, а последний показ был давно - переотправляем.
     */
    @Test
    public void testUnarchiveOldCampaignResources_NotWallet_WithOrderId_WithoutRecentShow() {
        LocalDate lastShow = LocalDate.now().minusDays(BS_RESOURCE_ARC_PERIOD.toDays() * 2);
        doReturn(Collections.singletonMap(TEST_ORDER_ID, lastShow))
                .when(orderStatService).getLastDayOfCampaigns(any());

        Collection<BsResyncItem> resyncItems = Arrays.asList(
                new BsResyncItem(BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2, TEST_CAMPAIGN_ID),
                new BsResyncItem(BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2.value(), TEST_CAMPAIGN_ID, null,
                        1L),
                new BsResyncItem(BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2.value(), TEST_CAMPAIGN_ID, 1L, 1L)
        );

        doReturn(resyncItems).when(notifyOrderRepository).fetchCampaignItemsForBsResync(eq(TEST_SHARD), any(), any());

        service.unarchiveOldCampaignResources(TEST_SHARD, dbCampaignData, Collections.emptyList());

        ArgumentCaptor<Long> longCaptor = ArgumentCaptor.forClass(Long.class);
        verify(notifyOrderRepository).fetchCampaignItemsForBsResync(eq(TEST_SHARD), longCaptor.capture(),
                eq(BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2));
        assertThat("Запрос на получение ресурсов на переотправку содержит верный ID кампании",
                longCaptor.getValue(), equalTo(TEST_CAMPAIGN_ID));

        verify(bsResyncQueueRepository).addToResync(eq(TEST_SHARD), eq(resyncItems));
    }

    /**
     * Если кампания - кошелек, переотправляем все кампании под ней, которые были показаны давно,
     * но не помечены как архивные в директе.
     */
    @Test
    public void testUnarchiveOldCampaignResources_Wallet() {
        LocalDate lastShow = LocalDate.now().minusDays(BS_RESOURCE_ARC_PERIOD.toDays() * 2);
        doReturn(Collections.singletonMap(TEST_ORDER_ID, lastShow))
                .when(orderStatService).getLastDayOfCampaigns(any());

        List<CampaignForNotifyOrder> campsInWallet = Arrays.asList(
                new Campaign()
                        .withId(2L)
                        .withOrderId(0L)
                        .withStatusShow(false)
                        .withStatusArchived(false),
                new Campaign()
                        .withId(3L)
                        .withOrderId(3L)
                        .withStatusShow(true)
                        .withStatusArchived(false),
                new Campaign()
                        .withId(4L)
                        .withOrderId(4L)
                        .withStatusShow(true)
                        .withStatusArchived(true)
        );

        doReturn(Collections.emptyList())
                .when(notifyOrderRepository).fetchCampaignItemsForBsResync(eq(TEST_SHARD), any(), any());

        service.unarchiveOldCampaignResources(TEST_SHARD, dbCampaignData.withType(CampaignType.WALLET),
                campsInWallet);

        ArgumentCaptor<Long> longCaptor = ArgumentCaptor.forClass(Long.class);
        verify(notifyOrderRepository).fetchCampaignItemsForBsResync(eq(TEST_SHARD), longCaptor.capture(),
                eq(BsResyncPriority.UNARC_CAMP_IN_BS_ON_NOTIFY_ORDER2));
        assertThat("Запрос на получение ресурсов на переотправку содержит верный ID кампании",
                longCaptor.getValue(), equalTo(3L));

        verify(bsResyncQueueRepository).addToResync(eq(TEST_SHARD), any());
    }

    @Test
    public void testProcessSumOnCampChange_AlwaysSetsLastPayTimeAndNotifies() {
        service.processSumOnCampChange(
                TEST_SHARD,
                new NotifyOrderParameters().withSumUnits(BigDecimal.TEN),
                dbCampaignData,
                1L,
                Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB),
                Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB),
                MigrationSchema.State.NEW);

        verify(campaignRepository).setCampOptionsLastPayTimeNow(eq(TEST_SHARD), eq(TEST_CAMPAIGN_ID));
        verify(notifyOrderNotificationService)
                .sendNotification(
                        eq(dbCampaignData),
                        eq(BigDecimal.TEN),
                        eq(Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.00"), CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB)),
                        eq(1L),
                        eq(false));
    }

    @Test
    public void testProcessSumOnCampChange_SumPayedGreaterThanZero_NotWallet() {
        service.processSumOnCampChange(
                TEST_SHARD,
                new NotifyOrderParameters().withSumUnits(BigDecimal.TEN),
                dbCampaignData,
                1L,
                Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB),
                Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB),
                MigrationSchema.State.NEW);

        verify(campaignService).whenMoneyOnCampWas(eq(TEST_CAMPAIGN_ID), eq(WhenMoneyOnCampWasEvents.MONEY_IN));
    }

    @Test
    public void testProcessSumOnCampChange_SumPayedGreaterThanZero_HasWallet() {
        service.processSumOnCampChange(
                TEST_SHARD,
                new NotifyOrderParameters().withSumUnits(BigDecimal.TEN),
                dbCampaignData.withWalletId(120L),
                1L,
                Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB),
                Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB),
                MigrationSchema.State.NEW);

        verify(campaignService, never())
                .whenMoneyOnCampWas(eq(TEST_CAMPAIGN_ID), eq(WhenMoneyOnCampWasEvents.MONEY_IN));
    }

    @Test
    public void testProcessSumOnCampChange_SumPayedGreaterThanZero_IsWallet() {
        doReturn(true).when(notifyOrderRepository)
                .isThereAnyCampStartingInFutureUnderWallet(eq(TEST_SHARD), eq(TEST_CAMPAIGN_ID), eq(TEST_USER_ID));

        service.processSumOnCampChange(
                TEST_SHARD,
                new NotifyOrderParameters().withSumUnits(BigDecimal.TEN),
                dbCampaignData.withType(CampaignType.WALLET),
                1L,
                Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB),
                Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB),
                MigrationSchema.State.NEW);

        assertTrue("Для общего счета должен проставляться признак запуска кампаний в будущем",
                dbCampaignData.getStartTimeInFuture());

        verify(campaignService)
                .whenMoneyOnCampWas(eq(TEST_CAMPAIGN_ID), eq(WhenMoneyOnCampWasEvents.MONEY_IN));
    }

    @Test
    public void testProcessSumOnCampChange_SumPayedNotZero_Unarchive_Agency() {
        service.processSumOnCampChange(
                TEST_SHARD,
                new NotifyOrderParameters().withSumUnits(BigDecimal.TEN),
                dbCampaignData.withAgencyUid(41L).withAgencyId(32L),
                1L,
                Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB),
                Money.valueOf(new BigDecimal("-1.005"), CurrencyCode.RUB),
                MigrationSchema.State.NEW);

        ArgumentCaptor<Collection<ClientId>> clientIdCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(agencyClientRelationRepository)
                .unarchiveClients(eq(TEST_SHARD), eq(ClientId.fromLong(32L)), clientIdCaptor.capture());
        List<ClientId> clientIdsData = new ArrayList<>(clientIdCaptor.getValue());

        assertThat("Получили список из одного элемента", clientIdsData.size(), equalTo(1));
        assertThat("Разархивировали правильного клиента", clientIdsData.get(0), equalTo(TEST_CLIENT_ID));

        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(userRepository).updateLastChange(eq(TEST_SHARD), captor.capture(), any());
        List<Long> data = new ArrayList<>(captor.getValue());

        assertThat("Получили список из одного элемента", data.size(), equalTo(1));
        assertThat("Поменяли дату последнего изменения у правильного клиента", data.get(0), equalTo(TEST_USER_ID));
    }

    @Test
    public void testProcessSumOnCampChange_SumPayedNotZero_Unarchive_Manager() {
        service.processSumOnCampChange(
                TEST_SHARD,
                new NotifyOrderParameters().withSumUnits(BigDecimal.TEN),
                dbCampaignData.withManagerUid(41L),
                1L,
                Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB),
                Money.valueOf(new BigDecimal("-1.005"), CurrencyCode.RUB),
                MigrationSchema.State.NEW);

        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(userRepository).unarchiveUsers(eq(TEST_SHARD), captor.capture());

        List<Long> data = new ArrayList<>(captor.getValue());
        assertThat("Получили список из одного элемента", data.size(), equalTo(1));
        assertThat("Разархивировали правильного пользователя", data.get(0), equalTo(TEST_USER_ID));
    }

    @Test
    public void testIsCashbackOnly_Wallet_OnlyCashback() {
        dbCampaignData.withType(CampaignType.WALLET)
                .withSumUnits(10L)
                .withSum(BigDecimal.valueOf(20))
                .withCashback(BigDecimal.valueOf(30))
                .withTotalCashback(BigDecimal.valueOf(40));
        service.processSumOnCampChange(
                TEST_SHARD,
                new NotifyOrderParameters()
                        .withSumUnits(BigDecimal.valueOf(15))
                        .withTotalSum(BigDecimal.valueOf(25))
                        .withCashback(BigDecimal.valueOf(35))
                        .withTotalCashback(BigDecimal.valueOf(45)),
                dbCampaignData,
                1L,
                Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB),
                Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB),
                MigrationSchema.State.NEW);

        verify(notifyOrderNotificationService)
                .sendNotification(
                        eq(dbCampaignData),
                        eq(BigDecimal.valueOf(15)),
                        eq(Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.00"), CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB)),
                        eq(1L),
                        eq(true));
    }

    @Test
    public void testIsCashbackOnly_Wallet_NotOnlyCashback() {
        dbCampaignData.withType(CampaignType.WALLET)
                .withSumUnits(10L)
                .withSum(BigDecimal.valueOf(20))
                .withCashback(BigDecimal.valueOf(30))
                .withTotalCashback(BigDecimal.valueOf(40));
        service.processSumOnCampChange(
                TEST_SHARD,
                new NotifyOrderParameters()
                        .withSumUnits(BigDecimal.valueOf(15))
                        .withTotalSum(BigDecimal.valueOf(29))
                        .withCashback(BigDecimal.valueOf(35))
                        .withTotalCashback(BigDecimal.valueOf(45)),
                dbCampaignData,
                1L,
                Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB),
                Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB),
                MigrationSchema.State.NEW);

        verify(notifyOrderNotificationService)
                .sendNotification(
                        eq(dbCampaignData),
                        eq(BigDecimal.valueOf(15)),
                        eq(Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.00"), CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB)),
                        eq(1L),
                        eq(false));
    }

    @Test
    public void testIsCashbackOnly_Wallet_WithoutTotalSum() {
        dbCampaignData.withType(CampaignType.WALLET)
                .withSumUnits(10L)
                .withSum(BigDecimal.valueOf(20))
                .withCashback(BigDecimal.valueOf(30))
                .withTotalCashback(BigDecimal.valueOf(40));
        service.processSumOnCampChange(
                TEST_SHARD,
                new NotifyOrderParameters()
                        .withSumUnits(BigDecimal.valueOf(15))
                        .withCashback(BigDecimal.valueOf(35))
                        .withTotalCashback(BigDecimal.valueOf(45)),
                dbCampaignData,
                1L,
                Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB),
                Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB),
                MigrationSchema.State.NEW);

        verify(notifyOrderNotificationService)
                .sendNotification(
                        eq(dbCampaignData),
                        eq(BigDecimal.valueOf(15)),
                        eq(Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.00"), CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB)),
                        eq(1L),
                        eq(true));
    }

    @Test
    public void testIsCashbackOnly_NotWallet_OnlyCashback() {
        dbCampaignData
                .withSumUnits(10L)
                .withSum(BigDecimal.valueOf(20))
                .withCashback(BigDecimal.valueOf(30))
                .withTotalCashback(BigDecimal.valueOf(40));
        service.processSumOnCampChange(
                TEST_SHARD,
                new NotifyOrderParameters()
                        .withSumUnits(BigDecimal.valueOf(15))
                        .withTotalSum(BigDecimal.valueOf(29))
                        .withCashback(BigDecimal.valueOf(35))
                        .withTotalCashback(BigDecimal.valueOf(45)),
                dbCampaignData,
                1L,
                Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB),
                Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB),
                MigrationSchema.State.NEW);

        verify(notifyOrderNotificationService)
                .sendNotification(
                        eq(dbCampaignData),
                        eq(BigDecimal.valueOf(15)),
                        eq(Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.00"), CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB)),
                        eq(1L),
                        eq(true));
    }

    @Test
    public void testIsCashbackOnly_Wallet_WithOldMigrationSchema() {
        dbCampaignData.withType(CampaignType.WALLET)
                .withSumUnits(10L)
                .withSum(BigDecimal.valueOf(20))
                .withCashback(BigDecimal.valueOf(30))
                .withTotalCashback(BigDecimal.valueOf(40));
        service.processSumOnCampChange(
                TEST_SHARD,
                new NotifyOrderParameters()
                        .withSumUnits(BigDecimal.valueOf(15))
                        .withTotalSum(BigDecimal.valueOf(29))
                        .withCashback(BigDecimal.valueOf(35))
                        .withTotalCashback(BigDecimal.valueOf(45)),
                dbCampaignData,
                1L,
                Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB),
                Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB),
                MigrationSchema.State.OLD);

        verify(notifyOrderNotificationService)
                .sendNotification(
                        eq(dbCampaignData),
                        eq(BigDecimal.valueOf(15)),
                        eq(Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.00"), CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB)),
                        eq(1L),
                        eq(true));
    }

    @Test
    public void testIsCashbackOnly_OnlyCashback() {
        dbCampaignData.withType(CampaignType.WALLET)
                .withSumUnits(10L)
                .withCashback(BigDecimal.valueOf(30));
        service.processSumOnCampChange(
                TEST_SHARD,
                new NotifyOrderParameters()
                        .withSumUnits(BigDecimal.valueOf(15))
                        .withCashback(BigDecimal.valueOf(35)),
                dbCampaignData,
                1L,
                Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB),
                Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB),
                MigrationSchema.State.OLD);

        verify(notifyOrderNotificationService)
                .sendNotification(
                        eq(dbCampaignData),
                        eq(BigDecimal.valueOf(15)),
                        eq(Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.00"), CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB)),
                        eq(1L),
                        eq(true));
    }

    @Test
    public void testIsCashbackOnly_NotOnlyCashback() {
        dbCampaignData.withType(CampaignType.WALLET)
                .withSumUnits(10L)
                .withCashback(BigDecimal.valueOf(30));
        service.processSumOnCampChange(
                TEST_SHARD,
                new NotifyOrderParameters()
                        .withSumUnits(BigDecimal.valueOf(19))
                        .withCashback(BigDecimal.valueOf(35)),
                dbCampaignData,
                1L,
                Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB),
                Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB),
                MigrationSchema.State.OLD);

        verify(notifyOrderNotificationService)
                .sendNotification(
                        eq(dbCampaignData),
                        eq(BigDecimal.valueOf(19)),
                        eq(Money.valueOf(BigDecimal.TEN, CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.00"), CurrencyCode.RUB)),
                        eq(Money.valueOf(new BigDecimal("1.005"), CurrencyCode.RUB)),
                        eq(1L),
                        eq(false));
    }
}
