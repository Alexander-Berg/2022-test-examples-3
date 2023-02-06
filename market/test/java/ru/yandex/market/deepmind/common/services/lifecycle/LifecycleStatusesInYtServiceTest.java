package ru.yandex.market.deepmind.common.services.lifecycle;

import java.util.List;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.db.monitoring.DbMonitoring;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.category.DeepmindCategoryRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.services.DeepmindConstants;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusResult;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusService;
import ru.yandex.market.deepmind.common.services.statuses.pojo.MskuStatusWarning;
import ru.yandex.market.deepmind.common.services.statuses.pojo.SskuStatusWarning;
import ru.yandex.market.deepmind.common.services.statuses.pojo.StatusWarning.Type;
import ru.yandex.market.deepmind.common.solomon.DeepmindSolomonPushService;
import ru.yandex.market.deepmind.common.utils.Transliterator;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.config.YtAndYqlJdbcAutoCluster;
import ru.yandex.market.yql_query_service.service.QueryService;

public class LifecycleStatusesInYtServiceTest extends DeepmindBaseDbTestClass {

    private LifecycleStatusesInYtService service;

    @Resource
    private DbMonitoring monitoring;

    @Before
    public void setUp() {
        service = new LifecycleStatusesInYtService(
            Mockito.mock(DSLContext.class),
            Mockito.mock(SeasonRepository.class),
            Mockito.mock(SskuMskuStatusService.class),
            Mockito.mock(StorageKeyValueService.class),
            Mockito.mock(YtAndYqlJdbcAutoCluster.class),
            Mockito.mock(YPath.class),
            "test_pool",
            1,
            Mockito.mock(QueryService.class),
            Mockito.mock(YPath.class),
            Mockito.mock(YPath.class),
            Mockito.mock(YPath.class),
            Mockito.mock(YPath.class),
            Mockito.mock(YPath.class),
            Mockito.mock(YPath.class),
            Mockito.mock(YPath.class),
            Mockito.mock(YPath.class),
            Mockito.mock(YPath.class),
            Mockito.mock(YPath.class),
            Mockito.mock(YPath.class),
            Mockito.mock(YPath.class),
            Mockito.mock(DeepmindSolomonPushService.class),
            monitoring,
            Mockito.mock(DeepmindWarehouseRepository.class),
            Mockito.mock(DeepmindCategoryRepository.class)
        );
    }

    @After
    public void clearMonitoring() {
        monitoring.getOrCreateUnit(DeepmindConstants.LIFECYCLE_STATUSES_NOT_SAVED).ok();
    }

    @Test
    public void testMonitoringMessage() {
        var errorInfo = MbocErrors.get().offerDelistedHasStocks();
        SskuMskuStatusResult result = SskuMskuStatusResult.ok(List.of(), List.of(), List.of(
            new MskuStatusWarning(Type.VALIDATION, 1L, errorInfo),
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(11, "sku1"),
                errorInfo)
        ));

        var monitoringItem = new LifecycleStatusesInYtService.MonitoringItem();
        monitoringItem.addResult(result);

        service.processMonitoring(monitoringItem);

        var unit = monitoring.getOrCreateUnit(DeepmindConstants.LIFECYCLE_STATUSES_NOT_SAVED);
        Assertions.assertThat(unit.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(unit.getMessage()).isEqualTo(
            String.format("(1) -> %1$s; (supplier_id: 11; shop_sku: sku1) -> %1$s",
                Transliterator.translate(errorInfo.render()))
        );
    }

