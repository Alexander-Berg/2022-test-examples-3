package ru.yandex.market.checkout.checkouter.controller;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.common.tasks.ZooTaskConfigServiceDependsOnZooMigratorSpringBFPP;
import ru.yandex.market.checkout.common.tasks.ZooTaskRegistry;
import ru.yandex.market.checkout.common.tasks.config.ZooTaskConfig;
import ru.yandex.market.checkout.common.tasks.config.ZooTaskConfigService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextHierarchy(
        @ContextConfiguration(name = "task", classes = {ZooTaskConfigControllerTest.TestConfiguration.class})
)
public class ZooTaskConfigControllerTest extends AbstractWebTestBase {

    private static final String TASK_NAME = "TASK_NAME";
    private static final long REPEAT_PERIOD = 10;
    private static final long POSTPONE_PERIOD = 2;
    private static final TimeUnit PERIOD_UNIT = TimeUnit.MINUTES;
    private static final boolean IS_CRUCIAL = false;
    private static final float FAIL_PERIOD_FACTOR = 2;
    private static final int BATCH_SIZE = 100;
    private static final String ALLOWED_HOSTS = "[\"host1\", \"host2\"]";
    private static final String[] ALLOWED_HOSTS_VALUE = new String[]{"host1", "host2"};

    private ZooTaskConfig bootConfig = ZooTaskConfig.builder()
            .withRepeatPeriod(REPEAT_PERIOD)
            .withPostponePeriod(POSTPONE_PERIOD)
            .withPeriodUnit(PERIOD_UNIT)
            .withCrucial(IS_CRUCIAL)
            .withFailPeriodFactor(FAIL_PERIOD_FACTOR)
            .withBatchSize(BATCH_SIZE)
            .withAllowedHosts(Set.of(ALLOWED_HOSTS_VALUE))
            .build();

    @Autowired
    private ZooTask configurableTask;
    @Autowired
    private ZooTaskConfigService zooTaskConfigService;
    @Autowired
    private ZooTaskRegistry taskRegistry;

    @BeforeEach
    public void setupTask() {
        taskRegistry.addTask(configurableTask);
    }

    @AfterEach
    public void tearDown() {
        zooTaskConfigService.deleteAllOverridingConfigs();
    }

