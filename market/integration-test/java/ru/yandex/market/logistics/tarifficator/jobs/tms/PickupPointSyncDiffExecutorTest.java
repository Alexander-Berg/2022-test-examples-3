package ru.yandex.market.logistics.tarifficator.jobs.tms;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.configuration.properties.YtProperties;
import ru.yandex.market.logistics.tarifficator.facade.PickupPointFacade;
import ru.yandex.market.logistics.tarifficator.jobs.producer.GenerateRevisionProducer;
import ru.yandex.market.logistics.tarifficator.service.pickuppoint.YtPickupPointService;
import ru.yandex.market.logistics.tarifficator.service.pricelist.PriceListService;
import ru.yandex.market.logistics.tarifficator.service.tariff.TariffDestinationPartnerService;
import ru.yandex.yt.ytclient.proxy.SelectRowsRequest;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.wire.UnversionedRow;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@DatabaseSetup("/tags/tags.xml")
@DatabaseSetup("/tariffs/courier_without_active_price_lists_1.xml")
@DatabaseSetup(value = "/tariffs/pick_up_100.xml", type = DatabaseOperation.INSERT)
@DatabaseSetup(value = "/tariffs/pick_up_150.xml", type = DatabaseOperation.INSERT)
@DatabaseSetup(value = "/tariffs/post_200.xml", type = DatabaseOperation.INSERT)
@DatabaseSetup(value = "/tariffs/courier_300.xml", type = DatabaseOperation.INSERT)
@DatabaseSetup(value = "/tariffs/market_courier_600.xml", type = DatabaseOperation.INSERT)
@DisplayName("Интеграционный тест PickupPointSyncDiffExecutor")
class PickupPointSyncDiffExecutorTest extends AbstractPickupPointSyncTest {

    @Autowired
    private YtClient ytClient;
    @Autowired
    private PriceListService priceListService;
    @Autowired
    private GenerateRevisionProducer generateRevisionProducer;
    @Autowired
    private YtPickupPointService ytPickupPointService;
    @Autowired
    private PickupPointFacade pickupPointFacade;
    @Autowired
    private YtProperties ytProperties;
    @Autowired
    private TariffDestinationPartnerService tariffDestinationPartnerService;

    @AfterEach
    void afterEach() {
        verifyNoMoreInteractions(ytClient);
    }

    @Test
    @DisplayName("Отсутствуют точки с датой последнего обновления")
    void notSyncLastUpdatedAtIsNull() {
        doJob();
        verifyZeroInteractions(ytClient);
    }

    @Test
    @DisplayName("Синхронизация новых точек")
    @DatabaseSetup("/tms/points-sync/before/initial_points.xml")
    @ExpectedDatabase(
        value = "/tms/points-sync/after/actual_points_state.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void syncNewPoints() {
        syncPoints();
        assertGenerationScheduledForPriceLists(Set.of(600L, 200L, 100L));
    }

    @Test
    @DisplayName("Обновление старых точек")
    @DatabaseSetup("/tms/points-sync/before/initial_points.xml")
    @DatabaseSetup(value = "/tms/points-sync/before/old_points_102_173_194.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/tms/points-sync/after/actual_points_state.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateOldPoints() {
        syncPoints();
        assertGenerationScheduledForPriceLists(Set.of(600L, 100L));
    }

    @Test
    @DisplayName("Добавление точки, которая связанна с тарифом, триггерит переваривание прайс-листа")
    @DatabaseSetup("/tms/points-sync/before/initial_points.xml")
    void createPoint() {
        syncPoints(newPointRow(602L, 6200L, 1L, 20370L, true));
        assertGenerationScheduledForPriceLists(Set.of(600L));
    }

    @Test
    @DisplayName("Изменение точки (exact_region_id), которая связанна с тарифом, триггерит переваривание прайс-листа")
    @DatabaseSetup("/tms/points-sync/before/initial_points.xml")
    @DatabaseSetup("/tms/points-sync/before/old_points_6200.xml")
    void updateOldPointsExactRegionId() {
        syncPoints(newPointRow(602L, 6200L, 1L, 20371L, true));
        assertGenerationScheduledForPriceLists(Set.of(600L));
    }

    @Test
    @DisplayName("Обновление старых и добавление новых точек")
    @DatabaseSetup("/tms/points-sync/before/initial_points.xml")
    @DatabaseSetup(value = "/tms/points-sync/before/old_102_new_173_points.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/tms/points-sync/after/actual_points_state.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createAndUpdatePoints() {
        syncPoints();
        assertGenerationScheduledForPriceLists(Set.of(100L, 600L));
    }

    @Test
    @DisplayName("Точки не изменились")
    @DatabaseSetup("/tms/points-sync/after/actual_points_state.xml")
    @ExpectedDatabase(
        value = "/tms/points-sync/after/actual_points_state.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void noChanges() {
        syncPoints();
        assertNoGenerationScheduled();
    }

    @Test
    @DisplayName("Смена локации у точки, перегенерация обоих тарифов")
    @DatabaseSetup("/tms/points-sync/before/old_points_200.xml")
    @ExpectedDatabase(
        value = "/tms/points-sync/after/actual_points_200.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updatePointLocation() {
        syncPoints(newPointRow(150L, 200L, 42L, 108234L, false));
        assertGenerationScheduledForPriceLists(Set.of(100L, 150L));
    }

    private void syncPoints() {
        syncPoints(
            newPointRow(100L, 102L, 42L, 42L, true),
            newPointRow(200L, 173L, 197L, 108234L, false),
            newPointRow(100L, 194L, 197L, 108234L, true),
            newPointRow(602L, 6200L, 1L, 20370L, true)
        );
    }

    private void syncPoints(UnversionedRow... pointRows) {
        when(ytClient.selectRows(any(SelectRowsRequest.class)))
            .thenReturn(createResponse(List.of(pointRows)));
        doJob();
        verify(ytClient).selectRows(refEq(getSelectRowsRequest(SELECT_LAST_UPDATED_POINTS_QUERY)));
    }

    private void doJob() {
        new PickupPointSyncDiffExecutor(
            generateRevisionProducer,
            ytPickupPointService,
            pickupPointFacade,
            priceListService,
            tariffDestinationPartnerService,
            ytProperties.getBatchSize()
        )
            .doJob(context);
    }
}
