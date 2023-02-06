package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.stubs.GroupStorageUpdatesStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.EnumValueAlias;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getDefaultSkuBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuruBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getParentWithRelation;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getParentWithRelationBuilder;

/**
 * @author danfertev
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ParameterValueAliasesValidatorTest {

    private static final long ROOT_ID = 1L;
    private static final long SKU_ID1 = 11L;
    private static final long SKU_ID2 = 12L;

    private static final long RED = 1L;
    private static final long PURPLE = 2L;
    private static final long GREEN = 3L;

    private ParameterValueAliasesValidator validator;
    private ModelValidationContextStub context;
    private GroupStorageUpdatesStub groupStorageUpdatesStub;

    @Before
    public void setup() {
        validator = new ParameterValueAliasesValidator();
        groupStorageUpdatesStub = new GroupStorageUpdatesStub();

        context = spy(new ModelValidationContextStub(null));
        context.setStatsModelStorageService(groupStorageUpdatesStub);
        Map<Long, String> aliasNames = new HashMap<>();

        when(context.getReadableParameterName(anyLong(), anyLong())).thenReturn("param4");
        when(context.getOptionNames(anyLong(), anyCollection())).thenReturn(aliasNames);
    }

    @Test
    public void testNotRunOnSku() {
        CommonModel guru = getParentWithRelation(11L, 12L);
        CommonModel sku1 = getDefaultSkuBuilder()
            .id(11L)
            .param(4L).setOption(1L)
            .endModel();
        CommonModel sku2 = getDefaultSkuBuilder()
            .id(12L)
            .param(4L).setOption(2L)
            .endModel();

        guru.addAllEnumValueAliases(Arrays.asList(
            new EnumValueAlias(4L, "param4", 1L, 11L),
            new EnumValueAlias(4L, "param4", 1L, 12L),
            new EnumValueAlias(4L, "param4", 2L, 12L)
        ));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku1),
            Arrays.asList(guru, sku1, sku2)
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testEmptyAliases() {
        CommonModel guru = getParentWithRelation(11L, 12L);
        CommonModel sku1 = getDefaultSkuBuilder().id(11L).endModel();
        CommonModel sku2 = getDefaultSkuBuilder().id(12L).endModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            Arrays.asList(guru, sku1, sku2)
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testUniqueAliases() {
        CommonModel guru = getParentWithRelation(11L, 12L);
        CommonModel sku1 = getDefaultSkuBuilder()
            .id(11L)
            .param(4L).setOption(1L)
            .endModel();
        CommonModel sku2 = getDefaultSkuBuilder()
            .id(12L)
            .param(4L).setOption(2L)
            .endModel();

        guru.addAllEnumValueAliases(Arrays.asList(
            new EnumValueAlias(4L, "param4", 1L, 11L),
            new EnumValueAlias(4L, "param4", 1L, 12L),
            new EnumValueAlias(4L, "param4", 2L, 21L),
            new EnumValueAlias(4L, "param4", 2L, 22L),
            new EnumValueAlias(3L, "param3", 1L, 11L),
            new EnumValueAlias(3L, "param3", 1L, 12L)
        ));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            Arrays.asList(guru, sku1, sku2)
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testDuplicateAliases() {
        CommonModel guru = getParentWithRelation(11L, 12L);
        CommonModel sku1 = getDefaultSkuBuilder()
            .id(11L)
            .param(4L).setOption(1L)
            .endModel();
        CommonModel sku2 = getDefaultSkuBuilder()
            .id(12L)
            .param(4L).setOption(2L)
            .endModel();

        guru.addAllEnumValueAliases(Arrays.asList(
            new EnumValueAlias(4L, "param4", 1L, 11L),
            new EnumValueAlias(4L, "param4", 1L, 12L),
            new EnumValueAlias(4L, "param4", 2L, 12L)
        ));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            Arrays.asList(guru, sku1, sku2)
        );

        ModelValidationError expected1 = validator.createDuplicateParamValueAliasError(
            context, sku1.getCategoryId(), sku1.getId(), 4L, "param4", 12L, "12");
        ModelValidationError expected2 = validator.createDuplicateParamValueAliasError(
            context, sku2.getCategoryId(), sku2.getId(), 4L, "param4", 12L, "12");

        assertThat(errors).containsExactlyInAnyOrder(expected1, expected2);
    }

    @Test
    public void testMultipleDuplicateAliases() {
        CommonModel guru = getGuruBuilder()
            .param(4L).setOption(1L)
            .param(4L).setOption(2L)
            .param(3L).setOption(1L)
            .param(3L).setOption(2L)
            .endModel();

        guru.addAllEnumValueAliases(Arrays.asList(
            new EnumValueAlias(4L, "param4", 1L, 11L),
            new EnumValueAlias(4L, "param4", 1L, 12L),
            new EnumValueAlias(4L, "param4", 2L, 12L),
            new EnumValueAlias(3L, "param3", 1L, 21L),
            new EnumValueAlias(3L, "param3", 2L, 21L)
        ));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            Collections.singletonList(guru)
        );

        ModelValidationError expected1 = validator.createDuplicateParamValueAliasError(
            context, guru.getCategoryId(), guru.getId(), 4L, "param4", 12L, "12");
        ModelValidationError expected2 = validator.createDuplicateParamValueAliasError(
            context, guru.getCategoryId(), guru.getId(), 3L, "param3", 21L, "21");

        assertThat(errors).containsExactlyInAnyOrder(expected1, expected2);
    }

    @Test
    public void testInvalidAlias() {
        CommonModel guru = getGuruBuilder()
            .param(4L).setOption(11L)
            .endModel();
        CommonModel sku1 = getDefaultSkuBuilder()
            .id(11L)
            .param(4L).setOption(1L)
            .endModel();

        guru.addAllEnumValueAliases(Arrays.asList(
            new EnumValueAlias(4L, "param4", 1L, 11L),
            new EnumValueAlias(4L, "param4", 1L, 12L)
        ));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            Arrays.asList(guru, sku1)
        );

        ModelValidationError expected = validator.createInvalidParamValueAliasError(
            context, sku1.getCategoryId(), sku1.getId(), 4L, "param4", 11L, "11", guru.getId());

        assertThat(errors).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testInvalidAliasMultiValue() {
        CommonModel guru = getGuruBuilder()
            .param(4L).setOption(11L)
            .param(4L).setOption(12L)
            .endModel();
        CommonModel sku1 = getDefaultSkuBuilder()
            .id(11L)
            .param(4L).setOption(1L)
            .endModel();

        guru.addAllEnumValueAliases(Arrays.asList(
            new EnumValueAlias(4L, "param4", 1L, 11L),
            new EnumValueAlias(4L, "param4", 1L, 12L)
        ));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            Arrays.asList(guru, sku1)
        );

        ModelValidationError expected1 = validator.createInvalidParamValueAliasError(
            context, sku1.getCategoryId(), sku1.getId(), 4L, "param4", 11L, "11", guru.getId());
        ModelValidationError expected2 = validator.createInvalidParamValueAliasError(
            context, sku1.getCategoryId(), sku1.getId(), 4L, "param4", 12L, "12", guru.getId());

        assertThat(errors).containsExactlyInAnyOrder(expected1, expected2);
    }

    @Test
    public void testInvalidAliasInSku() {
        CommonModel guru = getParentWithRelation(11L, 12L);
        CommonModel sku1 = getDefaultSkuBuilder()
            .id(11L)
            .param(4L).setOption(1L)
            .endModel();
        CommonModel sku2 = getDefaultSkuBuilder()
            .id(12L)
            .param(4L).setOption(12L)
            .endModel();

        guru.addAllEnumValueAliases(Arrays.asList(
            new EnumValueAlias(4L, "param4", 1L, 11L),
            new EnumValueAlias(4L, "param4", 1L, 12L)
        ));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            Arrays.asList(guru, sku1, sku2)
        );

        ModelValidationError expected = validator.createInvalidParamValueAliasError(
            context, sku1.getCategoryId(), sku1.getId(), 4L, "param4", 12L, "12", sku2.getId());

        assertThat(errors).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testInvalidAliasMultiValueInSku() {
        CommonModel guru = getParentWithRelationBuilder(11L)
            .param(4L).setOption(1L)
            .endModel();
        CommonModel sku1 = getDefaultSkuBuilder()
            .id(11L)
            .param(4L).setOption(11L)
            .param(4L).setOption(12L)
            .endModel();

        guru.addAllEnumValueAliases(Arrays.asList(
            new EnumValueAlias(4L, "param4", 1L, 11L),
            new EnumValueAlias(4L, "param4", 1L, 12L)
        ));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            Arrays.asList(guru, sku1)
        );

        ModelValidationError expected1 = validator.createInvalidParamValueAliasError(
            context, guru.getCategoryId(), guru.getId(), 4L, "param4", 11L, "11", sku1.getId());
        ModelValidationError expected2 = validator.createInvalidParamValueAliasError(
            context, guru.getCategoryId(), guru.getId(), 4L, "param4", 12L, "12", sku1.getId());

        assertThat(errors).containsExactlyInAnyOrder(expected1, expected2);
    }

    @Test
    public void testModificationIgnored() {
        CommonModel modification = getParentWithRelationBuilder(SKU_ID1)
            .startParentModel()
                .id(ROOT_ID)
                .param(4L).setOption(RED)
            .endModel()
            .endModel();
        CommonModel sku1 = getDefaultSkuBuilder()
            .id(SKU_ID1)
            .param(4L).setOption(PURPLE)
            .endModel();

        modification.addAllEnumValueAliases(Collections.singletonList(
            new EnumValueAlias(4L, "param4", PURPLE, RED)
        ));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(modification),
            Arrays.asList(modification, sku1)
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testAliasConflictsWithRoot() {
        CommonModel modification = getParentWithRelationBuilder(SKU_ID1)
            .startParentModel()
                .id(ROOT_ID)
                .param(4L).setOption(RED)
            .endModel()
            .endModel();
        CommonModel sku = getDefaultSkuBuilder()
            .id(SKU_ID1)
            .param(4L).setOption(PURPLE)
            .endModel();
        CommonModel root = modification.getParentModel();

        when(context.getValidModifications(anyLong(), anyCollection()))
            .thenReturn(Collections.singletonList(modification));

        root.addAllEnumValueAliases(Collections.singletonList(
            new EnumValueAlias(4L, "param4", PURPLE, RED)
        ));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(root),
            Arrays.asList(modification, sku, root)
        );

        ModelValidationError expected = validator.createInvalidParamValueAliasError(
            context, sku.getCategoryId(), sku.getId(), 4L, "param4", RED, String.valueOf(RED), root.getId());

        assertThat(errors).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testAliasConflictsWithModification() {
        CommonModel modification = getParentWithRelationBuilder(SKU_ID1)
            .startParentModel()
            .id(ROOT_ID)
            .param(4L).setOption(PURPLE)
            .endModel()
            .param(4L).setOption(RED)
            .endModel();
        CommonModel sku = getDefaultSkuBuilder()
            .id(SKU_ID1)
            .param(4L).setOption(GREEN)
            .endModel();
        CommonModel root = modification.getParentModel();

        when(context.getValidModifications(anyLong(), anyCollection()))
            .thenReturn(Collections.singletonList(modification));

        root.addAllEnumValueAliases(Collections.singletonList(
            new EnumValueAlias(4L, "param4", PURPLE, RED)
        ));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(root),
            Arrays.asList(modification, sku, root)
        );

        ModelValidationError expected = validator.createInvalidParamValueAliasError(
            context, root.getCategoryId(), root.getId(), 4L, "param4", RED, String.valueOf(RED), modification.getId());

        assertThat(errors).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testAliasConflictsWithSkuUnderSameModif() {
        CommonModel modification = getParentWithRelationBuilder(SKU_ID1)
            .startParentModel()
            .id(ROOT_ID)
            .param(4L).setOption(PURPLE)
            .endModel()
            .param(4L).setOption(RED)
            .endModel();
        CommonModel sku = getDefaultSkuBuilder()
            .id(SKU_ID1)
            .param(4L).setOption(GREEN)
            .endModel();
        CommonModel root = modification.getParentModel();

        when(context.getValidModifications(anyLong(), anyCollection()))
            .thenReturn(Collections.singletonList(modification));

        root.addAllEnumValueAliases(Collections.singletonList(
            new EnumValueAlias(4L, "param4", PURPLE, GREEN)
        ));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(root),
            Arrays.asList(modification, sku, root)
        );

        ModelValidationError expected = validator.createInvalidParamValueAliasError(
            context, root.getCategoryId(), root.getId(), 4L, "param4", GREEN, String.valueOf(GREEN), sku.getId());

        assertThat(errors).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testAliasConflictsWithSkuUnderOtherModif() {
        CommonModel modification1 = getParentWithRelationBuilder(SKU_ID1)
            .startParentModel()
            .id(ROOT_ID)
            .endModel()
            .endModel();
        CommonModel modification2 = getParentWithRelationBuilder(SKU_ID2)
            .id(modification1.getId() + 1L)
            .parentModel(modification1.getParentModel())
            .endModel();
        CommonModel sku1 = getDefaultSkuBuilder()
            .id(SKU_ID1)
            .param(4L).setOption(RED) // root -> modif1 -> sku1
            .endModel();
        CommonModel sku2 = getDefaultSkuBuilder()
            .id(SKU_ID2)
            .param(4L).setOption(PURPLE) // root -> modif2 -> sku2
            .endModel();
        CommonModel root = modification1.getParentModel();

        when(context.getValidModifications(anyLong(), anyCollection()))
            .thenReturn(Arrays.asList(modification1, modification2));

        root.addAllEnumValueAliases(Collections.singletonList(
            new EnumValueAlias(4L, "param4", PURPLE, RED)
        ));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(root),
            Arrays.asList(root, modification1, modification2, sku1, sku2)
        );

        ModelValidationError expected = validator.createInvalidParamValueAliasError(
            context, sku2.getCategoryId(), sku2.getId(), 4L, "param4", RED, String.valueOf(RED), sku1.getId());

        assertThat(errors).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testRootModifSkuGoodAliases() {
        CommonModel modification = getParentWithRelationBuilder(SKU_ID1)
            .startParentModel()
            .id(ROOT_ID)
            .param(4L).setOption(6L)
            .endModel()
            .param(4L).setOption(1L)
            .endModel();
        CommonModel sku = getDefaultSkuBuilder()
            .id(SKU_ID1)
            .param(4L).setOption(2L)
            .endModel();
        CommonModel root = modification.getParentModel();

        when(context.getValidModifications(anyLong(), anyCollection()))
            .thenReturn(Collections.singletonList(modification));

        root.addAllEnumValueAliases(Arrays.asList(
            new EnumValueAlias(4L, "param4", 2L, 3L),
            new EnumValueAlias(4L, "param4", 2L, 4L),
            new EnumValueAlias(4L, "param4", 1L, 5L)
        ));
        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(root),
            Arrays.asList(modification, sku, root)
        );
        assertThat(errors).isEmpty();
    }

    private ModelChanges getModelChanges(CommonModel after) {
        return new ModelChanges(after, after, ModelChanges.Operation.UPDATE);
    }
}
