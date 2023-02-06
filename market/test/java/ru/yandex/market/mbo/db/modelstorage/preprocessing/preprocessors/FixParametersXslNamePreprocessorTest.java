package ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

@SuppressWarnings("checkstyle:magicnumber")
public class FixParametersXslNamePreprocessorTest extends BasePreprocessorTest {

    private FixParametersXslNamePreprocessor preprocessor;

    private static final Map<Long, String> TEST_PARAMS = ImmutableMap.of(1L, "Param1", 2L, "Param2");

    private CategoryEntities prepareCategoryEntries() {
        CategoryEntities entries = new CategoryEntities();
        entries.setHid(CATEGORY_HID);
        TEST_PARAMS.forEach((id, xslName) -> {
            entries.addParameter(
                CategoryParamBuilder.newBuilder(id, xslName)
                    .setUseForGuru(true)
                    .setType(Param.Type.STRING)
                    .build()
            );
        });
        return entries;
    }

    @Before
    public void before() {
        super.before();
        preprocessor = new FixParametersXslNamePreprocessor(categoryParametersServiceClient);
    }

    @Override
    protected void createCategoryParametersServiceClient() {
        categoryParametersServiceClient = CategoryParametersServiceClientStub.ofCategoryEntities(
            prepareCategoryEntries()
        );
    }

    @Test
    public void testFixChangedModel() {
        CommonModel modelBefore = model(1, CATEGORY_HID, builder -> {
            builder.putParameterValues(stringParamValue(1L, "Param2", "before"));
            builder.putParameterValues(stringParamValue(2L, "Param2", "before"));
        });

        CommonModel modelAfter = model(1, CATEGORY_HID, builder -> {
            builder.putParameterValues(stringParamValue(1L, "Param2", "after"));
            builder.putParameterValues(stringParamValue(2L, "Param2", "after"));
        });


        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(modelAfter),
            ImmutableList.of(modelBefore));

        preprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(modelAfter.getId())).matches(this::isParamXslNamesCorrect);
    }

    @Test
    public void testSkipNew() {
        CommonModel created = model(3, CATEGORY_HID, builder -> {
            builder.putParameterValues(stringParamValue(1L, "Param2", "after"));
            builder.putParameterValues(stringParamValue(2L, "Param2", "after"));
        });

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(created),
            ImmutableList.of());

        preprocessor.preprocess(modelSaveGroup, modelSaveContext);

        Assertions.assertThat(modelSaveGroup.getById(created.getId()))
            .matches(m -> isAllParameterNamesEqualsToModel(m, created));
    }

    private boolean isParamXslNamesCorrect(CommonModel commonModel) {
        return isAllParameterNamesEquals(commonModel, TEST_PARAMS);
    }

    private boolean isAllParameterNamesEqualsToModel(CommonModel model1, CommonModel model2) {
        Map<Long, String> oldParamXslNames = model2.getFlatParameterValues().stream()
            .collect(Collectors.toMap(ParameterValue::getParamId, ParameterValue::getXslName, (a, b) -> a));
        return isAllParameterNamesEquals(model1, oldParamXslNames);
    }

    private boolean isAllParameterNamesEquals(CommonModel commonModel, Map<Long, String> names) {
        return commonModel.getFlatParameterValues().stream()
            .allMatch(pv -> Optional.ofNullable(names.get(pv.getParamId()))
                .map(name -> name.equals(pv.getXslName()))
                .orElse(true)
            );
    }

    private ParameterValues stringParamValue(long paramId, String xslName, String value) {
        return new ParameterValues(paramId, xslName, Param.Type.STRING,
            WordUtil.defaultWord(value));
    }
}
