package ru.yandex.autotests.direct.cmd.data.conditions;

import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.group.Condition;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DynamicConditionsFactory {
    public static List<DynamicCondition> defaultConditions() {
        return Collections.singletonList(BeanLoadHelper.loadCmdBean(
                CmdBeans.COMMON_REQUEST_DYNAMIC_COND_FULL, DynamicCondition.class));
    }

    public static List<DynamicCondition> duplicateConditions() {
        return Collections.nCopies(2, BeanLoadHelper.loadCmdBean(
                CmdBeans.COMMON_REQUEST_DYNAMIC_COND_FULL, DynamicCondition.class));
    }

    public static List<DynamicCondition> conditionsWithEmptyName() {
        List<DynamicCondition> resultConditions = defaultConditions();
        resultConditions.get(0).setDynamicConditionName("");
        return resultConditions;
    }

    public static List<DynamicCondition> conditionWithExtraLowPrice() {
        List<DynamicCondition> resultConditions = defaultConditions();
        resultConditions.get(0).setPrice(0.1f);
        return resultConditions;
    }

    public static List<DynamicCondition> maxConditionsInGroup() {
        return IntStream.range(1, 52).mapToObj(x ->BeanLoadHelper.loadCmdBean(
                CmdBeans.COMMON_REQUEST_DYNAMIC_COND_FULL, DynamicCondition.class)
                .withConditions(Collections.singletonList(new Condition().withKind("exact").withType("URL")
                        .withValue(Collections.singletonList("qwe" + x)))))
                .collect(Collectors.toList());
    }
}
