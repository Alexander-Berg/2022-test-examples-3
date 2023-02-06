package ru.yandex.market.pricelabs.tms.yt;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.model.NewVendorBrandMap;
import ru.yandex.market.pricelabs.model.VendorBrandMap;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;
import ru.yandex.market.pricelabs.tms.yt.YtTablesCollector.TableProcessingState;
import ru.yandex.market.yt.YtClusters;
import ru.yandex.market.yt.client.YtClientProxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YtTablesCollectorTest extends AbstractTmsSpringConfiguration {

    @Autowired
    private YtTablesCollector collector;

    @Autowired
    private YtClusters source;

    private YtClientProxy primary;
    private YtClientProxy current;

    private String day;
    private YtSourceTargetScenarioExecutor<NewVendorBrandMap, VendorBrandMap> vendorBrandMap;

    @BeforeEach
    void init() {
        this.current = source.getPrimary();
        this.primary = current.withClusterName("primary-replica-for-test");

        this.day = "current_vendors";
        this.vendorBrandMap = executors.vendorBrandMap();

        testControls.executeInParallel(
                () -> {
                    vendorBrandMap.removeSourceTables();
                    vendorBrandMap.createSourceTable();
                },
                () -> testControls.cleanupTableRevisions()
        );
    }

    @Test
    void getTableState() {
        var state = collector.getTableState(current, vendorBrandMap.getSourcePrefix(), day);
        assertEquals(current.getClusterName(), state.getCluster());
        assertEquals(vendorBrandMap.getSourcePrefix(), state.getTablePrefix());
        assertEquals(day, state.getTableName());
        assertTrue(state.getRevision() > 0);
        assertEquals(0, state.getRowCount());
    }

    @Test
    void getLastTableName() {
        assertTrue(collector.getLastTableName(current.getClusterName(), vendorBrandMap.getSourcePrefix()).isEmpty());

        collector.confirmTableProcessed(collector.getTableState(current, vendorBrandMap.getSourcePrefix(), day));

        assertEquals(day, collector.getLastTableName(current.getClusterName(),
                vendorBrandMap.getSourcePrefix()).orElseThrow());
        assertTrue(collector.getLastTableName(primary.getClusterName(), vendorBrandMap.getSourcePrefix()).isEmpty());

    }

    @Test
    void collectUnprocessedTablesSame() {
        Supplier<List<String>> tables = () ->
                collector.collectUnprocessedTables(current, current, vendorBrandMap.getSourcePrefix());
        assertEquals(List.of(day), tables.get());

        collector.confirmTableProcessed(collector.getTableState(primary, vendorBrandMap.getSourcePrefix(), day));
        assertEquals(List.of(day), tables.get());

        collector.confirmTableProcessed(collector.getTableState(current, vendorBrandMap.getSourcePrefix(), day));
        assertEquals(List.of(), tables.get());
    }

    @Test
    void collectUnprocessedTablesOnlyPrimary() {
        Supplier<List<String>> tables = () ->
                collector.collectUnprocessedTables(primary, current, vendorBrandMap.getSourcePrefix());
        assertEquals(List.of(day), tables.get());

        collector.confirmTableProcessed(collector.getTableState(primary, vendorBrandMap.getSourcePrefix(), day));
        assertEquals(List.of(), tables.get());
    }

    @Test
    void collectUnprocessedTablesOnlyCurrent() {
        Supplier<List<String>> tables = () ->
                collector.collectUnprocessedTables(primary, current, vendorBrandMap.getSourcePrefix());
        assertEquals(List.of(day), tables.get());

        collector.confirmTableProcessed(collector.getTableState(current, vendorBrandMap.getSourcePrefix(), day));
        assertEquals(List.of(), tables.get());
    }

    @Test
    void getLastTableProcessingState() {
        Function<YtClientProxy, TableProcessingState> state = client ->
                collector.getLastTableProcessingState(client, vendorBrandMap.getSourcePrefix(), table -> true, null);

        assertTrue(Objects.requireNonNull(state.apply(current)).hasChange());
        assertTrue(Objects.requireNonNull(state.apply(primary)).hasChange());

        collector.confirmTableProcessed(collector.getTableState(current, vendorBrandMap.getSourcePrefix(), day));
        assertFalse(Objects.requireNonNull(state.apply(current)).hasChange());
        assertTrue(Objects.requireNonNull(state.apply(primary)).hasChange());
    }

    @Test
    void getTableProcessingState() {
        var procState = collector.getTableProcessingState(current, vendorBrandMap.getSourcePrefix(), day);
        assertTrue(procState.hasChange());
        assertTrue(procState.hasCreate());
        assertFalse(procState.hasUpdate());

        var state = procState.getState();
        assertEquals(current.getClusterName(), state.getCluster());
        assertEquals(vendorBrandMap.getSourcePrefix(), state.getTablePrefix());
        assertEquals(day, state.getTableName());
        assertTrue(state.getRevision() > 0);
        assertEquals(0, state.getRowCount());

        collector.confirmTableProcessed(state);

        vendorBrandMap.removeSourceTables();
        vendorBrandMap.createSourceTable();

        var procState2 = collector.getTableProcessingState(current, vendorBrandMap.getSourcePrefix(), day);
        assertTrue(procState2.hasChange());
        assertFalse(procState2.hasCreate());
        assertTrue(procState2.hasUpdate());

        var state2 = procState2.getState();
        assertEquals(current.getClusterName(), state2.getCluster());
        assertEquals(vendorBrandMap.getSourcePrefix(), state2.getTablePrefix());
        assertEquals(day, state2.getTableName());
        assertTrue(state2.getRevision() > 0);
        assertNotEquals(state2.getRevision(), state.getRevision());
        assertEquals(0, state2.getRowCount());
    }

}
