package ru.yandex.market.core.supplier.state;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;

import ru.yandex.common.transaction.LocalTransactionListener;
import ru.yandex.common.transaction.TransactionListener;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.delivery.LogisticPartnerService;
import ru.yandex.market.core.delivery.PartnerLmsRegHelperService;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.id.GetByPartnerRequest;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.model.ConfigureShopDto;
import ru.yandex.market.logistics.nesu.client.model.NamedEntity;
import ru.yandex.market.logistics.nesu.client.model.ShopWithSendersDto;
import ru.yandex.market.logistics.nesu.client.model.filter.ShopWithSendersFilter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


/**
 * Тесты для {@link LmsPartnerStateListener}
 */
@DbUnitDataSet(before = "LmsPartnerStateListenerTest.before.csv")
public class LmsPartnerStateListenerTest extends FunctionalTest {

    private static final Map<DeliveryServiceType, Long> PARTNERS = Map.of(
            DeliveryServiceType.DROPSHIP, 1L,
            DeliveryServiceType.DROPSHIP_BY_SELLER, 3L
    );

    private static final long DROPSHIP_DONTWANT_ID = 5;
    private static final long DROPSHIP_DONTWANT_WITH_OLD_CUTOFF_ID = 6;
    private static final long DSBS_DONTWANT_ID = 7;
    private static final long DSBS_DONTWANT_WITH_OLD_CUTOFF_ID = 8;

    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    @Autowired
    private LogisticPartnerService logisticPartnerService;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private ParamService paramService;

    @Autowired
    private PartnerLmsRegHelperService partnerLmsRegHelperService;

    private LmsPartnerStateListener lmsPartnerStateListener;

    private static long getPartner(DeliveryServiceType deliveryServiceType) {
        Long partnerId = PARTNERS.get(deliveryServiceType);
        if (partnerId == null) {
            fail("No partners for service type: " + deliveryServiceType);
        }

        return partnerId;
    }

    @BeforeEach
    void init() {
        lmsPartnerStateListener = new LmsPartnerStateListener(partnerTypeAwareService, logisticPartnerService,
                featureService, localTransactionListener(), paramService,
                partnerLmsRegHelperService);
        mockMarketId();
    }

    @AfterEach
    void afterEach() {
        verifyNoMoreInteractions(lmsClient);
    }

    @ParameterizedTest
    @DisplayName("Проверка прохождения полной двухшаговой регистрации партнёра")
    @CsvSource({
            "DROPSHIP,false",
            "DROPSHIP_BY_SELLER,true"
    })
    void testCompleteTwoStepsLogisticPartnerRegistration(DeliveryServiceType deliveryServiceType,
                                                         boolean twoStepsRegistrationEnabled) {
        if (twoStepsRegistrationEnabled) {
            long partnerId = getPartner(deliveryServiceType);

            lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
            verify(nesuClient, times(1)).configureShop(anyLong(), any(ConfigureShopDto.class));
            verify(lmsClient).getPartner(partnerId);
        }
        verifyNoMoreInteractions(nesuClient);
    }

    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.child.before.csv")
    @ParameterizedTest
    @DisplayName("Проверка прохождения двухшаговой регистрации партнёра созданного по донору")
    @CsvSource({
            "5,false",
            "6,true"
    })
    void testTwoStepsLogisticChildPartnerRegistration(long partnerId,
                                                      boolean secondStepExpected) {
        if (secondStepExpected) {
            lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
            verify(nesuClient, times(1)).configureShop(anyLong(), any(ConfigureShopDto.class));
        }
        verifyNoMoreInteractions(nesuClient);
    }

    @Test
    @DisplayName("Синхронизация стоков включается для DONT_WANT дропшипа ПИ, если нет катоффов старше 60 дней")
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.DropshipPI.dontwant.before.csv")
    void testDropshipPI_SyncStocksOnDontwantAndFreshCutoff() {
        long partnerId = DROPSHIP_DONTWANT_ID;
        featureService.openCutoff(100500, partnerId, FeatureType.DROPSHIP, FeatureCutoffType.HIDDEN);

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verifyStockSyncWasUpdatedForDropship(partnerId);
    }

