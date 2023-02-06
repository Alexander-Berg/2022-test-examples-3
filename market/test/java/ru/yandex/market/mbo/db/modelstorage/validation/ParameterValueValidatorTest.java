package ru.yandex.market.mbo.db.modelstorage.validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueStringBuilder;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.ParameterValueValidator.DEFINITION_MISMATCH_ERROR_LMP;
import static ru.yandex.market.mbo.gwt.models.visual.Word.DEFAULT_LANG_ID;

/**
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ParameterValueValidatorTest {

    private static final long CATEGORY_HID = 100500;
    private static final long USER_ID = 100500;
    private static final long DEFAULT_ID = 100500;
    private static final long PARAMETER_VALUE = 100500;
    private static final String DEFAULT_XSL_NAME = "default";
    private static final int DEFAULT_SIZE = 4001;
    private static final int TEST_HEX_NUMBER = 0x10001;

    private ParameterValueValidator validator;
    private ModelValidationContext context;

    @Before
    public void setup() {
        validator = new ParameterValueValidator();
        context = mock(ModelValidationContext.class);
        when(context.parameterMatchesDefinition(anyLong(), any())).thenReturn(true);
        when(context.getStringParamMaxLength(anyLong(), anyString())).thenReturn(4000);
        when(context.getParameterBounds(anyLong(), anyString())).thenReturn(NumericBounds.notDefined());
        when(context.getReadableParameterName(anyLong(), anyLong()))
            .thenAnswer(i -> "ParamName" + i.getArgument(1));
        when(context.getReadableParameterName(anyLong(), anyString()))
            .thenAnswer(i -> "ParamName" + i.getArgument(1));
    }

    @Test
    public void testMissingXslName() {
        ParameterValue value = createDefaultParameterValue(DEFAULT_ID);
        value.setXslName(null);
        CommonModel model = createModel(value);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );

        assertThat(errors).containsExactlyInAnyOrder(
            missingXslNameError(model, true, value)
        );

        errors = validator.validate(
            context,
            new ModelChanges(model, model),
            Collections.emptyList()
        );

        assertThat(errors).containsExactlyInAnyOrder(
            missingXslNameError(model, false, value)
        );
    }

    @Test
    public void testMissingId() {
        ParameterValue value = createDefaultParameterValue(0L);
        CommonModel model = createModel(value);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            missingIdError(model, true, value)
        );

        errors = validator.validate(
            context,
            new ModelChanges(model, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            missingIdError(model, false, value)
        );
    }

    @Test
    public void testMissingType() {
        ParameterValue value = createDefaultParameterValue(DEFAULT_ID);
        value.setType(null);
        CommonModel model = createModel(value);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            missingTypeError(model, true, value)
        );

        // We can't find out if value equals without parameter type - so it's critical as well
        errors = validator.validate(
            context,
            new ModelChanges(model, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            missingTypeError(model, true, value)
        );
    }

    @Test
    public void testMissingModificationSource() {
        ParameterValue value = createDefaultParameterValue(DEFAULT_ID);
        value.setModificationSource(null);
        CommonModel model = createModel(value);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            missingSourceError(model, true, value)
        );

        // Not creating errors for these as all models have this and it's fine
        errors = validator.validate(
            context,
            new ModelChanges(model, model),
            Collections.emptyList()
        );
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testDefinitionMismatch() {
        ModelValidationContext mismatchContext = mock(ModelValidationContext.class);
        when(mismatchContext.getParameterBounds(anyLong(), anyString())).thenReturn(NumericBounds.notDefined());
        when(mismatchContext.parameterMatchesDefinition(anyLong(), any())).thenReturn(false);
        when(mismatchContext.getReadableParameterName(anyLong(), anyLong()))
            .thenAnswer(i -> "ParamName" + i.getArgument(1));

        ParameterValue value = createDefaultParameterValue(DEFAULT_ID);
        CommonModel model = createModel(value);
        List<ModelValidationError> errors = validator.validate(
            mismatchContext,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            definitionMismatchError(model, true, value)
        );

        errors = validator.validate(mismatchContext,
            new ModelChanges(model, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            definitionMismatchError(model, false, value)
        );
    }

    @Test
    public void testDefinitionMismatchCategoryChanged() {
        ModelValidationContext mismatchContext = mock(ModelValidationContext.class);
        when(mismatchContext.getParameterBounds(anyLong(), anyString())).thenReturn(NumericBounds.notDefined());
        when(mismatchContext.parameterMatchesDefinition(anyLong(), any())).thenReturn(false);
        when(mismatchContext.getReadableParameterName(anyLong(), anyLong()))
            .thenAnswer(i -> "ParamName" + i.getArgument(1));

        ParameterValue value = createDefaultParameterValue(DEFAULT_ID);
        CommonModel model = createModel(value);
        CommonModel categoryChangedModel = new CommonModel(model);
        categoryChangedModel.setCategoryId(123456L);

        List<ModelValidationError> errors = validator.validate(mismatchContext,
            new ModelChanges(model, categoryChangedModel),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            definitionMismatchError(categoryChangedModel, true, value)
        );
    }

    @Test
    public void testHypothesisValue() {
        ParameterValue value = createParameterValue(DEFAULT_ID, DEFAULT_XSL_NAME, Param.Type.HYPOTHESIS);
        CommonModel model = createModel(value);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            hypothesisValueError(model, true, value)
        );

        errors = validator.validate(
            context,
            new ModelChanges(model, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            hypothesisValueError(model, false, value)
        );
    }

    @Test
    public void testEmptyString() {
        ParameterValue value = createParameterValue(DEFAULT_ID, DEFAULT_XSL_NAME, Param.Type.STRING);
        value.setStringValue(new ArrayList<>());
        value.getStringValue().add(new Word());
        CommonModel model = createModel(value);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            stringValueEmptyError(model, true, value)
        );

        errors = validator.validate(
            context,
            new ModelChanges(model, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            stringValueEmptyError(model, false, value)
        );
    }

    @Test
    public void testInvalidString() {
        ParameterValue value = createParameterValue(DEFAULT_ID, DEFAULT_XSL_NAME, Param.Type.STRING);
        value.setStringValue(new ArrayList<>());
        value.getStringValue().add(new Word(DEFAULT_LANG_ID, "qwerrqwe" + (char) TEST_HEX_NUMBER));
        CommonModel model = createModel(value);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            invalidStringError(model, true, value)
        );

        // Not creating errors for these as all models have this and it's fine
        errors = validator.validate(
            context,
            new ModelChanges(model, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            invalidStringError(model, false, value)
        );
    }

    @Test
    public void testNotUniquiString() {
        ParameterValue value = createParameterValue(DEFAULT_ID, DEFAULT_XSL_NAME, Param.Type.STRING);
        value.setStringValue(new ArrayList<>());
        value.getStringValue().add(new Word(DEFAULT_LANG_ID, "qwerty"));
        value.getStringValue().add(new Word(DEFAULT_LANG_ID, "qwerty"));
        value.getStringValue().add(new Word(DEFAULT_LANG_ID, "123"));

        CommonModel model = createModel(value);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            nonUniqueValue(model, true, value)
        );

        errors = validator.validate(
            context,
            new ModelChanges(model, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            nonUniqueValue(model, false, value)
        );
    }

    @Test
    public void testLongStringValue() {
        ParameterValue value = createParameterValue(DEFAULT_ID, "anyStringParam", Param.Type.STRING);
        value.setStringValue(new ArrayList<>());
        value.getStringValue().add(new Word(DEFAULT_LANG_ID, StringUtils.leftPad("foobar", DEFAULT_SIZE, '*')));
        CommonModel model = createModel(value);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            stringTooLongError(model, true, value, "4000")
        );

        // Not creating errors for these as all models have this and it's fine
        errors = validator.validate(
            context,
            new ModelChanges(model, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            stringTooLongError(model, false, value, "4000")
        );
    }

    @Test
    public void testInvalidBooleanValue() {
        ParameterValue value = createParameterValue(DEFAULT_ID, XslNames.URL, Param.Type.BOOLEAN);
        value.setBooleanValue(true);
        CommonModel model = createModel(value);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            invalidBooleanValueError(model, true, value)
        );
    }

    @Test
    public void testEmptyBooleanValue() {
        ParameterValue value = createParameterValue(DEFAULT_ID, XslNames.URL, Param.Type.BOOLEAN);
        CommonModel model = createModel(value);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            emptyValueError(model, true, value)
        );
    }

    @Test
    public void testEmptyPictureParameter() {
        ParameterValue value = createParameterValue(DEFAULT_ID, XslNames.URL, Param.Type.ENUM);
        CommonModel model = createModel();
        Picture picture = new Picture();
        picture.getParameterValues().add(value);
        model.getPictures().add(picture);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            emptyValueError(model, true, value)
        );
    }

    @Test
    public void testValueOutOfBounds() {
        ParameterValue value0 = createParameterValue(DEFAULT_ID, XslNames.PRICE, Param.Type.NUMERIC);
        value0.setNumericValue(BigDecimal.ZERO);
        ParameterValue value2 = createParameterValue(DEFAULT_ID, XslNames.PRICE, Param.Type.NUMERIC);
        value2.setNumericValue(BigDecimal.valueOf(2));
        ParameterValue value12 = createParameterValue(DEFAULT_ID, XslNames.PRICE, Param.Type.NUMERIC);
        value12.setNumericValue(BigDecimal.valueOf(12));
        ParameterValue value34 = createParameterValue(DEFAULT_ID, XslNames.PRICE, Param.Type.NUMERIC);
        value34.setNumericValue(BigDecimal.valueOf(34));

        when(context.getParameterBounds(CATEGORY_HID, XslNames.PRICE)).thenReturn(
            new NumericBounds(BigDecimal.ZERO, BigDecimal.TEN)
        );

        CommonModel model = createModel(value0, value2, value12, value34);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );

        assertThat(errors).containsExactlyInAnyOrder(
            outOfBoundsError(model, true, value0, "12, 34")
        );

        errors = validator.validate(
            context,
            new ModelChanges(model, model),
            Collections.emptyList()
        );


        assertThat(errors).containsExactlyInAnyOrder(
            outOfBoundsError(model, false, value0, "12, 34")
        );
    }

    @Test
    public void testValueOutOfBoundsCategoryChanged() {
        ParameterValue value0 = createParameterValue(DEFAULT_ID, XslNames.PRICE, Param.Type.NUMERIC);
        value0.setNumericValue(BigDecimal.ZERO);
        ParameterValue value2 = createParameterValue(DEFAULT_ID, XslNames.PRICE, Param.Type.NUMERIC);
        value2.setNumericValue(BigDecimal.valueOf(2));
        ParameterValue value12 = createParameterValue(DEFAULT_ID, XslNames.PRICE, Param.Type.NUMERIC);
        value12.setNumericValue(BigDecimal.valueOf(12));
        ParameterValue value34 = createParameterValue(DEFAULT_ID, XslNames.PRICE, Param.Type.NUMERIC);
        value34.setNumericValue(BigDecimal.valueOf(34));

        when(context.getParameterBounds(CATEGORY_HID, XslNames.PRICE)).thenReturn(
            new NumericBounds(BigDecimal.ZERO, BigDecimal.TEN)
        );

        CommonModel model = createModel(value0, value2, value12, value34);
        CommonModel chandedCategorymodel = new CommonModel(model);
        model.setCategoryId(123456L);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(model, chandedCategorymodel),
            Collections.emptyList()
        );

        assertThat(errors).containsExactlyInAnyOrder(
            outOfBoundsError(chandedCategorymodel, true, value0, "12, 34")
        );
    }

    @Test
    public void testNullNumericValue() {
        ParameterValue valueNull = createParameterValue(DEFAULT_ID, XslNames.PRICE, Param.Type.NUMERIC);
        valueNull.setNumericValue(null);
        ParameterValue value0 = createParameterValue(DEFAULT_ID, XslNames.PRICE, Param.Type.NUMERIC);
        value0.setNumericValue(BigDecimal.ZERO);
        ParameterValue value2 = createParameterValue(DEFAULT_ID, XslNames.PRICE, Param.Type.NUMERIC);
        value2.setNumericValue(BigDecimal.valueOf(2));
        ParameterValue value12 = createParameterValue(DEFAULT_ID, XslNames.PRICE, Param.Type.NUMERIC);
        value12.setNumericValue(BigDecimal.valueOf(12));

        when(context.getParameterBounds(CATEGORY_HID, XslNames.PRICE)).thenReturn(
            new NumericBounds(BigDecimal.ZERO, BigDecimal.TEN)
        );
        CommonModel model = createModel(valueNull, value0, value2, value12);

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );

        assertThat(errors).containsExactlyInAnyOrder(
            outOfBoundsError(model, true, value0, "null, 12")
        );
    }

    @Test
    public void testNotExistingOptionId() {
        ParameterValue value0 = createParameterValue(DEFAULT_ID, "xslName0", Param.Type.ENUM);
        value0.setOptionId(100L);
        ParameterValue value1 = createParameterValue(DEFAULT_ID + 1, "xslName1", Param.Type.NUMERIC_ENUM);
        value1.setOptionId(101L);
        ParameterValue value2 = createParameterValue(DEFAULT_ID + 2, "xslName2", Param.Type.BOOLEAN);
        value2.setBooleanValue(false);
        value2.setOptionId(102L);

        // For value 3 no error will return, because it doesn't match definition
        ParameterValue value3 = createParameterValue(DEFAULT_ID + 3, "xslName3", Param.Type.ENUM);
        value3.setOptionId(103L);
        // Don't process vendor xsl-name, because we have other validators for it
        ParameterValue value4 = createParameterValue(DEFAULT_ID + 4, XslNames.VENDOR, Param.Type.ENUM);
        value4.setOptionId(666L);

        when(context.getOptionNames(Mockito.eq(CATEGORY_HID), Mockito.anyLong())).thenReturn(
            ImmutableMap.of(1L, "option_1", 2L, "option_2")
        );
        when(context.parameterMatchesDefinition(anyLong(), Mockito.eq(value3))).thenReturn(false);
        CommonModel model = createModel(value0, value1, value2, value3, value4);

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.singleton(model)
        );
        assertThat(errors).containsExactlyInAnyOrder(
            invalidOptionId(model, true, value0),
            invalidOptionId(model, true, value1),
            invalidOptionId(model, true, value2),
            definitionMismatchError(model, true, value3)
        );

        errors = validator.validate(
            context,
            new ModelChanges(model, model),
            Collections.singleton(model)
        );
        assertThat(errors).containsExactlyInAnyOrder(
            invalidOptionId(model, false, value0),
            invalidOptionId(model, false, value1),
            invalidOptionId(model, false, value2),
            definitionMismatchError(model, false, value3)
        );
    }

    @Test
    public void testNoCheckDefinitionsInPartnerModel() {
        when(context.getParameterBounds(CATEGORY_HID, XslNames.PRICE)).thenReturn(
            new NumericBounds(BigDecimal.ONE, BigDecimal.TEN)
        );
        when(context.getStringParamMaxLength(anyLong(), anyString())).thenReturn(1);

        ParameterValue tooLongValue = createParameterValue(DEFAULT_ID + 1, "anyStringParam", Param.Type.STRING);
        tooLongValue.setStringValue(new ArrayList<>());
        tooLongValue.getStringValue().add(new Word(DEFAULT_LANG_ID, "foobar"));
        ParameterValue outOfBoundsValue = createParameterValue(DEFAULT_ID + 2, XslNames.PRICE, Param.Type.NUMERIC);
        outOfBoundsValue.setNumericValue(BigDecimal.ZERO);

        CommonModel model = createModel(tooLongValue, outOfBoundsValue);
        model.setCurrentType(CommonModel.Source.PARTNER);

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).isEmpty();

        errors = validator.validate(context,
            new ModelChanges(model, model),
            Collections.emptyList()
        );
        assertThat(errors).isEmpty();
    }

    @Test
    public void testRequiredChecksInPartnerModel() {
        when(context.parameterMatchesDefinition(anyLong(), any())).thenAnswer(invoc -> {
            ParameterValue value = invoc.getArgument(1);
            return value.getParamId() != DEFAULT_ID + 5;
        });
        when(context.getParameterBounds(CATEGORY_HID, XslNames.PRICE)).thenReturn(
            new NumericBounds(BigDecimal.ZERO, BigDecimal.TEN)
        );
        when(context.getStringParamMaxLength(anyLong(), anyString())).thenReturn(1);

        ParameterValue zeroIdValue = createDefaultParameterValue(0L);

        ParameterValue nullXlsValue = createDefaultParameterValue(DEFAULT_ID);
        nullXlsValue.setXslName(null);

        ParameterValue nullTypeValue = createDefaultParameterValue(DEFAULT_ID + 1);
        nullTypeValue.setType(null);

        ParameterValue emptyOptionBooleanValue = createParameterValue(DEFAULT_ID + 2, XslNames.URL, Param.Type.BOOLEAN);
        emptyOptionBooleanValue.setBooleanValue(true);

        ParameterValue emptyStringListValue = createParameterValue(DEFAULT_ID + 3, XslNames.URL, Param.Type.STRING);
        ParameterValue stringListWithEmptyValue = createParameterValue(DEFAULT_ID + 4, XslNames.URL, Param.Type.STRING);
        stringListWithEmptyValue.setStringValue(new ArrayList<Word>() {{
            add(new Word());
        }});
        ParameterValue invalidDefinition = createParameterValue(DEFAULT_ID + 5, XslNames.URL, Param.Type.ENUM);
        invalidDefinition.setOptionId(-1L);

        ParameterValue invalidSymbolsValue = createParameterValue(DEFAULT_ID + 6, DEFAULT_XSL_NAME, Param.Type.STRING);
        invalidSymbolsValue.setStringValue(new ArrayList<>());
        invalidSymbolsValue.getStringValue().add(new Word(DEFAULT_LANG_ID + 7, "qwerrqwe" + (char) TEST_HEX_NUMBER));

        ParameterValue nonUniqueValue = createParameterValue(DEFAULT_ID + 8, DEFAULT_XSL_NAME, Param.Type.STRING);
        nonUniqueValue.setStringValue(new ArrayList<>());
        nonUniqueValue.getStringValue().add(new Word(DEFAULT_LANG_ID, "qwerty"));
        nonUniqueValue.getStringValue().add(new Word(DEFAULT_LANG_ID, "qwerty"));
        nonUniqueValue.getStringValue().add(new Word(DEFAULT_LANG_ID, "123"));

        ParameterValue enumWithInvalidOptionId = createParameterValue(DEFAULT_ID + 9, "enum_param", Param.Type.ENUM);
        enumWithInvalidOptionId.setOptionId(100000L); // should be non existing

        CommonModel model = createModel(zeroIdValue, nullXlsValue, nullTypeValue, emptyOptionBooleanValue,
            emptyStringListValue, invalidDefinition, stringListWithEmptyValue, invalidSymbolsValue, nonUniqueValue,
            enumWithInvalidOptionId);
        model.setCurrentType(CommonModel.Source.PARTNER);

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            missingIdError(model, true, zeroIdValue),
            missingXslNameError(model, true, nullXlsValue),
            missingTypeError(model, true, nullTypeValue),
            invalidBooleanValueError(model, true, emptyOptionBooleanValue),
            emptyValueError(model, true, emptyStringListValue),
            stringValueEmptyError(model, true, stringListWithEmptyValue),
            definitionMismatchError(model, true, invalidDefinition),
            invalidStringError(model, true, invalidSymbolsValue),
            nonUniqueValue(model, true, nonUniqueValue),
            invalidOptionId(model, true, enumWithInvalidOptionId)
        );
        errors = validator.validate(context,
            new ModelChanges(model, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            missingIdError(model, false, zeroIdValue),
            missingXslNameError(model, false, nullXlsValue),
            missingTypeError(model, true, nullTypeValue),
            invalidBooleanValueError(model, false, emptyOptionBooleanValue),
            emptyValueError(model, false, emptyStringListValue),
            stringValueEmptyError(model, false, stringListWithEmptyValue),
            definitionMismatchError(model, false, invalidDefinition),
            invalidStringError(model, false, invalidSymbolsValue),
            nonUniqueValue(model, false, nonUniqueValue),
            invalidOptionId(model, false, enumWithInvalidOptionId)
        );
    }

    private ModelValidationError createError(CommonModel model, ModelValidationError.ErrorSubtype subtype,
                                             boolean critical, boolean allowForce, ParameterValue parameterValue) {
        ModelValidationError error = new ModelValidationError(
            model.getId(), ModelValidationError.ErrorType.INVALID_PARAMETER_VALUE, subtype, critical, allowForce);
        if (parameterValue.getParamId() != CommonModel.NO_ID) {
            error.addParam(ModelStorage.ErrorParamName.PARAMETER_NAME, "ParamName" + parameterValue.getParamId());
        } else {
            error.addParam(ModelStorage.ErrorParamName.PARAMETER_NAME, "ParamName" + parameterValue.getXslName());
        }
        if (parameterValue.getXslName() != null) {
            error.addParam(ModelStorage.ErrorParamName.PARAM_XSL_NAME, parameterValue.getXslName());
        }
        if (parameterValue.getParamId() != CommonModel.NO_ID) {
            error.addParam(ModelStorage.ErrorParamName.PARAM_ID, parameterValue.getParamId());
        }
        return error;
    }

    private ModelValidationError createError(CommonModel model, ModelValidationError.ErrorSubtype subtype,
                                             boolean critical, ParameterValue parameterValue) {
        return createError(model, subtype, critical, false, parameterValue);
    }

    private ModelValidationError missingXslNameError(CommonModel model, boolean critical,
                                                     ParameterValue parameterValue) {
        return createError(model, ModelValidationError.ErrorSubtype.MISSING_XSL_NAME, critical, parameterValue)
            .addLocalizedMessagePattern("Пустой xslName у параметра '%{PARAMETER_NAME}'(%{PARAM_ID})");
    }

    private ModelValidationError missingIdError(CommonModel model, boolean critical,
                                                ParameterValue parameterValue) {
        return createError(model, ModelValidationError.ErrorSubtype.MISSING_ID, critical, parameterValue)
            .addLocalizedMessagePattern("Пустой id у параметра %{PARAMETER_NAME}'(%{PARAM_XSL_NAME}).");
    }

    private ModelValidationError missingTypeError(CommonModel model, boolean critical,
                                                  ParameterValue parameterValue) {
        return createError(model, ModelValidationError.ErrorSubtype.MISSING_TYPE, critical, parameterValue)
            .addLocalizedMessagePattern("Тип параметра '%{PARAMETER_NAME}'(%{PARAM_ID}) не определен.");
    }

    private ModelValidationError missingSourceError(CommonModel model, boolean critical,
                                                    ParameterValue parameterValue) {
        return createError(model, ModelValidationError.ErrorSubtype.MISSING_MODIFICATION_SOURCE,
            critical, parameterValue)
            .addLocalizedMessagePattern("Пустой источник изменения у параметра '%{PARAMETER_NAME}'(%{PARAM_ID}).");
    }

    private ModelValidationError definitionMismatchError(CommonModel model, boolean critical,
                                                         ParameterValue parameterValue) {
        return createError(model, ModelValidationError.ErrorSubtype.DEFINITION_MISMATCH, critical,
            true, parameterValue)
            .addParam(ModelStorage.ErrorParamName.PARAM_VALUE,
                ParameterValueStringBuilder.createDefaultBuilder().toString(parameterValue))
            .addLocalizedMessagePattern(DEFINITION_MISMATCH_ERROR_LMP);
    }

    private ModelValidationError hypothesisValueError(CommonModel model, boolean critical,
                                                      ParameterValue parameterValue) {
        return createError(model, ModelValidationError.ErrorSubtype.HYPOTHESIS_VALUE, critical, parameterValue)
            .addLocalizedMessagePattern(
                "Параметр '%{PARAMETER_NAME}'(%{PARAM_ID}) содержит гипотезу в качестве значения.");
    }

    private ModelValidationError stringValueEmptyError(CommonModel model, boolean critical,
                                                       ParameterValue parameterValue) {
        return createError(model, ModelValidationError.ErrorSubtype.STRING_VALUE_EMPTY, critical, parameterValue)
            .addLocalizedMessagePattern(
                "Параметр '%{PARAMETER_NAME}'(%{PARAM_ID}) содержит пустое строковое значение.");
    }

    private ModelValidationError invalidStringError(CommonModel model, boolean critical,
                                                    ParameterValue parameterValue) {
        return createError(model, ModelValidationError.ErrorSubtype.STRING_VALUE_INVALID, critical, parameterValue)
            .addLocalizedMessagePattern(
                "Значение параметра '%{PARAMETER_NAME}'(%{PARAM_ID}) содержит некорректные символы.");
    }

    private ModelValidationError nonUniqueValue(CommonModel model, boolean critical,
                                                ParameterValue parameterValue) {
        String duplicates = parameterValue.getStringValue().stream()
            .map(Word::getWord)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.joining(", "));
        return createError(model, ModelValidationError.ErrorSubtype.NON_UNIQUE_VALUE, critical, parameterValue)
            .addParam(ModelStorage.ErrorParamName.PARAM_VALUE, duplicates)
            .addLocalizedMessagePattern(
                "Значения '%{PARAM_VALUE}' параметра '%{PARAMETER_NAME}'(%{PARAM_ID}) дублируются.");
    }

    private ModelValidationError stringTooLongError(CommonModel model, boolean critical,
                                                    ParameterValue parameterValue, String maxLength) {
        return createError(model, ModelValidationError.ErrorSubtype.VALUE_TOO_LONG, critical, parameterValue)
            .addLocalizedMessagePattern(
                "Длина значения параметра '%{PARAMETER_NAME}'(%{PARAM_ID}) не должна превышать %{MAX_LENGTH} символов.")
            .addParam(ModelStorage.ErrorParamName.MAX_LENGTH, maxLength);
    }

    private ModelValidationError invalidBooleanValueError(CommonModel model, boolean critical,
                                                          ParameterValue parameterValue) {
        return createError(model, ModelValidationError.ErrorSubtype.BOOLEAN_VALUE_WITHOUT_OPTION_ID,
            critical, parameterValue)
            .addLocalizedMessagePattern("Для булевого параметра '%{PARAMETER_NAME}'(%{PARAM_ID}) не указан optionId.");
    }

    private ModelValidationError emptyValueError(CommonModel model, boolean critical,
                                                 ParameterValue parameterValue) {
        return createError(model, ModelValidationError.ErrorSubtype.MISSING_VALUE,
            critical, parameterValue)
            .addLocalizedMessagePattern("Пустое значение у параметра '%{PARAMETER_NAME}'(%{PARAM_ID}).");
    }

    private ModelValidationError outOfBoundsError(CommonModel model, boolean critical,
                                                  ParameterValue parameterValue, String values) {
        return createError(model, ModelValidationError.ErrorSubtype.OUT_OF_BOUNDS_VALUE,
            critical, parameterValue)
            .addLocalizedMessagePattern("Значения '%{PARAM_VALUE}' параметра '%{PARAMETER_NAME}'(%{PARAM_ID}) " +
                "выходят за допустимые границы. Значения должны быть %{VALUE_BOUNDS}")
            .addParam(ModelStorage.ErrorParamName.PARAM_VALUE, values)
            .addParam(ModelStorage.ErrorParamName.VALUE_BOUNDS, "не меньше 0 и не больше 10");
    }

    private ModelValidationError invalidOptionId(CommonModel model, boolean critical,
                                                 ParameterValue parameterValue) {
        return createError(model, ModelValidationError.ErrorSubtype.INVALID_OPTION_ID,
            critical, parameterValue)
            .addLocalizedMessagePattern("Option id '%{OPTION_ID}' параметра '%{PARAMETER_NAME}'(%{PARAM_ID}) " +
                "не существует. Проверьте корректность введенных значений.")
            .addParam(ModelStorage.ErrorParamName.OPTION_ID, parameterValue.getOptionId());
    }

    private CommonModel createModel(ParameterValue... params) {
        CommonModel model = new CommonModel();
        model.setCategoryId(CATEGORY_HID);
        for (ParameterValue value : params) {
            model.addParameterValue(value);
        }
        model.setCurrentType(CommonModel.Source.GURU);
        return model;
    }

    private static ParameterValue createParameterValue(long id, String xslName, Param.Type type) {
        ParameterValue result = new ParameterValue();
        result.setParamId(id);
        result.setXslName(xslName);
        result.setType(type);
        result.setModificationSource(ModificationSource.OPERATOR_FILLED);
        result.setLastModificationDate(new Date());
        result.setLastModificationUid(USER_ID);
        return result;
    }

    private static ParameterValue createDefaultParameterValue(long id) {
        ParameterValue result = createParameterValue(id, DEFAULT_XSL_NAME, Param.Type.NUMERIC);
        result.setNumericValue(new BigDecimal(PARAMETER_VALUE));
        return result;
    }
}
