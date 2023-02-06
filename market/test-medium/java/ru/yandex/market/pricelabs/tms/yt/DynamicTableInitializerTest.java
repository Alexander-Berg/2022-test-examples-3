package ru.yandex.market.pricelabs.tms.yt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersProcessor;
import ru.yandex.market.yt.binding.BindingTable;
import ru.yandex.market.yt.binding.ProcessorCfg;
import ru.yandex.market.yt.binding.YTAttributes;
import ru.yandex.market.yt.binding.YTAttributes.AttributesBuilder;
import ru.yandex.market.yt.binding.YTAttributes.TrackingAttributes;
import ru.yandex.market.yt.binding.YTBinder;
import ru.yandex.market.yt.client.YtClientProxy;
import ru.yandex.market.yt.client.YtClientProxySource;
import ru.yandex.market.yt.client.YtClientReplicas;
import ru.yandex.market.yt.exception.ExecuteOperationException;
import ru.yandex.market.yt.migrations.DynamicTableInitializer;
import ru.yandex.market.yt.migrations.MigrationOptions;
import ru.yandex.market.yt.migrations.TableVersionService;
import ru.yandex.misc.thread.ThreadUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.yt.binding.YTAttributes.TrackingAttributes.CHECKING_ATTRIBUTES_DYNAMIC_TABLES;

@Slf4j
@Timeout(60)
class DynamicTableInitializerTest extends AbstractTmsSpringConfiguration {

    private static final int MAX_TRIES = 20;
    private static final AtomicInteger ID = new AtomicInteger();

    private static final YTBinder<TestV1> BINDERV1 = YTBinder.getBinder(TestV1.class);
    private static final YTBinder<TestV1a> BINDERV1A = YTBinder.getBinder(TestV1a.class);
    private static final YTBinder<TestV1n> BINDERV1N = YTBinder.getBinder(TestV1n.class);
    private static final YTBinder<TestV2> BINDERV2 = YTBinder.getBinder(TestV2.class);
    private static final YTBinder<TestV2a> BINDERV2A = YTBinder.getBinder(TestV2a.class);
    private static final YTBinder<TestVD> BINDERVD = YTBinder.getBinder(TestVD.class);

    @Autowired
    private OffersProcessor offersProcessor;

    @Autowired
    private DynamicTableInitializer initializer;

    @Autowired
    @Qualifier("internalTableVersionService")
    private TableVersionService tableVersionService;

    @Value("${pricelabs.target.yt.common_path}")
    private String schemaPrefix;

    private String testSchema;
    private String testSchemaReplicated;

    private String currentTestSchema;

    private ProcessorCfg<?> testCfg;
    private YtScenarioExecutor<TestV1> executorV1;
    private YtScenarioExecutor<TestV1a> executorV1a;
    private YtScenarioExecutor<TestV1n> executorV1n;
    private YtScenarioExecutor<TestV2> executorV2;
    private YtScenarioExecutor<TestV2a> executorV2a;
    private YtScenarioExecutor<TestVD> executorVD;

    @BeforeEach
    void init() {
        var replicationDir = schemaPrefix + "/replication";

        testSchema = replicationDir + "/test_schema_" + ID.incrementAndGet();
        testSchemaReplicated = testSchema + "_replicated";
        currentTestSchema = testSchema;

        testCfg = new ProcessorCfg<>(offersProcessor.getCfg().getClientSource(), testSchema, BINDERV1, 1);
        this.initExecutors();

        testControls.cleanupTableRevisions();

        try {
            testCfg.getClient().unsafe().deletePath(replicationDir); // Очистим все что могло остаться лежать
        } catch (Exception e) {
            log.info("Cannot delete {}: {}", replicationDir, e.getMessage());
        }
    }


    @Test
    void testMultipleInitialization() {
        initializer.configureDynamicTables();
        initializer.configureDynamicTables();
    }

