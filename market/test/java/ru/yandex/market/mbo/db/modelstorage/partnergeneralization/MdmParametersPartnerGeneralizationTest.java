package ru.yandex.market.mbo.db.modelstorage.partnergeneralization;

import java.math.BigDecimal;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;

/**
 * @author dmserebr
 * @date 14/04/2021
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MdmParametersPartnerGeneralizationTest extends BasePartnerGeneralizationTest {
    private static final long PMODEL_ID = 1L;
    private static final long PSKU_ID_1 = 2L;
    private static final long PSKU_ID_2 = 3L;
    private static final long CATEGORY_ID = 10L;
    private static final long VENDOR_ID = 20L;

    private static final Long MDM_PARAM_ID = 100L;
    private static final Long CARGOTYPE_PARAM_ID = 200L;
    private static final Long NOMDM_PARAM_ID = 300L;
    private static final Long NOMDM_PARAM_ID_2 = 301L;
    private static final String MDM_PARAM_XSL_NAME = XslNames.LIFE_SHELF;
    private static final String CARGOTYPE_PARAM_XSL_NAME = "cargoType485";
    private static final String NOMDM_PARAM_XSL_NAME = "xsl_name_nomdm";

    private static final BigDecimal MDM_PARAM_NUMERIC_VALUE = new BigDecimal(10);

    @Test
    public void testMdmParamAndCargoTypeNotGeneralizedInParent() {
        CommonModel model = CommonModelBuilder.newBuilder(PMODEL_ID, CATEGORY_ID, VENDOR_ID)
            .title("Model").currentType(CommonModel.Source.PARTNER)
            .withSkuRelations(CATEGORY_ID, PSKU_ID_1, PSKU_ID_2)
            .parameterValues(NOMDM_PARAM_ID_2, "param", 1L)
            .endModel();
        CommonModel sku1 = CommonModelBuilder.newBuilder(PSKU_ID_1, CATEGORY_ID, VENDOR_ID)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValues(MDM_PARAM_ID, MDM_PARAM_XSL_NAME, MDM_PARAM_NUMERIC_VALUE)
            .parameterValues(CARGOTYPE_PARAM_ID, CARGOTYPE_PARAM_XSL_NAME, true)
            .parameterValues(NOMDM_PARAM_ID, NOMDM_PARAM_XSL_NAME, "qqq")
            .endModel();
        CommonModel sku2 = CommonModelBuilder.newBuilder(PSKU_ID_2, CATEGORY_ID, VENDOR_ID)
            .title("Sku").currentType(CommonModel.Source.PARTNER_SKU)
            .withSkuParentRelation(model)
            .parameterValues(MDM_PARAM_ID, MDM_PARAM_XSL_NAME, MDM_PARAM_NUMERIC_VALUE)
            .parameterValues(CARGOTYPE_PARAM_ID, CARGOTYPE_PARAM_XSL_NAME, true)
            .parameterValues(NOMDM_PARAM_ID, NOMDM_PARAM_XSL_NAME, "qqq")
            .endModel();

        PartnerGeneralizationGroup partnerGeneralizationGroup = new PartnerGeneralizationGroup(
            model, ImmutableList.of(sku1, sku2));
        List<CommonModel> updatedModels = generalizationService.generalizeGroup(partnerGeneralizationGroup);
        Assert.assertEquals(1, updatedModels.size());

        CommonModel parentModel = updatedModels.stream()
            .filter(m -> m.getId() == partnerGeneralizationGroup.getPartnerId())
            .findFirst().get();
        Assert.assertEquals(3, parentModel.getParameterValues().size());
        Assert.assertNotNull(parentModel.getSingleParameterValue(KnownIds.NAME_PARAM_ID));
        Assert.assertNotNull(parentModel.getSingleParameterValue(KnownIds.VENDOR_PARAM_ID));
        Assert.assertNotNull(parentModel.getSingleParameterValue(NOMDM_PARAM_ID));
        Assert.assertNull(parentModel.getSingleParameterValue(MDM_PARAM_ID));
        Assert.assertNull(parentModel.getSingleParameterValue(CARGOTYPE_PARAM_ID));
    }
}
