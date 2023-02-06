package ru.yandex.market.mbo.db.modelstorage.partnergeneralization;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class GeneralizeValuesPartnerGeneralizationTest extends BasePartnerGeneralizationTest {

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
    public void testGroupWithSingleModel() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .parameterValues(1, "param-1", 1L)
            .parameterValues(2, "param-2", "value")
            .parameterValues(3, "param-3", new BigDecimal("100"))
            .parameterValues(4, "param-4", true)
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model));

        // generalization will clear all useless param
        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0))
            .doesNotContainParameterValues("param-1", "param-2", "param-3", "param-4")
            .containsParameterValues(XslNames.VENDOR, XslNames.NAME);
    }

    @Test
    public void testGroupWithSingleSku() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2)
            .parameterValues(1, "param-1", 1L)
            .parameterValues(2, "param-2", "value")
            .parameterValues(3, "param-3", new BigDecimal("100"))
            .parameterValues(4, "param-4", true)
            .parameterValues(5, "delete-param", "value-to-be-deleted")
            .endModel();
        CommonModel sku = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValues(1, "param-1", 2L)
            .parameterValues(2, "param-2", "value_2")
            .parameterValues(3, "param-3", new BigDecimal("200"))
            .parameterValues(4, "param-4", false)
            .parameterValues(6, "add-param", "value-to-be-added")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0)).getParameterValues("param-1").values(2L);
        MboAssertions.assertThat(models.get(0)).getParameterValues("param-2").values("value_2");
        MboAssertions.assertThat(models.get(0)).getParameterValues("param-3").values(new BigDecimal("200"));
        MboAssertions.assertThat(models.get(0)).getParameterValues("param-4").values(false);
        MboAssertions.assertThat(models.get(0)).getParameterValues("add-param").values("value-to-be-added");
        MboAssertions.assertThat(models.get(0)).doesNotContainParameterValues("delete-param");
    }

    @Test
    public void testGroupWithSeveralSku() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2, 3)
            .parameterValues(1, "param-1", 1L)
            .parameterValues(2, "param-2", "value")
            .parameterValues(3, "param-3", new BigDecimal("100"))
            .parameterValues(4, "param-4", true)
            .parameterValues(5, "delete-param", "value-to-be-deleted")
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValues(1, "param-1", 2L)
            .parameterValues(2, "param-2", "value")
            .parameterValues(4, "param-4", false)
            .parameterValues(6, "add-param", "value-to-be-added")
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValues(1, "param-1", 2L)
            .parameterValues(2, "param-2", "value_2")
            .parameterValues(3, "param-3", new BigDecimal("200"))
            .parameterValues(4, "param-4", false)
            .parameterValues(6, "add-param", "value-to-be-added")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0)).getParameterValues("param-1").values(2L);
        MboAssertions.assertThat(models.get(0)).getParameterValues("param-4").values(false);
        MboAssertions.assertThat(models.get(0)).getParameterValues("add-param").values("value-to-be-added");
        MboAssertions.assertThat(models.get(0)).doesNotContainParameterValues("param-2", "param-3", "delete-param");
    }

    @Test
    public void testGroupWithSeveralSkuWithoutChanges() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2, 3)
            .parameterValues(1, "param-1", 1L)
            .parameterValues(2, "param-2", "value")
            .parameterValues(3, "param-3", new BigDecimal("100"))
            .parameterValues(4, "param-4", true)
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValues(1, "param-1", 1L)
            .parameterValues(2, "param-2", "value")
            .parameterValues(3, "param-3", new BigDecimal("100"))
            .parameterValues(4, "param-4", true)
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValues(1, "param-1", 1L)
            .parameterValues(2, "param-2", "value")
            .parameterValues(3, "param-3", new BigDecimal("100"))
            .parameterValues(4, "param-4", true)
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(models).isEmpty();
    }

    @Test
    public void testGroupWithSingleSkuWithParameterValues() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2)
            .parameterValues(1, "param-1", 1L, 2L)
            .parameterValues(2, "param-2", "value1", "value2")
            .parameterValues(3, "param-3", new BigDecimal("100"), new BigDecimal("200"))
            .endModel();
        CommonModel sku = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValues(1, "param-1", 1L, 2L, 3L)
            .parameterValues(2, "param-2", "value3", "value4 ", "")
            .parameterValues(3, "param-3", new BigDecimal("1"), new BigDecimal("2"))
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0)).getParameterValues("param-1").values(1L, 2L, 3L);
        MboAssertions.assertThat(models.get(0)).getParameterValues("param-2").values("value3", "value4 ", "");
        MboAssertions.assertThat(models.get(0)).getParameterValues("param-3").values(1.0, 2.0);
    }

    @Test
    public void testGroupWithSeveralSkuWithParameterValues() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2, 3)
            .parameterValues(1, "param-1", 2L)
            .parameterValues(2, "param-2", "value1", "value2")
            .parameterValues(3, "param-3", new BigDecimal("100"), new BigDecimal("200"))
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValues(1, "param-1", 2L, 3L)
            .parameterValues(2, "param-2", "value1", "value2")
            .parameterValues(3, "param-3", new BigDecimal("1"), new BigDecimal("2"))

            .parameterValues(4, "param-4", 1L, 2L)
            .parameterValues(5, "param-5", "value1", "value2")
            .parameterValues(6, "param-6", new BigDecimal("1"), new BigDecimal("2"))
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValues(1, "param-1", 1L, 2L)
            .parameterValues(2, "param-2", "value1")
            .parameterValues(3, "param-3", new BigDecimal("100"), new BigDecimal("200"))

            .parameterValues(4, "param-4", 1L, 2L)
            .parameterValues(5, "param-5", "value1", "value2")
            .parameterValues(6, "param-6", new BigDecimal("1"), new BigDecimal("2"))
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0)).getParameterValues("param-4").values(1L, 2L);
        MboAssertions.assertThat(models.get(0)).getParameterValues("param-5").values("value1", "value2");
        MboAssertions.assertThat(models.get(0)).getParameterValues("param-6").values(1.0, 2.0);
        MboAssertions.assertThat(models.get(0)).doesNotContainParameterValues("param-1", "param-2", "param-3");
    }

    @Test
    public void testGeneralizationWithParamsWithEqualXslNames() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2, 3)
            .parameterValues(1, "param", 2L)
            .parameterValues(2, "param", "value1")
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValues(1, "param", 2L)
            .parameterValues(3, "param", "value3")
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValues(1, "param", 2L)
            .parameterValues(3, "param", "value3")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0)).getParameterValues(1L).values(2L);
        MboAssertions.assertThat(models.get(0)).getParameterValues(3L).values("value3");
        MboAssertions.assertThat(models.get(0)).doesNotContainParameterValues(2L);
    }
}
