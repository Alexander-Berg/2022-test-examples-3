package ru.yandex.direct.ess.router.components;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.models.rule.AbstractRule;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class RulesProcessingServiceTest {

    @Autowired
    private List<AbstractRule> rules;

    @Test
    void testRuleLogicNamesAreUnique() {
        var names = mapList(rules, AbstractRule::getLogicProcessName);
        assertThat(names).doesNotHaveDuplicates();
    }

}