    @Test
    void alterSchemaCompatible() {

        var attrs = new AttributesBuilder().limitDataTtl(48).build();

        var v1 = new VersionChain<>(executorV1,
                List.of(new TestV1(1, "1"), new TestV1(2, "2")),
                attrs, Map.of());

        var v2 = new VersionChain<>(executorV2,
                List.of(new TestV2(1, "1", "", 0), new TestV2(2, "2", "", 0)),
                attrs, Map.of());

        alterSchemaImpl(v1, v2);
    }

    @Test
    void alterSchemaIncompatibleRemoveField() {

        var attrs = new AttributesBuilder().medium(null).build(); // не будет применен

        var v1 = new VersionChain<>(executorV1,
                List.of(new TestV1(1, "1"), new TestV1(2, "2")),
                attrs, Map.of());

        var v2 = new VersionChain<>(executorV2,
                List.of(new TestV2(1, "1", "", 0), new TestV2(2, "2", "", 0)),
                attrs, Map.of());

        assertThrows(ExecuteOperationException.class, () -> alterSchemaImpl(v1, v2, v1));
    }

    @Test
    void alterSchemaIncompatiblePrimaryKey() {

        var attrs = new AttributesBuilder().desiredTabletCount(20).build();

        var v1 = new VersionChain<>(executorV1,
                List.of(new TestV1(1, "1"), new TestV1(2, "2")),
                attrs, Map.of());

        var v1a = new VersionChain<>(executorV1a,
                List.of(new TestV1a(1, "1"), new TestV1a(2, "2")),
                attrs, Map.of());

        alterSchemaImpl(v1, v1a);
    }

    @Test
    void alterSchemaIncompatibleColumn() {

        var v1 = new VersionChain<>(executorV1,
                List.of(new TestV1(1, "1"), new TestV1(2, "2")),
                Map.of(), Map.of());

        var v2 = new VersionChain<>(executorV2,
                List.of(new TestV2(1, "1", "", 0), new TestV2(2, "2", "", 0)),
                Map.of(), Map.of());

        var v2a = new VersionChain<>(executorV2a,
                List.of(new TestV2a(1, "1", "", 0), new TestV2a(2, "2", "", 0)),
                Map.of(), Map.of());

        alterSchemaImpl(v1, v2, v2a);
    }

    @Test
    void alterSchemaIncompatibleChangeKeys() {

        var attrs = new AttributesBuilder().medium(null).build(); // не будет применен

        var v1 = new VersionChain<>(executorV1,
                List.of(new TestV1(1, "1"), new TestV1(2, "2")),
                attrs, Map.of());

        var v1n = new VersionChain<>(executorV1n,
                List.of(new TestV1n(1, 1, "1"), new TestV1n(2, 2, "2")),
                attrs, Map.of());

        assertThrows(ExecuteOperationException.class, () -> alterSchemaImpl(v1, v1n));
    }


    @Test
    void alterSchemaWithReplicatedTableCompatible() {
        var attrs = new AttributesBuilder().desiredTabletCount(20).limitDataTtl(48).build();
        var replicatedAttrs = new AttributesBuilder().pivotKeysWithFarmHashSplit(10, TestV1.class).build();

        currentTestSchema = testSchemaReplicated;

        var nonReplicatedExecutor = executorV2; // не реплицированный

        // Реплицированная таблица, но в одном кластере
        var tempProxy = YtClientProxy.recombine(testCfg.getClient(), new YtClientReplicas(List.of(getReplica())));

        testCfg = new ProcessorCfg<>(YtClientProxySource.singleSource(tempProxy), testSchema, BINDERV1, 1);
        this.initExecutors();

        var v1 = new VersionChain<>(executorV1,
                List.of(new TestV1(1, "1"), new TestV1(2, "2")),
                attrs, replicatedAttrs);

        var v1same = new VersionChain<>(executorV1,
                List.of(new TestV1(1, "1"), new TestV1(2, "2")),
                attrs, replicatedAttrs);

        var v2 = new VersionChain<>(executorV2,
                List.of(new TestV2(1, "1", "", 0), new TestV2(2, "2", "", 0)),
                attrs, replicatedAttrs);

        alterSchemaImpl(v1, v1same, v2);

        // В реплике эти же данные
        nonReplicatedExecutor.verify(List.of(new TestV2(1, "1", "", 0), new TestV2(2, "2", "", 0)));
    }

