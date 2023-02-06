package ru.yandex.direct.ess.router.configuration;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import ru.yandex.direct.ess.common.models.BaseEssConfig;
import ru.yandex.direct.ess.router.models.rule.AbstractRule;
import ru.yandex.direct.ess.router.models.rule.EssRule;


class RouterConfigurationTest {
    private AnnotationConfigApplicationContext initApplicationContext() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(TestConfiguration.class);
        applicationContext.refresh();
        return applicationContext;
    }

    private void closeApplicationContext(AnnotationConfigApplicationContext applicationContext) {
        applicationContext.stop();
        applicationContext.close();
    }

    /**
     * Тест проверяет, что для всех бинов, наследуемых от {@link AbstractRule},
     * их параметризованный тип равен тому, который возвращает их {@link BaseEssConfig} в методе
     * {@link BaseEssConfig#getLogicObject()}
     */

    @Test
    @Disabled("требуется доработка, см запуск на IndoorBannerModerationRule")
    void testLogicObjectEquals() throws IllegalAccessException, InstantiationException {
        AnnotationConfigApplicationContext applicationContext = initApplicationContext();
        Collection<? extends AbstractRule> essRules =
                applicationContext.getBeansOfType(AbstractRule.class).values();
        closeApplicationContext(applicationContext);
        SoftAssertions softAssertions = new SoftAssertions();
        for (AbstractRule essRule : essRules) {
            BaseEssConfig config =
                    essRule.getClass().getAnnotation(EssRule.class).value().newInstance();
            ParameterizedType essRuleParameterized =
                    (ParameterizedType) essRule.getClass().getGenericSuperclass();
            softAssertions.assertThat(essRuleParameterized.getActualTypeArguments()[0].getTypeName())
                    .withFailMessage(
                            "Logic objects in ess rule %s and it's config class %s are different: %s and %s",
                            essRule.getClass(),
                            config.getClass().getName(),
                            essRuleParameterized.getActualTypeArguments()[0].getTypeName(),
                            config.getLogicObject().getTypeName())
                    .isEqualTo(config.getLogicObject().getTypeName());
        }
        softAssertions.assertAll();
    }

    /**
     * Тест проверяет, что все бины, наследуемые от {@link AbstractRule},
     * в своем классе {@link BaseEssConfig} задают разные топики logbroker'a {@link BaseEssConfig#getTopic()}}
     */
    @Test
    void testLogbrokerTopicUsedOnlyOnce() {
        AnnotationConfigApplicationContext applicationContext = initApplicationContext();
        Collection<? extends AbstractRule> essRules =
                applicationContext.getBeansOfType(AbstractRule.class).values();
        closeApplicationContext(applicationContext);
        Map<String, List<String>> topicFrequencyMap = essRules.stream()
                .collect(Collectors.groupingBy(
                        essRule -> {
                            try {
                                return essRule.getClass().getAnnotation(EssRule.class).value()
                                        .newInstance().getTopic();
                            } catch (InstantiationException | IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        Collectors
                                .mapping(essRule -> essRule.getClass().getName(), Collectors.toList())));

        SoftAssertions softAssertions = new SoftAssertions();
        for (Map.Entry<String, List<String>> topicToEssRules : topicFrequencyMap.entrySet()) {
            softAssertions.assertThat(topicToEssRules.getValue().size())
                    .withFailMessage("Logbroker topic %s used in more then one ess rules %s",
                            topicToEssRules.getKey(), topicToEssRules.getValue()).isEqualTo(1);
        }
        softAssertions.assertAll();
    }
}
