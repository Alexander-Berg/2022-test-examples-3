package ru.yandex.market.fulfillment.stockstorage.service.warehouse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.domain.converter.PartnerResponseConverter;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.Warehouse;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.JobWhPair;
import ru.yandex.market.fulfillment.stockstorage.service.warehouse.cache.WarehouseSyncInfo;
import ru.yandex.market.fulfillment.stockstorage.service.warehouse.group.StocksWarehouseGroupCache;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class WarehouseSyncServiceTest extends AbstractContextualTest {

    @Autowired
    private StocksWarehouseGroupCache stocksWarehouseGroupCache;

    @BeforeEach
    void loadCache() {
        stocksWarehouseGroupCache.reload();
    }

    /**
     * Проверяем, что все warehouse'ы, вернувшиеся от LMSClient,
     * попадут в список активных warehouse'ов после синка.
     */
    @Test
    public void syncEnabledWarehouses() {
        assertAllPartnersAreSynced(createActivePartnerResponses());
    }

    /**
     * Проверяем, что все warehouse'ы, вернувшиеся от LMSClient,
     * попадут в список активных warehouse'ов после синка, несмотря на их статус ACTIVE/TESTING/INACTIVE/FROZEN.
     */
    @Test
    public void syncWarehousesWithDifferentStatuses() {
        List<PartnerResponse> partnerResponses = Arrays.asList(
                PartnerResponse.newBuilder()
                        .id(2)
                        .partnerType(PartnerType.FULFILLMENT)
                        .name("MARKET_ROSTOV")
                        .status(PartnerStatus.ACTIVE)
                        .stockSyncEnabled(true)
                        .build(),
                PartnerResponse.newBuilder()
                        .id(3)
                        .partnerType(PartnerType.FULFILLMENT)
                        .name("CROSS_DOCK_VELO_SHOP")
                        .status(PartnerStatus.INACTIVE)
                        .stockSyncEnabled(true)
                        .build(),
                PartnerResponse.newBuilder()
                        .id(4)
                        .partnerType(PartnerType.FULFILLMENT)
                        .name("CROSS_DOCK_ZDRAVCITY_NG")
                        .status(PartnerStatus.TESTING)
                        .stockSyncEnabled(true)
                        .build(),
                PartnerResponse.newBuilder()
                        .id(5)
                        .partnerType(PartnerType.FULFILLMENT)
                        .name("CROSS_DOCK_ZDRAVCITY_ROV")
                        .status(PartnerStatus.FROZEN)
                        .stockSyncEnabled(true)
                        .build()
        );

        assertAllPartnersAreSynced(partnerResponses);
    }

    /**
     * Проверяем, что при пустом ответе от LMSClient -
     * WarehouseSyncService будет возвращать пустой набор активных складов.
     */
    @Test
    public void syncEmptyWarehouses() {
        assertAllPartnersAreSynced(Collections.emptyList());
    }

    /**
     * Проверяем, что при неудачной попытке синхронизовать кэш с golden source -
     * сервис продолжит работать со старым значением кэша, а в объекте с информацией будет присутствовать
     * класс ошибки.
     */
    @Test
    public void willWorkOnPreviousCacheIfFailedToSync() {
        List<PartnerResponse> partnerResponses = createActivePartnerResponses();

        assertAllPartnersAreSynced(partnerResponses);

        Mockito.reset(fulfillmentLmsClient);

        doThrow(RuntimeException.class).when(fulfillmentLmsClient).searchPartners(any(SearchPartnerFilter.class));

        warehouseSyncService.recomputeCache();

        verify(fulfillmentLmsClient, atLeastOnce()).searchPartners(any(SearchPartnerFilter.class));

        assertCacheContainsExactly(partnerResponses);
        WarehouseSyncInfo syncInfo = warehouseSyncService.getWarehouseSyncInfo();

        softly.assertThat(syncInfo.getLatestSyncAttemptFailureReason()).isPresent();
        softly.assertThat(syncInfo.getLatestSyncAttemptFailureReason())
                .hasValueSatisfying(
                        (val) -> softly.assertThat(val.getClass()).isEqualTo(RuntimeException.class)
                );
    }

    @Test
    public void dropShipWithDisabledStockSyncAreNotSynced() {
        List<PartnerResponse> responses = new ArrayList<>(createActivePartnerResponses());
        responses.add(
                PartnerResponse.newBuilder()
                        .id(148)
                        .partnerType(PartnerType.DROPSHIP)
                        .name("ANOTHER_DROPSHIP")
                        .status(PartnerStatus.ACTIVE)
                        .stockSyncEnabled(false)
                        .build()
        );
        softly.assertThat(responses).hasSize(4);
        assertAllPartnersAreSynced(responses);
    }

    @Test
    public void fulfillmentWithDisabledStockSyncAreNotSynced() {
        List<PartnerResponse> responses = new ArrayList<>(createActivePartnerResponses());
        responses.add(
                PartnerResponse.newBuilder()
                        .id(148)
                        .partnerType(PartnerType.FULFILLMENT)
                        .name("ANOTHER_DROPSHIP")
                        .status(PartnerStatus.ACTIVE)
                        .stockSyncEnabled(false)
                        .build()
        );
        softly.assertThat(responses).hasSize(4);
        assertAllPartnersAreSynced(responses);
    }

    /**
     * Проверяет, что каждому складу нашелся соответствующий интервал в таблице,
     * syncSchedulerCounter делится нацело на все интервалы, и пары JobWhPair сгенерированы.
     */
    @Test
    @DatabaseSetup("classpath:database/states/warehouse_sync/2.xml")
    public void runThreeJobsSuccessfully() {
        setActiveWarehouses(145, 146, 147);

        List<JobWhPair> actualJobWhPairs = warehouseSyncService.getSyncJobWHPairs(1);

        verifyJobWhPairs(actualJobWhPairs);
    }

    /**
     * Проверяет, что при active=false в таблице ff_interval пары JobWhPair не будут сгенерированы.
     */
    @Test
    @DatabaseSetup("classpath:database/states/warehouse_sync/3.xml")
    public void doNotStartInactiveFlow() {
        setActiveWarehouses(145, 146, 147);

        List<JobWhPair> actualJobWhPairs = warehouseSyncService.getSyncJobWHPairs(1);

        Assert.assertTrue(actualJobWhPairs.isEmpty());
    }

    /**
     * Проверяет, что если в SyncJobName не существует джобы из ff_interval.sync_job_name,
     * то не будут сгенерированы JobWhPair.
     */
    @Test
    @DatabaseSetup("classpath:database/states/warehouse_sync/4.xml")
    public void doNotStartNonexistentJob() {
        setActiveWarehouses(145, 146, 147);

        List<JobWhPair> actualJobWhPairs = warehouseSyncService.getSyncJobWHPairs(1);

        Assert.assertTrue(actualJobWhPairs.isEmpty());
    }

    /**
     * Проверяет, что если syncSchedulerCounter НЕ делится нацело на интервал джобы,
     * то пара JobWhPair сгенерирована не будет.
     */
    @Test
    @DatabaseSetup("classpath:database/states/warehouse_sync/5.xml")
    public void doNotStartJobIfCounterDoesNotMatchInterval() {
        setActiveWarehouses(145, 146, 147);

        List<JobWhPair> actualJobWhPairs = warehouseSyncService.getSyncJobWHPairs(1);

        Assert.assertTrue(actualJobWhPairs.isEmpty());
    }

    /**
     * Проверяет, что если syncSchedulerCounter делится нацело на интервал джобы,
     * то сгенерируется пара JobWhPair.
     */
    @Test
    @DatabaseSetup("classpath:database/states/warehouse_sync/6.xml")
    public void startJobIfCounterMatchesInterval() {
        setActiveWarehouses(145, 146, 147);

        List<JobWhPair> actualJobWhPairs = warehouseSyncService.getSyncJobWHPairs(1);

        Assert.assertEquals(1, actualJobWhPairs.size());
    }

    /**
     * Проверяет, что для второстепенного склада 146 не запустится джоба.
     */
    @Test
    @DatabaseSetup({"classpath:database/states/warehouse_sync/2.xml",
            "classpath:database/states/warehouse_sync/group.xml"})
    public void runSyncForGroup() {
        setActiveWarehouses(145, 146, 147);

        List<JobWhPair> actualJobWhPairs = warehouseSyncService.getSyncJobWHPairs(1);

        assertThat(actualJobWhPairs)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(new JobWhPair(145, "FullSync", 250),
                        new JobWhPair(147, "FullSync", 200));
    }

    private void verifyJobWhPairs(List<JobWhPair> jobWhPairs) {
        Assert.assertNotNull(jobWhPairs);
        Assert.assertEquals(3, jobWhPairs.size());

        JobWhPair firstJobWhPair = jobWhPairs.get(0);
        Assert.assertEquals(new Integer(145), firstJobWhPair.getWarehouseId());
        Assert.assertEquals("FullSync", firstJobWhPair.getSyncJobName());
        Assert.assertEquals(250, firstJobWhPair.getBatchSize());

        JobWhPair secondJobWhPair = jobWhPairs.get(1);
        Assert.assertEquals(new Integer(146), secondJobWhPair.getWarehouseId());
        Assert.assertEquals("FullSync", secondJobWhPair.getSyncJobName());
        Assert.assertEquals(200, secondJobWhPair.getBatchSize());

        JobWhPair thirdJobWhPair = jobWhPairs.get(2);
        Assert.assertEquals(new Integer(147), thirdJobWhPair.getWarehouseId());
        Assert.assertEquals("FullSync", thirdJobWhPair.getSyncJobName());
        Assert.assertEquals(200, thirdJobWhPair.getBatchSize());
    }

    private void assertAllPartnersAreSynced(List<PartnerResponse> partnerResponses) {
        mockSearchPartners(partnerResponses);

        warehouseSyncService.recomputeCache();

        assertCacheContainsExactly(partnerResponses);

        verify(fulfillmentLmsClient).searchPartners(any(SearchPartnerFilter.class));
    }

    private void assertCacheContainsExactly(List<PartnerResponse> partnerResponses) {
        Set<Integer> enabledIds = partnerResponses
                .stream()
                .filter(PartnerResponse::getStockSyncEnabled)
                .map(PartnerResponse::getId)
                .map(Long::intValue)
                .collect(Collectors.toSet());

        Set<Warehouse> warehouses = partnerResponses.stream()
                .map(PartnerResponseConverter::convert)
                .map(Warehouse::of)
                .collect(Collectors.toSet());

        ImmutableMap<Integer, Warehouse> warehousesMap = warehouses.stream()
                .collect(ImmutableMap.toImmutableMap(Warehouse::getId, Function.identity()));

        softly.assertThat(warehouseSyncService.getActiveWarehouseIds())
                .containsExactlyElementsOf(enabledIds);

        softly.assertThat(warehouseSyncService.getAllWarehousesMap())
                .containsAllEntriesOf(warehousesMap);
    }

    private List<PartnerResponse> createActivePartnerResponses() {
        return Arrays.asList(
                PartnerResponse.newBuilder()
                        .id(145)
                        .partnerType(PartnerType.FULFILLMENT)
                        .name("MARKET_ROSTOV")
                        .status(PartnerStatus.ACTIVE)
                        .stockSyncEnabled(true)
                        .build(),

                PartnerResponse.newBuilder()
                        .id(146)
                        .partnerType(PartnerType.FULFILLMENT)
                        .name("CROSS_DOCK_VELO_SHOP")
                        .status(PartnerStatus.ACTIVE)
                        .stockSyncEnabled(true)
                        .build(),

                PartnerResponse.newBuilder()
                        .id(147)
                        .partnerType(PartnerType.DROPSHIP)
                        .name("HOLODILNIK_RU")
                        .status(PartnerStatus.ACTIVE)
                        .stockSyncEnabled(true)
                        .build()
        );
    }
}
