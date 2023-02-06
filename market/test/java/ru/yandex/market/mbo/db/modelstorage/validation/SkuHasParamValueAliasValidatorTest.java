package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

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
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getSkuBuilder;

/**
 * @author danfertev
 * @since 05.04.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SkuHasParamValueAliasValidatorTest extends BaseValidatorTestClass {
    private SkuHasParamValueAliasValidator validator;
    private ModelValidationContext context;
    private CommonModel parent;

    @Before
    @Override
    public void before() {
        super.before();
        validator = new SkuHasParamValueAliasValidator();
        context = spy(new ModelValidationContextStub(null));
        Map<Long, String> aliasNames = new HashMap<>();
        when(context.getOptionNames(anyLong(), anyCollection())).thenReturn(aliasNames);

        parent = getGuruBuilder()
            .enumAlias(3L)
                .xslName("param3").optionId(1L).aliasOptionId(11L)
            .end()
            .enumAlias(3L)
                .xslName("param3").optionId(1L).aliasOptionId(12L)
            .end()
            .enumAlias(3L)
                .xslName("param3").optionId(2L).aliasOptionId(22L)
            .end()
            .endModel();
        storage.saveModels(ModelSaveGroup.fromModels(parent), saveContext);
    }

    @Test
    public void testNotRunOnGuru() {
        CommonModel guru = getGuruBuilder()
            .param(3L).setOption(2L)
            .endModel();
        storage.saveModels(ModelSaveGroup.fromModels(guru), saveContext);
        guru.setDeleted(true);

        List<ModelValidationError> errors = validator.validate(
            context,
            modelChanges(guru),
            Collections.singletonList(guru)
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testNotRunOnNotDeleteSku() {
        CommonModel sku = getDefaultSkuBuilder().endModel();
        storage.saveModels(ModelSaveGroup.fromModels(sku), saveContext);

        List<ModelValidationError> errors = validator.validate(
            context,
            modelChanges(sku),
            Collections.singletonList(sku)
        );

        assertThat(errors).isEmpty();
    }

    @Test(expected = RuntimeException.class)
    public void testOnSkuWithoutParentRelation() {
        CommonModel sku = getDefaultSkuBuilder().endModel();
        storage.saveModels(ModelSaveGroup.fromModels(sku), saveContext);
        sku.setRelations(Collections.emptyList());
        sku.setDeleted(true);

        List<ModelValidationError> errors = validator.validate(
            context,
            modelChanges(sku),
            Arrays.asList(parent, sku)
        );

        assertThat(errors).isEmpty();
    }

    @Test(expected = RuntimeException.class)
    public void testNoParentInUpdatedModels() {
        CommonModel sku = getDefaultSkuBuilder().endModel();
        storage.saveModels(ModelSaveGroup.fromModels(sku), saveContext);
        sku.setDeleted(true);

        validator.validate(
            context,
            modelChanges(sku),
            Collections.singletonList(sku)
        );
    }

    @Test
    public void testNoAliases() {
        CommonModel guru = getGuruBuilder()
            .id(123L)
            .endModel();
        CommonModel sku = getSkuBuilder(123L)
            .endModel();
        storage.saveModels(ModelSaveGroup.fromModels(guru, sku), saveContext);
        sku.setDeleted(true);

        List<ModelValidationError> errors = validator.validate(
            context,
            modelChanges(sku),
            Arrays.asList(guru, sku)
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testNoAliasesForSku() {
        CommonModel sku = getDefaultSkuBuilder()
            .param(3L).setOption(666L)
            .endModel();
        storage.saveModels(ModelSaveGroup.fromModels(sku), saveContext);
        sku.setDeleted(true);

        List<ModelValidationError> errors = validator.validate(
            context,
            modelChanges(sku),
            Arrays.asList(parent, sku)
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testSkuHasAlias() {
        CommonModel sku = getDefaultSkuBuilder()
            .param(3L).setOption(2L)
            .endModel();

        storage.saveModels(ModelSaveGroup.fromModels(sku), saveContext);
        sku.setDeleted(true);

        List<ModelValidationError> errors = validator.validate(
            context,
            modelChanges(sku),
            Arrays.asList(parent, sku)
        );

        ModelValidationError expected = validator.createError(
            context, sku, 3L, "param3", 22L, "22");

        assertThat(errors).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testSkuHasMultipleAliases() {
        CommonModel sku = getDefaultSkuBuilder()
            .param(3L).setOption(1L)
            .endModel();

        storage.saveModels(ModelSaveGroup.fromModels(sku), saveContext);
        sku.setDeleted(true);

        List<ModelValidationError> errors = validator.validate(
            context,
            modelChanges(sku),
            Arrays.asList(parent, sku)
        );

        ModelValidationError expected1 = validator.createError(
            context, sku, 3L, "param3", 11L, "11");
        ModelValidationError expected2 = validator.createError(
            context, sku, 3L, "param3", 12L, "12");

        assertThat(errors).containsExactlyInAnyOrder(expected1, expected2);
    }

    @Test
    public void testValueSetForAnotherParam() {
        CommonModel sku = getDefaultSkuBuilder()
            .param(1L).setOption(1L)
            .endModel();
        storage.saveModels(ModelSaveGroup.fromModels(sku), saveContext);
        sku.setDeleted(true);

        List<ModelValidationError> errors = validator.validate(
            context,
            modelChanges(sku),
            Arrays.asList(parent, sku)
        );

        assertThat(errors).isEmpty();
    }
}