    @Test
    @DisplayName("Синхронизация стоков включается для DONT_WANT дропшипа ПИ, если нет катоффов")
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.DropshipPI.dontwant.before.csv")
    void testDropshipPI_SyncStocksOnDontwant() {
        long partnerId = DROPSHIP_DONTWANT_ID;

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verifyStockSyncWasUpdatedForDropship(partnerId);
    }

    @Test
    @DisplayName("Синхронизация стоков не включается для DONT_WANT дропшипа ПИ, если есть катофф старше 60 дней")
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.DropshipPI.dontwant.before.csv")
    void testDropshipPI_NoSyncStocksOnDontwantWithOldCutoff() {
        long partnerId = DROPSHIP_DONTWANT_WITH_OLD_CUTOFF_ID;

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verifyStockSyncWasNotUpdatedForDropship(partnerId);
    }

    @Test
    @DisplayName("Синхронизация стоков включается для DONT_WANT ДСБС ПИ, если нет катоффов старше 60 дней")
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.DsbsPI.dontwant.before.csv")
    void testDsbsPI_SyncStocksOnDontwantAndFreshCutoff() {
        long partnerId = DSBS_DONTWANT_ID;
        featureService.openCutoff(100500, partnerId, FeatureType.DROPSHIP, FeatureCutoffType.HIDDEN);

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verifyStockSyncWasUpdatedForDsbs(partnerId);
    }

    @Test
    @DisplayName("Синхронизация стоков включается для DONT_WANT ДСБС ПИ, если нет катоффов")
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.DsbsPI.dontwant.before.csv")
    void testDsbsPI_SyncStocksOnDontwant() {
        long partnerId = DSBS_DONTWANT_ID;

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verifyStockSyncWasUpdatedForDsbs(partnerId);
    }

    @Test
    @DisplayName("Синхронизация стоков не включается для DONT_WANT ДСБС ПИ, если есть катофф старше 60 дней")
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.DsbsPI.dontwant.before.csv")
    void testDsbsPI_NoSyncStocksOnDontwantWithOldCutoff() {
        long partnerId = DSBS_DONTWANT_WITH_OLD_CUTOFF_ID;

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verifyStockSyncWasNotUpdatedForDsbs(partnerId);
    }

    @Test
    @DisplayName("Синхронизация стоков не включается для DONT_WANT ДСБС ПИ, если DROPSHIP_BY_SELLER FAIL")
    @DbUnitDataSet(before = "testDsbsPI_SyncStocksOnDontwantDBSCutoff.before.csv")
    void testDsbsPI_SyncStocksOnDontwantDBSCutoff() {
        long partnerId = 10L;

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verifyNoMoreInteractions(nesuClient);
    }

    @Test
    @DisplayName("Синхронизация стоков не включается для DONT_WANT ДСБС ПИ, если есть старый катоф на MARKETPLACE")
    @DbUnitDataSet(before = "testDsbsPI_SyncStocksOnDontwantDBSOldMarketplaceCutoff.before.csv")
    void testDsbsPI_SyncStocksOnDontwantDBSOldMarketplaceCutoff() {
        long partnerId = 10L;

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verifyStockSyncWasNotUpdatedForDsbs(partnerId);
    }

    @Test
    @DisplayName("Синхронизация стоков включается для DONT_WANT ДСБС ПИ, если есть катофф старше 60 дней и " +
            "есть параметр keep alive")
    @DbUnitDataSet(before = {
            "LmsPartnerStateListenerTest.DsbsPI.dontwant.before.csv",
            "testDsbsPI_NoSyncStocksOnDontwantWithOldCutoffKeepAlive.csv"
    })
    void testDsbsPI_NoSyncStocksOnDontwantWithOldCutoffKeepAlive() {
        long partnerId = DSBS_DONTWANT_WITH_OLD_CUTOFF_ID;

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verifyStockSyncWasUpdatedForDsbs(partnerId);
    }

