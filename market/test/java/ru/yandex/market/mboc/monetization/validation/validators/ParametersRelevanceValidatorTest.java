package ru.yandex.market.mboc.monetization.validation.validators;

import java.util.Collections;
import java.util.List;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mboc.app.controller.web.DisplayGroupingConfig;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.CategoryParameter;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ConfigParameter;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ConfigValidationError;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.GroupingConfig;
import ru.yandex.market.mboc.common.parameters.CategoryParameterRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.monetization.config.MonetizationJooqConfig;

/**
 * @author eremeevvo
 * @since 24.10.2019
 */
@ContextConfiguration(classes = {MonetizationJooqConfig.class})
public class ParametersRelevanceValidatorTest extends BaseDbTestClass {

    private static final int SEED = 23432;
    private static final long CONFIG_ID = 100500L;
    private static final long CATEGORY1 = 1L;
    private static final long CATEGORY2 = 2L;

    private GroupingConfigValidator parametersRelevanceValidator;
    private EnhancedRandom random;
    @Autowired
    private CategoryParameterRepository categoryParameterRepository;

    @Before
    public void setUp() {
        random = new EnhancedRandomBuilder().seed(SEED).build();
        parametersRelevanceValidator = new ParametersRelevanceValidator(categoryParameterRepository);
    }

    @Test
    public void testValidateWithErrors() {
        CategoryParameter saved = categoryParameterRepository.save(
            random.nextObject(CategoryParameter.class).setCategoryId(CATEGORY1)
        );
        CategoryParameter notSaved = random.nextObject(CategoryParameter.class).setCategoryId(CATEGORY1);

        GroupingConfig groupingConfig = new GroupingConfig().setCategoryId(CATEGORY1).setId(CONFIG_ID);

        ConfigParameter savedParameter = new ConfigParameter()
            .setConfigId(groupingConfig.getId())
            .setParamId(saved.getParamId());

        ConfigParameter notSavedParameter = new ConfigParameter()
            .setConfigId(groupingConfig.getId())
            .setParamId(notSaved.getParamId())
            .setName(notSaved.getName());

        DisplayGroupingConfig config = new DisplayGroupingConfig()
            .setGroupingConfig(groupingConfig)
            .setConfigParameters(List.of(savedParameter, notSavedParameter));

        List<ConfigValidationError> errors = parametersRelevanceValidator
            .validate(Collections.singleton(config));

        ConfigValidationError expectedError = new ConfigValidationError()
            .setConfigId(groupingConfig.getId())
            .setMessage(ParametersRelevanceValidator.errorMessage(groupingConfig, notSavedParameter));

        Assertions.assertThat(errors).isNotEmpty();
        Assertions.assertThat(errors).containsExactly(expectedError);
    }

    @Test
    public void testValidateNotFoundInCategory() {
        CategoryParameter saved = categoryParameterRepository.save(
            random.nextObject(CategoryParameter.class).setCategoryId(CATEGORY1)
        );

        GroupingConfig groupingConfig = new GroupingConfig().setCategoryId(CATEGORY2).setId(CONFIG_ID);

        ConfigParameter otherCategoryParam = new ConfigParameter()
            .setConfigId(groupingConfig.getId())
            .setParamId(saved.getParamId());

        DisplayGroupingConfig config = new DisplayGroupingConfig()
            .setGroupingConfig(groupingConfig)
            .setConfigParameters(List.of(otherCategoryParam));

        List<ConfigValidationError> errors = parametersRelevanceValidator
            .validate(Collections.singleton(config));

        ConfigValidationError expectedError = new ConfigValidationError()
            .setConfigId(groupingConfig.getId())
            .setMessage(ParametersRelevanceValidator.errorMessage(groupingConfig, otherCategoryParam));

        Assertions.assertThat(errors).isNotEmpty();
        Assertions.assertThat(errors).containsExactly(expectedError);
    }

    @Test
    public void testValidateWithoutErrors() {
        CategoryParameter saved1 = categoryParameterRepository.save(
            random.nextObject(CategoryParameter.class).setCategoryId(CATEGORY1)
        );
        CategoryParameter saved2 = categoryParameterRepository.save(
            random.nextObject(CategoryParameter.class).setCategoryId(CATEGORY1)
        );

        GroupingConfig groupingConfig = new GroupingConfig().setCategoryId(CATEGORY1).setId(CONFIG_ID);

        ConfigParameter savedParam1 = new ConfigParameter()
            .setConfigId(groupingConfig.getId())
            .setParamId(saved1.getParamId());

        ConfigParameter savedParam2 = new ConfigParameter()
            .setConfigId(groupingConfig.getId())
            .setParamId(saved2.getParamId());

        DisplayGroupingConfig config = new DisplayGroupingConfig()
            .setGroupingConfig(groupingConfig)
            .setConfigParameters(List.of(savedParam1, savedParam2));

        List<ConfigValidationError> errors = parametersRelevanceValidator.validate(List.of(config));
        Assertions.assertThat(errors).isEmpty();
    }
}
