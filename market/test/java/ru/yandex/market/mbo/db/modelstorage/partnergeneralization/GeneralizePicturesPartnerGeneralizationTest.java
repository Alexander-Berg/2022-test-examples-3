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
public class GeneralizePicturesPartnerGeneralizationTest extends BasePartnerGeneralizationTest {

    @Test
    public void testEmptyModelWontChange() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("")
            .currentType(CommonModel.Source.PARTNER)
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model));

        Assertions.assertThat(models).isEmpty();
    }

    @Test
    public void testGroupWithSingleModelWillBeEmpty() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .picture("http://test-url.ru/pic1")
            .picture("http://test-url.ru/pic2")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0))
            .doesContainPictures();
    }

    @Test
    public void testGroupWithSingleSku() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2)
            .endModel();
        CommonModel sku = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .picture("http://test-url.ru/pic1")
            .picture("http://test-url.ru/pic2")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0))
            .containsUrlPicturesExactlyInOrder("http://test-url.ru/pic1", "http://test-url.ru/pic2");
    }

    @Test
    public void testGroupWithSeveralSku() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2, 3)
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .picture("http://test-url.ru/pic1")
            .picture("http://test-url.ru/pic2")
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .picture("http://test-url.ru/pic2")
            .picture("http://test-url.ru/pic3")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0))
            .containsUrlPicturesExactlyInOrder("http://test-url.ru/pic1", "http://test-url.ru/pic2",
                "http://test-url.ru/pic3");
    }

    @Test
    public void testGroupWithSeveralSkuWithNotEmptyModelPictures() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2, 3)
            .picture("http://test-url.ru/aaaaa")
            .picture("http://test-url.ru/bbbbb")
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .picture("http://test-url.ru/pic1")
            .picture("http://test-url.ru/pic2")
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .picture("http://test-url.ru/pic2")
            .picture("http://test-url.ru/pic3")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0))
            .containsUrlPicturesExactlyInOrder("http://test-url.ru/pic1", "http://test-url.ru/pic2",
                "http://test-url.ru/pic3");
    }

    @Test
    public void testGroupWithSeveralSkuWithoutChanges() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2, 3)
            .picture("http://test-url.ru/pic1")
            .picture("http://test-url.ru/pic2")
            .picture("http://test-url.ru/pic3")
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .picture("http://test-url.ru/pic1")
            .picture("http://test-url.ru/pic2")
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .picture("http://test-url.ru/pic2")
            .picture("http://test-url.ru/pic3")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(models).isEmpty();
    }
}
