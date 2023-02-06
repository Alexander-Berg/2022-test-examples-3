package ru.yandex.market.logistics.tarifficator.jobs.tms;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.tarifficator.configuration.properties.YtProperties;
import ru.yandex.market.logistics.tarifficator.facade.PickupPointFacade;
import ru.yandex.market.logistics.tarifficator.jobs.producer.GenerateRevisionProducer;
import ru.yandex.market.logistics.tarifficator.service.pickuppoint.YtPickupPointService;
import ru.yandex.market.logistics.tarifficator.service.pricelist.PriceListService;
import ru.yandex.market.logistics.tarifficator.service.tariff.TariffDestinationPartnerService;
import ru.yandex.yt.ytclient.proxy.YtClient;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DatabaseSetup("/tags/tags.xml")
@DatabaseSetup("/tariffs/courier_without_active_price_lists_1.xml")
@DatabaseSetup(value = "/tariffs/pick_up_100.xml", type = DatabaseOperation.INSERT)
@DatabaseSetup(value = "/tariffs/pick_up_150.xml", type = DatabaseOperation.INSERT)
@DatabaseSetup(value = "/tariffs/post_200.xml", type = DatabaseOperation.INSERT)
@DatabaseSetup(value = "/tariffs/courier_300.xml", type = DatabaseOperation.INSERT)
@DatabaseSetup(value = "/tariffs/market_courier_600.xml", type = DatabaseOperation.INSERT)
@DisplayName("Интеграционный тест PickupPointFullSyncExecutor")
class PickupPointFullSyncExecutorTest extends AbstractPickupPointSyncTest {

    private final JobExecutionContext context = mock(JobExecutionContext.class);

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
    private FeatureProperties featureProperties;
    @Autowired
    private TariffDestinationPartnerService tariffDestinationPartnerService;

    @Test
    @DisplayName("Синхронизация новых точек")
    @ExpectedDatabase(
        value = "/tms/points-sync/after/actual_points_full_state.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void syncNewPoints() {
        syncPoints();
        assertGenerationScheduledForPriceLists(Set.of(200L, 100L, 600L));
    }

    @Test
    @DisplayName(
        "Синхронизация новых точек. Изменение точки в нижележащем регионе " +
            "триггерит переваривание прайс листа с флагом locality_only = false"
    )
    void syncNewPointsTriggersPriceListGenerationWithSuperLocationWithLocalityOnlyFalse() {
        when(ytClient.selectRows(refEq(getSelectRowsRequestForPointsAfterIdQuery(0L)))).thenReturn(
            createResponse(List.of(
                newPointRow(100L, 102L, 42L, 42L, true),
                newPointRow(200L, 173L, 108234L, 100L, false),
                newPointRow(100L, 194L, 197L, 108234L, true)
            ))
        );
        when(ytClient.selectRows(refEq(getSelectRowsRequestForPointsAfterIdQuery(194L)))).thenReturn(
            createResponse(List.of())
        );
        doJob();
        assertGenerationScheduledForPriceLists(Set.of(200L, 100L));
    }

    @Test
    @DisplayName(
        "Синхронизация новых точек. Изменение точки в нижележащем регионе " +
            "не триггерит переваривание прайс листа с флагом locality_only = true"
    )
    void syncNewPointsTriggersPriceListGenerationWithSuperLocationWithLocalityOnlyTrue() {
        when(ytClient.selectRows(refEq(getSelectRowsRequestForPointsAfterIdQuery(0L)))).thenReturn(
            createResponse(List.of(
                newPointRow(100L, 102L, 42L, 100L, true),
                newPointRow(200L, 173L, 108234L, 100L, false),
                newPointRow(100L, 194L, 197L, 108234L, true)
            ))
        );
        when(ytClient.selectRows(refEq(getSelectRowsRequestForPointsAfterIdQuery(194L)))).thenReturn(
            createResponse(List.of())
        );
        doJob();
        assertGenerationScheduledForPriceLists(Set.of(100L, 200L));
    }

