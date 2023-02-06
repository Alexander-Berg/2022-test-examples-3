package ru.yandex.direct.api.v5.entity.smartadtargets.delegate;

import java.util.List;

import com.yandex.direct.api.v5.smartadtargets.ConditionsArray;
import com.yandex.direct.api.v5.smartadtargets.ConditionsItem;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;

import ru.yandex.direct.api.v5.entity.smartadtargets.converter.CommonConverters;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;

import static ru.yandex.direct.api.v5.entity.smartadtargets.converter.CommonConverters.IS_NOT_AVAILABLE_CONDITION;

public class TestUtils {

    static ConditionsArray getConditionsArray(List<PerformanceFilterCondition> conditions) {
        if (conditions == null) {
            return null;
        }
        List<ConditionsItem> conditionsItems = StreamEx.of(conditions)
                .filter(IS_NOT_AVAILABLE_CONDITION)
                .map(cond -> new ConditionsItem()
                        .withOperand(cond.getFieldName())
                        .withOperator(CommonConverters.API_OPERATOR_BY_CORE_OPERATOR.get(cond.getOperator()))
                        .withArguments(getArguments(cond.getStringValue())))
                .toList();
        return new ConditionsArray()
                .withItems(conditionsItems);
    }

    private static String[] getArguments(String stringValue) {
        if (stringValue.length() > 2 && stringValue.startsWith("[\"")) {
            String subValue = stringValue.substring(2, stringValue.length() - 2);
            return subValue.split("\",\"");
        }
        if (stringValue.length() > 2 && stringValue.startsWith("[")) {
            String subValue = stringValue.substring(1, stringValue.length() - 1);
            if (subValue.contains(",")) {
                String[] elements = subValue.split(",");
                if (StreamEx.of(elements).allMatch(StringUtils::isNumeric)) {
                    return elements;
                }
            }
            return new String[]{subValue};
        }
        return new String[]{stringValue};
    }

}
