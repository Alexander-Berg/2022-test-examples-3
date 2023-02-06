package ru.yandex.market.mbo.db.modelstorage.validation;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.validation.context.CachingModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getDefaultSkuBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuru;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuruBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getModificationBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getParentWithRelation;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getParentWithRelationBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getSkuBuilder;

/**
 * @author danfertev
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MandatoryParametersValidatorTest {
    private MandatoryParametersValidator validator;
    private ModelValidationContext context;

    @Before
    public void setup() {
        validator = new MandatoryParametersValidator();
        context = mock(CachingModelValidationContext.class);
        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(),
            anyCollection(), anyBoolean())).thenCallRealMethod();
        when(context.getModels(anyLong(), anyList())).thenReturn(Collections.emptyList());
        when(context.getReadableParameterName(anyLong(), anyLong()))
            .thenAnswer(i -> "paramName" + i.getArgument(1));
    }

    @Test
    public void testGuruNoMandatoryParams() {
        CommonModel guru = getGuru();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(Collections.emptyMap());

        List<ModelValidationError> errors = validator.validate(
            context,
            getNewModelChanges(guru),
            Collections.singletonList(guru)
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testValidationNotRunOnSku() {
        CommonModel sku = getDefaultSkuBuilder()
            .id(10L)
            .endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNewModelChanges(sku),
            Collections.singletonList(sku));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testValidationNotRunOnPartnerGuruSku() {
        CommonModel partnerGuru = getGuru();
        partnerGuru.setSource(CommonModel.Source.PARTNER);

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );
        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE))).thenReturn(
            ImmutableMap.of(1L, "param1")
        );
        when(context.isSkipPsku20MandatoryParamsValidation()).thenReturn(true);

        List<ModelValidationError> errors = validator.validate(
            context,
            getNewModelChanges(partnerGuru),
            Collections.singletonList(partnerGuru));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testValidationNotRunOnModification() {
        CommonModel model = getGuru();
        model.setParentModelId(1L);

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNewModelChanges(model),
            Collections.singletonList(model)
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testGuru() {
        CommonModel guru = getGuruBuilder()
            .param(1L).setNumeric(1)
            .endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE))).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNewModelChanges(guru),
            Collections.singletonList(guru));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testGuruHypothesis() {
        CommonModel guru = getGuruBuilder()
            .parameterValueHypothesis(1L, "param1", Param.Type.ENUM, "test")
            .endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE))).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNewModelChanges(guru),
            Collections.singletonList(guru));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testGuruWithSkus() {
        CommonModel guru = getParentWithRelationBuilder(10L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku = getDefaultSkuBuilder().id(10L)
            .endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE))).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNewModelChanges(guru),
            Arrays.asList(guru, sku));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testNotUpdatedGuruMissingGuruMandatory() {
        CommonModel guru = getGuru();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE))).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdatedModelChanges(guru, guru),
            Collections.singletonList(guru));

        List<ModelValidationError> expected = createErrors(guru, false, 1L);

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNotUpdatedGuruMissingGuruMandatoryCategoryChange() {
        CommonModel guru = getGuru();
        CommonModel guruCategoryChanged = new CommonModel(guru);
        guruCategoryChanged.setCategoryId(123456L);

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE))).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdatedModelChanges(guru, guruCategoryChanged),
            Collections.singletonList(guruCategoryChanged));

        List<ModelValidationError> expected = createErrors(guruCategoryChanged, true, 1L);

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNewGuruMissingGuruMandatory() {
        CommonModel guru = getGuru();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE))).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNewModelChanges(guru),
            Collections.singletonList(guru));

        List<ModelValidationError> expected = createErrors(guru, true, 1L);
        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNewGuruMissingSkuMandatory() {
        CommonModel guru = getGuru();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNewModelChanges(guru),
            Collections.singletonList(guru));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testNewSku() {
        CommonModel guru = getParentWithRelation(10L);
        CommonModel sku = getDefaultSkuBuilder().id(10L)
            .param(1L).setNumeric(1)
            .endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, sku));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testNewSkuMissingGuruMandatory() {
        CommonModel guru = getParentWithRelationBuilder(10L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku = getDefaultSkuBuilder().id(10L)
            .endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE))).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, sku));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testNewSkuMissingSkuMandatory() {
        CommonModel guru = getParentWithRelation(10L, 11L, 12L);
        CommonModel sku1 = getDefaultSkuBuilder().id(10L).endModel();
        CommonModel sku2 = getDefaultSkuBuilder().id(11L).endModel();
        CommonModel sku3 = getDefaultSkuBuilder()
            .id(12L)
            .param(1L).setNumeric(1)
            .endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, sku1, sku2, sku3));

        List<ModelValidationError> expected = new ArrayList<>();
        expected.addAll(createErrors(sku1, 1L));
        expected.addAll(createErrors(sku2, 1L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNotUpdatedSkuMissingSkuMandatoryInheritedCritical() {
        CommonModel before = getParentWithRelationBuilder(10L, 11L, 12L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel after = getParentWithRelation(10L, 11L, 12L);
        CommonModel sku1 = getDefaultSkuBuilder().id(10L).endModel();
        CommonModel sku2 = getDefaultSkuBuilder().id(11L).endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(), eq(Collections.emptyList()),
            anyBoolean()))
            .thenReturn(new RelatedModelsContainer(Collections.emptyList(), Arrays.asList(sku1, sku2),
                ModelRelation.RelationType.SKU_MODEL));

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdatedModelChanges(before, after),
            Collections.singletonList(after));

        List<ModelValidationError> expected = new ArrayList<>();
        expected.addAll(createErrors(sku1, 1L));
        expected.addAll(createErrors(sku2, 1L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNotUpdatedSkuMissingSkuMandatoryInheritedNotCritical() {
        CommonModel guru = getParentWithRelation(10L, 11L, 12L);
        CommonModel sku1 = getDefaultSkuBuilder().id(10L).endModel();
        CommonModel sku2 = getDefaultSkuBuilder().id(11L).endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(), eq(Collections.emptyList()),
            anyBoolean()))
            .thenReturn(new RelatedModelsContainer(Collections.emptyList(), Arrays.asList(sku1, sku2),
                ModelRelation.RelationType.SKU_MODEL));

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Collections.singletonList(guru));

        List<ModelValidationError> expected = new ArrayList<>();
        expected.addAll(createErrors(sku1, false, 1L));
        expected.addAll(createErrors(sku2, false, 1L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNotUpdatedSkuMissingSkuMandatoryInheritedCategoryChanged() {
        CommonModel guru = getParentWithRelation(10L, 11L, 12L);
        CommonModel guruCategoryChanged = new CommonModel(guru);
        guru.setCategoryId(123456L);
        CommonModel sku1 = getDefaultSkuBuilder().id(10L).endModel();
        CommonModel sku2 = getDefaultSkuBuilder().id(11L).endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(), eq(Collections.emptyList()),
            anyBoolean()))
            .thenReturn(new RelatedModelsContainer(Collections.emptyList(), Arrays.asList(sku1, sku2),
                ModelRelation.RelationType.SKU_MODEL));

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdatedModelChanges(guru, guruCategoryChanged),
            Collections.singletonList(guruCategoryChanged));

        List<ModelValidationError> expected = new ArrayList<>();
        expected.addAll(createErrors(sku1, true, 1L));
        expected.addAll(createErrors(sku2, true, 1L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNewModifications() {
        CommonModel guru = getGuru();
        CommonModel modification = getModificationBuilder().id(10L)
            .param(1L).setNumeric(1)
            .endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );
        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE))).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, modification));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testNewModificationsMissingGuruMandatory() {
        CommonModel guru = getGuru();
        CommonModel modification1 = getModificationBuilder().id(10L).endModel();
        CommonModel modification2 = getModificationBuilder().id(11L).endModel();
        CommonModel modification3 = getModificationBuilder()
            .id(12L)
            .param(1L).setNumeric(1)
            .endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );
        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE))).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, modification1, modification2, modification3));

        List<ModelValidationError> expected = new ArrayList<>();
        expected.addAll(createErrors(modification1, 1L));
        expected.addAll(createErrors(modification2, 1L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNotUpdatedModificationsMissingGuruMandatoryInheritedCritical() {
        CommonModel before = getGuruBuilder()
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel after = getGuru();
        CommonModel modification1 = getModificationBuilder().id(10L).endModel();
        CommonModel modification2 = getModificationBuilder().id(11L).endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Arrays.asList(modification1, modification2)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );
        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE))).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdatedModelChanges(before, after),
            Collections.singletonList(after));

        List<ModelValidationError> expected = new ArrayList<>();
        expected.addAll(createErrors(modification1, 1L));
        expected.addAll(createErrors(modification2, 1L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNotUpdatedModificationsMissingGuruMandatory() {
        CommonModel guru = getGuru();
        CommonModel modification1 = getModificationBuilder().id(10L).endModel();
        CommonModel modification2 = getModificationBuilder().id(11L).endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Arrays.asList(modification1, modification2)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );
        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE))).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Collections.singletonList(guru));

        List<ModelValidationError> expected = new ArrayList<>();
        expected.addAll(createErrors(modification1, false, 1L));
        expected.addAll(createErrors(modification2, false, 1L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNotUpdatedModificationsMissingGuruMandatoryCategoryChanged() {
        CommonModel guru = getGuru();
        CommonModel guruCategoryChanged = new CommonModel(guru);
        guru.setCategoryId(123456L);
        CommonModel modification1 = getModificationBuilder().id(10L).endModel();
        CommonModel modification2 = getModificationBuilder().id(11L).endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Arrays.asList(modification1, modification2)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );
        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE))).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdatedModelChanges(guru, guruCategoryChanged),
            Collections.singletonList(guruCategoryChanged));

        List<ModelValidationError> expected = new ArrayList<>();
        expected.addAll(createErrors(modification1, true, 1L));
        expected.addAll(createErrors(modification2, true, 1L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNewModificationSku() {
        CommonModel guru = getGuru();
        CommonModel modification = getModificationBuilder()
            .id(10L)
            .endModel();
        CommonModel sku = getSkuBuilder(10L)
            .id(11L)
            .param(1L).setNumeric(1)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList()))
            .thenReturn(Collections.singletonList(modification));
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, modification, sku));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testNewModificationSkuMissingGuruMandatory() {
        CommonModel guru = getGuru();
        CommonModel modification = getModificationBuilder()
            .id(10L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku = getSkuBuilder(10L)
            .id(11L)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList()))
            .thenReturn(Collections.singletonList(modification));
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );
        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE))).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, modification, sku));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testModificationContainsSkuMandatory() {
        CommonModel guru = getGuru();
        CommonModel modification = getModificationBuilder()
            .id(10L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku = getSkuBuilder(10L)
            .id(11L)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList()))
            .thenReturn(Collections.singletonList(modification));
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, modification, sku));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testModificationContainsSkuMandatoryHypothesis() {
        CommonModel guru = getGuru();
        CommonModel modification = getModificationBuilder()
            .id(10L)
            .parameterValueHypothesis(1L, "param1", Param.Type.ENUM, "test")
            .endModel();
        CommonModel sku = getSkuBuilder(10L)
            .id(11L)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList()))
            .thenReturn(Collections.singletonList(modification));
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, modification, sku));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testNewModificationSkuMissingSkuMandatory() {
        CommonModel guru = getGuru();
        CommonModel modification1 = getModificationBuilder()
            .id(10L)
            .endModel();
        CommonModel sku11 = getSkuBuilder(10L)
            .id(11L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku12 = getSkuBuilder(10L)
            .id(12L)
            .endModel();

        CommonModel modification2 = getModificationBuilder()
            .id(20L)
            .endModel();
        CommonModel sku21 = getSkuBuilder(20L)
            .id(21L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku22 = getSkuBuilder(20L)
            .id(22L)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Arrays.asList(modification1, modification2)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, modification1, sku11, sku12, modification2, sku21, sku22));

        List<ModelValidationError> expected = new ArrayList<>();
        expected.addAll(createErrors(sku12, 1L));
        expected.addAll(createErrors(sku22, 1L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNotUpdatedModificationSku() {
        CommonModel guru = getGuru();
        CommonModel modification1 = getModificationBuilder()
            .id(10L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku11 = getSkuBuilder(10L)
            .id(11L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku12 = getSkuBuilder(10L)
            .id(12L)
            .endModel();

        CommonModel modification2 = getModificationBuilder()
            .id(20L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku21 = getSkuBuilder(20L)
            .id(21L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku22 = getSkuBuilder(20L)
            .id(22L)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Arrays.asList(modification1, modification2)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );
        when(context.getRelatedModelsForModels(
            anyLong(),
            eq(Arrays.asList(modification1, modification2)),
            anyCollection(),
            eq(Collections.emptyList()),
            anyBoolean())
        ).thenReturn(
            new RelatedModelsContainer(Collections.emptyList(), Arrays.asList(sku11, sku12, sku21, sku22),
                ModelRelation.RelationType.SKU_MODEL)
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, modification1, sku11, sku12, modification2, sku21, sku22));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testNotUpdatedModificationSkuMissingSkuMandatoryInheritedCritical() {
        CommonModel before = getGuruBuilder()
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel after = getGuru();
        CommonModel modification1 = getModificationBuilder()
            .id(10L)
            .endModel();
        CommonModel sku11 = getSkuBuilder(10L)
            .id(11L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku12 = getSkuBuilder(10L)
            .id(12L)
            .endModel();

        CommonModel modification2 = getModificationBuilder()
            .id(20L)
            .endModel();
        CommonModel sku21 = getSkuBuilder(20L)
            .id(21L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku22 = getSkuBuilder(20L)
            .id(22L)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Arrays.asList(modification1, modification2)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );
        when(context.getRelatedModelsForModels(
            anyLong(),
            eq(Arrays.asList(modification1, modification2)),
            anyCollection(),
            eq(Collections.emptyList()),
            anyBoolean())
        ).thenReturn(
            new RelatedModelsContainer(Collections.emptyList(), Arrays.asList(sku11, sku12, sku21, sku22),
                ModelRelation.RelationType.SKU_MODEL)
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdatedModelChanges(before, after),
            Arrays.asList(after, modification1, sku11, sku12, modification2, sku21, sku22));

        List<ModelValidationError> expected = new ArrayList<>();
        expected.addAll(createErrors(sku12, 1L));
        expected.addAll(createErrors(sku22, 1L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNotUpdatedModificationSkuMissingSkuMandatoryInheritedNotCritical() {
        CommonModel guru = getGuru();
        CommonModel modification1 = getModificationBuilder()
            .id(10L)
            .endModel();
        CommonModel sku11 = getSkuBuilder(10L)
            .id(11L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku12 = getSkuBuilder(10L)
            .id(12L)
            .endModel();

        CommonModel modification2 = getModificationBuilder()
            .id(20L)
            .endModel();
        CommonModel sku21 = getSkuBuilder(20L)
            .id(21L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku22 = getSkuBuilder(20L)
            .id(22L)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Arrays.asList(modification1, modification2)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );
        when(context.getRelatedModelsForModels(
            anyLong(),
            eq(Arrays.asList(modification1, modification2)),
            anyCollection(),
            eq(Collections.emptyList()),
            anyBoolean())
        ).thenReturn(
            new RelatedModelsContainer(Collections.emptyList(), Arrays.asList(sku11, sku12, sku21, sku22),
                ModelRelation.RelationType.SKU_MODEL)
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, modification1, sku11, sku12, modification2, sku21, sku22));

        List<ModelValidationError> expected = new ArrayList<>();
        expected.addAll(createErrors(sku12, false, 1L));
        expected.addAll(createErrors(sku22, false, 1L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNotUpdatedModificationSkuMissingSkuMandatoryInheritedCategoryChanged() {
        CommonModel guru = getGuru();
        CommonModel guruCategoryChanged = new CommonModel(guru);
        guru.setCategoryId(123456L);
        CommonModel modification1 = getModificationBuilder()
            .id(10L)
            .endModel();
        CommonModel sku11 = getSkuBuilder(10L)
            .id(11L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku12 = getSkuBuilder(10L)
            .id(12L)
            .endModel();

        CommonModel modification2 = getModificationBuilder()
            .id(20L)
            .endModel();
        CommonModel sku21 = getSkuBuilder(20L)
            .id(21L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku22 = getSkuBuilder(20L)
            .id(22L)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Arrays.asList(modification1, modification2)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );
        when(context.getRelatedModelsForModels(
            anyLong(),
            eq(Arrays.asList(modification1, modification2)),
            anyCollection(),
            eq(Collections.emptyList()),
            anyBoolean())
        ).thenReturn(
            new RelatedModelsContainer(Collections.emptyList(), Arrays.asList(sku11, sku12, sku21, sku22),
                ModelRelation.RelationType.SKU_MODEL)
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getUpdatedModelChanges(guru, guruCategoryChanged),
            Arrays.asList(guruCategoryChanged, modification1, sku11, sku12, modification2, sku21, sku22));

        List<ModelValidationError> expected = new ArrayList<>();
        expected.addAll(createErrors(sku12, true, 1L));
        expected.addAll(createErrors(sku22, true, 1L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNewParentSkuAndModificationSkuMissingSkuMandatory() {
        CommonModel guru = getParentWithRelation(21L, 22L);
        CommonModel modification1 = getModificationBuilder()
            .id(10L)
            .endModel();
        CommonModel sku11 = getSkuBuilder(10L)
            .id(11L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku12 = getSkuBuilder(10L)
            .id(12L)
            .endModel();

        CommonModel sku21 = getDefaultSkuBuilder()
            .id(21L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku22 = getDefaultSkuBuilder()
            .id(22L)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Collections.singletonList(modification1)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, modification1, sku11, sku12, sku21, sku22));

        List<ModelValidationError> expected = new ArrayList<>();
        expected.addAll(createErrors(sku12, 1L));
        expected.addAll(createErrors(sku22, 1L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testMultipleMissing() {
        CommonModel guru = getGuru();
        CommonModel modification = getModificationBuilder()
            .id(10L)
            .endModel();
        CommonModel sku1 = getSkuBuilder(10L)
            .id(11L)
            .param(1L).setNumeric(1)
            .endModel();
        CommonModel sku2 = getSkuBuilder(10L)
            .id(12L)
            .param(2L).setString("2")
            .endModel();
        CommonModel sku3 = getSkuBuilder(10L)
            .id(13L)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Collections.singletonList(modification)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1", 2L, "param2")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, modification, sku1, sku2, sku3));

        List<ModelValidationError> expected = new ArrayList<>();
        expected.addAll(createErrors(sku1, 2L));
        expected.addAll(createErrors(sku2, 1L));
        expected.addAll(createErrors(sku3, 1L, 2L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNewGuruIsSkuMissingSkuMandatory() {
        CommonModel guru = getGuruBuilder()
            .param(XslNames.IS_SKU).setBoolean(true)
            .endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNewModelChanges(guru),
            Collections.singletonList(guru));

        List<ModelValidationError> expected = createErrors(guru, 1L);

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNotUpdatedGuruIsSkuHasSkuMandatory() {
        CommonModel guru = getGuruBuilder()
            .param(XslNames.IS_SKU).setBoolean(true)
            .param(1L).setNumeric(1)
            .endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Collections.singletonList(guru));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testNotUpdatedGuruIsSkuMissingSkuMandatory() {
        CommonModel guru = getGuruBuilder()
            .param(XslNames.IS_SKU).setBoolean(true)
            .endModel();

        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Collections.singletonList(guru));

        List<ModelValidationError> expected = createErrors(guru, false, 1L);

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNewModificationIsSkuMissingSkuMandatory() {
        CommonModel guru = getGuru();
        CommonModel modification = getModificationBuilder()
            .id(10L)
            .param(XslNames.IS_SKU).setBoolean(true)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Collections.singletonList(modification)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNewModelChanges(guru),
            Arrays.asList(guru, modification));

        List<ModelValidationError> expected = createErrors(modification, 1L);

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNewModificationIsSkuHasSkuMandatory() {
        CommonModel guru = getGuru();
        CommonModel modification = getModificationBuilder()
            .id(10L)
            .param(XslNames.IS_SKU).setBoolean(true)
            .param(1L).setNumeric(1)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Collections.singletonList(modification)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNewModelChanges(guru),
            Arrays.asList(guru, modification));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testNotUpdatedModificationIsSkuMissingSkuMandatory() {
        CommonModel guru = getGuru();
        CommonModel modification = getModificationBuilder()
            .id(10L)
            .param(XslNames.IS_SKU).setBoolean(true)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Collections.singletonList(modification)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, modification));


        List<ModelValidationError> expected = createErrors(modification, false, 1L);

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testNotUpdatedModificationIsSkuHasSkuMandatory() {
        CommonModel guru = getGuru();
        CommonModel modification = getModificationBuilder()
            .id(10L)
            .param(XslNames.IS_SKU).setBoolean(true)
            .param(1L).setNumeric(1)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Collections.singletonList(modification)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNotUpdatedModelChanges(guru),
            Arrays.asList(guru, modification));


        assertThat(errors).isEmpty();
    }

    @Test
    public void testNewModificationIsSkuMultipleMissingSkuMandatory() {
        CommonModel guru = getGuru();
        CommonModel modification = getModificationBuilder()
            .id(10L)
            .param(XslNames.IS_SKU).setBoolean(true)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Collections.singletonList(modification)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1", 2L, "param2")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNewModelChanges(guru),
            Arrays.asList(guru, modification));

        List<ModelValidationError> expected = createErrors(modification, 1L, 2L);

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    @Test
    public void testMultipleModificationsIsSkuMissingSkuMandatory() {
        CommonModel guru = getGuru();
        CommonModel modification1 = getModificationBuilder()
            .id(10L)
            .param(XslNames.IS_SKU).setBoolean(true)
            .endModel();
        CommonModel modification2 = getModificationBuilder()
            .id(11L)
            .param(XslNames.IS_SKU).setBoolean(true)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList())).thenReturn(
            Arrays.asList(modification1, modification2)
        );
        when(context.getMandatoryParamNames(anyLong())).thenReturn(
            ImmutableMap.of(1L, "param1")
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getNewModelChanges(guru),
            Arrays.asList(guru, modification1, modification2));

        List<ModelValidationError> expected = createErrors(modification1, 1L);
        expected.addAll(createErrors(modification2, 1L));

        assertThat(errors).containsExactlyInAnyOrder(toArray(expected));
    }

    private ModelChanges getNewModelChanges(CommonModel after) {
        return new ModelChanges(null, after);
    }

    private ModelChanges getNotUpdatedModelChanges(CommonModel after) {
        return new ModelChanges(after, after);
    }

    private ModelChanges getUpdatedModelChanges(CommonModel before, CommonModel after) {
        return new ModelChanges(before, after);
    }

    private List<ModelValidationError> createErrors(CommonModel model, long... paramIds) {
        return createErrors(model, true, paramIds);
    }

    private List<ModelValidationError> createErrors(CommonModel model, boolean critical, long... paramIds) {
        List<ModelValidationError> errors = new ArrayList<>();

        for (long paramId : paramIds) {
            errors.add(new ModelValidationError(model.getId(),
                ModelValidationError.ErrorType.MANDATORY_PARAM_EMPTY,
                ModelValidationError.ErrorSubtype.MISSING_MANDATORY,
                critical)
                .addLocalizedMessagePattern("Обязательный параметр '%{PARAMETER_NAME}'(%{PARAM_ID}) не заполнен.")
                .addParam(ModelStorage.ErrorParamName.PARAM_ID, paramId)
                .addParam(ModelStorage.ErrorParamName.PARAM_XSL_NAME, "param" + paramId)
                .addParam(ModelStorage.ErrorParamName.PARAMETER_NAME, "paramName" + paramId));
        }

        return errors;
    }

    private static ModelValidationError[] toArray(List<ModelValidationError> errors) {
        return errors.toArray(new ModelValidationError[errors.size()]);
    }
}