    @Test
    public void configManagementTest() throws Exception {
        mockMvc.perform(get("/zoo-task-config"))
                .andExpect(status().isOk())
                .andExpect(content().string("{}"));

        getRuntimeConfig(TASK_NAME)
                .andExpect(status().isNoContent());
        checkTask(configurableTask, bootConfig, ZooTaskConfig.EMPTY, bootConfig);
        checkTask(configurableTask, 600_000, 120_000, false, 2);

        // Редактируем crucial
        checkResponse(setRuntimeConfig(TASK_NAME, "crucial", "true"),
                null, null, null, true, null, null, null);
        checkResponse(getRuntimeConfig(TASK_NAME),
                null, null, null, true, null, null, null);
        checkTask(configurableTask, 600_000, 120_000, true, 2);

        // Редактируем repeatPeriod
        checkResponse(setRuntimeConfig(TASK_NAME, "repeatPeriod", "1000"),
                1000, null, null, true, null, null, null);
        checkResponse(getRuntimeConfig(TASK_NAME),
                1000, null, null, true, null, null, null);
        checkTask(configurableTask, 60_000_000, 120_000, true, 2);

        // Редактируем postponePeriod
        checkResponse(setRuntimeConfig(TASK_NAME, "postponePeriod", "200"),
                1000, 200, null, true, null, null, null);
        checkResponse(getRuntimeConfig(TASK_NAME),
                1000, 200, null, true, null, null, null);
        checkTask(configurableTask, 60_000_000, 12_000_000, true, 2);

        // Редактируем failPeriodFactor
        checkResponse(setRuntimeConfig(TASK_NAME, "failPeriodFactor", "3"),
                1000, 200, null, true, 3d, null, null);
        checkResponse(getRuntimeConfig(TASK_NAME),
                1000, 200, null, true, 3d, null, null);
        checkTask(configurableTask, 60_000_000, 12_000_000, true, 3);

        // Устанавливаем TimeUnit.MINUTES
        checkResponse(setRuntimeConfig(TASK_NAME, "convert-time", "MINUTES"),
                1000, 200, "MINUTES", true, 3d, null, null);
        checkResponse(getRuntimeConfig(TASK_NAME),
                1000, 200, "MINUTES", true, 3d, null, null);
        checkTask(configurableTask, 60_000_000, 12_000_000, true, 3);

        // Конвертируем время в секунды (значения периодов не меняются)
        checkResponse(setRuntimeConfig(TASK_NAME, "convert-time", "SECONDS"),
                60_000, 12_000, "SECONDS", true, 3d, null, null);
        checkResponse(getRuntimeConfig(TASK_NAME),
                60_000, 12_000, "SECONDS", true, 3d, null, null);
        checkTask(configurableTask, 60_000_000, 12_000_000, true, 3);

        // Редактируем batchSize
        checkResponse(setRuntimeConfig(TASK_NAME, "batchSize", "100"),
                60_000, 12_000, "SECONDS", true, 3d, 100, null);
        checkResponse(getRuntimeConfig(TASK_NAME),
                60_000, 12_000, "SECONDS", true, 3d, 100, null);
        checkTask(configurableTask, 60_000_000, 12_000_000, true, 3);

        // Редактируем allowedHosts
        checkResponse(setRuntimeConfig(TASK_NAME, "allowedHosts", ALLOWED_HOSTS),
                60_000, 12_000, "SECONDS", true, 3d, 100, ALLOWED_HOSTS_VALUE);
        checkResponse(getRuntimeConfig(TASK_NAME),
                60_000, 12_000, "SECONDS", true, 3d, 100, ALLOWED_HOSTS_VALUE);
        checkTask(configurableTask, 60_000_000, 12_000_000, true, 3);


        // Удаляем runtime-конфиги. Проверяем возвращение таски к bootConfig
        deleteConfig(TASK_NAME);
        getRuntimeConfig(TASK_NAME)
                .andExpect(status().isNoContent());
        checkTask(configurableTask, 600_000, 120_000, false, 2);
    }

