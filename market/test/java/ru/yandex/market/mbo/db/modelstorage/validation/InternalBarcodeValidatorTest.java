package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorSubtype;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorType;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class InternalBarcodeValidatorTest {
    private static final long USER_ID = 100500;
    private static final long CATEGORY_ID = 100500;

    private InternalBarcodeValidator validator;

    @Mock
    private ModelValidationContext context;

    @Before
    public void setup() {
        validator = new InternalBarcodeValidator();
    }

    @Test
    public void testInternalBarcode() {
        ParameterValue invalidBarcode = createParameterValue(CATEGORY_ID, XslNames.BAR_CODE, Param.Type.STRING);
        invalidBarcode.setStringValue(new ArrayList<>());
        invalidBarcode.getStringValue().add(new Word(Word.DEFAULT_LANG_ID, "2001234567890"));
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, createModel(invalidBarcode)),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(0L,
                ModelValidationError.ErrorType.INVALID_BARCODE,
                ModelValidationError.ErrorSubtype.BARCODE_IS_NOT_PUBLIC, true)
                .addLocalizedMessagePattern("Некорректный баркод '%{PARAM_VALUE}'. " +
                    "13-значные баркоды, начинающиеся с '2', являются нерегистрируемыми баркодами, " +
                    "предназначенными для внутреннего использования на складах и предприятиях.")
                .addParam(ModelStorage.ErrorParamName.PARAM_VALUE, "2001234567890")
        );
    }

    @Test
    public void testLongBarcode() {
        ParameterValue invalidBarcode = createParameterValue(CATEGORY_ID, XslNames.BAR_CODE, Param.Type.STRING);
        invalidBarcode.setStringValue(new ArrayList<>());
        invalidBarcode.getStringValue().add(new Word(Word.DEFAULT_LANG_ID, "1001234567890000"));
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, createModel(invalidBarcode)),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(0L,
                ErrorType.INVALID_PARAMETER_VALUE,
                ErrorSubtype.VALUE_TOO_LONG, true)
                .addLocalizedMessagePattern("Некорректный баркод '%{PARAM_VALUE}'. " +
                    "Баркод не должен превышать " + InternalBarcodeValidator.GTIN_14_BARCODE_LENGTH + " символов.")
                .addParam(ModelStorage.ErrorParamName.PARAM_VALUE, "1001234567890000")
        );
    }

    @Test
    public void testAlphabeticalBarcode() {
        ParameterValue invalidBarcode = createParameterValue(CATEGORY_ID, XslNames.BAR_CODE, Param.Type.STRING);
        invalidBarcode.setStringValue(new ArrayList<>());
        invalidBarcode.getStringValue().add(new Word(Word.DEFAULT_LANG_ID, "46803a6yh2"));
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, createModel(invalidBarcode)),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(0L,
                ErrorType.INVALID_PARAMETER_VALUE,
                ErrorSubtype.STRING_VALUE_INVALID, true)
                .addLocalizedMessagePattern("Некорректный баркод '%{PARAM_VALUE}'. " +
                    "Баркод должен содержать только цифры.")
                .addParam(ModelStorage.ErrorParamName.PARAM_VALUE, "46803a6yh2")
        );
    }

    @Test
    public void testValidEAN13BarcodeFormat() {
        ParameterValue validBarcode = createParameterValue(CATEGORY_ID, XslNames.BAR_CODE, Param.Type.STRING);
        validBarcode.setStringValue(new ArrayList<>());
        validBarcode.getStringValue().add(new Word(Word.DEFAULT_LANG_ID, "1231234567890"));
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, createModel(validBarcode)),
            Collections.emptyList()
        );
        assertEquals(0, errors.size());
    }

    @Test
    public void testValidGTIN14BarcodeFormat() {
        ParameterValue validBarcode = createParameterValue(CATEGORY_ID, XslNames.BAR_CODE, Param.Type.STRING);
        validBarcode.setStringValue(new ArrayList<>());
        validBarcode.getStringValue().add(new Word(Word.DEFAULT_LANG_ID, "12312345678909"));
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, createModel(validBarcode)),
            Collections.emptyList()
        );
        assertEquals(0, errors.size());
    }

    @Test
    public void whenNotChangedWarn() {
        ParameterValue invalidBarcode = createParameterValue(CATEGORY_ID, XslNames.BAR_CODE, Param.Type.STRING);
        invalidBarcode.setStringValue(new ArrayList<>());
        invalidBarcode.getStringValue().add(new Word(Word.DEFAULT_LANG_ID, "46803a6yh2"));
        CommonModel unchangedModel = createModel(invalidBarcode);
        assertThat(validator.validate(
            context,
            new ModelChanges(unchangedModel, unchangedModel),
            Collections.emptyList()
        )).containsExactlyInAnyOrder(
            new ModelValidationError(unchangedModel.getId(),
                ErrorType.INVALID_PARAMETER_VALUE,
                ErrorSubtype.STRING_VALUE_INVALID, false) // <-- warn
                .addLocalizedMessagePattern("Некорректный баркод '%{PARAM_VALUE}'. " +
                    "Баркод должен содержать только цифры.")
                .addParam(ModelStorage.ErrorParamName.PARAM_VALUE, "46803a6yh2")
        );
    }

    @Test
    public void whenChangedCrit() {
        ParameterValue validBarcode = createParameterValue(CATEGORY_ID, XslNames.BAR_CODE, Param.Type.STRING);
        validBarcode.setStringValue(new ArrayList<>());
        validBarcode.getStringValue().add(new Word(Word.DEFAULT_LANG_ID, "1231234567890"));

        ParameterValue invalidBarcode = createParameterValue(CATEGORY_ID, XslNames.BAR_CODE, Param.Type.STRING);
        invalidBarcode.setStringValue(new ArrayList<>());
        invalidBarcode.getStringValue().add(new Word(Word.DEFAULT_LANG_ID, "46803a6yh2"));

        CommonModel goodModel = createModel(validBarcode);
        CommonModel badModel = createModel(invalidBarcode);
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(goodModel, badModel),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(badModel.getId(),
                ErrorType.INVALID_PARAMETER_VALUE,
                ErrorSubtype.STRING_VALUE_INVALID, true)
                .addLocalizedMessagePattern("Некорректный баркод '%{PARAM_VALUE}'. " +
                    "Баркод должен содержать только цифры.")
                .addParam(ModelStorage.ErrorParamName.PARAM_VALUE, "46803a6yh2")
        );
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

    private CommonModel createModel(ParameterValue... params) {
        CommonModel model = new CommonModel();
        model.setCategoryId(CATEGORY_ID);
        for (ParameterValue value : params) {
            model.addParameterValue(value);
        }
        model.setCurrentType(CommonModel.Source.GURU);
        return model;
    }
}
