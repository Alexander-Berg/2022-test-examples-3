package ru.yandex.market.mbo.db.modelstorage.generalization;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.GeneralizationStrategy;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelGroupGeneralizationServiceTest extends BaseGeneralizationTest {

    @Test
    public void testGroupMultipleModifed() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 2);
        group.setStrategyType(GeneralizationStrategy.AUTO);
        group.getParentModel().addParameterValue(PARAMETER_VALUE_21);
        group.getAliveModifications().get(0).addParameterValue(PARAMETER_VALUE_11);
        group.getAliveModifications().get(1).addParameterValue(PARAMETER_VALUE_12);
        copyToBeforeModels(group);

        // param 2 is set in model - now we are changing it in modification
        group.getAliveModifications().get(1).addParameterValue(PARAMETER_VALUE_22);
        // param 1 is set in modifications - now we are changing it in model
        group.getParentModel().addParameterValue(PARAMETER_VALUE_13);

        generalizationService.generalizeGroup(group, true, true);
        List<ParameterValues> pv1values = group.getModifications().stream()
            .map(m -> m.getParameterValues(PARAMETER_VALUE_11.getXslName()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        assertTrue(pv1values.isEmpty());
        assertNull(group.getParentModel().getParameterValues(PARAMETER_VALUE_21.getXslName()));
    }

    @Test
    public void testGroupCategoryOneParentModel() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 0);
        group.setStrategyType(GeneralizationStrategy.MODEL);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        getAndAssertOnlyModel(updatedModels, group.getParentModelId());
    }

    @Test
    public void testIgnoreGeneralizationIfParentModelIsDeleted() throws Exception {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        modifications.get(0).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(1).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(2).addParameterValue(PARAMETER_VALUE_11);
        group.setStrategyType(GeneralizationStrategy.MODEL);
        group.getParentModel().setDeleted(true);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        Assert.assertEquals(0, updatedModels.size());
    }

    @Test
    public void testSaveModelParamSetAllModifSet() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        group.getParentModel().addParameterValue(PARAMETER_VALUE_11);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        modifications.get(0).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(1).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(2).addParameterValue(PARAMETER_VALUE_11);
        group.setStrategyType(GeneralizationStrategy.MODEL);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        CommonModel modif1 = getAndAssertModel(updatedModels, modifications.get(0).getId());
        CommonModel modif2 = getAndAssertModel(updatedModels, modifications.get(1).getId());
        CommonModel modif3 = getAndAssertModel(updatedModels, modifications.get(2).getId());

        assertParamValue(parentModel, PARAMETER_VALUE_11);
        assertNoParamValue(modif1, PARAMETER_VALUE_11.getParamId());
        assertNoParamValue(modif2, PARAMETER_VALUE_11.getParamId());
        assertNoParamValue(modif3, PARAMETER_VALUE_11.getParamId());
    }

    @Test
    public void testSaveModelParamSetNotAllModifSetNoForce() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        group.getParentModel().addParameterValue(PARAMETER_VALUE_11);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        modifications.get(0).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(1).addParameterValue(PARAMETER_VALUE_11);
        group.setStrategyType(GeneralizationStrategy.MODEL);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        CommonModel modif1 = getAndAssertModel(updatedModels, modifications.get(0).getId());
        CommonModel modif2 = getAndAssertModel(updatedModels, modifications.get(1).getId());
        assertNoModel(updatedModels, modifications.get(2).getId());

        assertParamValue(parentModel, PARAMETER_VALUE_11);
        assertNoParamValue(modif1, PARAMETER_VALUE_11.getParamId());
        assertNoParamValue(modif2, PARAMETER_VALUE_11.getParamId());
    }

    @Test
    public void testSaveModelParamSetDifferentModifSetNoForce() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        group.getParentModel().addParameterValue(PARAMETER_VALUE_11);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        modifications.get(0).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(1).addParameterValue(PARAMETER_VALUE_12);
        group.setStrategyType(GeneralizationStrategy.MODEL);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertOnlyModel(updatedModels, group.getParentModelId());

        assertParamValue(parentModel, PARAMETER_VALUE_11);
    }

    @Test
    public void testSaveModelParamSetDifferentModifSetForce() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        group.getParentModel().addParameterValue(PARAMETER_VALUE_11);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        modifications.get(0).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(1).addParameterValue(PARAMETER_VALUE_12);
        group.setStrategyType(GeneralizationStrategy.MODEL);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, true);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        CommonModel modif1 = getAndAssertModel(updatedModels, modifications.get(0).getId());
        CommonModel modif2 = getAndAssertModel(updatedModels, modifications.get(1).getId());
        assertNoModel(updatedModels, modifications.get(2).getId());

        assertParamValue(parentModel, PARAMETER_VALUE_11);
        assertNoParamValue(modif1, PARAMETER_VALUE_11.getParamId());
        assertNoParamValue(modif2, PARAMETER_VALUE_11.getParamId());
    }

    @Test
    public void testSaveModelParamNotSet() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        modifications.get(0).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(1).addParameterValue(PARAMETER_VALUE_12);
        group.setStrategyType(GeneralizationStrategy.MODEL);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertOnlyModel(updatedModels, group.getParentModelId());

        assertNoParamValue(parentModel, PARAMETER_VALUE_11.getParamId());
    }

    @Test
    public void testSaveModifModelParamNotSetModifSameEmpty() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        modifications.get(0).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(1).addParameterValue(PARAMETER_VALUE_11);
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        assertNoModel(updatedModels, modifications.get(0).getId());
        assertNoModel(updatedModels, modifications.get(1).getId());
        assertNoModel(updatedModels, modifications.get(2).getId());

        assertNoParamValue(parentModel, PARAMETER_VALUE_11.getParamId());
    }

    @Test
    public void testSaveModifModelParamNotSetModifAllSame() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        modifications.get(0).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(1).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(2).addParameterValue(PARAMETER_VALUE_11);
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        CommonModel modif1 = getAndAssertModel(updatedModels, modifications.get(0).getId());
        CommonModel modif2 = getAndAssertModel(updatedModels, modifications.get(1).getId());
        CommonModel modif3 = getAndAssertModel(updatedModels, modifications.get(2).getId());

        assertParamValueGeneralized(parentModel, PARAMETER_VALUE_11);
        assertNoParamValue(modif1, PARAMETER_VALUE_11.getParamId());
        assertNoParamValue(modif2, PARAMETER_VALUE_11.getParamId());
        assertNoParamValue(modif3, PARAMETER_VALUE_11.getParamId());
    }

    @Test
    public void testSaveModifModelParamNotSetModifDifferentEmpty() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        modifications.get(0).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(1).addParameterValue(PARAMETER_VALUE_12);
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        assertNoModel(updatedModels, modifications.get(0).getId());
        assertNoModel(updatedModels, modifications.get(1).getId());
        assertNoModel(updatedModels, modifications.get(2).getId());

        assertNoParamValue(parentModel, PARAMETER_VALUE_11.getParamId());
    }

    @Test
    public void testSaveModifModelParamNotSetModifAllDifferent() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        modifications.get(0).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(1).addParameterValue(PARAMETER_VALUE_12);
        modifications.get(2).addParameterValue(PARAMETER_VALUE_13);
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        assertNoModel(updatedModels, modifications.get(0).getId());
        assertNoModel(updatedModels, modifications.get(1).getId());
        assertNoModel(updatedModels, modifications.get(2).getId());

        assertNoParamValue(parentModel, PARAMETER_VALUE_11.getParamId());
    }

    @Test
    public void testSaveModifModelParamSetModifSameEmpty() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        group.getParentModel().addParameterValue(PARAMETER_VALUE_11);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        modifications.get(0).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(1).addParameterValue(PARAMETER_VALUE_11);
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        CommonModel modif1 = getAndAssertModel(updatedModels, modifications.get(0).getId());
        CommonModel modif2 = getAndAssertModel(updatedModels, modifications.get(1).getId());
        assertNoModel(updatedModels, modifications.get(2).getId());

        assertParamValueGeneralized(parentModel, PARAMETER_VALUE_11);
        assertNoParamValue(modif1, PARAMETER_VALUE_11.getParamId());
        assertNoParamValue(modif2, PARAMETER_VALUE_11.getParamId());
    }

    @Test
    public void testSaveModifModelParamSetModifDiffEmpty() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        group.getParentModel().addParameterValue(PARAMETER_VALUE_11);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        modifications.get(0).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(1).addParameterValue(PARAMETER_VALUE_12);
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        assertNoModel(updatedModels, modifications.get(0).getId());
        assertNoModel(updatedModels, modifications.get(1).getId());
        CommonModel modif3 = getAndAssertModel(updatedModels, modifications.get(2).getId());

        assertNoParamValue(parentModel, PARAMETER_VALUE_11.getParamId());
        assertParamValue(modif3, PARAMETER_VALUE_11);
    }

    @Test
    public void testSaveModifDelete() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        modifications.get(0).addParameterValue(PARAMETER_VALUE_12);
        modifications.get(0).setDeleted(true);
        modifications.get(1).addParameterValue(PARAMETER_VALUE_11);
        modifications.get(2).addParameterValue(PARAMETER_VALUE_11);
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);

        //parameter exists in models => during generalization ModificationSource is not preserved
        group.addBeforeModel(new CommonModel(modifications.get(0)));
        group.addBeforeModel(new CommonModel(modifications.get(1)));
        group.addBeforeModel(new CommonModel(modifications.get(2)));

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        assertNoModel(updatedModels, modifications.get(0).getId());
        CommonModel modif2 = getAndAssertModel(updatedModels, modifications.get(1).getId());
        CommonModel modif3 = getAndAssertModel(updatedModels, modifications.get(2).getId());

        assertParamValueGeneralized(parentModel, PARAMETER_VALUE_11);
        assertNoParamValue(modif2, PARAMETER_VALUE_11.getParamId());
        assertNoParamValue(modif3, PARAMETER_VALUE_11.getParamId());
    }

    @Test
    public void testOneModelOneModifPreserveModificationSource() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 1);
        CommonModel modification = group.getModifications().iterator().next();
        modification.addParameterValue(PARAMETER_VALUE_11);
        group.setStrategyType(GeneralizationStrategy.AUTO);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        assertParamValueGeneralizedWithPreservingMS(parentModel, PARAMETER_VALUE_11);
    }
}