    @Test
    @DisplayName("Обновление старых точек")
    @DatabaseSetup("/tms/points-sync/before/old_points_102_173_194.xml")
    @DatabaseSetup(value = "/tms/points-sync/before/old_points_251_267.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/tms/points-sync/after/actual_points_full_state.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateOldPoints() {
        syncPoints();
        assertGenerationScheduledForPriceLists(Set.of(100L, 600L));
    }

    @Test
    @DisplayName("Обновление старых и добавление новых точек")
    @DatabaseSetup("/tms/points-sync/before/old_102_new_173_points.xml")
    @ExpectedDatabase(
        value = "/tms/points-sync/after/actual_points_full_state.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createAndUpdatePoints() {
        syncPoints();
        assertGenerationScheduledForPriceLists(Set.of(200L, 100L, 600L));
    }

    @Test
    @DisplayName("Точки не изменились")
    @DatabaseSetup("/tms/points-sync/after/actual_points_full_state.xml")
    @ExpectedDatabase(
        value = "/tms/points-sync/after/actual_points_full_state.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void noChanges() {
        syncPoints();
        assertNoGenerationScheduled();
    }


    @Test
    @DisplayName("Изменения активности точки не триггерит переваривание прайс-листа")
    @DatabaseSetup("/tms/points-sync/before/old_points_194.xml")
    @DatabaseSetup(value = "/tms/points-sync/before/old_points_6200.xml", type = DatabaseOperation.REFRESH)
    void pointActivityChangeDoesNotTriggerPriceListGeneration() {
        when(featureProperties.isPickupPointsFetchInactiveByDefault()).thenReturn(true);
        when(ytClient.selectRows(refEq(getSelectRowsRequestForPointsAfterIdQuery(0L)))).thenReturn(
            createResponse(List.of(
                newPointRow(100L, 194L, 197L, 108234L, false),
                newPointRow(602L, 6200L, 1L, 20370L, false)
            ))
        );
        when(ytClient.selectRows(refEq(getSelectRowsRequestForPointsAfterIdQuery(6200L)))).thenReturn(
            createResponse(List.of())
        );
        doJob();
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
        when(ytClient.selectRows(refEq(getSelectRowsRequestForPointsAfterIdQuery(0L)))).thenReturn(
            createResponse(List.of(
                newPointRow(150L, 200L, 42L, 108234L, false)
            ))
        );
        when(ytClient.selectRows(refEq(getSelectRowsRequestForPointsAfterIdQuery(200L)))).thenReturn(
            createResponse(List.of())
        );
        doJob();
        assertGenerationScheduledForPriceLists(Set.of(100L, 150L));
    }

    private void syncPoints() {
        mockYtClient();
        doJob();
        verifyYtClient();
    }

    private void mockYtClient() {
        when(ytClient.selectRows(refEq(getSelectRowsRequestForPointsAfterIdQuery(0L)))).thenReturn(
            createResponse(List.of(
                newPointRow(100L, 102L, 42L, 42L, true),
                newPointRow(200L, 173L, 197L, 108234L, false),
                newPointRow(100L, 194L, 197L, 108234L, true)
            ))
        );
        when(ytClient.selectRows(refEq(getSelectRowsRequestForPointsAfterIdQuery(194L)))).thenReturn(
            createResponse(List.of(
                newPointRow(200L, 251L, 197L, 108234L, true),
                newPointRow(100L, 267L, 197L, 108234L, false),
                newPointRow(602L, 6200L, 1L, 20370L, true)
            ))
        );
        when(ytClient.selectRows(refEq(getSelectRowsRequestForPointsAfterIdQuery(6200L)))).thenReturn(
            createResponse(List.of())
        );
    }

    private void doJob() {
        new PickupPointFullSyncExecutor(
            generateRevisionProducer,
            ytPickupPointService,
            pickupPointFacade,
            priceListService,
            tariffDestinationPartnerService,
            ytProperties.getBatchSize()
        )
            .doJob(context);
    }

    private void verifyYtClient() {
        verify(ytClient).selectRows(refEq(getSelectRowsRequestForPointsAfterIdQuery(0L)));
        verify(ytClient).selectRows(refEq(getSelectRowsRequestForPointsAfterIdQuery(194L)));
        verify(ytClient).selectRows(refEq(getSelectRowsRequestForPointsAfterIdQuery(6200L)));
    }
}
