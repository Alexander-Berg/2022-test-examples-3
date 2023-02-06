package ru.yandex.market.mbo.db.modelstorage.validation.dump;

import org.mockito.Mock;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.user.AutoUser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class AbstractDumpValidationServiceTest {

    DumpValidationService dumpValidationService;

    @Mock
    AutoUser autoUser;

    protected void testTitleGeneratedInternal() {
        CommonModel rootModel = buildRootModel();
        CommonModel modification = buildModification();
        CommonModel sku = buildSku();

        Map<CommonModel, ModelStorage.Model> dumpModels = new HashMap<>();
        dumpValidationService.applyValidationPipes(
            dumpValidationService.createCategoryPipe(1L),
            rootModel,
            Collections.singletonList(modification),
            Collections.singletonList(sku),
            mr -> dumpModels.put(mr.getOriginal(), mr.getDump()));

        assertEquals(3, dumpModels.size());

        ModelStorage.Model dumpRootModel = dumpModels.get(rootModel);
        ModelStorage.Model dumpModification = dumpModels.get(modification);
        ModelStorage.Model dumpSku = dumpModels.get(sku);

        assertTitle("Vendor1 Model", dumpRootModel);
        assertTitleWithoutVendor("Model", dumpRootModel);
        assertTitle("Vendor1 Modification", dumpModification);
        assertTitleWithoutVendor("Modification", dumpModification);
        assertTitle("Vendor1 Modification 100 kg size2 (unit1) strValue", dumpSku);
    }

    protected void testTitleGeneratedIsSkuInternal() {
        CommonModel rootModel = buildRootModel();
        CommonModel modification = buildModificationAsSku();
        CommonModel sku = buildSku();

        Map<CommonModel, ModelStorage.Model> dumpModels = new HashMap<>();
        Map<CommonModel, ModelStorage.Model> dumpIsSku = new HashMap<>();
        dumpValidationService.applyValidationPipes(
            dumpValidationService.createCategoryPipe(1L),
            rootModel,
            Collections.singletonList(modification),
            Collections.singletonList(sku),
            mr -> {
                dumpModels.put(mr.getOriginal(), mr.getDump());
                if (mr.getConstructedSku() != null) {
                    dumpIsSku.put(mr.getOriginal(), mr.getConstructedSku());
                }
            });

        assertEquals(3, dumpModels.size());
        assertEquals(1, dumpIsSku.size());
        assertTrue(dumpIsSku.containsKey(modification));
        ModelStorage.Model constructedSku = dumpIsSku.get(modification);
        assertEquals(modification.getId(), constructedSku.getId());
        assertEquals(constructedSku.getCurrentType(), CommonModel.Source.SKU.name());
    }

    private CommonModel buildRootModel() {
        return CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .title("Model")
            .startParameterValue()
                .paramId(1).xslName("numeric").num(100)
            .endParameterValue()
            .endModel();
    }

    private CommonModel buildModification() {
        return CommonModelBuilder.newBuilder(2, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .parentModelId(1)
            .title("Modification")
            .startParameterValue()
                .paramId(4).xslName("string").words("strValue")
            .endParameterValue()
            .startModelRelation()
                .categoryId(1)
                .id(3)
                .type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .endModel();
    }

    private CommonModel buildModificationAsSku() {
        return CommonModelBuilder.newBuilder(2, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .parentModelId(1)
            .title("Modification")
            .startParameterValue()
            .paramId(4).xslName("string").words("strValue")
            .endParameterValue()
            .startParameterValue()
                .paramId(5).xslName(XslNames.IS_SKU).booleanValue(true, 1)
            .endParameterValue()
            .startModelRelation()
            .categoryId(1)
            .id(3)
            .type(ModelRelation.RelationType.SKU_MODEL)
            .endModelRelation()
            .endModel();
    }

    private void assertTitle(String expected, ModelStorage.Model model) {
        assertEquals(expected, model.getTitlesList().get(0).getValue());
    }

    private void assertTitleWithoutVendor(String expected, ModelStorage.Model model) {
        assertEquals(expected, model.getTitleWithoutVendor().getValue());
    }

    private CommonModel buildSku() {
        return CommonModelBuilder.newBuilder(3, 1, 1)
            .currentType(CommonModel.Source.SKU)
            .parentModelId(1)
            .startParameterValue()
                .paramId(2).xslName("sizeValue").optionId(11)
            .endParameterValue()
            .startModelRelation()
                .categoryId(1)
                .id(2)
                .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .endModel();
    }
}
