package ru.yandex.market.checkout.common.tasks;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.common.tasks.config.ZooTaskConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZooTaskRuntimeConfigTest extends AbstractZooTaskTest {

    private final ZooTaskConfig bootConfig = ZooTaskConfig.builder()
            .withRepeatPeriod(10L)
            .withPostponePeriod(20L)
            .withPeriodUnit(TimeUnit.MINUTES)
            .withCrucial(false)
            .withFailPeriodFactor(10f)
            .build();

    private final ZooTaskConfig someRuntimeConfig = ZooTaskConfig.builder()
            .withRepeatPeriod(5L)
            .withCrucial(true)
            .withFailPeriodFactor(3f)
            .build();

    @Autowired
    private ZooTaskRegistry taskRegistry;

    @BeforeEach
    public void setup() {

        // Создаем boot-конфигурацию
        zooTask.setRepeatPeriod(10);
        zooTask.setPostponePeriod(20);
        zooTask.setPeriodUnit(TimeUnit.MINUTES);
        zooTask.setCrucial(false);
        zooTask.setFailPeriodFactor(10);

        taskRegistry.addTask(zooTask);
    }

    @AfterEach
    public void tearDown() {
        zooTaskConfigService.deleteAllOverridingConfigs();
        waitZk();
    }

    @Test
    public void zooTaskConfigEntityTest() {

        // Проверяем билдинг
        ZooTaskConfig someConfig = ZooTaskConfig
                .builder(bootConfig)
                .build();
        assertEquals(bootConfig, someConfig);

        // Проверяем консистенстность
        assertEquals((long) someConfig.getPostponePeriodMs(),
                someConfig.getPeriodUnit().toMillis(someConfig.getPostponePeriod()));
        assertEquals((long) someConfig.getRepeatPeriodMs(),
                someConfig.getPeriodUnit().toMillis(someConfig.getRepeatPeriod()));

        // Проверяем, что правильно мержатся
        ZooTaskConfig checkConfig = ZooTaskConfig.merge(bootConfig, someRuntimeConfig);
        assertEquals(5, (long) checkConfig.getRepeatPeriod());
        assertEquals(20, (long) checkConfig.getPostponePeriod());
        assertEquals(TimeUnit.MINUTES, checkConfig.getPeriodUnit());
        assertTrue(checkConfig.isCrucial());
        assertEquals(3, checkConfig.getFailPeriodFactor(), 0);
    }

    @Test
    public void testWithoutRuntimeConfig() throws InterruptedException {

        // Проверяем, когда runtime-конфигурация не настроена
        assertNull(zooTaskConfigService.getOverridingConfig(ZOO_TASK_NAME));
        assertEquals(bootConfig, zooTask.getStartupConfig());
        assertEquals(ZooTaskConfig.EMPTY, zooTask.getOverridingConfig());
        // Ожидаем, что actualConfig = bootConfig
        assertEquals(bootConfig, zooTask.getActualConfig());

        // То же самое должно быть, если насроена пустая runtime-конфигурация
        zooTaskConfigService.setOverridingConfig(ZOO_TASK_NAME, ZooTaskConfig.EMPTY);
        assertEquals(ZooTaskConfig.EMPTY, zooTaskConfigService.getOverridingConfig(ZOO_TASK_NAME));
        assertEquals(bootConfig, zooTask.getStartupConfig());
        assertEquals(ZooTaskConfig.EMPTY, zooTask.getOverridingConfig());
        // Ожидаем, что actualConfig = bootConfig
        assertEquals(bootConfig, zooTask.getActualConfig());
    }

    @Test
    public void testSomeRuntimeConfig() throws InterruptedException {

        // Устанавливам runtime-конфиги для таски
        ZooTaskConfig runtimeConfig = ZooTaskConfig.builder()
                .withRepeatPeriod(5L)
                .withCrucial(true)
                .withFailPeriodFactor(3f)
                .build();
        zooTaskConfigService.setOverridingConfig(ZOO_TASK_NAME, runtimeConfig);

        // Ждем пока конфиги проедут через zk
        waitZk();

        // Проверяем, что сервис возвращает правильные runtime-конфиги
        assertEquals(1, zooTaskConfigService.getAllOverridingConfigs().size());
        Arrays.asList(
                zooTaskConfigService.getOverridingConfig(ZOO_TASK_NAME),
                zooTaskConfigService.getAllOverridingConfigs().get(ZOO_TASK_NAME)
        ).forEach(checkConfig -> {
            assertEquals(runtimeConfig, checkConfig);
        });

        // Проверяем, что ZooTask правильно вычисляет свои конфиги
        assertEquals(TimeUnit.MINUTES.toMillis(5), zooTask.getRepeatPeriodMsActual());
        assertEquals(TimeUnit.MINUTES.toMillis(20), zooTask.getPostponePeriodMsActual());
        assertEquals(TimeUnit.MINUTES, zooTask.getPeriodUnitActual());
        assertTrue(zooTask.isCrucialActually());
        assertEquals(3, zooTask.getFailPeriodFactorActual(), 0);

        // Удаляем конфиги для таски. Ожидаем, что действующая конфигурация вернется к boot-конфигурации
        zooTaskConfigService.deleteOverridingConfig(ZOO_TASK_NAME);

        assertEquals(0, zooTaskConfigService.getAllOverridingConfigs().size());
        assertNull(zooTaskConfigService.getOverridingConfig(ZOO_TASK_NAME));

        // Проверяем, что ZooTask правильно вычисляет свои конфиги
        assertEquals(TimeUnit.MINUTES.toMillis(10), zooTask.getRepeatPeriodMsActual());
        assertEquals(TimeUnit.MINUTES.toMillis(20), zooTask.getPostponePeriodMsActual());
        assertEquals(TimeUnit.MINUTES, zooTask.getPeriodUnitActual());
        assertFalse(zooTask.isCrucialActually());
        assertEquals(10, zooTask.getFailPeriodFactorActual(), 0);
    }

    private void waitZk() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
