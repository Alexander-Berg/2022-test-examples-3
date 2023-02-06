package ru.yandex.market.mbo.db.modelstorage.generalization;

import javolution.testing.AssertionException;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.GeneralizationStrategy;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CreateGroupGeneralizationService extends BaseGeneralizationTest {


    private static final CommonModel PARENT_MODEL_10 = CommonModelBuilder.newBuilder(10, GROUP_CATEGORY_HID, 0)
        .getModel();
    private static final CommonModel MODIFICATION_11 = CommonModelBuilder.newBuilder(11, GROUP_CATEGORY_HID, 0)
        .parentModelId(10).getModel();
    private static final CommonModel MODIFICATION_12 = CommonModelBuilder.newBuilder(12, GROUP_CATEGORY_HID, 0)
        .parentModelId(10).getModel();
    private static final CommonModel MODIFICATION_13 = CommonModelBuilder.newBuilder(13, GROUP_CATEGORY_HID, 0)
        .parentModelId(10).getModel();

    private static final CommonModel PARENT_MODEL_20 = CommonModelBuilder.newBuilder(20, GROUP_CATEGORY_HID, 0)
        .getModel();
    private static final CommonModel MODIFICATION_21 = CommonModelBuilder.newBuilder(21, GROUP_CATEGORY_HID, 0)
        .parentModelId(20).getModel();

    private static final CommonModel MODEL_30 = CommonModelBuilder.newBuilder(30, NON_GROUP_CATEGORY_HID, 0)
        .getModel();

    private static final CommonModel PARENT_MODEL_40 = CommonModelBuilder.newBuilder(40, GROUP_CATEGORY_HID, 0)
            .getModel();

    private static final CommonModel MODIFICATION_41 = CommonModelBuilder.newBuilder(41, GROUP_CATEGORY_HID, 0)
            .parentModelId(40).parentModel(PARENT_MODEL_40).getModel();

    private static final List<CommonModel> ALL_MODELS = Arrays.asList(PARENT_MODEL_10, MODIFICATION_11,
        MODIFICATION_12, MODIFICATION_13, PARENT_MODEL_20, MODIFICATION_21, MODEL_30, PARENT_MODEL_40, MODIFICATION_41);

    private Map<Long, CommonModel> beforeModels;

    @Override
    public void setup() {
        super.setup();
        beforeModels = ALL_MODELS.stream().collect(Collectors.toMap(CommonModel::getId, Function.identity()));
    }

    @Test
    public void testGroupWithPassedModelAndAllModifications() throws Exception {
        List<CommonModel> models = Arrays.asList(PARENT_MODEL_10, MODIFICATION_11, MODIFICATION_12, MODIFICATION_13);
        List<GeneralizationGroup> groups = order(generalizationService.createGroups(
            models, beforeModels, GeneralizationStrategy.AUTO));

        assertEquals(1, groups.size());
        assertGroupConsists(groups.get(0), PARENT_MODEL_10, MODIFICATION_11, MODIFICATION_12, MODIFICATION_13);
    }

    @Test
    public void testGroupWithDifferentSetOfModels() throws Exception {
        List<CommonModel> models = Arrays.asList(
            PARENT_MODEL_10, MODIFICATION_11, PARENT_MODEL_20, MODIFICATION_21, MODEL_30);
        List<GeneralizationGroup> groups = order(generalizationService.createGroups(
            models, beforeModels, GeneralizationStrategy.AUTO));

        assertEquals(2, groups.size());
        assertGroupConsists(groups.get(0), PARENT_MODEL_10, MODIFICATION_11);
        assertGroupConsists(groups.get(1), PARENT_MODEL_20, MODIFICATION_21);
    }

    @Test
    public void testGroupWithPassedModelsWillNotBeOverridedInGroup() throws Exception {
        CommonModel parentModel = new CommonModel(PARENT_MODEL_10);
        parentModel.setPublished(true);
        CommonModel modification = new CommonModel(MODIFICATION_11);
        modification.setDeleted(true);

        List<CommonModel> models = Arrays.asList(parentModel, modification);
        List<GeneralizationGroup> groups = order(generalizationService.createGroups(
            models, beforeModels, GeneralizationStrategy.AUTO));

        assertEquals(1, groups.size());
        GeneralizationGroup group = groups.get(0);
        assertGroupConsists(group, PARENT_MODEL_10, MODIFICATION_11);

        CommonModel parentModelFromGroup = group.getParentModel();
        CommonModel modificationFromGroup = group.getModifications().stream()
            .filter(m -> m.getId() == modification.getId())
            .findFirst()
            .orElseThrow(() -> new AssertionException("Failed to find modification: " + modification.getId()));

        assertEquals(true, parentModelFromGroup.isPublished());
        assertEquals(true, modificationFromGroup.isDeleted());
        // additional check of correct getAliveModifications method
        assertEquals(group.getAliveModifications().size() + 1, group.getModifications().size());
    }

    @Test
    public void testGroupWithPassedDeletedModel() throws Exception {
        CommonModel parentModel = new CommonModel(PARENT_MODEL_10);
        parentModel.setDeleted(true);

        List<CommonModel> models = Arrays.asList(parentModel, MODIFICATION_11);
        List<GeneralizationGroup> groups = order(generalizationService.createGroups(
            models, beforeModels, GeneralizationStrategy.AUTO));

        assertEquals(1, groups.size());
        assertGroupConsists(groups.get(0), PARENT_MODEL_10, MODIFICATION_11);
    }

    @Test
    public void testGroupWithPassedDeletedModification() throws Exception {
        CommonModel modification = new CommonModel(MODIFICATION_21);
        modification.setDeleted(true);

        List<CommonModel> models = Arrays.asList(PARENT_MODEL_20, modification);
        List<GeneralizationGroup> groups = order(generalizationService.createGroups(
            models, beforeModels, GeneralizationStrategy.AUTO));

        assertEquals(1, groups.size());
        GeneralizationGroup group = groups.get(0);
        assertGroupConsists(group, PARENT_MODEL_20, MODIFICATION_21);
        // additional check of correct getAliveModifications method
        assertEquals(group.getAliveModifications().size() + 1, group.getModifications().size());
    }

    @Test
    public void testModificationMovedToOtherParentModel() throws Exception {
        CommonModel modification = new CommonModel(MODIFICATION_11);
        modification.setParentModelId(PARENT_MODEL_20.getId());

        List<CommonModel> models = Arrays.asList(PARENT_MODEL_10, PARENT_MODEL_20, modification);
        List<GeneralizationGroup> groups = order(generalizationService.createGroups(
            models, beforeModels, GeneralizationStrategy.AUTO));

        assertEquals(2, groups.size());
        assertGroupConsists(groups.get(0), PARENT_MODEL_10);
        assertGroupConsists(groups.get(1), PARENT_MODEL_20, modification);
    }

    @Test(expected = ModelGeneralizationException.class)
    public void testParentModelNotFound() throws Exception {
        CommonModel modification = new CommonModel(MODIFICATION_21);
        modification.setParentModelId(100500);

        List<CommonModel> models = Arrays.asList(modification);
        generalizationService.createGroups(models, beforeModels, GeneralizationStrategy.AUTO);
    }

    private void assertGroupConsists(GeneralizationGroup group, CommonModel parentModel, CommonModel... modifications) {
        String expectedModificationsStr = Stream.of(modifications)
            .map(m -> String.format("(model-id: %d, parent-id: %d)", m.getId(), m.getParentModelId()))
            .collect(Collectors.joining("; "));
        String expectedParentStr = String.format("(parent-model-id: %d)", parentModel.getId());

        String actualModificationsStr = group.getModifications().stream()
            .map(m -> String.format("(model-id: %d, parent-id: %d)", m.getId(), m.getParentModelId()))
            .collect(Collectors.joining("; "));
        String actualParentStr = String.format("(parent-model-id: %d)", group.getParentModel().getId());

        String expectedStr = expectedParentStr + " " + expectedModificationsStr;
        String actualStr = actualParentStr + " " + actualModificationsStr;

        assertEquals("Group doesn't consist of expected models", expectedStr, actualStr);
    }

    private static List<GeneralizationGroup> order(List<GeneralizationGroup> generalizationGroups) {
        return generalizationGroups.stream()
            .sorted(Comparator.comparingLong(GeneralizationGroup::getParentModelId))
            .collect(Collectors.toList());
    }
}
