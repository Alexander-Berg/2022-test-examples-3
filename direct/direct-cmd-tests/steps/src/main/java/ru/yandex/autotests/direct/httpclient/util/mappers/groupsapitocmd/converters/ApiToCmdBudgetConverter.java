package ru.yandex.autotests.direct.httpclient.util.mappers.groupsapitocmd.converters;

import org.dozer.CustomConverter;

/**
 * Created by shmykov on 23.04.15.
 */
public class ApiToCmdBudgetConverter implements CustomConverter {

    private static final String BUDGET_RESPECTIVE_STRATEGY = "WeeklyBudget";

    private static final String BUDGET_STRATEGY_NAME = "distributed";

    @Override
    public Object convert(Object existingDestinationFieldValue, Object sourceFieldValue, Class<?> destinationClass, Class<?> sourceClass) {
        if (sourceFieldValue.equals(BUDGET_RESPECTIVE_STRATEGY)) {
            return BUDGET_STRATEGY_NAME;
        }
        return sourceFieldValue;
    }
}