    @Test
    void alterSchemaReplicatedDifferentPivotKeys() {

        currentTestSchema = testSchemaReplicated;

        var tempProxy = YtClientProxy.recombine(testCfg.getClient(), new YtClientReplicas(List.of(getReplica())));

        testCfg = new ProcessorCfg<>(YtClientProxySource.singleSource(tempProxy), testSchema, BINDERVD, 1);
        this.initExecutors();

        var v1 = new VersionChain<>(executorVD,
                List.of(new TestVD("2020-02-01", "1"), new TestVD("2020-02-07", "2")),
                Map.of(), Map.of());

        var v1pivot = new VersionChain<>(executorVD,
                List.of(new TestVD("2020-02-01", "1"), new TestVD("2020-02-07", "2")),
                Map.of(), new AttributesBuilder().pivotKeysByWeeks(6).build());

        alterSchemaImpl(v1, v1pivot, v1);
    }

    @Test
    void alterSchemaDifferentTtlSettings() {
        var v1 = new VersionChain<>(executorVD,
                List.of(new TestVD("2020-02-01", "1"), new TestVD("2020-02-07", "2")),
                Map.of(), Map.of());

        var v1Ttl = new VersionChain<>(executorVD,
                List.of(new TestVD("2020-02-01", "1"), new TestVD("2020-02-07", "2")),
                new AttributesBuilder().limitDataTtl(4).build(), Map.of());

        alterSchemaImpl(v1, v1Ttl, v1);
    }

    @Test
    void alterSchemaDifferentTtlSettingsWithPivot() {
        var v1 = new VersionChain<>(executorVD,
                List.of(new TestVD("2020-02-01", "1"), new TestVD("2020-02-07", "2")),
                new AttributesBuilder()
                        .pivotKeys(List.of("2020-06-01", "2020-07-01"))
                        .build(), Map.of());

        var v1Ttl = new VersionChain<>(executorVD,
                List.of(new TestVD("2020-02-01", "1"), new TestVD("2020-02-07", "2")),
                new AttributesBuilder()
                        .pivotKeys(List.of("2020-06-01", "2020-07-01"))
                        .limitDataTtl(4).build(), Map.of());

        alterSchemaImpl(v1, v1Ttl, v1);
    }

    @Test
    void alterSchemaDifferentInMemoryMode() {
        var v1 = new VersionChain<>(executorVD,
                List.of(new TestVD("2020-02-01", "1"), new TestVD("2020-02-07", "2")),
                Map.of(), Map.of());

        var v11 = new VersionChain<>(executorVD,
                List.of(new TestVD("2020-02-01", "1"), new TestVD("2020-02-07", "2")),
                new AttributesBuilder().inMemoryMode(YTAttributes.InMemoryMode.compressed).build(), Map.of());

        var v12 = new VersionChain<>(executorVD,
                List.of(new TestVD("2020-02-01", "1"), new TestVD("2020-02-07", "2")),
                new AttributesBuilder().inMemoryMode(YTAttributes.InMemoryMode.uncompressed).build(), Map.of());

        var v13 = new VersionChain<>(executorVD,
                List.of(new TestVD("2020-02-01", "1"), new TestVD("2020-02-07", "2")),
                new AttributesBuilder().inMemoryMode(YTAttributes.InMemoryMode.none).build(), Map.of());

        alterSchemaImpl(v1, v11, v12, v13, v1);
    }

    @Test
    void alterSchemaDifferentEnableDynamicStoreReader() {
        var v1 = new VersionChain<>(executorVD,
                List.of(new TestVD("2020-02-01", "1"), new TestVD("2020-02-07", "2")),
                Map.of(), Map.of());

        var v11 = new VersionChain<>(executorVD,
                List.of(new TestVD("2020-02-01", "1"), new TestVD("2020-02-07", "2")),
                new AttributesBuilder().enableDynamicStoreRead(true).build(), Map.of());

        var v12 = new VersionChain<>(executorVD,
                List.of(new TestVD("2020-02-01", "1"), new TestVD("2020-02-07", "2")),
                new AttributesBuilder().enableDynamicStoreRead(false).build(), Map.of());

        alterSchemaImpl(v1, v11, v12, v1);
    }