    @ParameterizedTest
    @DisplayName("Выключение синхронизации стоков по параметру IGNORE_STOCKS (CPA через ПИ)")
    @EnumSource(value = DeliveryServiceType.class, names = {"DROPSHIP", "DROPSHIP_BY_SELLER"})
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.CpaPI.IgnoreStocks.before.csv")
    void testCpaPI_IgnoreStocksShouldSwitchOffStockSync(DeliveryServiceType deliveryServiceType) {
        long partnerId = getPartner(deliveryServiceType);

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        PartnerSettingDto value = verifyLmsClient(deliveryServiceType);
        Assertions.assertFalse(value.getStockSyncEnabled());
    }

    @ParameterizedTest
    @DisplayName("Выключение синхронизации стоков по параметру IGNORE_STOCKS (CPA через API)")
    @EnumSource(value = DeliveryServiceType.class, names = {"DROPSHIP", "DROPSHIP_BY_SELLER"})
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.CpaAPI.IgnoreStocks.before.csv")
    void testCpaAPI_IgnoreStocksShouldSwitchOffStockSync(DeliveryServiceType deliveryServiceType) {
        long partnerId = getPartner(deliveryServiceType);

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verify(nesuClient).setStockSyncStrategy(eq(partnerId), anyLong(), eq(false));
        PartnerSettingDto value = verifyLmsClient(deliveryServiceType);
        Assertions.assertFalse(value.getStockSyncEnabled());
    }

    @ParameterizedTest
    @DisplayName("Выключение синхронизации стоков по параметру IGNORE_STOCKS (CPA через API)")
    @CsvSource({"DROPSHIP,true", "DROPSHIP_BY_SELLER,true", "DROPSHIP,false", "DROPSHIP_BY_SELLER,false"})
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.CpaAPI.IgnoreStocks.before.csv")
    void testCpaAPI_IgnoreStocksShouldSwitchOffStockSyncWithValues(
            DeliveryServiceType deliveryServiceType, boolean syncStocks) {
        long partnerId = getPartner(deliveryServiceType);

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .stockSyncEnabled(syncStocks)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verify(nesuClient).setStockSyncStrategy(eq(partnerId), anyLong(), eq(false));
        PartnerSettingDto value = verifyLmsClient(deliveryServiceType);
        Assertions.assertFalse(value.getStockSyncEnabled());
    }

    @ParameterizedTest
    @DisplayName("Включение синхронизации стоков по отсутствию параметра IGNORE_STOCKS (CPA через ПИ)")
    @EnumSource(value = DeliveryServiceType.class, names = {"DROPSHIP", "DROPSHIP_BY_SELLER"})
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.CpaPI.NotIgnoreStocks.before.csv")
    void testCpaPI_NoIgnoreStocksShouldSwitchOnStockSync(DeliveryServiceType deliveryServiceType) {
        long partnerId = getPartner(deliveryServiceType);

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        PartnerSettingDto value = verifyLmsClient(deliveryServiceType);
        Assertions.assertTrue(value.getStockSyncEnabled());
    }

    @ParameterizedTest
    @DisplayName("Не меняем синхронизации стоков по отсутствию параметра IGNORE_STOCKS (CPA через API)")
    @EnumSource(value = DeliveryServiceType.class, names = {"DROPSHIP", "DROPSHIP_BY_SELLER"})
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.CpaAPI.NotIgnoreStocks.before.csv")
    void testCpaAPI_NoIgnoreStocksAPIShouldNotSwitchOnStockSync(DeliveryServiceType deliveryServiceType) {
        long partnerId = getPartner(deliveryServiceType);

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verify(lmsClient).getPartner(partnerId);
        verify(lmsClient).changePartnerStatus(partnerId, PartnerStatus.ACTIVE);
        verify(lmsClient, never())
                .updatePartnerSettings(eq(partnerId), argThat(PartnerSettingDto::getStockSyncEnabled));
    }

    @ParameterizedTest
    @DisplayName("Проверка включённости синхронизации весогабаритов")
    @CsvSource({
            "DROPSHIP,false",
            "DROPSHIP_BY_SELLER,false"
    })
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.CpaPI.NotIgnoreStocks.before.csv")
    void testKorobyteEnabledByPartnerType(DeliveryServiceType deliveryServiceType, boolean korobyteSyncEnabled) {
        long partnerId = getPartner(deliveryServiceType);

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .korobyteSyncEnabled(false)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        PartnerSettingDto value = verifyLmsClient(deliveryServiceType);
        Assertions.assertEquals(korobyteSyncEnabled, value.getKorobyteSyncEnabled());
    }

