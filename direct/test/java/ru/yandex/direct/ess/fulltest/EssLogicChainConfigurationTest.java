package ru.yandex.direct.ess.fulltest;

import java.util.Set;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.reflections.Reflections;

import ru.yandex.direct.ess.router.models.rule.AbstractRule;
import ru.yandex.direct.ess.router.models.rule.EssRule;
import ru.yandex.direct.logicprocessor.common.BaseLogicProcessorNotScheduled;
import ru.yandex.direct.logicprocessor.common.EssLogicProcessor;
import ru.yandex.direct.scheduler.Hourglass;

import static java.util.Objects.requireNonNull;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

public class EssLogicChainConfigurationTest {

    private static final String PACKAGE_NAME = "ru.yandex.direct";
    private static final Set<String> EXCLUDE_CONFIG_CLASS_NAME = Set.of(
            "ru.yandex.direct.ess.router.models.rule.AbstractRuleAdditionalObjectsTest$TestConfig");

    /**
     * Проверяем соответствие условий запуска rule и processor в рамках одного конфига
     */
    @Test
    public void testConfigConditionsOfProcessorAndRule() {
        Reflections reflections = new Reflections(PACKAGE_NAME);
        var essConfigClassToProcessorConditionMap =
                StreamEx.of(reflections.getSubTypesOf(BaseLogicProcessorNotScheduled.class))
                        .filter(clazz -> findAnnotation(clazz, EssLogicProcessor.class) != null)
                        .mapToEntry(clazz -> requireNonNull(findAnnotation(clazz, Hourglass.class)).needSchedule())
                        .mapKeys(clazz -> requireNonNull(findAnnotation(clazz, EssLogicProcessor.class)).value())
                        .toMap();
        var essConfigClassToRuleConditionMap =
                StreamEx.of(reflections.getSubTypesOf(AbstractRule.class))
                        .filter(clazz -> findAnnotation(clazz, EssRule.class) != null)
                        .map(clazz -> findAnnotation(clazz, EssRule.class))
                        .mapToEntry(EssRule::value, EssRule::runCondition)
                        .toMap();
        SoftAssertions softAssertions = new SoftAssertions();
        for (var essConfigToRuleCondition : essConfigClassToRuleConditionMap.entrySet()) {
            var essConfigClass = essConfigToRuleCondition.getKey();
            var ruleCondition = essConfigToRuleCondition.getValue();

            if (EXCLUDE_CONFIG_CLASS_NAME.contains(essConfigClass.getName())) {
                continue;
            }

            var processorCondition = essConfigClassToProcessorConditionMap.get(essConfigClass);
            softAssertions.assertThat(processorCondition)
                    .withFailMessage(
                            "For ess config %s rule condition %s should be equal to logic processor condition %s",
                            essConfigClass,
                            ruleCondition,
                            processorCondition)
                    .isEqualTo(ruleCondition);
        }
        softAssertions.assertAll();
    }
}
