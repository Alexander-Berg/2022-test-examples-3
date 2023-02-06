package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.common.model.KnownIds.BARCODE_ID;
import static ru.yandex.market.mbo.common.model.KnownIds.FORMER_BARCODES_ID;

public class BarCodeRenewalValidatorTest extends BaseValidatorTestClass {

    public static final long MODEL_ID = 1L;
    public static final String VALUE_1 = "82736487";
    public static final String VALUE_2 = "23479874";
    public static final String VALUE_3 = "61523465";

    private BarCodeRenewalValidator validator;

    @Before
    public void init() {
        validator = new BarCodeRenewalValidator();
    }

    @Test
    public void validRemoving() {
        CommonModel before = createModel(MODEL_ID, CommonModel.Source.GURU, cb ->
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.AUTO, VALUE_1, VALUE_2));

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_1));
        ParameterValue pv = new ParameterValue(FORMER_BARCODES_ID, XslNames.FORMER_BARCODES, Param.Type.STRING);
        pv.setStringValue(WordUtil.defaultWords(VALUE_1));
        after.putParameterValues(ParameterValues.of(pv));

        context.setOperationSource(ModificationSource.ASSESSOR);

        List<ModelValidationError> errors =
            validator.validate(context, new ModelChanges(before, after), Collections.singletonList(after));

        assertThat(errors).isEmpty();
    }

    @Test
    public void invalidRemoving() {
        CommonModel before = createModel(MODEL_ID, CommonModel.Source.GURU, cb ->
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.OPERATOR_CONFIRMED, VALUE_1, VALUE_2));

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_1));

        List<ModelValidationError> errors =
            validator.validate(context, new ModelChanges(before, after), Collections.singletonList(after));

        assertThat(errors).containsExactlyInAnyOrder(
            validator.createInvalidRemovingError(context, before, after.getId(), VALUE_1));
    }

    @Test
    public void validAdding() {
        CommonModel before = createModel(MODEL_ID, CommonModel.Source.GURU, cb ->
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.ASSESSOR, VALUE_1, VALUE_2));

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().add(WordUtil.defaultWord(VALUE_3));

        List<ModelValidationError> errors =
            validator.validate(context, new ModelChanges(before, after), Collections.singletonList(after));

        assertThat(errors).isEmpty();
    }

    @Test
    public void invalidAdding() {
        CommonModel before = createModel(MODEL_ID, CommonModel.Source.GURU, cb -> {
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.ASSESSOR, VALUE_1, VALUE_2);
            cb.parameterValues(FORMER_BARCODES_ID, XslNames.FORMER_BARCODES,
                ModificationSource.OPERATOR_CONFIRMED, VALUE_3);
        });

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().add(WordUtil.defaultWord(VALUE_3));

        List<ModelValidationError> errors =
            validator.validate(context, new ModelChanges(before, after), Collections.singletonList(after));

        assertThat(errors).containsExactlyInAnyOrder(
            validator.createInvalidAddingError(context, before, after.getId(), VALUE_3));
    }

    @Test
    public void validAddingAndValidRemoving() {
        CommonModel before = createModel(MODEL_ID, CommonModel.Source.GURU, cb -> {
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.ASSESSOR, VALUE_1, VALUE_2);
            cb.parameterValues(FORMER_BARCODES_ID, XslNames.FORMER_BARCODES,
                ModificationSource.OPERATOR_CONFIRMED, VALUE_3);
        });

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_1));
        after.getSingleParameterValue(FORMER_BARCODES_ID).getStringValue().add(WordUtil.defaultWord(VALUE_1));
        after.getSingleParameterValue(BARCODE_ID).getStringValue().add(WordUtil.defaultWord(VALUE_3));
        after.getSingleParameterValue(FORMER_BARCODES_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_3));

        List<ModelValidationError> errors =
            validator.validate(context, new ModelChanges(before, after), Collections.singletonList(after));

        assertThat(errors).isEmpty();
    }

    @Test
    public void invalidAddingAndInvalidRemoving() {
        CommonModel before = createModel(MODEL_ID, CommonModel.Source.GURU, cb -> {
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.ASSESSOR, VALUE_1, VALUE_2);
            cb.parameterValues(FORMER_BARCODES_ID, XslNames.FORMER_BARCODES,
                ModificationSource.OPERATOR_CONFIRMED, VALUE_3);
        });

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_1));
        after.getSingleParameterValue(BARCODE_ID).getStringValue().add(WordUtil.defaultWord(VALUE_3));

        List<ModelValidationError> errors =
            validator.validate(context, new ModelChanges(before, after), Collections.singletonList(after));

        assertThat(errors).containsExactlyInAnyOrder(
            validator.createInvalidAddingError(context, before, after.getId(), VALUE_3),
            validator.createInvalidRemovingError(context, before, after.getId(), VALUE_1));
    }

    @Test
    public void validAddingAndInvalidRemoving() {
        CommonModel before = createModel(MODEL_ID, CommonModel.Source.GURU, cb -> {
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.OPERATOR_CONFIRMED, VALUE_1, VALUE_2);
            cb.parameterValues(FORMER_BARCODES_ID, XslNames.FORMER_BARCODES,
                ModificationSource.VENDOR_OFFICE, VALUE_3);
        });

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_1));
        after.getSingleParameterValue(BARCODE_ID).getStringValue().add(WordUtil.defaultWord(VALUE_3));
        after.getSingleParameterValue(FORMER_BARCODES_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_3));

        List<ModelValidationError> errors =
            validator.validate(context, new ModelChanges(before, after), Collections.singletonList(after));

        assertThat(errors).containsExactlyInAnyOrder(
            validator.createInvalidRemovingError(context, before, after.getId(), VALUE_1));
    }

    @Test
    public void invalidAddingAndValidRemoving() {
        CommonModel before = createModel(MODEL_ID, CommonModel.Source.GURU, cb -> {
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.VENDOR_OFFICE, VALUE_1, VALUE_2);
            cb.parameterValues(FORMER_BARCODES_ID, XslNames.FORMER_BARCODES,
                ModificationSource.OPERATOR_CONFIRMED, VALUE_3);
        });

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_1));
        after.getSingleParameterValue(FORMER_BARCODES_ID).getStringValue().add(WordUtil.defaultWord(VALUE_1));
        after.getSingleParameterValue(BARCODE_ID).getStringValue().add(WordUtil.defaultWord(VALUE_3));

        List<ModelValidationError> errors =
            validator.validate(context, new ModelChanges(before, after), Collections.singletonList(after));

        assertThat(errors).containsExactlyInAnyOrder(
            validator.createInvalidAddingError(context, before, after.getId(), VALUE_3));
    }
}