    @ParameterizedTest
    @DisplayName("Выключение auto-switch-stock (CPA через ПИ)")
    @EnumSource(value = DeliveryServiceType.class, names = {"DROPSHIP", "DROPSHIP_BY_SELLER"})
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.CpaPI.NotIgnoreStocks.before.csv")
    void testCpaPI_SwitchOffAutoSwitchStockSync(DeliveryServiceType deliveryServiceType) {
        long partnerId = getPartner(deliveryServiceType);

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        PartnerSettingDto value = verifyLmsClient(deliveryServiceType);
        verify(nesuClient).setStockSyncStrategy(eq(partnerId), anyLong(), eq(true));
        Assertions.assertFalse(value.getAutoSwitchStockSyncEnabled());
    }

    @ParameterizedTest
    @DisplayName("Игнорирование auto-switch-stock (CPA через API)")
    @EnumSource(value = DeliveryServiceType.class, names = {"DROPSHIP", "DROPSHIP_BY_SELLER"})
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.CpaAPI.NotIgnoreStocks.before.csv")
    void testCpaAPI_IgnoreAutoSwitchStockSync(DeliveryServiceType deliveryServiceType) {
        long partnerId = getPartner(deliveryServiceType);

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verify(lmsClient).getPartner(partnerId);
        verify(lmsClient).changePartnerStatus(partnerId, PartnerStatus.ACTIVE);
        verify(lmsClient, never())
                .updatePartnerSettings(eq(partnerId), argThat(PartnerSettingDto::getStockSyncEnabled));
    }