    @Test
    public void testAllConfigs() throws Exception {
        checkResponse(getBootConfig(TASK_NAME)
                        .andExpect(status().isOk()),
                10, 2, "MINUTES", false, 2d, 100, ALLOWED_HOSTS_VALUE);
        checkResponse(getActualConfig(TASK_NAME)
                        .andExpect(status().isOk()),
                10, 2, "MINUTES", false, 2d, 100, ALLOWED_HOSTS_VALUE);

        getAllConfigs(TASK_NAME)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.STARTUP.repeatPeriod").value(10))
                .andExpect(jsonPath("$.STARTUP.postponePeriod").value(2))
                .andExpect(jsonPath("$.STARTUP.periodUnit").value("MINUTES"))
                .andExpect(jsonPath("$.STARTUP.crucial").value(false))
                .andExpect(jsonPath("$.STARTUP.failPeriodFactor").value(2))
                .andExpect(jsonPath("$.OVERRIDING.repeatPeriod").doesNotExist())
                .andExpect(jsonPath("$.OVERRIDING.postponePeriod").doesNotExist())
                .andExpect(jsonPath("$.OVERRIDING.periodUnit").doesNotExist())
                .andExpect(jsonPath("$.OVERRIDING.crucial").doesNotExist())
                .andExpect(jsonPath("$.OVERRIDING.failPeriodFactor").doesNotExist())
                .andExpect(jsonPath("$.ACTUAL.repeatPeriod").value(10))
                .andExpect(jsonPath("$.ACTUAL.postponePeriod").value(2))
                .andExpect(jsonPath("$.ACTUAL.periodUnit").value("MINUTES"))
                .andExpect(jsonPath("$.ACTUAL.crucial").value(false))
                .andExpect(jsonPath("$.ACTUAL.failPeriodFactor").value(2));

        // Меняем конфигурацию
        setRuntimeConfig(TASK_NAME, "postponePeriod", "200");
        setRuntimeConfig(TASK_NAME, "convert-time", "SECONDS");
        setRuntimeConfig(TASK_NAME, "failPeriodFactor", "5");

        getAllConfigs(TASK_NAME)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.STARTUP.repeatPeriod").value(10))
                .andExpect(jsonPath("$.STARTUP.postponePeriod").value(2))
                .andExpect(jsonPath("$.STARTUP.periodUnit").value("MINUTES"))
                .andExpect(jsonPath("$.STARTUP.crucial").value(false))
                .andExpect(jsonPath("$.STARTUP.failPeriodFactor").value(2))
                .andExpect(jsonPath("$.OVERRIDING.repeatPeriod").doesNotExist())
                .andExpect(jsonPath("$.OVERRIDING.postponePeriod").value(200))
                .andExpect(jsonPath("$.OVERRIDING.periodUnit").value("SECONDS"))
                .andExpect(jsonPath("$.OVERRIDING.crucial").doesNotExist())
                .andExpect(jsonPath("$.OVERRIDING.failPeriodFactor").value(5))
                .andExpect(jsonPath("$.ACTUAL.repeatPeriod").value(10))
                .andExpect(jsonPath("$.ACTUAL.postponePeriod").value(200))
                .andExpect(jsonPath("$.ACTUAL.periodUnit").value("SECONDS"))
                .andExpect(jsonPath("$.ACTUAL.crucial").value(false))
                .andExpect(jsonPath("$.ACTUAL.failPeriodFactor").value(5));
    }

    @Test
    public void testAllConfigsForNonExistingTask() throws Exception {
        String nonExistingTask = "QWERTY";

        getRuntimeConfig(nonExistingTask)
                .andExpect(status().isNoContent());

        getBootConfig(nonExistingTask)
                .andExpect(status().is4xxClientError());
        getActualConfig(nonExistingTask)
                .andExpect(status().is4xxClientError());
        getAllConfigs(nonExistingTask)
                .andExpect(status().is4xxClientError());

        setRuntimeConfig(nonExistingTask, "repeatPeriod", String.valueOf(REPEAT_PERIOD));
        setRuntimeConfig(nonExistingTask, "postponePeriod", String.valueOf(POSTPONE_PERIOD));
        setRuntimeConfig(nonExistingTask, "convert-time", String.valueOf(PERIOD_UNIT));
        setRuntimeConfig(nonExistingTask, "crucial", String.valueOf(IS_CRUCIAL));
        setRuntimeConfig(nonExistingTask, "failPeriodFactor", String.valueOf(FAIL_PERIOD_FACTOR));
        setRuntimeConfig(nonExistingTask, "batchSize", String.valueOf(BATCH_SIZE));
        setRuntimeConfig(nonExistingTask, "allowedHosts", ALLOWED_HOSTS);

        checkResponse(getRuntimeConfig(nonExistingTask)
                        .andExpect(status().isOk()),
                (int) REPEAT_PERIOD, (int) POSTPONE_PERIOD, String.valueOf(PERIOD_UNIT), IS_CRUCIAL,
                (double) FAIL_PERIOD_FACTOR, BATCH_SIZE, ALLOWED_HOSTS_VALUE);
    }

    private static void checkTask(ZooTask task, long repeatPeriodMs, long postponePeriodMs,
                                  boolean isCrucial, float failPeriodFactor) {
        assertEquals(repeatPeriodMs, task.getRepeatPeriodMsActual());
        assertEquals(postponePeriodMs, task.getPostponePeriodMsActual());
        assertEquals(isCrucial, task.isCrucialActually());
        assertEquals(failPeriodFactor, task.getFailPeriodFactorActual(), 0);
    }

