package ru.yandex.market.mboc.monetization.validation.validators;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mboc.app.controller.web.DisplayGroupingConfig;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.enums.ConfigParameterType;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.enums.ParameterValueType;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ConfigParameter;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ConfigValidationError;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.GroupingConfig;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.monetization.config.MonetizationJooqConfig;

/**
 * @author eremeevvo
 * @since 24.10.2019
 */
@ContextConfiguration(classes = {MonetizationJooqConfig.class})
public class ParametersValueEventTypeValidatorTest extends BaseDbTestClass {

    private GroupingConfigValidator parametersValueTypeValidator;

    @Before
    public void setUp() {
        parametersValueTypeValidator = new ParametersValueTypeValidator();
    }

    @Test
    public void testValidateWithErrors() {
        GroupingConfig groupingConfig = new GroupingConfig().setId(1L);

        ConfigParameter param1 = new ConfigParameter()
            .setConfigId(groupingConfig.getId())
            .setParamValueType(ParameterValueType.STRING)
            .setName("test")
            .setParamGroupingType(ConfigParameterType.NORM_COEFFICIENT);

        ConfigParameter param2 = new ConfigParameter()
            .setConfigId(groupingConfig.getId())
            .setParamValueType(ParameterValueType.NUMERIC)
            .setParamGroupingType(ConfigParameterType.DETERMINANT);

        DisplayGroupingConfig config = new DisplayGroupingConfig()
            .setGroupingConfig(groupingConfig)
            .setConfigParameters(Arrays.asList(param1, param2));

        List<ConfigValidationError> errors = parametersValueTypeValidator
            .validate(Collections.singleton(config));

        ConfigValidationError expectedError = new ConfigValidationError()
            .setConfigId(groupingConfig.getId())
            .setMessage(ParametersValueTypeValidator.errorMessage(groupingConfig.getId(), param1));

        Assertions.assertThat(errors).isNotEmpty();
        Assertions.assertThat(errors).containsExactly(expectedError);
    }

    @Test
    public void testValidateWithoutErrors() {
        GroupingConfig groupingConfig = new GroupingConfig().setId(1L);

        ConfigParameter param1 = new ConfigParameter()
            .setConfigId(groupingConfig.getId())
            .setParamGroupingType(ConfigParameterType.NORM_COEFFICIENT)
            .setParamValueType(ParameterValueType.NUMERIC)
            .setParamId(1L);

        ConfigParameter param2 = new ConfigParameter()
            .setConfigId(groupingConfig.getId())
            .setParamValueType(ParameterValueType.NUMERIC_ENUM)
            .setParamGroupingType(ConfigParameterType.DETERMINANT)
            .setParamId(2L);

        DisplayGroupingConfig config = new DisplayGroupingConfig()
            .setGroupingConfig(groupingConfig)
            .setConfigParameters(Arrays.asList(param1, param2));

        List<ConfigValidationError> errors = parametersValueTypeValidator
            .validate(Collections.singleton(config));

        Assertions.assertThat(errors).isEmpty();
    }
}
