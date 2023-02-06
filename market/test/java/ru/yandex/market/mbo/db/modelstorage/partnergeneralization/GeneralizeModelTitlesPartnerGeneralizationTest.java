package ru.yandex.market.mbo.db.modelstorage.partnergeneralization;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.util.Arrays;
import java.util.List;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class GeneralizeModelTitlesPartnerGeneralizationTest extends BasePartnerGeneralizationTest {

    @Test
    public void testModelWithoutTitleWillCreateEmptyTitle() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .currentType(CommonModel.Source.PARTNER)
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0)).hasTitle("");
    }

    @Test
    public void testModelWithEmptyTitleWontChange() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("").currentType(CommonModel.Source.PARTNER)
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model));

        Assertions.assertThat(models).isEmpty();
    }

    @Test
    public void testModelWithTitleChangeToEmpty() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Title").currentType(CommonModel.Source.PARTNER)
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0)).hasTitle("");
    }

    @Test
    public void testGroupWithSingleSku() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2)
            .endModel();
        CommonModel sku = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Sku title").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0)).hasTitle("Sku title");
    }

    @Test
    public void testGroupWithSingleSkuAndEqualTitle() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Title").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2)
            .endModel();
        CommonModel sku = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Title").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku));

        Assertions.assertThat(models).isEmpty();
    }

    @Test
    public void testGroupWithSeveralSku() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2, 3)
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Title 1").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, 2, 3)
            .title("Title 2").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0)).hasTitle("Title");
    }

    @Test
    public void testGroupWithSeveralSkuAndEqualTitle() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Title").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2, 3)
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Title 1").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, 2, 3)
            .title("Title 2").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(models).isEmpty();
    }
}