    @Test
    public void testMonitoringMessageManyWarnings() {
        var errorInfo = MbocErrors.get().offerDelistedHasStocks();
        SskuMskuStatusResult result = SskuMskuStatusResult.ok(List.of(), List.of(), List.of(
            new MskuStatusWarning(Type.VALIDATION, 1L, errorInfo),
            new MskuStatusWarning(Type.VALIDATION, 2L, errorInfo),
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(11, "sku1"), errorInfo),
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(12, "sku2"), errorInfo),
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(13, "sku3"), errorInfo),
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(14, "sku4"), errorInfo)
        ));

        var monitoringItem = new LifecycleStatusesInYtService.MonitoringItem();
        monitoringItem.addResult(result);

        service.processMonitoring(monitoringItem);

        var unit = monitoring.getOrCreateUnit(DeepmindConstants.LIFECYCLE_STATUSES_NOT_SAVED);
        Assertions.assertThat(unit.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(unit.getMessage()).isEqualTo(
            String.format("(1) -> %1$s; (2) -> %1$s...+4", Transliterator.translate(errorInfo.render()))
        );
    }

    @Test
    public void testMonitoringMessageBatches() {
        var errorInfo = MbocErrors.get().offerDelistedHasStocks();
        SskuMskuStatusResult result1 = SskuMskuStatusResult.ok(List.of(), List.of(), List.of(
            new MskuStatusWarning(Type.VALIDATION, 1L, errorInfo),
            new MskuStatusWarning(Type.VALIDATION, 2L, errorInfo)
        ));

        SskuMskuStatusResult result2 = SskuMskuStatusResult.ok(List.of(), List.of(), List.of(
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(11, "sku1"), errorInfo),
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(12, "sku2"), errorInfo)
        ));

        var monitoringItem = new LifecycleStatusesInYtService.MonitoringItem();
        monitoringItem.addResult(result1);
        monitoringItem.addResult(result2);

        service.processMonitoring(monitoringItem);

        var unit = monitoring.getOrCreateUnit(DeepmindConstants.LIFECYCLE_STATUSES_NOT_SAVED);
        Assertions.assertThat(unit.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(unit.getMessage()).isEqualTo(
            String.format("(1) -> %1$s; (2) -> %1$s...+2", Transliterator.translate(errorInfo.render()))
        );
    }

    @Test
    public void testMonitoringMessageManyBatches() {
        var errorInfo = MbocErrors.get().offerDelistedHasStocks();
        SskuMskuStatusResult result1 = SskuMskuStatusResult.ok(List.of(), List.of(), List.of(
            new MskuStatusWarning(Type.VALIDATION, 1L, errorInfo),
            new MskuStatusWarning(Type.VALIDATION, 2L, errorInfo),
            new MskuStatusWarning(Type.VALIDATION, 3L, errorInfo),
            new MskuStatusWarning(Type.VALIDATION, 4L, errorInfo)
        ));

        SskuMskuStatusResult result2 = SskuMskuStatusResult.ok(List.of(), List.of(), List.of(
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(11, "sku1"), errorInfo),
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(12, "sku2"), errorInfo)
        ));

        SskuMskuStatusResult result3 = SskuMskuStatusResult.ok(List.of(), List.of(), List.of(
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(13, "sku3"), errorInfo),
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(14, "sku4"), errorInfo)
        ));

        var monitoringItem = new LifecycleStatusesInYtService.MonitoringItem();
        monitoringItem.addResult(result1);
        monitoringItem.addResult(result2);
        monitoringItem.addResult(result3);

        service.processMonitoring(monitoringItem);

        var unit = monitoring.getOrCreateUnit(DeepmindConstants.LIFECYCLE_STATUSES_NOT_SAVED);
        Assertions.assertThat(unit.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(unit.getMessage()).isEqualTo(
            String.format("(1) -> %1$s; (2) -> %1$s...+6", Transliterator.translate(errorInfo.render()))
        );
    }

    @Test
    public void testMonitoringOK() {
        SskuMskuStatusResult result = SskuMskuStatusResult.ok(List.of(), List.of(), List.of());

        var monitoringItem = new LifecycleStatusesInYtService.MonitoringItem();
        monitoringItem.addResult(result);

        service.processMonitoring(monitoringItem);

        var unit = monitoring.getOrCreateUnit(DeepmindConstants.LIFECYCLE_STATUSES_NOT_SAVED);
        Assertions.assertThat(unit.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void testMonitoringWarningAfterOk() {
        SskuMskuStatusResult resultOk = SskuMskuStatusResult.ok(List.of(), List.of(), List.of());
        var errorInfo = MbocErrors.get().offerDelistedHasStocks();
        SskuMskuStatusResult resultWarning = SskuMskuStatusResult.ok(List.of(), List.of(), List.of(
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(11, "sku1"), errorInfo),
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(12, "sku2"), errorInfo)
        ));

        var monitoringItem = new LifecycleStatusesInYtService.MonitoringItem();
        monitoringItem.addResult(resultOk);
        monitoringItem.addResult(resultWarning);

        service.processMonitoring(monitoringItem);

        var unit = monitoring.getOrCreateUnit(DeepmindConstants.LIFECYCLE_STATUSES_NOT_SAVED);
        Assertions.assertThat(unit.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(unit.getMessage()).isEqualTo(
            String.format("(supplier_id: 11; shop_sku: sku1) -> %1$s; (supplier_id: 12; shop_sku: sku2) -> %1$s",
                Transliterator.translate(errorInfo.render()))
        );
    }

    @Test
    public void testMonitoringOkAfterWarning() {
        SskuMskuStatusResult resultOk = SskuMskuStatusResult.ok(List.of(), List.of(), List.of());
        var errorInfo = MbocErrors.get().offerDelistedHasStocks();
        SskuMskuStatusResult resultWarning = SskuMskuStatusResult.ok(List.of(), List.of(), List.of(
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(11, "sku1"), errorInfo),
            new SskuStatusWarning(Type.VALIDATION, new ServiceOfferKey(12, "sku2"), errorInfo)
        ));

        var monitoringItem = new LifecycleStatusesInYtService.MonitoringItem();
        monitoringItem.addResult(resultWarning);
        monitoringItem.addResult(resultOk);

        service.processMonitoring(monitoringItem);

        var unit = monitoring.getOrCreateUnit(DeepmindConstants.LIFECYCLE_STATUSES_NOT_SAVED);
        Assertions.assertThat(unit.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(unit.getMessage()).isEqualTo(
            String.format("(supplier_id: 11; shop_sku: sku1) -> %1$s; (supplier_id: 12; shop_sku: sku2) -> %1$s",
                Transliterator.translate(errorInfo.render()))
        );
    }

    @Test
    public void testMonitoringIgnoreNonValidationWarning() {
        var errorInfo = MbocErrors.get().offerDelistedHasStocks();
        SskuMskuStatusResult result = SskuMskuStatusResult.ok(List.of(), List.of(), List.of(
            new SskuStatusWarning(Type.CONCURRENT_UPDATE, 11, "sku1", errorInfo),
            new SskuStatusWarning(Type.VALIDATION, 12, "sku2", errorInfo)
        ));

        var monitoringItem = new LifecycleStatusesInYtService.MonitoringItem();
        monitoringItem.addResult(result);

        service.processMonitoring(monitoringItem);

        var unit = monitoring.getOrCreateUnit(DeepmindConstants.LIFECYCLE_STATUSES_NOT_SAVED);
        Assertions.assertThat(unit.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(unit.getMessage()).isEqualTo(
            "(supplier_id: 12; shop_sku: sku2) -> " + Transliterator.translate(errorInfo.render())
        );
    }

    @Test
    public void testMonitoringCritical() {
        var errorInfo = MbocErrors.get().offerDelistedHasStocks();
        SskuMskuStatusResult result = SskuMskuStatusResult.failed(List.of(
            new SskuStatusWarning(Type.NOT_EXISTING_MSKU, 11, "sku1", errorInfo)
        ));

        var monitoringItem = new LifecycleStatusesInYtService.MonitoringItem();
        monitoringItem.addResult(result);

        service.processMonitoring(monitoringItem);

        var unit = monitoring.getOrCreateUnit(DeepmindConstants.LIFECYCLE_STATUSES_NOT_SAVED);
        Assertions.assertThat(unit.getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
        Assertions.assertThat(unit.getMessage()).isEqualTo(String.format("Failed to save statutes. Errors:\n" +
            "SskuStatusWarning(type=NOT_EXISTING_MSKU, supplierId=11, shopSku=sku1, errorInfo=%s)",
            Transliterator.translate(errorInfo.render())));
    }

    @Test
    public void testTranslite() {
        var errorInfo = MbocErrors.get().offerDelistedHasStocks();
        SskuMskuStatusResult result = SskuMskuStatusResult.ok(List.of(), List.of(), List.of(
            new MskuStatusWarning(Type.VALIDATION, 1L, errorInfo)
        ));

        var monitoringItem = new LifecycleStatusesInYtService.MonitoringItem();
        monitoringItem.addResult(result);

        service.processMonitoring(monitoringItem);

        var unit = monitoring.getOrCreateUnit(DeepmindConstants.LIFECYCLE_STATUSES_NOT_SAVED);
        Assertions.assertThat(unit.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(unit.getMessage()).isEqualTo(
            "(1) -> Nelzia postavit status DELISTED, tak kak tovar est na stoke."
        );
    }
}
