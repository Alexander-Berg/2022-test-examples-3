package ru.yandex.market.mbo.db.modelstorage.validation;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.validation.context.CachingModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.CATEGORY_ID;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.VENDOR_ID;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.createSkuDuplicateParamError;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getDefaultSkuBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuru;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getParentWithRelation;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getSkuBuilderWithoutDefiningParams;

/**
 * @author danfertev
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SkuUniquenessValidatorTest {
    private static final Map<Long, String> SKU_DEFINING_PARAMS = ImmutableMap.of(
        1L, "param1",
        2L, "param2",
        3L, "param3",
        4L, "param4"
    );

    private SkuUniquenessValidator validator;
    private ModelValidationContext context;

    @Before
    public void setup() {
        validator = new SkuUniquenessValidator();
        context = mock(CachingModelValidationContext.class);
        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(), anyCollection(), anyBoolean()))
            .thenCallRealMethod();
    }

    @Test
    public void testValidationNotRunOnSku() {
        CommonModel sku = getDefaultSkuBuilder().id(10L).endModel();

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_DEFINING)))
            .thenReturn(SKU_DEFINING_PARAMS);

        List<ModelValidationError> errors = validator.validate(
            context,
            getCreateOperation(sku),
            Collections.singletonList(sku)
        );

        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidationNotRunWithoutSku() {
        CommonModel parent = getGuru();
        CommonModel guru1 = CommonModelBuilder.newBuilder(1L, CATEGORY_ID, VENDOR_ID).getModel();
        CommonModel guru2 = CommonModelBuilder.newBuilder(2L, CATEGORY_ID, VENDOR_ID).getModel();

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_DEFINING)))
            .thenReturn(SKU_DEFINING_PARAMS);

        List<ModelValidationError> errors = validator.validate(
            context,
            getCreateOperation(parent),
            Arrays.asList(parent, guru1, guru2)
        );

        assertTrue(errors.isEmpty());
    }

    @Test
    public void testNotDefining() {
        CommonModel parent = getGuru();

        //Duplicate values of informative and sku_none parameters do not affect validation
        CommonModel sku1 = getSkuBuilderWithoutDefiningParams()
            .id(10L)
            .param(1L).setNumeric(1)
            .param(2L).setNumeric(1)
            .endModel();

        CommonModel sku2 = getSkuBuilderWithoutDefiningParams()
            .id(11L)
            .param(1L).setNumeric(1)
            .param(2L).setNumeric(1)
            .endModel();

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_DEFINING)))
            .thenReturn(Collections.emptyMap());

        applyModelStorageServiceMock(context, Arrays.asList(sku1, sku2));

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdateOperation(parent),
            Arrays.asList(parent, sku1, sku2)
        );

        assertTrue(errors.isEmpty());
    }

    @Test
    public void testUnique() {
        CommonModel parent = getParentWithRelation(10L, 11L, 12L, 13L);

        CommonModel sku1 = getDefaultSkuBuilder()
            .id(10L)
            .param(1L).setNumeric(1)
            .param(2L).setString("A")
            .param(3L).setOption(1)
            .param(4L).setOption(1)
            .endModel();

        CommonModel sku2 = getDefaultSkuBuilder()
            .id(11L)
            .param(1L).setNumeric(1)
            .param(2L).setString("A")
            .param(3L).setOption(1)
            .param(4L).setOption(2)
            .endModel();

        CommonModel sku3 = getDefaultSkuBuilder()
            .id(12L)
            .param(2L).setString("A")
            .endModel();

        CommonModel sku4 = getDefaultSkuBuilder()
            .id(13L)
            .endModel();

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_DEFINING)))
            .thenReturn(SKU_DEFINING_PARAMS);

        applyModelStorageServiceMock(context, Arrays.asList(sku1, sku2, sku3, sku4));

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdateOperation(parent),
            Arrays.asList(parent, sku1, sku2)
        );

        assertTrue(errors.isEmpty());
    }

    @Test
    public void testInconsistentModelAddNewSku() {
        CommonModel parent = getParentWithRelation(10L, 11L, 12L, -13L);

        CommonModel sku1 = getDefaultSkuBuilder()
            .id(10L)
            .param(2L).setString("FIRST")
            .endModel();

        CommonModel sku2 = getDefaultSkuBuilder()
            .id(11L)
            .param(2L).setString("A")
            .endModel();

        CommonModel sku3 = getDefaultSkuBuilder()
            .id(12L)
            .param(2L).setString("A")
            .endModel();

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_DEFINING)))
            .thenReturn(SKU_DEFINING_PARAMS);

        applyModelStorageServiceMock(context, Arrays.asList(sku1, sku2, sku3));

        CommonModel sku4 = getDefaultSkuBuilder()
            .id(-13L)
            .param(2L).setString("B")
            .endModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdateOperation(parent),
            Arrays.asList(parent, sku1, sku2, sku3, sku4)
        );

        assertFalse(errors.get(0).isCritical());
    }

    @Test
    public void testInconsistentModelAddDuplicate() {
        CommonModel parent = getParentWithRelation(10L, 11L, 12L, -13L);

        CommonModel sku1 = getDefaultSkuBuilder()
            .id(10L)
            .param(2L).setString("FIRST")
            .endModel();

        CommonModel sku2 = getDefaultSkuBuilder()
            .id(11L)
            .param(2L).setString("A")
            .endModel();

        CommonModel sku3 = getDefaultSkuBuilder()
            .id(12L)
            .param(2L).setString("A")
            .endModel();

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_DEFINING)))
            .thenReturn(SKU_DEFINING_PARAMS);

        applyModelStorageServiceMock(context, Arrays.asList(sku1, sku2, sku3));

        CommonModel sku4 = getDefaultSkuBuilder()
            .id(-13L)
            .param(2L).setString("FIRST")
            .endModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdateOperation(parent),
            Arrays.asList(parent, sku1, sku2, sku3, sku4)
        );

        assertTrue(errors.get(0).isCritical());
    }

    @Test
    public void testInconsistentModelRemoveDuplicate() {
        CommonModel parent = getParentWithRelation(10L, 11L, 12L);

        CommonModel sku1 = getDefaultSkuBuilder()
            .id(10L)
            .param(2L).setString("FIRST")
            .endModel();

        CommonModel sku2 = getDefaultSkuBuilder()
            .id(11L)
            .param(2L).setString("A")
            .endModel();

        CommonModel sku3 = getDefaultSkuBuilder()
            .id(12L)
            .param(2L).setString("A")
            .endModel();

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_DEFINING)))
            .thenReturn(SKU_DEFINING_PARAMS);

        applyModelStorageServiceMock(context, Arrays.asList(sku1, sku2, sku3));

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdateOperation(parent),
            Arrays.asList(parent, sku1, sku2)
        );

        assertFalse(errors.get(0).isCritical());
    }

    @Test
    public void testInconsistentModelChangeDuplicateToUnique() {
        CommonModel parent = getParentWithRelation(10L, 11L, 12L);

        CommonModel sku1 = getDefaultSkuBuilder()
            .id(10L)
            .param(2L).setString("FIRST")
            .endModel();

        CommonModel sku2 = getDefaultSkuBuilder()
            .id(11L)
            .param(2L).setString("A")
            .endModel();

        CommonModel sku3 = getDefaultSkuBuilder()
            .id(12L)
            .param(2L).setString("A")
            .endModel();

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_DEFINING)))
            .thenReturn(SKU_DEFINING_PARAMS);

        applyModelStorageServiceMock(context, Arrays.asList(sku1, sku2, sku3));

        sku3 = getDefaultSkuBuilder()
            .id(12L)
            .param(2L).setString("B")
            .endModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdateOperation(parent),
            Arrays.asList(parent, sku1, sku2, sku3)
        );

        assertTrue(errors.isEmpty());
    }

    @Test
    public void testUniqueHypothesis() {
        CommonModel parent = getParentWithRelation(10L, 11L, 12L, 13L);

        CommonModel sku1 = getDefaultSkuBuilder()
            .id(10L)
            .param(1L).setNumeric(1)
            .param(2L).setString("A")
            .param(3L).setOption(1)
            .parameterValueHypothesis(4L, "param4", Param.Type.ENUM, "test")
            .endModel();

        CommonModel sku2 = getDefaultSkuBuilder()
            .id(11L)
            .param(1L).setNumeric(1)
            .param(2L).setString("A")
            .param(3L).setOption(1)
            .parameterValueHypothesis(4L, "param4", Param.Type.ENUM, "test1")
            .endModel();

        CommonModel sku3 = getDefaultSkuBuilder()
            .id(12L)
            .param(2L).setString("A")
            .parameterValueHypothesis(3L, "param3", Param.Type.ENUM, "test1")
            .endModel();

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_DEFINING)))
            .thenReturn(SKU_DEFINING_PARAMS);

        applyModelStorageServiceMock(context, Arrays.asList(sku1, sku2, sku3));

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdateOperation(parent),
            Arrays.asList(parent, sku1, sku2)
        );

        assertTrue(errors.isEmpty());
    }

    @Test
    public void testDuplicateHypothesis() {
        CommonModel parent = getParentWithRelation(10L, 11L);

        CommonModel sku1 = getDefaultSkuBuilder()
            .id(10L)
            .param(1L).setNumeric(1)
            .param(2L).setString("A")
            .parameterValueHypothesis(4L, "param4", Param.Type.ENUM, "test")
            .endModel();

        CommonModel sku2 = getDefaultSkuBuilder()
            .id(11L)
            .param(1L).setNumeric(1)
            .param(2L).setString("A")
            .parameterValueHypothesis(4L, "param4", Param.Type.ENUM, "test")
            .endModel();

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_DEFINING)))
            .thenReturn(SKU_DEFINING_PARAMS);

        applyModelStorageServiceMock(context, Collections.emptyList());

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdateOperation(parent),
            Arrays.asList(parent, sku1, sku2)
        );

        List<ModelValidationError> expected = Arrays.asList(
            createSkuDuplicateParamError(sku1.getId()),
            createSkuDuplicateParamError(sku2.getId())
        );

        assertEquals(expected.size(), errors.size());
        assertThat(errors, IsIterableContainingInAnyOrder.containsInAnyOrder(expected.toArray()));
    }

    @Test
    public void testDuplicateDeleted() {
        CommonModel parent = getParentWithRelation(10L, 11L);

        CommonModel sku1 = getDefaultSkuBuilder()
            .id(10L)
            .param(1L).setNumeric(1)
            .endModel();

        CommonModel sku2 = getDefaultSkuBuilder()
            .id(11L)
            .deleted(true)
            .param(1L).setNumeric(1)
            .endModel();

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_DEFINING)))
            .thenReturn(SKU_DEFINING_PARAMS);

        applyModelStorageServiceMock(context, Arrays.asList(sku1, sku2));

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdateOperation(parent),
            Arrays.asList(parent, sku1, sku2)
        );

        assertTrue(errors.isEmpty());
    }

    @Test
    public void testDuplicateNew() {
        CommonModel parent = getGuru();

        CommonModel sku1 = getDefaultSkuBuilder()
            .id(CommonModel.NO_ID)
            .param(1L).setNumeric(1)
            .endModel();

        CommonModel sku2 = getDefaultSkuBuilder()
            .id(CommonModel.NO_ID)
            .param(1L).setNumeric(1)
            .endModel();

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_DEFINING)))
            .thenReturn(SKU_DEFINING_PARAMS);

        applyModelStorageServiceMock(context, Collections.emptyList());

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdateOperation(parent),
            Arrays.asList(parent, sku1, sku2)
        );

        List<ModelValidationError> expected = Arrays.asList(
            createSkuDuplicateParamError(sku1.getId()),
            createSkuDuplicateParamError(sku2.getId())
        );

        assertEquals(expected.size(), errors.size());
        assertThat(errors, IsIterableContainingInAnyOrder.containsInAnyOrder(expected.toArray()));
    }

    @Test
    public void testDuplicateMixed() {
        CommonModel parent = getParentWithRelation(10L);

        CommonModel sku1 = getDefaultSkuBuilder()
            .id(10L)
            .param(1L).setNumeric(1)
            .endModel();

        CommonModel sku2 = getDefaultSkuBuilder()
            .id(CommonModel.NO_ID)
            .param(1L).setNumeric(1)
            .endModel();

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_DEFINING)))
            .thenReturn(SKU_DEFINING_PARAMS);

        applyModelStorageServiceMock(context, Collections.singletonList(sku1));

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdateOperation(parent),
            Arrays.asList(parent, sku1, sku2)
        );

        List<ModelValidationError> expected = Arrays.asList(
            createSkuDuplicateParamError(sku1.getId()),
            createSkuDuplicateParamError(sku2.getId())
        );

        assertEquals(expected.size(), errors.size());
        assertThat(errors, IsIterableContainingInAnyOrder.containsInAnyOrder(expected.toArray()));
    }

    private ModelChanges getCreateOperation(CommonModel after) {
        return new ModelChanges(null, after, ModelChanges.Operation.CREATE);
    }

    private ModelChanges getUpdateOperation(CommonModel after) {
        return new ModelChanges(after, after, ModelChanges.Operation.UPDATE);
    }

    private void applyModelStorageServiceMock(ModelValidationContext context, List<CommonModel> models) {
        when(context.getModels(anyLong(), anyList()))
            .then(ids -> models.stream()
                .filter(m -> ids.<List<Long>>getArgument(1).contains(m.getId()))
                .collect(Collectors.toList()));

        when(context.getModel(anyLong(), anyLong()))
            .then(id -> models.stream()
                .filter(m -> id.<Long>getArgument(1) == m.getId())
                .findAny());
    }
}
