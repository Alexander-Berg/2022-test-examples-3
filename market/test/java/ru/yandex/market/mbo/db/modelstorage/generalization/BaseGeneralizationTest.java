package ru.yandex.market.mbo.db.modelstorage.generalization;

import org.junit.Before;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.params.guru.BaseGuruServiceImpl;
import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.user.AutoUser;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class BaseGeneralizationTest {

    protected static final long AUTO_USER_ID = 28027378L;
    protected static final long USER_ID = 100500;

    private static final long GROUP_GURU_CATEGORY_ID = 1;
    private static final long RULES_GROUP_GURU_CATEGORY_ID = 2;
    private static final long NON_GROUP_GURU_CATEGORY_ID = 10;

    protected static final long GROUP_CATEGORY_HID = 101;
    protected static final long RULES_GROUP_CATEGORY_HID = 102;
    protected static final long NON_GROUP_CATEGORY_HID = 110;

    protected static final ParameterValue PARAMETER_VALUE_11 = createParameterValue(1L, "value1");
    protected static final ParameterValue PARAMETER_VALUE_12 = createParameterValue(1L, "value2");
    protected static final ParameterValue PARAMETER_VALUE_13 = createParameterValue(1L, "value3");
    protected static final ParameterValue PARAMETER_VALUE_21 = createParameterValue(2L, "value21");
    protected static final ParameterValue PARAMETER_VALUE_22 = createParameterValue(2L, "value22");
    protected static final ParameterValue PARAMETER_VALUE_31 = createParameterValue(3L, "value31");

    protected static final Date VALUE_MODIFICATION_DATE = ModelStorageServiceStub.LAST_MODIFIED_START_DATE;

    protected ModelGeneralizationServiceImpl generalizationService;
    protected BaseGuruServiceImpl guruService;

    @Before
    public void setup() {
        guruService = new BaseGuruServiceImpl();
        guruService.addCategory(GROUP_CATEGORY_HID, GROUP_GURU_CATEGORY_ID, true);
        guruService.addCategory(RULES_GROUP_CATEGORY_HID, RULES_GROUP_GURU_CATEGORY_ID, true);
        guruService.addCategory(NON_GROUP_CATEGORY_HID, NON_GROUP_GURU_CATEGORY_ID, false);

        generalizationService = new ModelGeneralizationServiceImpl(
            guruService, new AutoUser(AUTO_USER_ID),
            CategoryParametersServiceClientStub.ofCategory(GROUP_CATEGORY_HID, Collections.emptyList()));
    }

    protected CommonModel createModel(long id, CommonModel parent, long categoryHid, ParameterValue... params) {
        CommonModel model = buildModel(id, categoryHid, parent.getId(), params);
        model.setParentModel(parent);

        return model;
    }

    protected CommonModel createModel(long id, long categoryHid, Long parentId, ParameterValue... params) {
        return buildModel(id, categoryHid, parentId, params);
    }

    private CommonModel buildModel(long id, long categoryHid, Long parentId, ParameterValue... params) {
        CommonModel model = new CommonModel();
        model.setId(id);
        model.setCategoryId(categoryHid);
        if (parentId != null) {
            model.setParentModelId(parentId);
        }
        for (ParameterValue value : params) {
            model.addParameterValue(value);
        }
        model.setCurrentType(CommonModel.Source.GURU);
        return model;
    }

    private static ParameterValue createParameterValue(Long id, String value) {
        return createParameterValue(id, "xsl_name" + id, value);
    }

    protected static ParameterValue createParameterValue(Long id, String xslName, String value) {
        ParameterValue result = new ParameterValue();
        result.setParamId(id);
        result.setXslName(xslName);
        result.setType(Param.Type.STRING);
        result.setStringValue(WordUtil.defaultWords(value));
        result.setModificationSource(ModificationSource.OPERATOR_FILLED);
        result.setLastModificationDate(VALUE_MODIFICATION_DATE);
        result.setLastModificationUid(USER_ID);
        return result;
    }

    protected GeneralizationGroup createGroup(long categoryHid, int modifs) {
        CommonModel parentModel = createModel(1, categoryHid, null);
        List<CommonModel> modifications = IntStream.range(2, modifs + 2)
            .mapToObj(i -> createModel(i + 1, categoryHid, parentModel.getId()))
            .collect(Collectors.toList());

        GeneralizationGroup generalizationGroup = new GeneralizationGroup(parentModel.getId());
        generalizationGroup.setParentModel(parentModel);
        generalizationGroup.setModifications(modifications);
        return generalizationGroup;
    }

    protected void assertParamValue(CommonModel model, ParameterValue value) {
        ParameterValue paramValue = model.getSingleParameterValue(value.getParamId());
        assertNotNull(paramValue);
        assertEquals(value, paramValue);
    }

    protected void assertParamValueGeneralized(CommonModel model, ParameterValue value) {
        assertParamValueGeneralized(model, value.getParamId());
    }

    protected void assertParamValueGeneralizedWithPreservingMS(CommonModel model, ParameterValue value) {
        long paramId = value.getParamId();
        ParameterValue paramValue = model.getSingleParameterValue(paramId);
        assertNotNull(paramValue);
        assertTrue(paramValue.valueEquals(paramValue));
        assertEquals(value.getModificationSource(), paramValue.getModificationSource());
        assertEquals(value.getLastModificationUid(), paramValue.getLastModificationUid());
    }

    protected void assertParamValueGeneralized(CommonModel model, Long paramId) {
        ParameterValue paramValue = model.getSingleParameterValue(paramId);
        assertNotNull(paramValue);
        assertTrue(paramValue.valueEquals(paramValue));
        assertEquals(ModificationSource.GENERALIZATION, paramValue.getModificationSource());
    }

    protected void assertParamValueNotGeneralized(CommonModel model, Long paramId) {
        ParameterValue paramValue = model.getSingleParameterValue(paramId);
        assertNotNull(paramValue);
        assertTrue(paramValue.valueEquals(paramValue));
        assertNotEquals(ModificationSource.GENERALIZATION, paramValue.getModificationSource());
    }

    protected void assertNoParamValue(CommonModel model, long id) {
        ParameterValue paramValue = model.getSingleParameterValue(id);
        assertNull(paramValue);
    }

    protected CommonModel getAndAssertModel(List<CommonModel> models, long modelId) {
        CommonModel result = null;
        for (CommonModel m : models) {
            if (m.getId() == modelId) {
                result = m;
                break;
            }
        }
        assertNotNull("No model id: " + modelId + " in list", result);
        return result;
    }

    protected CommonModel getAndAssertOnlyModel(List<CommonModel> models, long modelId) {
        assertEquals(models.size(), 1);
        CommonModel result = models.get(0);
        assertEquals(result.getId(), modelId);
        return result;
    }

    protected void assertNoModel(List<CommonModel> models, long modelId) {
        CommonModel result = null;
        for (CommonModel m : models) {
            if (m.getId() == modelId) {
                result = m;
                break;
            }
        }
        assertNull("List contains model id: " + modelId, result);
    }

    protected void copyToBeforeModels(GeneralizationGroup group) {
        group.addBeforeModel(new CommonModel(group.getParentModel()));
        group.getModifications().forEach(m -> group.addBeforeModel(new CommonModel(m)));
    }
}
