package ru.yandex.market.mbo.db.modelstorage.data.group;

import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation.RelationType;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class SplitModelGroupTest {

    private ModelSaveGroup mainGroup;
    private static final Comparator<ModelSaveGroup> DEEP_CMP = (g1, g2) -> {
        if (listsReferenceDiffer(g1.getRequestedModels(), g2.getRequestedModels())) {
            return -1;
        }
        if (listsReferenceDiffer(g1.getAdditionalModels(), g2.getAdditionalModels())) {
            return -1;
        }
        // Before models can be copied if needed
        if (!Objects.equals(g1.getBeforeModels(), g2.getBeforeModels())) {
            return -1;
        }
        GroupOperationStatus status1 = g1.generateOverallStatus(true);
        GroupOperationStatus status2 = g2.generateOverallStatus(true);
        List<OperationStatus> statuses1 = status1.getRequestedModelStatuses();
        List<OperationStatus> statuses2 = status2.getRequestedModelStatuses();
        if (statusesDiffer(statuses1, statuses2)) {
            return -1;
        }
        statuses1 = status1.getAdditionalModelStatues();
        statuses2 = status1.getAdditionalModelStatues();
        if (statusesDiffer(statuses1, statuses2)) {
            return -1;
        }
        return 0;
    };

    @Test
    public void testSplitFewIndependentModels() {
        // Data
        CommonModel model1 = anyModel(1L);
        CommonModel model2 = anyModel(2L);
        CommonModel model3 = anyModel(3L);
        mainGroup = ModelSaveGroup.fromModels(model1, model2, model3);

        // Expected
        ModelSaveGroup group1 = ModelSaveGroup.fromModels(model1);
        ModelSaveGroup group2 = ModelSaveGroup.fromModels(model2);
        ModelSaveGroup group3 = ModelSaveGroup.fromModels(model3);

        // Split it!
        List<ModelSaveGroup> subgroups = GroupModelSaveUtils.splitGroups(mainGroup);

        // Check
        assertThat(subgroups).usingElementComparator(DEEP_CMP).containsExactlyInAnyOrder(
            group1,
            group2,
            group3
        );
    }

    @Test
    public void testOperationStatusKeptIntact() {
        // Data
        CommonModel model1 = anyModel(1L);
        CommonModel model2 = anyModel(2L);
        CommonModel model3 = anyModel(3L);
        mainGroup = ModelSaveGroup.fromModels(model1, model2, model3);
        mainGroup.setStatus(model1, OperationStatusType.OK);
        mainGroup.setStatus(model2, OperationStatusType.INTERNAL_ERROR);
        mainGroup.setStatus(model3, OperationStatusType.FAILED_MODEL_IN_GROUP, "Ololo happened");

        // Expected
        ModelSaveGroup group1 = ModelSaveGroup.fromModels(model1);
        ModelSaveGroup group2 = ModelSaveGroup.fromModels(model2);
        ModelSaveGroup group3 = ModelSaveGroup.fromModels(model3);
        group1.setStatus(model1, OperationStatusType.OK);
        group2.setStatus(model2, OperationStatusType.INTERNAL_ERROR);
        group3.setStatus(model3, OperationStatusType.FAILED_MODEL_IN_GROUP, "Ololo happened");

        // Split it!
        List<ModelSaveGroup> subgroups = GroupModelSaveUtils.splitGroups(mainGroup);

        // Check
        assertThat(subgroups).usingElementComparator(DEEP_CMP).containsExactlyInAnyOrder(
            group1,
            group2,
            group3
        );
    }

    @Test
    public void testValidationErrorsKeptIntact() {
        // Data
        CommonModel model1 = anyModel(1L);
        CommonModel model2 = anyModel(2L);
        CommonModel model3 = anyModel(3L);
        mainGroup = ModelSaveGroup.fromModels(model1, model2, model3);
        mainGroup.addValidationErrors(model1,
            singletonList(new ModelValidationError(model1.getId(), ErrorType.INVALID_BARCODE)));
        mainGroup.addValidationErrors(model3,
            singletonList(new ModelValidationError(model3.getId(), ErrorType.MISSING_CATEGORY)));

        // Expected
        ModelSaveGroup group1 = ModelSaveGroup.fromModels(model1);
        ModelSaveGroup group2 = ModelSaveGroup.fromModels(model2);
        ModelSaveGroup group3 = ModelSaveGroup.fromModels(model3);
        group1.addValidationErrors(model1,
            singletonList(new ModelValidationError(model1.getId(), ErrorType.INVALID_BARCODE)));
        group3.addValidationErrors(model3,
            singletonList(new ModelValidationError(model3.getId(), ErrorType.MISSING_CATEGORY)));

        // Split it!
        List<ModelSaveGroup> subgroups = GroupModelSaveUtils.splitGroups(mainGroup);

        // Check
        assertThat(subgroups).usingElementComparator(DEEP_CMP).containsExactlyInAnyOrder(
            group1,
            group2,
            group3
        );
    }

    @Test
    public void testBeforeModelsCopiedToSubgroups() {
        // Data
        CommonModel model1 = anyModel(1L);
        CommonModel model2 = anyModel(2L);
        CommonModel model3 = anyModel(3L);
        mainGroup = ModelSaveGroup.fromModels(model1, model2, model3);
        CommonModel before1 = anyModel(1L);
        CommonModel before3 = anyModel(3L);
        mainGroup.addBeforeModels(Arrays.asList(before1, before3));

        // Expected
        ModelSaveGroup group1 = ModelSaveGroup.fromModels(model1);
        ModelSaveGroup group2 = ModelSaveGroup.fromModels(model2);
        ModelSaveGroup group3 = ModelSaveGroup.fromModels(model3);
        group1.addBeforeModels(singletonList(before1));
        group3.addBeforeModels(singletonList(before3));

        // Split it!
        List<ModelSaveGroup> subgroups = GroupModelSaveUtils.splitGroups(mainGroup);

        // Check
        assertThat(subgroups).usingElementComparator(DEEP_CMP).containsExactlyInAnyOrder(
            group1,
            group2,
            group3
        );
    }

    @Test
    public void testStatusesErrorsAndBeforeModelsTogether() {
        // Data
        CommonModel model1 = anyModel(1L);
        CommonModel model2 = anyModel(2L);
        CommonModel model3 = anyModel(3L);
        mainGroup = ModelSaveGroup.fromModels(model1, model2, model3);
        mainGroup.setStatus(model1, OperationStatusType.INTERNAL_ERROR);
        mainGroup.addValidationErrors(model2,
            singletonList(new ModelValidationError(model2.getId(), ErrorType.INVALID_BARCODE)));
        CommonModel before3 = anyModel(3L);
        mainGroup.addBeforeModels(singletonList(before3));

        // Expected
        ModelSaveGroup group1 = ModelSaveGroup.fromModels(model1);
        ModelSaveGroup group2 = ModelSaveGroup.fromModels(model2);
        ModelSaveGroup group3 = ModelSaveGroup.fromModels(model3);
        group1.setStatus(model1, OperationStatusType.INTERNAL_ERROR);
        group2.addValidationErrors(model2,
            singletonList(new ModelValidationError(model2.getId(), ErrorType.INVALID_BARCODE)));
        group3.addBeforeModels(singletonList(before3));

        // Split it!
        List<ModelSaveGroup> subgroups = GroupModelSaveUtils.splitGroups(mainGroup);

        // Check
        assertThat(subgroups).usingElementComparator(DEEP_CMP).containsExactlyInAnyOrder(
            group1,
            group2,
            group3
        );
    }

    @Test
    public void testParentModelsGroupedWithChildren() {
        // Data
        CommonModel parent1 = anyModel(1);
        CommonModel child1a = childModelOf(parent1, 11);
        CommonModel child1b = childModelOf(parent1, 12);

        CommonModel parent2 = anyModel(2);
        CommonModel child2a = childModelOf(parent2, 21);
        // (please note the order of these models - it will be kept in the subgroups)
        mainGroup = ModelSaveGroup.fromModels(parent1, child1b, child2a, child1a, parent2);

        // Expected
        ModelSaveGroup group1 = ModelSaveGroup.fromModels(parent1, child1b, child1a);
        ModelSaveGroup group2 = ModelSaveGroup.fromModels(child2a, parent2);

        // Split it!
        List<ModelSaveGroup> subgroups = GroupModelSaveUtils.splitGroups(mainGroup);

        // Check
        assertThat(subgroups).usingElementComparator(DEEP_CMP).containsExactlyInAnyOrder(
            group1,
            group2
        );
    }

    @Test
    public void testRelatedModelsGroupedWithEachOther() {
        // Data
        CommonModel guru = anyModel(1);
        CommonModel sku1 = anyModel(11);
        CommonModel sku2 = anyModel(12);
        connectWithRelation(guru, sku1, RelationType.SKU_MODEL);
        connectWithRelation(guru, sku2, RelationType.SKU_MODEL);

        CommonModel vendorModel = anyModel(2);
        CommonModel syncByVendor = anyModel(21);
        connectWithRelation(vendorModel, syncByVendor, RelationType.SYNC_SOURCE);

        CommonModel experimental = anyModel(3);
        CommonModel syncByExp = anyModel(31);
        connectWithRelation(experimental, syncByExp, RelationType.EXPERIMENTAL_MODEL);

        mainGroup = ModelSaveGroup.fromModels(experimental, sku2, vendorModel, sku1, syncByExp, syncByVendor, guru);

        // Expected
        ModelSaveGroup guruSkuGroup = ModelSaveGroup.fromModels(sku2, sku1, guru);
        ModelSaveGroup vendorGroup = ModelSaveGroup.fromModels(vendorModel, syncByVendor);
        ModelSaveGroup experimentGroup = ModelSaveGroup.fromModels(experimental, syncByExp);

        // Split it!
        List<ModelSaveGroup> subgroups = GroupModelSaveUtils.splitGroups(mainGroup);

        // Check
        assertThat(subgroups).usingElementComparator(DEEP_CMP).containsExactlyInAnyOrder(
            guruSkuGroup,
            vendorGroup,
            experimentGroup
        );
    }

    @Test
    public void testRelationsAndParentsTogether() {
        // Data
        CommonModel root1 = anyModel(1);
        CommonModel guru1 = childModelOf(root1, 10);
        CommonModel sku11 = anyModel(101);
        CommonModel sku12 = anyModel(102);
        CommonModel rootedSku = anyModel(111);
        connectWithRelation(guru1, sku11, RelationType.SKU_MODEL);
        connectWithRelation(guru1, sku12, RelationType.SKU_MODEL);
        connectWithRelation(root1, rootedSku, RelationType.SKU_MODEL);

        CommonModel root2 = anyModel(2);
        CommonModel guru2 = childModelOf(root2, 20);
        CommonModel sku21 = anyModel(201);
        connectWithRelation(guru2, sku21, RelationType.SKU_MODEL);

        mainGroup = ModelSaveGroup.fromModels(rootedSku, sku21, guru2, root1, guru1, sku11, sku12, root2);

        // Expected
        ModelSaveGroup group1 = ModelSaveGroup.fromModels(rootedSku, root1, guru1, sku11, sku12);
        ModelSaveGroup group2 = ModelSaveGroup.fromModels(sku21, guru2, root2);

        // Split it!
        List<ModelSaveGroup> subgroups = GroupModelSaveUtils.splitGroups(mainGroup);

        // Check
        assertThat(subgroups).usingElementComparator(DEEP_CMP).containsExactlyInAnyOrder(
            group1,
            group2
        );
    }

    @Test
    public void testChildrenAndParentsInAdditionalCollectionTreatedWell() {
        // Data
        CommonModel parent1 = anyModel(1);
        CommonModel child1a = childModelOf(parent1, 11);
        CommonModel child1b = childModelOf(parent1, 12);

        CommonModel parent2 = anyModel(2);
        CommonModel child2a = childModelOf(parent2, 21);

        mainGroup = ModelSaveGroup.fromModels(child2a, parent1);
        mainGroup.addStorageModels(Arrays.asList(child1b, child1a, parent2));

        // Expected
        ModelSaveGroup group1 = ModelSaveGroup.fromModels(parent1);
        group1.addStorageModels(Arrays.asList(child1b, child1a));
        ModelSaveGroup group2 = ModelSaveGroup.fromModels(child2a);
        group2.addStorageModels(singletonList(parent2));

        // Split it!
        List<ModelSaveGroup> subgroups = GroupModelSaveUtils.splitGroups(mainGroup);

        // Check
        assertThat(subgroups).usingElementComparator(DEEP_CMP).containsExactlyInAnyOrder(
            group1,
            group2
        );
    }

    @Test
    public void testRelatedModelsInAdditionalCollectionTreatedWell() {
        // Data
        CommonModel guru = anyModel(1);
        CommonModel sku1 = anyModel(11);
        connectWithRelation(guru, sku1, RelationType.SKU_MODEL);

        CommonModel vendorModel = anyModel(2);
        CommonModel syncByVendor = anyModel(21);
        connectWithRelation(vendorModel, syncByVendor, RelationType.SYNC_SOURCE);

        mainGroup = ModelSaveGroup.fromModels(guru, syncByVendor);
        mainGroup.addStorageModels(Arrays.asList(sku1, vendorModel));

        // Expected
        ModelSaveGroup guruSkuGroup = ModelSaveGroup.fromModels(guru);
        guruSkuGroup.addStorageModels(singletonList(sku1));
        ModelSaveGroup vendorGroup = ModelSaveGroup.fromModels(syncByVendor);
        vendorGroup.addStorageModels(singletonList(vendorModel));

        // Split it!
        List<ModelSaveGroup> subgroups = GroupModelSaveUtils.splitGroups(mainGroup);

        // Check
        assertThat(subgroups).usingElementComparator(DEEP_CMP).containsExactlyInAnyOrder(
            guruSkuGroup,
            vendorGroup
        );
    }

    @Test
    public void testRelatedModelsNotPresentInSaveGroupIgnored() {
        // Data
        CommonModel guru = anyModel(1);
        CommonModel sku = anyModel(2);

        CommonModel weirdGuruModel = anyModel(10);
        CommonModel weirdSkuModel = anyModel(21);
        guru.setParentModel(weirdGuruModel);
        guru.setParentModelId(weirdGuruModel.getId());
        sku.setParentModel(weirdSkuModel);
        sku.setParentModelId(weirdSkuModel.getId());

        mainGroup = ModelSaveGroup.fromModels(guru, sku); // weird models are not present in group, yet they're related

        // Expected
        ModelSaveGroup guruGroup = ModelSaveGroup.fromModels(guru);
        ModelSaveGroup skuGroup = ModelSaveGroup.fromModels(sku);

        // Split it!
        List<ModelSaveGroup> subgroups = GroupModelSaveUtils.splitGroups(mainGroup);

        // Check
        assertThat(subgroups).usingElementComparator(DEEP_CMP).containsExactlyInAnyOrder(
            guruGroup,
            skuGroup
        ); // extra models didn't make it into any of these groups and were ignored == OK
    }

    @Test
    public void testOperationStatusesCarriedOverToAddedModels() {
        // Data
        CommonModel guru = anyModel(1);
        CommonModel sku = anyModel(11);
        connectWithRelation(guru, sku, RelationType.SKU_MODEL);

        CommonModel parent = anyModel(2);
        CommonModel child = childModelOf(parent, 21);

        mainGroup = ModelSaveGroup.fromModels(guru, child);
        mainGroup.addStorageModels(Arrays.asList(sku, parent));
        mainGroup.setStatus(sku, OperationStatusType.INTERNAL_ERROR, "MOLODOY CHELOVEK, PROYDEMTE!");

        mainGroup.setStatus(parent, OperationStatusType.VALIDATION_ERROR, "Бёзбёжнё плёхёё мёдёль");
        mainGroup.addValidationErrors(parent, singletonList(
            new ModelValidationError(parent.getId(), ErrorType.INVALID_BARCODE)));

        // Expected
        ModelSaveGroup guruSkuGroup = ModelSaveGroup.fromModels(guru);
        guruSkuGroup.addStorageModels(singletonList(sku));
        guruSkuGroup.setStatus(sku, OperationStatusType.INTERNAL_ERROR, "MOLODOY CHELOVEK, PROYDEMTE!");

        ModelSaveGroup parentChildGroup = ModelSaveGroup.fromModels(child);
        parentChildGroup.addStorageModels(singletonList(parent));
        parentChildGroup.setStatus(parent, OperationStatusType.VALIDATION_ERROR, "Бёзбёжнё плёхёё мёдёль");
        parentChildGroup.addValidationErrors(parent, singletonList(
            new ModelValidationError(parent.getId(), ErrorType.INVALID_BARCODE)));

        // Split it!
        List<ModelSaveGroup> subgroups = GroupModelSaveUtils.splitGroups(mainGroup);

        // Check
        assertThat(subgroups).usingElementComparator(DEEP_CMP).containsExactlyInAnyOrder(
            guruSkuGroup,
            parentChildGroup
        );
    }

    private CommonModel anyModel(long id) {
        return CommonModelBuilder.newBuilder(id, 100500L).endModel();
    }

    private CommonModel childModelOf(CommonModel model, long id) {
        CommonModel child = CommonModelBuilder.newBuilder(id, 100500L).getModel();
        child.setParentModel(model);
        child.setParentModelId(model.getId());
        return child;
    }

    private void connectWithRelation(CommonModel model1, CommonModel model2, RelationType relationType) {
        ModelRelation forward = new ModelRelation();
        forward.setId(model2.getId());
        forward.setType(relationType);
        model1.addRelation(forward);

        relationType = relationType.getOppositeRelation();
        ModelRelation backward = new ModelRelation();
        backward.setId(model1.getId());
        backward.setType(relationType);
        model2.addRelation(backward);
    }

    private static boolean listsReferenceDiffer(List one, List two) {
        if (one.size() != two.size()) {
            return true;
        }

        for (int i = 0; i < one.size(); i++) {
            if (one.get(i) != two.get(i)) {
                return true;
            }
        }
        return false;
    }

    private static boolean statusesDiffer(List<OperationStatus> one, List<OperationStatus> two) {
        if (one.size() != two.size()) {
            return true;
        }

        for (int i = 0; i < one.size(); i++) {
            if (!one.get(i).fullyEquals(two.get(i))) {
                return true;
            }
        }
        return false;
    }
}
