package ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.common.model.KnownIds.BARCODE_ID;
import static ru.yandex.market.mbo.common.model.KnownIds.FORMER_BARCODES_ID;

public class BarCodeRenewalPreprocessorTest extends BasePreprocessorTest {

    public static final long MODEL_ID = 1L;
    public static final String VALUE_1 = "723654765";
    public static final String VALUE_2 = "541336231";
    public static final String VALUE_3 = "354218323";
    public static final String VALUE_4 = "928374657";

    private BarCodeRenewalPreprocessor preprocessor;

    @Before
    public void init() {
        super.before();
        preprocessor = new BarCodeRenewalPreprocessor();
    }

    @Test
    public void validRemoving() {
        CommonModel before = model(MODEL_ID, cb ->
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.AUTO, VALUE_1, VALUE_2));

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_1));

        preprocessor.preprocess(getModelSaveGroup(before, after), modelSaveContext);

        ParameterValue fpv = after.getSingleParameterValue(FORMER_BARCODES_ID);
        assertThat(fpv).isNotNull();

        assertThat(fpv.getStringValue()).isNotNull().containsExactlyInAnyOrder(WordUtil.defaultWord(VALUE_1));
    }

    @Test
    public void validRemovingWrongModel() {
        // @formatter:off
        CommonModel before = model(MODEL_ID, cb -> cb
            .startParameterValue()
                .paramId(BARCODE_ID)
                .xslName(XslNames.BAR_CODE)
                .type(Param.Type.STRING)
                .words(VALUE_1, VALUE_2)
            .endParameterValue()
            .startParameterValue()
                .paramId(FORMER_BARCODES_ID)
                .xslName(XslNames.FORMER_BARCODES)
                .type(Param.Type.STRING)
                .words(VALUE_3)
            .endParameterValue()
            .startParameterValue()
                .paramId(FORMER_BARCODES_ID)
                .xslName(XslNames.FORMER_BARCODES)
                .type(Param.Type.STRING)
                .words(VALUE_4)
            .endParameterValue());
        // @formatter:on

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_1));

        preprocessor.preprocess(getModelSaveGroup(before, after), modelSaveContext);

        ParameterValues fpv = after.getParameterValues(FORMER_BARCODES_ID);
        assertThat(fpv).isNotNull();
        List<ParameterValue> formerPvList = fpv.getValues();
        assertThat(formerPvList.size()).isEqualTo(1);
        assertThat(formerPvList.get(0).getStringValue()).containsExactlyInAnyOrder(
            WordUtil.defaultWord(VALUE_1),
            WordUtil.defaultWord(VALUE_3),
            WordUtil.defaultWord(VALUE_4)
        );
    }

    @Test
    public void invalidRemoving() {
        CommonModel before = model(MODEL_ID, cb -> {
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.OPERATOR_CONFIRMED, VALUE_1, VALUE_2);
            cb.parameterValues(FORMER_BARCODES_ID, XslNames.FORMER_BARCODES,
                ModificationSource.OPERATOR_CONFIRMED, VALUE_3);
        });

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_1));

        modelSaveContext.setOperationSource(ModificationSource.ASSESSOR);

        preprocessor.preprocess(getModelSaveGroup(before, after), modelSaveContext);

        ParameterValue fpv = after.getSingleParameterValue(FORMER_BARCODES_ID);
        assertThat(fpv).isNotNull();
        assertThat(fpv.getStringValue()).isNotNull().containsExactlyInAnyOrder(WordUtil.defaultWord(VALUE_3));
    }

    @Test
    public void validAdding() {
        CommonModel before = model(MODEL_ID, cb -> {
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.ASSESSOR, VALUE_1);
            cb.parameterValues(FORMER_BARCODES_ID, XslNames.FORMER_BARCODES,
                ModificationSource.ASSESSOR, VALUE_2, VALUE_3);
        });

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().add(WordUtil.defaultWord(VALUE_2));

        modelSaveContext.setOperationSource(ModificationSource.ASSESSOR);

        preprocessor.preprocess(getModelSaveGroup(before, after), modelSaveContext);

        ParameterValue fpv = after.getSingleParameterValue(FORMER_BARCODES_ID);
        assertThat(fpv).isNotNull();
        assertThat(fpv.getStringValue()).isNotNull().containsExactlyInAnyOrder(WordUtil.defaultWord(VALUE_3));

        after.getSingleParameterValue(BARCODE_ID).getStringValue().add(WordUtil.defaultWord(VALUE_3));

        preprocessor.preprocess(getModelSaveGroup(before, after), modelSaveContext);

        assertThat(after.getSingleParameterValue(FORMER_BARCODES_ID)).isNull();
    }

    @Test
    public void invalidAdding() {
        CommonModel before = model(MODEL_ID, cb -> {
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.ASSESSOR, VALUE_1);
            cb.parameterValues(FORMER_BARCODES_ID, XslNames.FORMER_BARCODES,
                ModificationSource.OPERATOR_FILLED, VALUE_2, VALUE_3);
        });

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().add(WordUtil.defaultWord(VALUE_3));

        modelSaveContext.setOperationSource(ModificationSource.ASSESSOR);

        preprocessor.preprocess(getModelSaveGroup(before, after), modelSaveContext);

        ParameterValue fpv = after.getSingleParameterValue(FORMER_BARCODES_ID);
        assertThat(fpv).isNotNull();
        assertThat(fpv.getStringValue()).isNotNull()
            .containsExactlyInAnyOrder(WordUtil.defaultWord(VALUE_2), WordUtil.defaultWord(VALUE_3));
    }

    @Test
    public void validAddingAndValidRemoving() {
        CommonModel before = model(MODEL_ID, cb -> {
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.ASSESSOR, VALUE_1, VALUE_2);
            cb.parameterValues(FORMER_BARCODES_ID, XslNames.FORMER_BARCODES, ModificationSource.ASSESSOR, VALUE_3);
        });

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().add(WordUtil.defaultWord(VALUE_3));
        after.getSingleParameterValue(BARCODE_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_1));

        modelSaveContext.setOperationSource(ModificationSource.ASSESSOR);

        preprocessor.preprocess(getModelSaveGroup(before, after), modelSaveContext);

        ParameterValue fpv = after.getSingleParameterValue(FORMER_BARCODES_ID);
        assertThat(fpv).isNotNull();
        assertThat(fpv.getStringValue()).isNotEmpty().containsExactlyInAnyOrder(WordUtil.defaultWord(VALUE_1));
    }

    @Test
    public void invalidAddingAndInvalidRemoving() {
        CommonModel before = model(MODEL_ID, cb -> {
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.OPERATOR_FILLED, VALUE_1, VALUE_2);
            cb.parameterValues(FORMER_BARCODES_ID, XslNames.FORMER_BARCODES,
                ModificationSource.OPERATOR_FILLED, VALUE_3);
        });

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().add(WordUtil.defaultWord(VALUE_3));
        after.getSingleParameterValue(BARCODE_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_1));

        modelSaveContext.setOperationSource(ModificationSource.ASSESSOR);

        preprocessor.preprocess(getModelSaveGroup(before, after), modelSaveContext);

        ParameterValue fpv = after.getSingleParameterValue(FORMER_BARCODES_ID);
        assertThat(fpv).isNotNull();
        assertThat(fpv.getStringValue()).isNotEmpty().containsExactlyInAnyOrder(WordUtil.defaultWord(VALUE_3));
    }

    @Test
    public void validAddingAndInvalidRemoving() {
        CommonModel before = model(MODEL_ID, cb -> {
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.OPERATOR_FILLED, VALUE_1, VALUE_2);
            cb.parameterValues(FORMER_BARCODES_ID, XslNames.FORMER_BARCODES,
                ModificationSource.ASSESSOR, VALUE_3);
        });

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().add(WordUtil.defaultWord(VALUE_3));
        after.getSingleParameterValue(BARCODE_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_1));

        modelSaveContext.setOperationSource(ModificationSource.ASSESSOR);

        preprocessor.preprocess(getModelSaveGroup(before, after), modelSaveContext);

        assertThat(after.getSingleParameterValue(FORMER_BARCODES_ID)).isNull();
    }

    @Test
    public void invalidAddingAndValidRemoving() {
        CommonModel before = model(MODEL_ID, cb -> {
            cb.parameterValues(BARCODE_ID, XslNames.BAR_CODE, ModificationSource.ASSESSOR, VALUE_1, VALUE_2);
            cb.parameterValues(FORMER_BARCODES_ID, XslNames.FORMER_BARCODES,
                ModificationSource.OPERATOR_FILLED, VALUE_3);
        });

        CommonModel after = new CommonModel(before);
        after.getSingleParameterValue(BARCODE_ID).getStringValue().add(WordUtil.defaultWord(VALUE_3));
        after.getSingleParameterValue(BARCODE_ID).getStringValue().remove(WordUtil.defaultWord(VALUE_1));

        modelSaveContext.setOperationSource(ModificationSource.ASSESSOR);

        preprocessor.preprocess(getModelSaveGroup(before, after), modelSaveContext);

        ParameterValue fpv = after.getSingleParameterValue(FORMER_BARCODES_ID);
        assertThat(fpv).isNotNull();
        assertThat(fpv.getStringValue()).isNotEmpty().containsExactlyInAnyOrder(
            WordUtil.defaultWord(VALUE_1),
            WordUtil.defaultWord(VALUE_3)
        );
    }

    @NotNull
    private ModelSaveGroup getModelSaveGroup(CommonModel before, CommonModel after) {
        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModels(after);
        modelSaveGroup.addBeforeModels(ImmutableList.of(before));
        return modelSaveGroup;
    }
}
