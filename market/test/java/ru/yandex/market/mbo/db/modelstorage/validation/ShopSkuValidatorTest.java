package ru.yandex.market.mbo.db.modelstorage.validation;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author york
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ShopSkuValidatorTest {
    private ShopSkuValidator shopSkuValidator;
    private ModelValidationContext context;

    @Before
    public void setUp() throws Exception {
        shopSkuValidator = new ShopSkuValidator();
        context = mock(ModelValidationContext.class);
    }

    @Test
    public void noShopSku() {
        CommonModel model = CommonModelBuilder.newBuilder()
                .id(1L)
                .currentType(CommonModel.Source.FAST_SKU)
                .getModel();

        List<ModelValidationError> errors = shopSkuValidator.validate(context, new ModelChanges(model),
            Collections.singletonList(model));

        assertThat(errors).containsExactlyInAnyOrder(ShopSkuValidator.createMissingError(model));
    }

    @Test
    public void hasShopSku() {
        CommonModel model = CommonModelBuilder.newBuilder()
                .id(1L)
                .currentType(CommonModel.Source.FAST_SKU)
                .startParameterValue()
                    .paramId(10L)
                    .xslName(XslNames.SHOP_SKU)
                    .words("bla")
                .endParameterValue()
                .getModel();

        List<ModelValidationError> errors = shopSkuValidator.validate(context, new ModelChanges(model),
            Collections.singletonList(model));

        assertThat(errors).isEmpty();
    }

    @Test
    public void updatedShopSku() {
        CommonModel before = CommonModelBuilder.newBuilder()
                .id(1L)
                .currentType(CommonModel.Source.FAST_SKU)
                .startParameterValue()
                    .paramId(10L)
                    .xslName(XslNames.SHOP_SKU)
                    .words("bla")
                .endParameterValue()
                .getModel();


        CommonModel after = new CommonModel(before);
        ParameterValue newValue = new ParameterValue(before.getSingleParameterValue(XslNames.SHOP_SKU));
        newValue.setStringValue(Collections.singletonList(
                WordUtil.defaultWord("ble")
        ));
        after.removeAllParameterValues(XslNames.SHOP_SKU);
        after.addParameterValue(newValue);

        List<ModelValidationError> errors = shopSkuValidator.validate(context, new ModelChanges(before, after),
            Collections.singletonList(after));

        assertThat(errors).containsExactlyInAnyOrder(ShopSkuValidator.createChangedError(after));
    }

    @Test
    public void notUpdatedShopSku() {
        CommonModel before = CommonModelBuilder.newBuilder()
                .id(1L)
                .currentType(CommonModel.Source.FAST_SKU)
                .startParameterValue()
                .paramId(10L)
                .xslName(XslNames.SHOP_SKU)
                .words("bla")
                .endParameterValue()
                .getModel();


        CommonModel after = new CommonModel(before);

        List<ModelValidationError> errors = shopSkuValidator.validate(context, new ModelChanges(before, after),
                Collections.singletonList(after));

        assertThat(errors).isEmpty();
    }
}
