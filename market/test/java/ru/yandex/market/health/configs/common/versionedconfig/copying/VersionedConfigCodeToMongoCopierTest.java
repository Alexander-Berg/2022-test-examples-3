package ru.yandex.market.health.configs.common.versionedconfig.copying;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigDaoBaseTest;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigSource;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.monitoring.MonitoringStatus;
import ru.yandex.market.monitoring.MonitoringUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;

public class VersionedConfigCodeToMongoCopierTest extends VersionedConfigDaoBaseTest {
    private final ComplicatedMonitoring complicatedMonitoring = new ComplicatedMonitoring();
    private List<TestConfigVersionEntity> configsInCode;
    private VersionedConfigCodeToMongoCopier<TestConfigEntity, TestConfigVersionEntity> copier;

    private static Tuple activeConfig(String id, int data) {
        return tuple(id, data);
    }

    private static Tuple disabledConfig(String id) {
        return tuple(id, null);
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        copier = new VersionedConfigCodeToMongoCopier<>(
            () -> configsInCode,
            dao,
            complicatedMonitoring,
            configId -> new TestConfigEntity(configId, null, null, null),
            MonitoringUnit::warning
        );
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldDoNothing_whenThereAreNoConfigs() {
        givenNoConfigsInCode();
        givenNoConfigsInMongo();

        copier.copyConfigsFromCodeToMongo();

        assertThat(dao.getAllConfigs()).isEmpty();
        assertThatMonitoringStatusIsOk();
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldCopy_whenThereAreTwoConfigsInCode() {
        givenConfigsInCode(
            version("name1", -1, VersionedConfigSource.CODE, 123),
            version("name2", -1, VersionedConfigSource.CODE, 456)
        );
        givenNoConfigsInMongo();

        copier.copyConfigsFromCodeToMongo();

        assertMongoContains(
            activeConfig("name1", 123),
            activeConfig("name2", 456)
        );
        assertThatMonitoringStatusIsOk();
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldCopy_whenConfigInCodeIsUpdated() {
        givenConfigsInCode(version("name1", -1, VersionedConfigSource.CODE, 456));
        givenActiveConfigsInMongo(version("name1", -1, VersionedConfigSource.CODE, 123));

        copier.copyConfigsFromCodeToMongo();

        assertMongoContains(activeConfig("name1", 456));
        assertThatMonitoringStatusIsOk();
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldDoNothing_whenConfigInCodeIsNotUpdated() {
        givenConfigsInCode(version("name1", -1, VersionedConfigSource.CODE, 123));
        givenActiveConfigsInMongo(version("name1", -1, VersionedConfigSource.CODE, 123));

        copier.copyConfigsFromCodeToMongo();

        assertMongoContains(activeConfig("name1", 123));
        assertThatMonitoringStatusIsOk();
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldDisableConfigInMongo_whenConfigInCodeIsRemoved() {
        givenNoConfigsInCode();
        givenActiveConfigsInMongo(version("name1", -1, VersionedConfigSource.CODE, 123));

        copier.copyConfigsFromCodeToMongo();

        assertMongoContains(disabledConfig("name1"));
        assertThatMonitoringStatusIsOk();
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldNotEnableConfigInMongo_whenConfigInCodeIsRestored() {
        givenConfigsInCode(version("name1", -1, VersionedConfigSource.CODE, 123));
        givenActiveConfigsInMongo(version("name1", -1, VersionedConfigSource.CODE, 123));
        dao.deactivate("name1");

        copier.copyConfigsFromCodeToMongo();

        assertMongoContains(disabledConfig("name1"));
        assertThatMonitoringStatusIsOk();
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldOk_whenConfigInMongoWasEditedInUi() {
        givenConfigsInCode(version("name1", -1, VersionedConfigSource.CODE, 123));
        givenActiveConfigsInMongo(version("name1", -1, VersionedConfigSource.UI, 123));

        copier.copyConfigsFromCodeToMongo();

        assertMongoContains(activeConfig("name1", 123));
        assertThatMonitoringStatusIsOk();
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldDoNothing_whenConfigInCodeWasRemoved_andConfigInMongoWasEditedInUi() {
        givenNoConfigsInCode();
        givenActiveConfigsInMongo(version("name1", -1, VersionedConfigSource.UI, 123));

        copier.copyConfigsFromCodeToMongo();

        assertMongoContains(activeConfig("name1", 123));
        assertThatMonitoringStatusIsOk();
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldWarn_whenAnythingFails() {
        givenConfigsInCode(version("name1", -1, VersionedConfigSource.CODE, 123));
        givenNoConfigsInMongo();

        doThrow(new RuntimeException())
            .when(mongoTemplate)
            .insert(any(TestConfigVersionEntity.class));

        copier.copyConfigsFromCodeToMongo();

        assertThatMonitoringStatusIsWarn();
    }

    /**
     * На всякий случай ещё отдельно тестируем самую распространённую последовательность действий чтобы убедиться что
     * все запуски {@link VersionedConfigCodeToMongoCopier#copyConfigsFromCodeToMongo} смогут прочитать и понять что
     * сделали предыдущие запуски.
     */
    @Test
    public void fullLifecycle() {
        // Изначально есть один конфиг в коде, ожидаем что он скопируется
        givenConfigsInCode(version("name1", -1, VersionedConfigSource.CODE, 123));
        copier.copyConfigsFromCodeToMongo();
        assertMongoContains(activeConfig("name1", 123));
        assertThatMonitoringStatusIsOk();

        // Повторное копирование ничего не меняет
        copier.copyConfigsFromCodeToMongo();
        assertMongoContains(activeConfig("name1", 123));
        assertThatMonitoringStatusIsOk();

        // Конфиг в коде поменялся
        givenConfigsInCode(version("name1", -1, VersionedConfigSource.CODE, 456));
        copier.copyConfigsFromCodeToMongo();
        assertMongoContains(activeConfig("name1", 456));
        assertThatMonitoringStatusIsOk();

        // Конфиг в Монге поредактировали через UI
        dao.activateVersion(dao.createValidVersion(version("name1", -1, VersionedConfigSource.UI, 789), null), null);
        copier.copyConfigsFromCodeToMongo();
        assertMongoContains(activeConfig("name1", 789));
        assertThatMonitoringStatusIsOk();

        // Конфиг удалили из кода
        givenNoConfigsInCode();
        copier.copyConfigsFromCodeToMongo();
        assertMongoContains(activeConfig("name1", 789));
        assertThatMonitoringStatusIsOk();
    }

    private void givenConfigsInCode(TestConfigVersionEntity... configsInCode) {
        this.configsInCode = Arrays.asList(configsInCode);
    }

    private void givenNoConfigsInCode() {
        givenConfigsInCode();
    }

    private void givenActiveConfigsInMongo(TestConfigVersionEntity... configsInCode) {
        for (TestConfigVersionEntity configVersion : configsInCode) {
            dao.createConfig(new TestConfigEntity(
                configVersion.getId().getConfigId(),
                null,
                null,
                null
            ));
            dao.activateVersion(dao.createValidVersion(configVersion, null), null);
        }
    }

    private void givenNoConfigsInMongo() {
        givenActiveConfigsInMongo();
    }

    private void assertThatMonitoringStatusIsOk() {
        assertThat(complicatedMonitoring.getResult().getStatus())
            .isEqualTo(MonitoringStatus.OK);
    }

    private void assertThatMonitoringStatusIsWarn(String... configIds) {
        assertThat(complicatedMonitoring.getResult().getStatus())
            .isEqualTo(MonitoringStatus.WARNING);
        if (configIds.length > 0) {
            assertThat(complicatedMonitoring.getResult().getMessage())
                .contains(configIds);
        }
    }

    private void assertMongoContains(Tuple... configs) {
        assertThat(dao.getAllConfigs())
            .extracting(
                VersionedConfigEntity::getId,
                config -> config.getCurrentVersion() == null ? null : config.getCurrentVersion().getData()
            )
            .containsExactlyInAnyOrder(configs);
    }
}