    void alterSchemaImpl(VersionChain<?>... chains) {
        assertTrue(chains.length > 1);

        var first = chains[0];
        first.executor.removeTargetTable();

        first.configure();

        var schemaV1 = getSchema(getAttributes(), first.binder);
        log.info("Schema v0: {}", schemaV1);

        first.insertImpl();
        first.verify();

// В настоящий момент изменение схемы может занять значительное время - до 30 секунд, причем это никак нельзя проверить

        for (int i = 1; i < chains.length; i++) {
            var chain = chains[i];
            var attrs = currentTestSchema.equals(testSchemaReplicated) ?
                    chain.replicatedAttributes : chain.attributes;
            var expectAttributes = testCfg.getClient().mergeAttributes(chain.binder, attrs);
            var expectSchema = getSchema(expectAttributes, chain.binder);

            log.info("Schema v{}: {}", i, expectSchema);
            resetTableRevisions();

            chain.configure();

            int attemptNumber = 0;
            while (true) {
                var currentAttributes = getAttributes();
                var currentSchema = getSchema(currentAttributes, chain.binder);
                log.info("Checking schema {} for {}", chain.binder.getBindingClass(), currentTestSchema);

                if (!DynamicTableInitializer.isAlterRequired(chain.binder, CHECKING_ATTRIBUTES_DYNAMIC_TABLES,
                        currentAttributes, expectAttributes)) {
                    break; // Никаких изменений не требуется
                }
                if (++attemptNumber >= MAX_TRIES) {
                    fail(String.format("Attributes are still different for %s after %d tries",
                            currentTestSchema,
                            attemptNumber)
                    );
                }
                ThreadUtils.sleep(1, TimeUnit.SECONDS); // Все еще старое закэшированное значение в прокси
            }

// Пока не тратим время на эту операцию - уж очень долго оно выполняется
//        while (true) {
//            try {
//                target.insertRows(currentTestSchema, BINDER2, List.of(new TestV2(3, "3", "33")));
//            } catch (Exception e) {
//                // Все еще старое закэшированное значение в прокси
//                ThreadUtils.sleep(1, TimeUnit.SECONDS);
//                continue; // ---
//            }
//            break;
//        }

            chain.verify();
        }
    }


    private void resetTableRevisions() {
        // Сбросим записи в БД
        // -1 - это "нормальная версия", которая проставляется в артефакт, собранный из локального arc-а
        List<YtClientProxy> ytClients = new ArrayList<>();
        ytClients.add(testCfg.getClient());
        ytClients.addAll(testCfg.getClient().getReplicas());

        for (YtClientProxy ytClient : ytClients) {
            for (String table : List.of(testSchema, testSchemaReplicated)) {
                tableVersionService.saveTableRevision(ytClient.getClusterName(), table, "", -2);
            }
        }
    }

    private YtClientProxy getReplica() {
        var clusters = testCfg.getClient().get("//sys/clusters", "").asMap();
        if (clusters.isEmpty()) {
            throw new RuntimeException("No cluster configuration found in //sys/clusters");
        } else if (clusters.size() == 1) { // Локальный запуск
            String clusterName = clusters.keySet().iterator().next();
            log.info("Using temporary cluster name: {}", clusterName);
            return testCfg.getClient().withClusterName(clusterName);
        } else {
            log.info("Using default cluster because of complete configuration: {}", clusters);
            return testCfg.getClient();
        }
    }

    private Map<String, YTreeNode> getAttributes() {
        return testCfg.getClient().get(currentTestSchema, TrackingAttributes.ALL_ATTRIBUTES).asMap();
    }

    private <T> YTreeNode getSchema(Map<String, YTreeNode> attributes, YTBinder<T> binder) {
        var schema = Objects.requireNonNull(attributes.get(YTAttributes.SCHEMA), "Schema is required");
        return binder.cleanCypressSchema(schema);
    }

