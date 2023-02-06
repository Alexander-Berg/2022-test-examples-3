package ru.yandex.market.mbo.db.modelstorage.partnergeneralization;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.util.Arrays;
import java.util.List;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class GeneralizeHypothesisPartnerGeneralizationTest extends BasePartnerGeneralizationTest {

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
            .parameterValueHypothesis(1, "param-1", Param.Type.ENUM, "test enum 1")
            .parameterValueHypothesis(2, "param-2", Param.Type.NUMERIC_ENUM, "100.5")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model));

        // generalization will clear all useless param
        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0))
            .doesNotContainParameterValuesHypothesis("param-1", "param-2")
            .containsParameterValues(XslNames.VENDOR, XslNames.NAME);
    }

    @Test
    public void testGroupWithSingleSku() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2)
            .parameterValueHypothesis(1, "param-1", Param.Type.ENUM, "test enum 1")
            .parameterValueHypothesis(2, "param-2", Param.Type.NUMERIC_ENUM, "100.5")
            .parameterValueHypothesis(3, "param-3", Param.Type.ENUM, "emum 3")
            .endModel();
        CommonModel sku = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValueHypothesis(1, "param-1", Param.Type.ENUM, "test enum 1")
            .parameterValueHypothesis(2, "param-2", Param.Type.NUMERIC_ENUM, "200")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0)).getParameterValuesHypothesis("param-1").values("test enum 1");
        MboAssertions.assertThat(models.get(0)).getParameterValuesHypothesis("param-2").values("200");
        MboAssertions.assertThat(models.get(0)).doesNotContainParameterValuesHypothesis("param-3");
    }

    @Test
    public void testGroupWithSeveralSku() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2, 3)
            .parameterValueHypothesis(1, "param-1", Param.Type.ENUM, "test enum 1")
            .parameterValueHypothesis(2, "param-2", Param.Type.NUMERIC_ENUM, "100.5")
            .parameterValueHypothesis(3, "param-3", Param.Type.ENUM, "emum 3")
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValueHypothesis(1, "param-1", Param.Type.ENUM, "test enum 1")
            .parameterValueHypothesis(2, "param-2", Param.Type.NUMERIC_ENUM, "100.5")
            .parameterValueHypothesis(4, "param-4", Param.Type.NUMERIC_ENUM, "enum 4")
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValueHypothesis(1, "param-1", Param.Type.ENUM, "test enum 1")
            .parameterValueHypothesis(2, "param-2", Param.Type.NUMERIC_ENUM, "200")
            .parameterValueHypothesis(4, "param-4", Param.Type.NUMERIC_ENUM, "enum 4")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0)).getParameterValuesHypothesis("param-1").values("test enum 1");
        MboAssertions.assertThat(models.get(0)).getParameterValuesHypothesis("param-4").values("enum 4");
        MboAssertions.assertThat(models.get(0)).doesNotContainParameterValuesHypothesis("param-2", "param-3");
    }

    @Test
    public void testGroupWithSeveralSkuWithoutChanges() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2, 3)
            .parameterValueHypothesis(1, "param-1", Param.Type.ENUM, "test enum 1")
            .parameterValueHypothesis(2, "param-2", Param.Type.NUMERIC_ENUM, "100.5")
            .parameterValueHypothesis(3, "param-3", Param.Type.ENUM, "emum 3")
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValueHypothesis(1, "param-1", Param.Type.ENUM, "test enum 1")
            .parameterValueHypothesis(2, "param-2", Param.Type.NUMERIC_ENUM, "100.5")
            .parameterValueHypothesis(3, "param-3", Param.Type.ENUM, "emum 3")
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValueHypothesis(1, "param-1", Param.Type.ENUM, "test enum 1")
            .parameterValueHypothesis(2, "param-2", Param.Type.NUMERIC_ENUM, "100.5")
            .parameterValueHypothesis(3, "param-3", Param.Type.ENUM, "emum 3")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(models).isEmpty();
    }

    @Test
    public void testGroupWithSingleSkuWithParameterValues() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2)
            .parameterValueHypothesis(1, "param-1", Param.Type.ENUM, "test enum 0.1")
            .parameterValueHypothesis(2, "param-2", Param.Type.NUMERIC_ENUM, "100.5")
            .parameterValueHypothesis(3, "param-3", Param.Type.ENUM, "enum 3", "b")
            .endModel();
        CommonModel sku = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValueHypothesis(1, "param-1", Param.Type.ENUM, "test enum 0.1", "0.2")
            .parameterValueHypothesis(2, "param-2", Param.Type.NUMERIC_ENUM, "100.5", "200")
            .parameterValueHypothesis(3, "param-3", Param.Type.ENUM, "a", " b", "")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0)).getParameterValuesHypothesis("param-1").values("test enum 0.1", "0.2");
        MboAssertions.assertThat(models.get(0)).getParameterValuesHypothesis("param-2").values("100.5", "200");
        MboAssertions.assertThat(models.get(0)).getParameterValuesHypothesis("param-3").values("a", " b", "");
    }

    @Test
    public void testGroupWithSeveralSkuWithParameterValues() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2, 3)
            .parameterValueHypothesis(1, "param-1", Param.Type.ENUM, "2L")
            .parameterValueHypothesis(2, "param-2", Param.Type.ENUM, "value1", "value2")
            .parameterValueHypothesis(3, "param-3", Param.Type.NUMERIC_ENUM, "100", "200")
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValueHypothesis(1, "param-1", Param.Type.ENUM, "2L", "3L")
            .parameterValueHypothesis(2, "param-2", Param.Type.ENUM, "value1", "value2")
            .parameterValueHypothesis(3, "param-3", Param.Type.NUMERIC_ENUM, "1", "2")

            .parameterValueHypothesis(4, "param-4", Param.Type.ENUM, "1L", "2L")
            .parameterValueHypothesis(5, "param-5", Param.Type.ENUM, "value1", "value2")
            .parameterValueHypothesis(6, "param-6", Param.Type.NUMERIC_ENUM, "1", "2")
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValueHypothesis(1, "param-1", Param.Type.ENUM, "1L", "2L")
            .parameterValueHypothesis(2, "param-2", Param.Type.ENUM, "value1")
            .parameterValueHypothesis(3, "param-3", Param.Type.NUMERIC_ENUM, "100", "200")

            .parameterValueHypothesis(4, "param-4", Param.Type.ENUM, "1L", "2L")
            .parameterValueHypothesis(5, "param-5", Param.Type.ENUM, "value1", "value2")
            .parameterValueHypothesis(6, "param-6", Param.Type.NUMERIC_ENUM, "1", "2")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0)).getParameterValuesHypothesis("param-4").values("1L", "2L");
        MboAssertions.assertThat(models.get(0)).getParameterValuesHypothesis("param-5").values("value1", "value2");
        MboAssertions.assertThat(models.get(0)).getParameterValuesHypothesis("param-6").values("1", "2");
        MboAssertions.assertThat(models.get(0))
            .doesNotContainParameterValuesHypothesis("param-1", "param-2", "param-3");
    }

    @Test
    public void testGeneralizationWithParamsWithEqualXslNames() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2, 3)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(2, 2, 3)
            .parameterValueHypothesis(1, "param", Param.Type.ENUM, "value2")
            .parameterValueHypothesis(2, "param", Param.Type.NUMERIC_ENUM, "value1")
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(2, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValueHypothesis(1, "param", Param.Type.ENUM, "value2")
            .parameterValueHypothesis(3, "param", Param.Type.NUMERIC_ENUM, "value1")
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(3, 2, 3)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValueHypothesis(1, "param", Param.Type.ENUM, "value2")
            .parameterValueHypothesis(3, "param", Param.Type.NUMERIC_ENUM, "value1")
            .endModel();

        List<CommonModel> models = generalizationService.generalize(Arrays.asList(model, sku1, sku2));

        Assertions.assertThat(models).extracting(CommonModel::getId).containsExactlyInAnyOrder(1L);
        MboAssertions.assertThat(models.get(0)).getParameterValuesHypothesis(1L).values("value2");
        MboAssertions.assertThat(models.get(0)).getParameterValuesHypothesis(3L).values("value1");
        MboAssertions.assertThat(models.get(0)).doesNotContainParameterValuesHypothesis(2L);
    }
}
