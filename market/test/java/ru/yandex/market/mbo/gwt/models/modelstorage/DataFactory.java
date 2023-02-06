package ru.yandex.market.mbo.gwt.models.modelstorage;

import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 23.03.2017
 */
class DataFactory {

    static final CategoryParam SINGLE_PARAM = param(1, "single", Param.Type.NUMERIC);
    static final CategoryParam MULTI_PARAM = param(2, "multi", Param.Type.NUMERIC);

    static final CategoryParam HYPOTHESIS_PARAM1 = param(1, "single", Param.Type.HYPOTHESIS);
    static final CategoryParam HYPOTHESIS_PARAM2 = param(3, "another", Param.Type.HYPOTHESIS);

    private static final int VALUE5 = 5;
    private static final int VALUE6 = 6;
    private static final int VALUE7 = 7;
    private static final int VALUE8 = 8;

    private DataFactory() {
    }

    private static CategoryParam param(long id, String xslName, Param.Type type) {
        CategoryParam param = new Parameter();
        param.setId(id);
        param.setXslName(xslName);
        param.setType(type);
        return param;
    }

    static CommonModel notEmptyModelOnly() {
        CommonModel model = model();
        model.addParameterValue(singleModelValue());
        model.addParameterValues(multiModelValues().getValues());
        return model;
    }

    static CommonModel modificationEmptyModelEmpty() {
        return modificationWithModel();
    }

    static CommonModel modificationEmptyModelNotEmpty() {
        CommonModel modification = modificationWithModel();
        CommonModel model = modification.getParentModel();
        model.addParameterValue(singleModelValue());
        model.addParameterValues(multiModelValues().getValues());

        return modification;
    }

    static CommonModel modificationNotEmptyModelEmpty() {
        CommonModel modification = modificationWithModel();
        modification.addParameterValue(singleModificationValue());
        modification.addParameterValues(multiModificationValues().getValues());

        return modification;
    }

    static CommonModel modificationNotEmptyModelNotEmpty() {
        CommonModel modification = modificationWithModel();
        CommonModel model = modification.getParentModel();

        model.addParameterValue(singleModelValue());
        model.addParameterValues(multiModelValues().getValues());

        modification.addParameterValue(singleModificationValue());
        modification.addParameterValues(multiModificationValues().getValues());

        return modification;
    }

    static CommonModel modificationNotEmptyModelNotEmptyWithHypothesis() {
        CommonModel modification = modificationWithModel();
        CommonModel parentModel = modification.getParentModel();

        parentModel.addParameterValue(singleModelValue());
        parentModel.addParameterValues(multiModelValues().getValues());

        modification.putParameterValueHypothesis(singleModificationHypothesisValue());
        modification.addParameterValues(multiModificationValues().getValues());

        return modification;
    }

    static CommonModel modificationNotEmptyModelNotEmptyWithAnotherHypothesis() {
        CommonModel modification = modificationWithModel();
        CommonModel parentModel = modification.getParentModel();

        parentModel.addParameterValue(singleModelValue());
        parentModel.addParameterValues(multiModelValues().getValues());

        modification.putParameterValueHypothesis(anotherModificationHypothesisValue());
        modification.addParameterValues(multiModificationValues().getValues());

        return modification;
    }

    private static CommonModel modificationWithModel() {
        CommonModel modification = new CommonModel();
        modification.setId(2);
        modification.setParentModelId(1);

        CommonModel model = model();
        modification.setParentModel(model);

        return modification;
    }

    private static CommonModel model() {
        CommonModel model = new CommonModel();
        model.setId(1);
        return model;
    }

    static ParameterValue singleModelValue() {
        return ParameterValueTestHelper.numeric(SINGLE_PARAM.getId(), SINGLE_PARAM.getXslName(), 1);
    }

    static ParameterValue singleModificationValue() {
        return ParameterValueTestHelper.numeric(SINGLE_PARAM.getId(), SINGLE_PARAM.getXslName(), 2);
    }

    static ParameterValueHypothesis singleModificationHypothesisValue() {
        return new ParameterValueHypothesis(
            HYPOTHESIS_PARAM1.getId(),
            HYPOTHESIS_PARAM1.getXslName(),
            HYPOTHESIS_PARAM1.getType(),
            Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "TestHypothesisValue1")),
            null);
    }

    static ParameterValueHypothesis anotherModificationHypothesisValue() {
        return new ParameterValueHypothesis(
            HYPOTHESIS_PARAM2.getId(),
            HYPOTHESIS_PARAM2.getXslName(),
            HYPOTHESIS_PARAM2.getType(),
            Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "TestHypothesisValue2")),
            null);
    }

    static ParameterValues multiModelValues() {
        return ParameterValues.of(Arrays.asList(
            ParameterValueTestHelper.numeric(MULTI_PARAM.getId(), MULTI_PARAM.getXslName(), VALUE5),
            ParameterValueTestHelper.numeric(MULTI_PARAM.getId(), MULTI_PARAM.getXslName(), VALUE6))
        );
    }

    static ParameterValues multiModificationValues() {
        return ParameterValues.of(Arrays.asList(
            ParameterValueTestHelper.numeric(MULTI_PARAM.getId(), MULTI_PARAM.getXslName(), VALUE7),
            ParameterValueTestHelper.numeric(MULTI_PARAM.getId(), MULTI_PARAM.getXslName(), VALUE8))
        );
    }
}
