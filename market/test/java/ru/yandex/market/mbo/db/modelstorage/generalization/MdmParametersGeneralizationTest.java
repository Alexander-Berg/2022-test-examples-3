package ru.yandex.market.mbo.db.modelstorage.generalization;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.GeneralizationStrategy;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.ParameterBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.user.AutoUser;

@SuppressWarnings("checkstyle:magicNumber")
public class MdmParametersGeneralizationTest extends BaseGeneralizationTest {

    private static final Long MDM_PARAM_ID = 1L;
    private static final Long CARGOTYPE_PARAM_ID = 2L;
    private static final Long NOMDM_PARAM_ID = 3L;

    private static final String MDM_PARAM_XSL_NAME = XslNames.LIFE_SHELF;
    private static final String CARGOTYPE_PARAM_XSL_NAME = "cargoType485";
    private static final String NOMDM_PARAM_XSL_NAME = "xsl_name_nomdm";

    private static final ParameterValue MDM_PARAM_VALUE =
        createParameterValue(MDM_PARAM_ID, MDM_PARAM_XSL_NAME, "value1");
    private static final ParameterValue CARGOTYPE_PARAM_VALUE =
        createParameterValue(CARGOTYPE_PARAM_ID, CARGOTYPE_PARAM_XSL_NAME, "true");
    private static final ParameterValue NOMDM_PARAM_VALUE =
        createParameterValue(NOMDM_PARAM_ID, NOMDM_PARAM_XSL_NAME, "value2");

    @Before
    public void before() {
        List<CategoryParam> params = new ArrayList<>();
        params.add(ParameterBuilder.builder()
            .id(MDM_PARAM_ID).xsl(MDM_PARAM_XSL_NAME).type(Param.Type.ENUM).endParameter());
        params.add(ParameterBuilder.builder()
            .id(CARGOTYPE_PARAM_ID).xsl(CARGOTYPE_PARAM_XSL_NAME).type(Param.Type.BOOLEAN).endParameter());
        params.add(ParameterBuilder.builder()
            .id(NOMDM_PARAM_ID).xsl(NOMDM_PARAM_XSL_NAME).type(Param.Type.STRING).endParameter());

        generalizationService = new ModelGeneralizationServiceImpl(
            guruService, new AutoUser(AUTO_USER_ID),
            CategoryParametersServiceClientStub.ofCategoryParams(GROUP_CATEGORY_HID, params));
    }

    @Test
    public void testMdmParamAndCargoTypeNotGeneralizedInParent() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 2);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        modifications.get(0).addParameterValue(MDM_PARAM_VALUE);
        modifications.get(0).addParameterValue(CARGOTYPE_PARAM_VALUE);
        modifications.get(0).addParameterValue(NOMDM_PARAM_VALUE);
        modifications.get(1).addParameterValue(MDM_PARAM_VALUE);
        modifications.get(1).addParameterValue(CARGOTYPE_PARAM_VALUE);
        modifications.get(1).addParameterValue(NOMDM_PARAM_VALUE);
        group.setStrategyType(GeneralizationStrategy.AUTO);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        assertParamValue(parentModel, NOMDM_PARAM_VALUE);
        assertNoParamValue(parentModel, MDM_PARAM_ID);
        assertNoParamValue(parentModel, CARGOTYPE_PARAM_ID);

        for (CommonModel modification : modifications) {
            CommonModel updatedModification = getAndAssertModel(updatedModels, modification.getId());

            assertParamValue(updatedModification, MDM_PARAM_VALUE);
            assertParamValue(updatedModification, CARGOTYPE_PARAM_VALUE);
            assertNoParamValue(updatedModification, NOMDM_PARAM_ID);
        }
    }
}