    private static void checkTask(ZooTask task, ZooTaskConfig boot, ZooTaskConfig runtime, ZooTaskConfig actual) {
        assertEquals(boot, task.getStartupConfig());
        assertEquals(runtime, task.getOverridingConfig());
        assertEquals(actual, task.getActualConfig());
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static void checkResponse(ResultActions resultActions,
                                      Integer repeatPeriod,
                                      Integer postponePeriod,
                                      String periodUnit,
                                      Boolean crucial,
                                      Double failPeriodFactor,
                                      Integer batchSize,
                                      String... allowedHosts) throws Exception {
        resultActions
                .andExpect(repeatPeriod == null ? jsonPath(".repeatPeriod").doesNotExist() :
                        jsonPath(".repeatPeriod").value(repeatPeriod))
                .andExpect(postponePeriod == null ? jsonPath(".postponePeriod").doesNotExist() :
                        jsonPath(".postponePeriod").value(postponePeriod))
                .andExpect(periodUnit == null ? jsonPath(".periodUnit").doesNotExist() :
                        jsonPath(".periodUnit").value(periodUnit))
                .andExpect(crucial == null ? jsonPath(".crucial").doesNotExist() :
                        jsonPath(".crucial").value(crucial))
                .andExpect(failPeriodFactor == null ? jsonPath(".failPeriodFactor").doesNotExist() :
                        jsonPath(".failPeriodFactor").value(failPeriodFactor))
                .andExpect(batchSize == null ? jsonPath(".batchSize").doesNotExist() :
                        jsonPath(".batchSize").value(batchSize))
                .andExpect(allowedHosts == null ? jsonPath(".allowedHosts").doesNotExist() :
                        jsonPath(".allowedHosts").value(Matchers.hasItem(Matchers.containsInAnyOrder(allowedHosts))));
    }

    private ResultActions setRuntimeConfig(String taskName, String configName, String configValue) throws Exception {
        return mockMvc.perform(post("/zoo-task-config/" + taskName + "/" + configName)
                .contentType(MediaType.APPLICATION_JSON)
                .content(configValue))
                .andExpect(status().isOk());
    }

    private ResultActions getRuntimeConfig(String taskName) throws Exception {
        return mockMvc.perform(get("/zoo-task-config/" + taskName + "/overriding"));
    }

    private ResultActions getBootConfig(String taskName) throws Exception {
        return mockMvc.perform(get("/zoo-task-config/" + taskName + "/startup"));
    }

    private ResultActions getActualConfig(String taskName) throws Exception {
        return mockMvc.perform(get("/zoo-task-config/" + taskName + "/actual"));
    }

    private ResultActions getAllConfigs(String taskName) throws Exception {
        return mockMvc.perform(get("/zoo-task-config/" + taskName));
    }

    private ResultActions deleteConfig(String taskName) throws Exception {
        return mockMvc.perform(delete("/zoo-task-config/" + taskName))
                .andExpect(status().isOk());
    }

    @Configuration
    public static class TestConfiguration {

        @Bean("configurableTask")
        public ZooTask testTask() {
            ZooTask task = new ZooTask() {
                @Nonnull
                @Override
                public String getName() {
                    return TASK_NAME;
                }
            };
            task.setRepeatPeriod(REPEAT_PERIOD);
            task.setPostponePeriod(POSTPONE_PERIOD);
            task.setPeriodUnit(PERIOD_UNIT);
            task.setCrucial(IS_CRUCIAL);
            task.setFailPeriodFactor(FAIL_PERIOD_FACTOR);
            task.setBatchSize(BATCH_SIZE);
            task.setAllowedHosts(Set.of(ALLOWED_HOSTS_VALUE));
            task.setRunnable((t, token) -> {
            });
            task.init();
            return task;
        }

        @Bean
        public static ZooTaskConfigServiceDependsOnZooMigratorSpringBFPP zooTaskConfigServiceBFPP() {
            return new ZooTaskConfigServiceDependsOnZooMigratorSpringBFPP("zooMigratorSpring");
        }
    }
}


