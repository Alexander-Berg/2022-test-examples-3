package ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueMetadata;
import ru.yandex.market.mbo.gwt.models.params.Param;

import java.util.List;

public class ParameterValuesMetadataPreprocessorTest extends BasePreprocessorTest {

    private ParameterValuesMetadataPreprocessor parameterValuesMetadataPreprocessor;

    @Before
    public void before() {
        super.before();
        parameterValuesMetadataPreprocessor = new ParameterValuesMetadataPreprocessor();
    }

    @Test
    public void testSetMetadataOnHigherPriorityChanges() {
        CommonModel beforeModel = model(1L);
        beforeModel.addParameterValue(createTestParameterValue(1L, false, ModificationSource.MDM));
        beforeModel.getParameterValuesMetadata().put(
            1L,
            new ParameterValueMetadata(1L, ModificationSource.MDM)
        );

        CommonModel afterModel = model(1L);
        afterModel.addParameterValue(createTestParameterValue(1L, true, ModificationSource.OPERATOR_FILLED));

        ModelSaveGroup group = ModelSaveGroup.fromModelsCopy(List.of(afterModel), List.of(beforeModel));
        parameterValuesMetadataPreprocessor.preprocess(group, modelSaveContext);

        Assertions.assertThat(
            group.getModelChanges().get(0).getAfter().getParameterValuesMetadata().get(1L).getParameterSource()
        ).isEqualTo(ModificationSource.OPERATOR_FILLED);
    }

    @Test
    public void testSetMetadataOnSamePriorityChanges() {
        CommonModel beforeModel = model(1L);
        beforeModel.addParameterValue(createTestParameterValue(1L, false, ModificationSource.OPERATOR_FILLED));
        beforeModel.getParameterValuesMetadata().put(
            1L,
            new ParameterValueMetadata(1L, ModificationSource.OPERATOR_FILLED)
        );

        CommonModel afterModel = model(1L);
        afterModel.addParameterValue(createTestParameterValue(1L, true, ModificationSource.OPERATOR_VIEWED));

        ModelSaveGroup group = ModelSaveGroup.fromModelsCopy(List.of(afterModel), List.of(beforeModel));
        parameterValuesMetadataPreprocessor.preprocess(group, modelSaveContext);

        Assertions.assertThat(
            group.getModelChanges().get(0).getAfter().getParameterValuesMetadata().get(1L).getParameterSource()
        ).isEqualTo(ModificationSource.OPERATOR_VIEWED);
    }

    @Test
    public void testSetMetadataOnNoMetadata() {
        CommonModel beforeModel = model(1L);
        beforeModel.addParameterValue(createTestParameterValue(1L, false, ModificationSource.VENDOR_OFFICE));

        CommonModel afterModel = model(1L);
        afterModel.addParameterValue(createTestParameterValue(1L, true, ModificationSource.FORMALIZATION));

        ModelSaveGroup group = ModelSaveGroup.fromModelsCopy(List.of(afterModel), List.of(beforeModel));
        parameterValuesMetadataPreprocessor.preprocess(group, modelSaveContext);

        Assertions.assertThat(
            group.getModelChanges().get(0).getAfter().getParameterValuesMetadata().get(1L).getParameterSource()
        ).isEqualTo(ModificationSource.FORMALIZATION);
    }

    @Test
    public void testNotSetMetadataOnLowerPriorityChanges() {
        CommonModel beforeModel = model(1L);
        beforeModel.addParameterValue(createTestParameterValue(1L, false, ModificationSource.VENDOR_OFFICE));
        beforeModel.getParameterValuesMetadata().put(
            1L,
            new ParameterValueMetadata(1L, ModificationSource.VENDOR_OFFICE)
        );

        CommonModel afterModel = model(1L);
        afterModel.addParameterValue(createTestParameterValue(1L, true, ModificationSource.FORMALIZATION));

        ModelSaveGroup group = ModelSaveGroup.fromModelsCopy(List.of(afterModel), List.of(beforeModel));
        parameterValuesMetadataPreprocessor.preprocess(group, modelSaveContext);

        Assertions.assertThat(group.getModelChanges().get(0).getAfter().getParameterValuesMetadata().get(1L)).isNull();
    }

    private ParameterValue createTestParameterValue(long paramId,
                                                    boolean value,
                                                    ModificationSource modificationSource) {
        ParameterValue parameterValue = new ParameterValue();
        parameterValue.setParamId(paramId);
        parameterValue.setBooleanValue(value);
        parameterValue.setType(Param.Type.BOOLEAN);
        parameterValue.setModificationSource(modificationSource);
        return parameterValue;
    }
}