    private <T> DynamicTableInitializer initializer(VersionChain<T> chain) {
        return new DynamicTableInitializer(
                List.of(wrap(chain)),
                tableVersionService,
                YtClientReplicas.EMPTY,
                chain.executor.getCfg().getClientSource(),
                testControls.getExecutor(),
                MigrationOptions.newBuilder()
                        .setReplicationLagWaitSec(15)
                        .setSleepBetweenChecksSec(5)
                        .build()
        ) {
            @Override
            protected String toReplicatedTableName(String targetPath) {
                assertEquals(testSchema, targetPath);
                return testSchemaReplicated;
            }
        };
    }

    private <T> BindingTable<T> wrap(VersionChain<T> chain) {
        var cfg = chain.executor.getCfg();
        return new BindingTable<>(cfg.getTable(), cfg.getBinder().getBindingClass(), chain.attributes,
                chain.replicatedAttributes);
    }

    private void initExecutors() {
        executorV1 = executorV1(testCfg);
        executorV1a = executorV1a(testCfg);
        executorV1n = executorV1n(testCfg);
        executorV2 = executorV2(testCfg);
        executorV2a = executorV2a(testCfg);
        executorVD = executorVD(testCfg);
    }

    YtScenarioExecutor<TestV1> executorV1(ProcessorCfg<?> processorCfg) {
        return YtScenarioExecutor.from(rebindUnchecked(processorCfg, BINDERV1));
    }

    YtScenarioExecutor<TestV1a> executorV1a(ProcessorCfg<?> processorCfg) {
        return YtScenarioExecutor.from(rebindUnchecked(processorCfg, BINDERV1A));
    }

    YtScenarioExecutor<TestV1n> executorV1n(ProcessorCfg<?> processorCfg) {
        return YtScenarioExecutor.from(rebindUnchecked(processorCfg, BINDERV1N));
    }

    YtScenarioExecutor<TestV2> executorV2(ProcessorCfg<?> processorCfg) {
        return YtScenarioExecutor.from(rebindUnchecked(processorCfg, BINDERV2));
    }

    YtScenarioExecutor<TestV2a> executorV2a(ProcessorCfg<?> processorCfg) {
        return YtScenarioExecutor.from(rebindUnchecked(processorCfg, BINDERV2A));
    }

    YtScenarioExecutor<TestVD> executorVD(ProcessorCfg<?> processorCfg) {
        return YtScenarioExecutor.from(rebindUnchecked(processorCfg, BINDERVD));
    }

    private static <T, R> ProcessorCfg<R> rebindUnchecked(ProcessorCfg<T> cfg, YTBinder<R> binder) {
        return new ProcessorCfg<>(cfg.getClientSource(), cfg.getTable(), binder, cfg.getDefaultBatchSize());
    }

    private class VersionChain<T> {
        private final YtScenarioExecutor<T> executor;
        private final YTBinder<T> binder;
        private final List<T> values;
        private final Map<String, YTreeNode> attributes;
        private final Map<String, YTreeNode> replicatedAttributes;

        VersionChain(YtScenarioExecutor<T> executor, List<T> values, Map<String, YTreeNode> attributes,
                     Map<String, YTreeNode> replicatedAttributes) {
            this.executor = executor;
            this.binder = executor.getBinder();
            this.values = values;
            this.attributes = attributes;
            this.replicatedAttributes = replicatedAttributes;
        }

        void configure() {
            log.info("Applying schema: {}", binder.getBindingClass());
            initializer(this).configureDynamicTables();
        }

        void insertImpl() {
            testCfg.getClient().withRetryCount(10).insertRows(currentTestSchema, binder, values);
        }

        void verify() {
            int attemptNumber = 0;
            while (!Thread.interrupted()) {
                try {
                    executor.verify(values);
                    return; // ---
                } catch (AssertionError e) {
                    if (++attemptNumber >= MAX_TRIES) {
                        fail(String.format("Can't verify chain after %d tries", attemptNumber));
                    }
                    ThreadUtils.sleep(1, TimeUnit.SECONDS); // Все еще старое закэшированное значение в прокси
                }
            }
        }
    }


}
