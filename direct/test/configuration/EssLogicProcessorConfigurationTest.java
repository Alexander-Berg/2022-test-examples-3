package ru.yandex.direct.test.configuration;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import ru.yandex.direct.ess.common.models.BaseEssConfig;
import ru.yandex.direct.logicprocessor.common.BaseLogicProcessor;
import ru.yandex.direct.logicprocessor.common.EssLogicProcessor;
import ru.yandex.direct.logicprocessor.configuration.EssLogicProcessorConfiguration;

public class EssLogicProcessorConfigurationTest {
    AnnotationConfigApplicationContext applicationContext;

    @BeforeEach
    public void setUp() {
        applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(EssLogicProcessorConfiguration.class);
        applicationContext.refresh();
    }

    @AfterEach
    public void tearDown() {
        applicationContext.close();
    }

    /**
     * Тест проверяет, что для всех бинов, наследуемых от {@link BaseLogicProcessor},
     * их параметризованный тип равен тому, который возвращает их {@link BaseEssConfig} в методе
     * {@link BaseEssConfig#getLogicObject()}
     */

    @Test
    @Disabled("требуется доработка, см запуск на IndoorBannerModerationRule")
    void testLogicObjectEquals() throws IllegalAccessException, InstantiationException {
        Collection<? extends BaseLogicProcessor> logicProcessors =
                applicationContext.getBeansOfType(BaseLogicProcessor.class).values();
        SoftAssertions softAssertions = new SoftAssertions();
        for (BaseLogicProcessor logicProcessor : logicProcessors) {
            BaseEssConfig config =
                    logicProcessor.getClass().getAnnotation(EssLogicProcessor.class).value().newInstance();
            ParameterizedType logicProcessorParameterized =
                    (ParameterizedType) logicProcessor.getClass().getGenericSuperclass();
            softAssertions.assertThat(logicProcessorParameterized.getActualTypeArguments()[0].getTypeName())
                    .withFailMessage(
                            "Logic objects in logic processor %s and it's config class %s are different: %s and %s",
                            logicProcessor.getClass(),
                            config.getClass().getCanonicalName(),
                            logicProcessorParameterized.getActualTypeArguments()[0].getTypeName(),
                            config.getLogicObject().getTypeName())
                    .isEqualTo(config.getLogicObject().getTypeName());
        }
        softAssertions.assertAll();
    }

    /**
     * Тест проверяет, что все бины, наследуемые от {@link BaseLogicProcessor},
     * в своем классе {@link BaseEssConfig} задают разные топики logbroker'a {@link BaseEssConfig#getTopic()}}
     */
    @Test
    void testEssConfigUsedOnlyOnce() {
        Collection<? extends BaseLogicProcessor> logicProcessors =
                applicationContext.getBeansOfType(BaseLogicProcessor.class).values();
        Map<String, List<String>> topicFrequencyMap = logicProcessors.stream()
                .collect(Collectors.groupingBy(
                        logicProcessor -> {
                            try {
                                return logicProcessor.getClass().getAnnotation(EssLogicProcessor.class).value()
                                        .newInstance().getTopic();
                            } catch (InstantiationException | IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        Collectors
                                .mapping(logicProcessor -> logicProcessor.getClass().getCanonicalName(),
                                        Collectors.toList())));

        SoftAssertions softAssertions = new SoftAssertions();
        for (Map.Entry<String, List<String>> topicToLogicProcessors : topicFrequencyMap.entrySet()) {
            softAssertions.assertThat(topicToLogicProcessors.getValue().size())
                    .withFailMessage("Logbroker topic %s used in more then one logic processors %s",
                            topicToLogicProcessors.getKey(), topicToLogicProcessors.getValue()).isEqualTo(1);
        }
        softAssertions.assertAll();
    }

}
