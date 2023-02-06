package ru.yandex.market.mbo.db.modelstorage.validation;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.modelstorage.validation.context.CachingModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuru;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getParentWithRelation;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getSkuBuilderWithoutDefiningParams;

/**
 * @author danfertev
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SkuParameterValidatorTest {
    private SkuParametersValidator validator;
    private ModelValidationContext context;

    @Before
    public void setup() {
        validator = new SkuParametersValidator();
        context = mock(CachingModelValidationContext.class);
        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE)))
            .thenReturn(ImmutableMap.of(
                KnownIds.NAME_PARAM_ID, XslNames.NAME,
                KnownIds.VENDOR_PARAM_ID, XslNames.VENDOR,
                2L, "param2"
            ));
        when(context.getReadableParameterName(anyLong(), anyString())).then(args -> args.getArgument(1));
        when(context.getOptionNames(anyLong(), anyCollection())).thenReturn(Collections.emptyMap());
    }

    @Test
    public void testNoErrorOnGuru() {
        CommonModel guru = getGuru();

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            Collections.singletonList(guru)
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testIncorrectParameterValue() {
        CommonModel sku = getSkuBuilderWithoutDefiningParams()
            .id(10L)
            .param(2L).setNumeric(1)
            .endModel();
        CommonModel parent = getParentWithRelation(10L);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku),
            Arrays.asList(parent, sku)
        );

        ModelValidationError expected =
            validator.createInvalidParamForModelTypeError(context, sku, sku, 2L, "param2", "1");

        assertThat(errors).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testIncorrectParameterValueHypothesis() {
        CommonModel sku = getSkuBuilderWithoutDefiningParams()
            .id(10L)
            .parameterValueHypothesis(2L, "param2", Param.Type.ENUM, "test")
            .endModel();
        CommonModel parent = getParentWithRelation(10L);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku),
            Arrays.asList(parent, sku)
        );

        ModelValidationError expected =
            validator.createInvalidParamForModelTypeError(context, sku, sku, 2L, "param2", "ru: test");

        assertThat(errors).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testIncorrectMultipleParameterValues() {
        CommonModel sku = getSkuBuilderWithoutDefiningParams()
            .id(10L)
            .param(2L).setNumeric(1)
            .param(2L).setNumeric(2)
            .endModel();
        CommonModel parent = getParentWithRelation(10L);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku),
            Arrays.asList(parent, sku)
        );

        ModelValidationError expected1 = validator
            .createInvalidParamForModelTypeError(context, sku, sku, 2L, "param2",  "1");
        ModelValidationError expected2 = validator
            .createInvalidParamForModelTypeError(context, sku, sku, 2L, "param2", "2");

        assertThat(errors).containsExactlyInAnyOrder(expected1, expected2);
    }

    @Test
    public void testIgnoreVendorAndNameParameters() {
        CommonModel sku = getSkuBuilderWithoutDefiningParams()
            .id(10L)
            .title("name")
            .endModel();
        CommonModel parent = getParentWithRelation(10L);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku),
            Arrays.asList(parent, sku)
        );

        assertThat(errors).isEmpty();
    }

    private ModelChanges getModelChanges(CommonModel after) {
        return new ModelChanges(after, after, ModelChanges.Operation.UPDATE);
    }
}
