package ru.yandex.direct.jobs.monitoring;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.ansiblejuggler.PlaybookBuilder;
import ru.yandex.direct.ansiblejuggler.model.JugglerPlaybook;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.config.DirectConfigPropertySource;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.env.EnvironmentTypeProvider;
import ru.yandex.direct.jobs.monitoring.checks.SchedulerOnTestingJugglerChecks;
import ru.yandex.direct.juggler.check.DirectNumericCheck;
import ru.yandex.direct.juggler.check.DirectNumericChecksBundle;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.io.FileUtils.expandHome;

/**
 * Проверяем что в заданных типах окружений - плейбук с проверками корректно собирается:
 * <ul>
 * <li>все проверки валидны</li>
 * <li>не дублируются</li>
 * <li>плейбук не пустой и в нем не меньше {@code MINIMUM_TASK_COUNT} задач</li>
 * <li>плейбук успешно сериализуется</li>
 * </ul>
 * <p>
 * Для отладки-сравнения проверок - можно включить {@link #DEBUG_DUMP_PLAYBOOKS},
 * тогда содержимое плейбуков для разных окружений будет сохранено в домашней директории
 */
@ContextConfiguration(classes = {MonitoringTestConfiguration.class})
@ExtendWith(SpringExtension.class)
class JugglerChecksTest {

    private static final boolean DEBUG_DUMP_PLAYBOOKS = false;

    private static final int MINIMUM_TASK_COUNT = 10;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private JobJugglerChecksSynchronizer jugglerChecksSynchronizer;

    @Autowired
    private EnvironmentTypeProvider environmentTypeProvider;

    @Autowired
    private MonitoringTestConfiguration.MutableDirectConfigPropertySource directConfigPropertySource;

    static Iterable<EnvironmentType> params() {
        return asList(EnvironmentType.DEVELOPMENT, EnvironmentType.DEVTEST, EnvironmentType.DEV7,
                EnvironmentType.TESTING, EnvironmentType.PRODUCTION);
    }

    void configureEnvironmentTypeProvider(EnvironmentType testEnvironmentType) {
        ((MonitoringTestConfiguration.MutableEnvironmentTypeProvider) environmentTypeProvider).set(testEnvironmentType);
        // подсовываем фейковый property-резолвер от нужной нам конфигурации для того,
        // чтобы получившийся плейбук содержал настоящие параметры
        var config = DirectConfigFactory.getConfig(testEnvironmentType);
        var override = new DirectConfigPropertySource("fake", config);
        directConfigPropertySource.set(override);
        jugglerChecksSynchronizer = applicationContext.getBean(JobJugglerChecksSynchronizer.class);
        // сюда тоже протекают juggler-настройки, поэтому инстанцируем бины
        applicationContext.getBeansOfType(DirectNumericCheck.class);
        applicationContext.getBeansOfType(DirectNumericChecksBundle.class);
        applicationContext.getBeansOfType(SchedulerOnTestingJugglerChecks.class);

        // отлючаем фейк, иначе не получится получать из контекста сами джобы для получения с них параметров
        directConfigPropertySource.set(null);
    }

    @ParameterizedTest(name = "EnvironmentType = {0}")
    @MethodSource("params")
    @DirtiesContext
    void addChecksFromAnnotationsIsSuccessful(EnvironmentType testEnvironmentType) throws JsonProcessingException {
        configureEnvironmentTypeProvider(testEnvironmentType);

        PlaybookBuilder playbookBuilder = jugglerChecksSynchronizer.getBuilder();
        jugglerChecksSynchronizer.addChecksFromAnnotations(playbookBuilder);

        JugglerPlaybook playbook = playbookBuilder.build();
        assertThat(playbook).as("playbook successfully built").isNotNull();

        assertThat(playbook.getTasksCount()).isGreaterThanOrEqualTo(MINIMUM_TASK_COUNT);
        debugDump(playbook, testEnvironmentType);

        String dumpedValue = playbook.dump();
        assertThat(dumpedValue).as("playbook dump result is not empty").isNotBlank();
    }

    @Test
    void debugShouldBeDisabled() {
        assertThat(DEBUG_DUMP_PLAYBOOKS).as("playbook dump should be disabled").isFalse();
    }

    private void debugDump(JugglerPlaybook playbook, EnvironmentType testEnvironmentType) {
        if (DEBUG_DUMP_PLAYBOOKS) {
            try {
                playbook.saveToFile(expandHome("~/jugglerChecksTest-" + testEnvironmentType + ".yml").toFile());
            } catch (IOException e) {
                System.err.println("Failed to dump playbook");
            }
        }
    }
}
