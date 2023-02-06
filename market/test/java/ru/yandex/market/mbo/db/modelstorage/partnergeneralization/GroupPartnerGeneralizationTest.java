package ru.yandex.market.mbo.db.modelstorage.partnergeneralization;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class GroupPartnerGeneralizationTest extends BasePartnerGeneralizationTest {
    private static final long CATEGORY_ID = 100L;
    private static final ReadStats STATS = new ReadStats();

    @Test
    public void testZeroGroupsInNotPartnerModels() {
        List<CommonModel> models = Arrays.stream(CommonModel.Source.values())
            .filter(t -> t != CommonModel.Source.PARTNER && t != CommonModel.Source.PARTNER_SKU)
            .map(t -> CommonModelBuilder.newBuilder().currentType(t).endModel())
            .collect(Collectors.toList());

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(models);
        Assertions.assertThat(groups).isEmpty();
    }

    @Test
    public void testCreateOneGroupForNewPModel() {
        CommonModel model = CommonModelBuilder.newBuilder()
            .currentType(CommonModel.Source.PARTNER)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Collections.singleton(model));

        Assertions.assertThat(groups)
            .containsExactlyInAnyOrder(new PartnerGeneralizationGroup(model));
    }

    @Test
    public void testCreateOneGroupForPModel() {
        CommonModel model = CommonModelBuilder.newBuilder(1, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Collections.singleton(model));

        Assertions.assertThat(groups)
            .containsExactlyInAnyOrder(new PartnerGeneralizationGroup(model));
    }

    @Test
    public void testCreateSeveralGroupsForNewPModels() {
        CommonModel model1 = CommonModelBuilder.newBuilder()
            .currentType(CommonModel.Source.PARTNER)
            .endModel();
        CommonModel model2 = CommonModelBuilder.newBuilder()
            .currentType(CommonModel.Source.PARTNER)
            .endModel();
        CommonModel model3 = CommonModelBuilder.newBuilder()
            .currentType(CommonModel.Source.PARTNER)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Arrays.asList(model1, model2, model3));

        Assertions.assertThat(groups)
            .containsExactlyInAnyOrder(
                new PartnerGeneralizationGroup(model1),
                new PartnerGeneralizationGroup(model2),
                new PartnerGeneralizationGroup(model3)
            );
    }

    @Test
    public void testCreateSeveralGroupsForPModels() {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .endModel();
        CommonModel model2 = CommonModelBuilder.newBuilder(2, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .endModel();
        CommonModel model3 = CommonModelBuilder.newBuilder(3, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Arrays.asList(model1, model2, model3));

        Assertions.assertThat(groups)
            .containsExactlyInAnyOrder(
                new PartnerGeneralizationGroup(model1),
                new PartnerGeneralizationGroup(model2),
                new PartnerGeneralizationGroup(model3)
            );
    }

    @Test
    public void testCreateSeveralGroupsForNegativePModels() {
        CommonModel model1 = CommonModelBuilder.newBuilder(-1, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .endModel();
        CommonModel model2 = CommonModelBuilder.newBuilder(-2, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .endModel();
        CommonModel model3 = CommonModelBuilder.newBuilder(-3, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Arrays.asList(model1, model2, model3));

        Assertions.assertThat(groups)
            .containsExactlyInAnyOrder(
                new PartnerGeneralizationGroup(model1),
                new PartnerGeneralizationGroup(model2),
                new PartnerGeneralizationGroup(model3)
            );
    }

    @Test
    public void testCreateSeveralGroupsNewAndExistingPModels() {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .endModel();
        CommonModel model2 = CommonModelBuilder.newBuilder(2, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .endModel();
        CommonModel model3 = CommonModelBuilder.newBuilder(0, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .endModel();
        CommonModel model4 = CommonModelBuilder.newBuilder(0, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Arrays.asList(model1, model2, model3, model4));

        Assertions.assertThat(groups)
            .containsExactlyInAnyOrder(
                new PartnerGeneralizationGroup(model1),
                new PartnerGeneralizationGroup(model2),
                new PartnerGeneralizationGroup(model3),
                new PartnerGeneralizationGroup(model4)
            );
    }

    @Test
    public void testFailIfPartnerSkuHasLinkToZeroPartnerModel() {
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(CATEGORY_ID, 0)
            .endModel();

        Assertions.assertThatThrownBy(() -> {
            generalizationService.createGroups(Collections.singletonList(sku1));
        }).hasMessage("Model id: 2. Partner_sku contains link to partner model with id = 0");
    }

    @Test
    public void testFailIfSaveGroupWithNegativeId() {
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(CATEGORY_ID, -1)
            .endModel();

        Assertions.assertThatThrownBy(() -> {
            generalizationService.createGroups(Collections.singletonList(sku1));
        }).hasMessage("Model id: -1. Partner model is missing! id: -1");
    }

    @Test
    public void testFailIfPartnerModelHasLinkToMissingModel() {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(CATEGORY_ID, -2)
            .endModel();

        Assertions.assertThatThrownBy(() -> {
            generalizationService.createGroups(Collections.singletonList(model1));
        }).hasMessage("Model id: 1. Related pskus (ids: -2) are missing!");
    }

    @Test
    public void testNotProcessDeletedModel() {
        CommonModel model = CommonModelBuilder.newBuilder()
            .currentType(CommonModel.Source.PARTNER).deleted(true)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Collections.singleton(model));

        Assertions.assertThat(groups).isEmpty();
    }

    @Test
    public void testNotProcessDeletedModelWithSku() {
        CommonModel model = CommonModelBuilder.newBuilder(1, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER).deleted(true)
            .withSkuRelations(CATEGORY_ID, 2, 3)
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU).deleted(true)
            .withSkuParentRelation(model)
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(groups).isEmpty();
    }

    @Test
    public void testCreateGroupWithModelAndDeletedSku() {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(CATEGORY_ID, 2, 3)
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU).deleted(true)
            .withSkuParentRelation(model1)
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model1)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Arrays.asList(sku2, model1, sku1));

        Assertions.assertThat(groups)
            .containsExactlyInAnyOrder(
                new PartnerGeneralizationGroup(model1, sku2)
            );
    }

    @Test
    public void testCreateGroupWithModelAndDeletedSkuWitchExistsInModelStorage() {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(CATEGORY_ID, 2, 3)
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model1)
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model1)
            .endModel();
        // mark sku1 as deleted
        sku1.setDeleted(true);

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Arrays.asList(sku2, model1, sku1));

        // result group won't contain deleted sku
        Assertions.assertThat(groups)
            .containsExactlyInAnyOrder(
                new PartnerGeneralizationGroup(model1, sku2)
            );
    }

    @Test
    public void testProcessUnpublishedModel() {
        CommonModel modelUnpublishedOnWhiteMarket = CommonModelBuilder.newBuilder()
            .currentType(CommonModel.Source.PARTNER).published(false)
            .endModel();
        CommonModel modelUnpublishedOnBlueMarket = CommonModelBuilder.newBuilder()
            .currentType(CommonModel.Source.PARTNER).publishedOnBlue(false)
            .endModel();
        CommonModel modelUnpublishedOAnyMarket = CommonModelBuilder.newBuilder()
            .currentType(CommonModel.Source.PARTNER).published(false).publishedOnBlue(false)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Arrays.asList(modelUnpublishedOnWhiteMarket, modelUnpublishedOnBlueMarket, modelUnpublishedOAnyMarket));

        Assertions.assertThat(groups)
            .containsExactlyInAnyOrder(
                new PartnerGeneralizationGroup(modelUnpublishedOnWhiteMarket),
                new PartnerGeneralizationGroup(modelUnpublishedOnBlueMarket),
                new PartnerGeneralizationGroup(modelUnpublishedOAnyMarket)
            );
    }

    @Test
    public void testProcessUnpublishedModelWithSku() {
        CommonModel model = CommonModelBuilder.newBuilder(1, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER).published(false).publishedOnBlue(false)
            .withSkuRelations(CATEGORY_ID, 2, 3)
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU).published(false).publishedOnBlue(false)
            .withSkuParentRelation(model)
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(groups)
            .containsExactlyInAnyOrder(
                new PartnerGeneralizationGroup(model, sku2)
            );
    }

    @Test
    public void testCreateGroupWithModelAndUnpublishedSku() {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(CATEGORY_ID, 2, 3)
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU).published(false).publishedOnBlue(false)
            .withSkuParentRelation(model1)
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU).published(false)
            .withSkuParentRelation(model1)
            .endModel();
        CommonModel sku3 = CommonModelBuilder.newBuilder(4, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU).publishedOnBlue(false)
            .withSkuParentRelation(model1)
            .endModel();
        CommonModel sku4 = CommonModelBuilder.newBuilder(5, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model1)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Arrays.asList(sku2, model1, sku1, sku3, sku4));

        Assertions.assertThat(groups)
            .containsExactlyInAnyOrder(
                new PartnerGeneralizationGroup(model1, sku2, sku3, sku4)
            );
    }

    @Test
    public void testCreateGroupWithPModelAndPSkus() {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(CATEGORY_ID, 2, 3)
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model1)
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model1)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Arrays.asList(sku2, model1, sku1));

        Assertions.assertThat(groups)
            .containsExactlyInAnyOrder(
                new PartnerGeneralizationGroup(model1, sku1, sku2)
            );
    }

    @Test
    public void testCreateGroupWithNegativePModelAndPSkus() {
        CommonModel model1 = CommonModelBuilder.newBuilder(-1, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(CATEGORY_ID, -2, -3)
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(-2, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model1)
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(-3, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model1)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Arrays.asList(sku2, model1, sku1));

        Assertions.assertThat(groups)
            .containsExactlyInAnyOrder(
                new PartnerGeneralizationGroup(model1, sku1, sku2)
            );
    }

    @Test
    public void testCreateSeveralGroupsWithPModelAndSkus() {
        CommonModel model1 = CommonModelBuilder.newBuilder(1, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(CATEGORY_ID, 2, 3)
            .endModel();
        CommonModel sku11 = CommonModelBuilder.newBuilder(2, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model1)
            .endModel();
        CommonModel sku21 = CommonModelBuilder.newBuilder(3, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model1)
            .endModel();
        CommonModel model2 = CommonModelBuilder.newBuilder(-1, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(CATEGORY_ID, -2, -3)
            .endModel();
        CommonModel sku12 = CommonModelBuilder.newBuilder(-2, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model2)
            .endModel();
        CommonModel sku22 = CommonModelBuilder.newBuilder(-3, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model2)
            .endModel();
        CommonModel model3 = CommonModelBuilder.newBuilder(0, CATEGORY_ID)
            .currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(CATEGORY_ID)
            .endModel();

        List<PartnerGeneralizationGroup> groups = generalizationService.createGroups(
            Arrays.asList(sku11, sku22, model1, model2, sku21, sku12, model3));

        Assertions.assertThat(groups)
            .containsExactlyInAnyOrder(
                new PartnerGeneralizationGroup(model1, sku11, sku21),
                new PartnerGeneralizationGroup(model2, sku12, sku22),
                new PartnerGeneralizationGroup(model3)
            );
    }
}