    @Test
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.twoStepsDropship.before.csv")
    @DisplayName("Проверка прохождения полной двухшаговой регистрации партнёра")
    void testCompleteTwoStepsLogisticPartnerRegistrationDropship() {
        long partnerId = DROPSHIP_DONTWANT_ID;

        when(nesuClient.searchShopWithSenders(ShopWithSendersFilter.builder().shopIds(Set.of(partnerId)).build()))
                .thenReturn(List.of(ShopWithSendersDto.builder()
                        .id(partnerId)
                        .balanceContractId(123L)
                        .marketId(null)
                        .name("Ололо")
                        .senders(List.of(NamedEntity.builder().id(321L).name("Кое-кто").build()))
                        .build())
                );
        when(lmsClient.getPartner(partnerId)).thenReturn(
                Optional.of(PartnerResponse.newBuilder().id(partnerId).build()));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));

        verify(nesuClient, times(1)).configureShop(anyLong(), any(ConfigureShopDto.class));
        verify(nesuClient).setStockSyncStrategy(partnerId, partnerId, true);

        verify(lmsClient, times(2)).getPartner(partnerId);
        verify(lmsClient).changePartnerStatus(partnerId, PartnerStatus.TESTING);
        verify(lmsClient).updatePartnerSettings(eq(partnerId), argThat(PartnerSettingDto::getStockSyncEnabled));

        verifyNoMoreInteractions(nesuClient);
    }

    @Test
    @DisplayName("Синхронизация стоков не включается для DONT_WANT дропшипа ПИ, если есть катофф старше 60 дней")
    @DbUnitDataSet(before = "LmsPartnerStateListenerTest.DropshipAPI.success.before.csv")
    void testDropshipAPI_NoSyncStocks() {
        long partnerId = DROPSHIP_DONTWANT_WITH_OLD_CUTOFF_ID;

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verifyStockSyncWasNotUpdatedForDropship(partnerId);
    }

    @Test
    @DisplayName("Синхронизация стоков не включается для DONT_WANT дропшипа ПИ, " +
            "если есть катофф старше 60 дней на MARKETPLACE")
    @DbUnitDataSet(before = "testDropshipAPI_NoSyncStocksMarketplace.before.csv")
    void testDropshipAPI_NoSyncStocksMarketplace() {
        long partnerId = DROPSHIP_DONTWANT_ID;

        when(lmsClient.getPartner(partnerId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(partnerId)
                                .stockSyncSwitchReason(StockSyncSwitchReason.CHANGED_THROUGH_NESU_UI)
                                .build()
                ));

        lmsPartnerStateListener.onApplicationEvent(new PartnerStateChangedEvent(partnerId));
        verifyStockSyncWasNotUpdatedForDropship(partnerId);
    }

    private PartnerSettingDto verifyLmsClient(DeliveryServiceType deliveryServiceType) {
        long partnerId = getPartner(deliveryServiceType);

        ArgumentCaptor<PartnerSettingDto> capture = ArgumentCaptor.forClass(PartnerSettingDto.class);
        verify(lmsClient).getPartner(partnerId);
        verify(lmsClient).updatePartnerSettings(eq(partnerId), capture.capture());
        verify(lmsClient).changePartnerStatus(partnerId, PartnerStatus.ACTIVE);
        PartnerSettingDto value = capture.getValue();
        Assertions.assertEquals(StockSyncSwitchReason.NEW, value.getStockSyncSwitchReason());
        return value;
    }

    private void verifyStockSyncWasUpdatedForDropship(long partnerId) {
        ArgumentCaptor<PartnerSettingDto> capture = ArgumentCaptor.forClass(PartnerSettingDto.class);
        verify(lmsClient, times(2)).getPartner(partnerId);
        verify(lmsClient, times(1))
                .updatePartnerSettings(eq(partnerId), capture.capture());
        verify(lmsClient).changePartnerStatus(partnerId, PartnerStatus.TESTING);
        PartnerSettingDto value = capture.getValue();
        Assertions.assertEquals(StockSyncSwitchReason.NEW, value.getStockSyncSwitchReason());
        assertTrue(value.getStockSyncEnabled());
    }

    private void verifyStockSyncWasUpdatedForDsbs(long partnerId) {
        ArgumentCaptor<PartnerSettingDto> capture = ArgumentCaptor.forClass(PartnerSettingDto.class);
        verify(lmsClient, times(1)).getPartner(partnerId);
        verify(lmsClient, times(1))
                .updatePartnerSettings(eq(partnerId), capture.capture());
        verify(lmsClient).changePartnerStatus(partnerId, PartnerStatus.TESTING);
        PartnerSettingDto value = capture.getValue();
        Assertions.assertEquals(StockSyncSwitchReason.NEW, value.getStockSyncSwitchReason());
        assertTrue(value.getStockSyncEnabled());
    }

    private void verifyStockSyncWasNotUpdatedForDropship(long partnerId) {
        verify(lmsClient, times(2)).getPartner(partnerId);
        verify(lmsClient).changePartnerStatus(partnerId, PartnerStatus.INACTIVE);
        verify(lmsClient, never())
                .updatePartnerSettings(eq(partnerId), argThat(PartnerSettingDto::getStockSyncEnabled));
    }

    private void verifyStockSyncWasNotUpdatedForDsbs(long partnerId) {
        verify(lmsClient, times(1)).getPartner(partnerId);
        verify(lmsClient).changePartnerStatus(partnerId, PartnerStatus.INACTIVE);
        verify(lmsClient, never())
                .updatePartnerSettings(eq(partnerId), argThat(PartnerSettingDto::getStockSyncEnabled));
    }

    private LocalTransactionListener localTransactionListener() {
        LocalTransactionListener localTransactionListener = mock(LocalTransactionListener.class);
        doAnswer(invocation -> {
            TransactionListener listener = invocation.getArgument(0);
            listener.onBeforeCommit(mock(TransactionStatus.class));
            return null;
        }).when(localTransactionListener).addListener(any());
        return localTransactionListener;
    }

    private void mockMarketId() {
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            GetByPartnerResponse response = GetByPartnerResponse.newBuilder()
                    .setMarketId(MarketAccount.newBuilder().setMarketId(100500L).build())
                    .setSuccess(true)
                    .build();

            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(any(GetByPartnerRequest.class), any());
    }
}
