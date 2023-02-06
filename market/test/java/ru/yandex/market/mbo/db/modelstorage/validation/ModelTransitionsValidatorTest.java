package ru.yandex.market.mbo.db.modelstorage.validation;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.EntityType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionReason;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.enums.ModelTransitionType;
import ru.yandex.market.mbo.common.db.jooq.generated.model_storage.tables.pojos.ModelTransition;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.validation.context.CachingModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuruBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getSkuBuilder;

/**
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelTransitionsValidatorTest {
    private ModelTransitionsValidator validator;
    private ModelValidationContext context;

    @Before
    public void setup() {
        validator = new ModelTransitionsValidatorImpl();
        context = mock(CachingModelValidationContext.class);
    }

    @Test
    public void testNoErrorOnValidErrorRemoval() {
        CommonModel guru = getGuruBuilder().id(1).deleted(true).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.ERROR)
                .setEntityType(EntityType.MODEL)
                .setReason(ModelTransitionReason.ERROR_REMOVAL)
                .setOldEntityId(guru.getId())
                .setOldEntityDeleted(true)
                .setNewEntityId(null)
        );

        mockContext(guru);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            transitions
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testNoErrorOnValidDuplicateRemoval() {
        CommonModel sku1 = getSkuBuilder(1).id(1).deleted(true).getModel();
        CommonModel sku2 = getSkuBuilder(1).id(1).id(2).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.DUPLICATE)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setPrimaryTransition(true)
                .setOldEntityId(sku1.getId())
                .setOldEntityDeleted(true)
                .setNewEntityId(sku2.getId())
        );

        mockContext(sku1, sku2);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku1),
            transitions
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testNoErrorOnValidSplitRemoval() {
        CommonModel guru = getGuruBuilder().deleted(true).id(1).getModel();
        CommonModel modif1 = getGuruBuilder().parentModel(guru).id(2).getModel();
        CommonModel modif2 = getGuruBuilder().parentModel(guru).id(3).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.SPLIT)
                .setEntityType(EntityType.MODEL)
                .setReason(ModelTransitionReason.MODEL_SPLIT_TO_MODIF)
                .setOldEntityId(guru.getId())
                .setOldEntityDeleted(true)
                .setNewEntityId(modif1.getId()),
            new ModelTransition()
                .setType(ModelTransitionType.SPLIT)
                .setEntityType(EntityType.MODEL)
                .setReason(ModelTransitionReason.MODEL_SPLIT_TO_MODIF)
                .setOldEntityId(guru.getId())
                .setOldEntityDeleted(true)
                .setNewEntityId(modif2.getId())
        );

        mockContext(guru, modif1, modif2);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            transitions
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testDifferentType() {
        CommonModel guru = getGuruBuilder().deleted(true).id(1).getModel();
        CommonModel modif1 = getGuruBuilder().parentModel(guru).id(2).getModel();
        CommonModel modif2 = getGuruBuilder().parentModel(guru).id(3).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.DUPLICATE)
                .setEntityType(EntityType.MODEL)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setPrimaryTransition(true)
                .setOldEntityId(guru.getId())
                .setOldEntityDeleted(true)
                .setNewEntityId(modif1.getId()),
            new ModelTransition()
                .setType(ModelTransitionType.SPLIT)
                .setEntityType(EntityType.MODEL)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setPrimaryTransition(true)
                .setOldEntityId(guru.getId())
                .setOldEntityDeleted(true)
                .setNewEntityId(modif2.getId())
        );

        mockContext(guru, modif1, modif2);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(
            createInvalidTransitionError(guru.getId(),
                "Переезды с модели %{MODEL_ID} должны быть одного типа.")
        );
    }

    @Test
    public void testDifferentReason() {
        CommonModel guru = getGuruBuilder().deleted(true).id(1).getModel();
        CommonModel modif1 = getGuruBuilder().parentModel(guru).id(2).getModel();
        CommonModel modif2 = getGuruBuilder().parentModel(guru).id(3).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.DUPLICATE)
                .setEntityType(EntityType.MODEL)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setPrimaryTransition(true)
                .setOldEntityId(guru.getId())
                .setOldEntityDeleted(true)
                .setNewEntityId(modif1.getId()),
            new ModelTransition()
                .setType(ModelTransitionType.DUPLICATE)
                .setEntityType(EntityType.MODEL)
                .setReason(ModelTransitionReason.PARTNER_TO_GURU)
                .setOldEntityId(guru.getId())
                .setOldEntityDeleted(true)
                .setNewEntityId(modif2.getId())
        );

        mockContext(guru, modif1, modif2);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(
            createInvalidTransitionError(guru.getId(),
                "Переезды с модели %{MODEL_ID} должны иметь одну и ту же причину.")
        );
    }

    @Test
    public void testErrorTransitionToSomething() {
        CommonModel sku1 = getSkuBuilder(1).id(1).deleted(true).getModel();
        CommonModel sku2 = getSkuBuilder(1).id(1).id(2).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.ERROR)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.ERROR_REMOVAL)
                .setOldEntityId(sku1.getId())
                .setOldEntityDeleted(true)
                .setNewEntityId(sku2.getId())
        );

        mockContext(sku1, sku2);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku1),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(
            createInvalidTransitionError(sku1.getId(),
                "Переезд с ошибочной модели %{MODEL_ID} не может быть на другую модель.")
        );
    }

    @Test
    public void testMultipleErrorTransitions() {
        CommonModel sku1 = getSkuBuilder(1).id(1).deleted(true).getModel();
        CommonModel sku2 = getSkuBuilder(1).id(1).id(2).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.ERROR)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.ERROR_REMOVAL)
                .setOldEntityId(sku1.getId())
                .setOldEntityDeleted(true)
                .setNewEntityId(sku2.getId()),
            new ModelTransition()
                .setType(ModelTransitionType.ERROR)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.ERROR_REMOVAL)
                .setOldEntityId(sku1.getId())
                .setOldEntityDeleted(true)
        );

        mockContext(sku1, sku2);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku1),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(
            createInvalidTransitionError(sku1.getId(),
                "Переездов с ошибочной модели %{MODEL_ID} не может быть несколько.")
            );
    }

    @Test
    public void testDuplicateTransitionToNowhere() {
        CommonModel sku1 = getSkuBuilder(1).id(1).deleted(true).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.DUPLICATE)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setPrimaryTransition(true)
                .setOldEntityId(sku1.getId())
                .setOldEntityDeleted(true)
        );

        mockContext(sku1);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku1),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(
            createInvalidTransitionError(sku1.getId(),
                "Переезд с дублирующейся модели %{MODEL_ID} не может указывать вникуда.")
        );
    }

    @Test
    public void testMultipleDuplicateTransitions() {
        CommonModel sku1 = getSkuBuilder(1).id(1).deleted(true).getModel();
        CommonModel sku2 = getSkuBuilder(1).id(1).id(2).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.DUPLICATE)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setOldEntityId(sku1.getId())
                .setOldEntityDeleted(true)
                .setPrimaryTransition(true)
                .setNewEntityId(sku2.getId()),
            new ModelTransition()
                .setType(ModelTransitionType.DUPLICATE)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setOldEntityId(sku1.getId())
                .setOldEntityDeleted(true)
                .setPrimaryTransition(true)
                .setNewEntityId(sku2.getId())
            );

        mockContext(sku1, sku2);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku1),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(
            createInvalidTransitionError(sku1.getId(),
                "Переездов с дублирующейся модели %{MODEL_ID} не может быть несколько.")
        );
    }

    @Test
    public void testSplitToNowhere() {
        CommonModel sku1 = getSkuBuilder(1).id(1).deleted(true).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.SPLIT)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setOldEntityId(sku1.getId())
                .setOldEntityDeleted(true)
        );

        mockContext(sku1);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku1),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(
            createInvalidTransitionError(sku1.getId(),
                "Переезд при разделении модели %{MODEL_ID} не может указывать вникуда.")
        );
    }

    @Test
    public void testInconsistentTypeAndReason() {
        CommonModel sku1 = getSkuBuilder(1).id(1).deleted(true).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.ERROR)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.SKU_SPLIT)
                .setOldEntityId(sku1.getId())
                .setOldEntityDeleted(true)
        );

        mockContext(sku1);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku1),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(
            createInvalidTransitionError(sku1.getId(),
                "Переезд c модели %{MODEL_ID} имеет несовместиные тип и причину.")
        );
    }

    @Test
    public void testInconsistentEntityTypeAndReason() {
        CommonModel sku1 = getSkuBuilder(1).id(1).deleted(true).getModel();
        CommonModel sku2 = getSkuBuilder(1).id(2).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.SPLIT)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.MODEL_SPLIT_TO_MODIF)
                .setOldEntityId(sku1.getId())
                .setOldEntityDeleted(true)
                .setNewEntityId(sku2.getId())
        );

        mockContext(sku1, sku2);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku1),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(
            createInvalidTransitionError(sku1.getId(),
                "Переезд c модели %{MODEL_ID} имеет несовместиные тип сущности и причину.")
        );
    }

    @Test
    public void testModelTransitionForSku() {
        CommonModel sku1 = getSkuBuilder(1).id(1).deleted(true).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.ERROR)
                .setEntityType(EntityType.MODEL)
                .setReason(ModelTransitionReason.ERROR_REMOVAL)
                .setOldEntityId(sku1.getId())
                .setOldEntityDeleted(true)
        );

        mockContext(sku1);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku1),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(sku1.getId(),
                ModelValidationError.ErrorType.TRANSITION_ERROR,
                ModelValidationError.ErrorSubtype.TRANSITION_MISSING,
                true)
                .addLocalizedMessagePattern("Для удаления SKU %{MODEL_ID} необходимо создать переезды типа SKU.")
                .addParam(ModelStorage.ErrorParamName.MODEL_ID, sku1.getId()),
            new ModelValidationError(sku1.getId(),
                ModelValidationError.ErrorType.TRANSITION_ERROR,
                ModelValidationError.ErrorSubtype.TRANSITION_TYPE_INVALID,
                true)
                .addLocalizedMessagePattern("Переезд с модели %{MODEL_ID} имеет некорретый тип %{TRANSITION_TYPE}.")
                .addParam(ModelStorage.ErrorParamName.MODEL_ID, sku1.getId())
                .addParam(ModelStorage.ErrorParamName.TRANSITION_TYPE, EntityType.MODEL.name())
        );
    }

    @Test
    public void testSkuTransitionForModel() {
        CommonModel guru = getGuruBuilder().id(1).deleted(true).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.ERROR)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.ERROR_REMOVAL)
                .setOldEntityId(guru.getId())
                .setOldEntityDeleted(true)
        );

        mockContext(guru);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(guru.getId(),
                ModelValidationError.ErrorType.TRANSITION_ERROR,
                ModelValidationError.ErrorSubtype.TRANSITION_MISSING,
                true)
                .addLocalizedMessagePattern("Для удаления модели %{MODEL_ID} необходимо создать переезды типа MODEL.")
                .addParam(ModelStorage.ErrorParamName.MODEL_ID, guru.getId()),
            new ModelValidationError(guru.getId(),
                ModelValidationError.ErrorType.TRANSITION_ERROR,
                ModelValidationError.ErrorSubtype.TRANSITION_TYPE_INVALID,
                true)
                .addLocalizedMessagePattern("Переезд с модели %{MODEL_ID} имеет некорретый тип %{TRANSITION_TYPE}.")
                .addParam(ModelStorage.ErrorParamName.MODEL_ID, guru.getId())
                .addParam(ModelStorage.ErrorParamName.TRANSITION_TYPE, EntityType.SKU.name())
        );
    }

    @Test
    public void testInconsistentTargetModelAndTransitionType() {
        CommonModel sku = getSkuBuilder(1).id(2).deleted(true).getModel();
        CommonModel guru = getGuruBuilder().id(1).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.DUPLICATE)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setPrimaryTransition(true)
                .setOldEntityId(sku.getId())
                .setOldEntityDeleted(true)
                .setNewEntityId(guru.getId())
        );

        mockContext(sku, guru);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(new ModelValidationError(sku.getId(),
            ModelValidationError.ErrorType.TRANSITION_ERROR,
            ModelValidationError.ErrorSubtype.TRANSITION_TARGET_INVALID,
            true)
            .addLocalizedMessagePattern("Переезд на модель %{MODEL_ID} имеет некорретый тип %{TRANSITION_TYPE}.")
            .addParam(ModelStorage.ErrorParamName.MODEL_ID, guru.getId())
            .addParam(ModelStorage.ErrorParamName.TRANSITION_TYPE, EntityType.SKU.name())
        );
    }

    @Test
    public void testMissingTargetModel() {
        CommonModel sku = getSkuBuilder(1).id(2).deleted(true).getModel();
        CommonModel guru = getGuruBuilder().id(1).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.DUPLICATE)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setOldEntityId(sku.getId())
                .setOldEntityDeleted(true)
                .setPrimaryTransition(true)
                .setNewEntityId(guru.getId())
        );

        mockContext(sku);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(new ModelValidationError(sku.getId(),
            ModelValidationError.ErrorType.TRANSITION_ERROR,
            ModelValidationError.ErrorSubtype.TRANSITION_MODEL_MISSING,
            true)
            .addLocalizedMessagePattern("Нельзя сделать переезд на несуществующую модель %{MODEL_ID}")
            .addParam(ModelStorage.ErrorParamName.MODEL_ID, guru.getId())
            .addParam(ModelStorage.ErrorParamName.TRANSITION_TYPE, EntityType.SKU.name())
        );
    }

    @Test
    public void testDeletionStatusInconsistent() {
        CommonModel sku = getSkuBuilder(1).id(2).deleted(true).getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.ERROR)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.ERROR_REMOVAL)
                .setOldEntityId(sku.getId())
                .setOldEntityDeleted(false)
        );

        mockContext(sku);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(new ModelValidationError(sku.getId(),
            ModelValidationError.ErrorType.TRANSITION_ERROR,
            ModelValidationError.ErrorSubtype.TRANSITION_TYPE_INVALID,
            true)
            .addLocalizedMessagePattern("Не совпадает признак удаления сущности в модели %{MODEL_ID} и переезде с нее.")
            .addParam(ModelStorage.ErrorParamName.MODEL_ID, sku.getId())
        );
    }

    @Test
    public void testIsSkuUncheck() {
        CommonModel before = getGuruBuilder().id(1)
            .withIsSkuParam(true)
            .getModel();
        CommonModel after = getGuruBuilder().id(1)
            .withIsSkuParam(false)
            .getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.ERROR)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.ERROR_REMOVAL)
                .setOldEntityId(after.getId())
                .setOldEntityDeleted(false)
        );

        mockContext(after);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(before, after),
            transitions
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testIsSkuUncheckAndRemove() {
        CommonModel before = getGuruBuilder().id(1)
            .withIsSkuParam(true)
            .getModel();
        CommonModel after = getGuruBuilder().id(1)
            .withIsSkuParam(false)
            .deleted(true)
            .getModel();

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.ERROR)
                .setEntityType(EntityType.SKU)
                .setReason(ModelTransitionReason.ERROR_REMOVAL)
                .setOldEntityId(after.getId())
                .setOldEntityDeleted(true),
            new ModelTransition()
                .setType(ModelTransitionType.ERROR)
                .setEntityType(EntityType.MODEL)
                .setReason(ModelTransitionReason.ERROR_REMOVAL)
                .setOldEntityId(after.getId())
                .setOldEntityDeleted(true)
        );
        mockContext(after);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(before, after),
            transitions
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testSkuDeletionNoTransition() {
        CommonModel before = getSkuBuilder(1).id(2)
            .setDeleted(false)
            .getModel();
        CommonModel after = getSkuBuilder(1).id(2)
            .setDeleted(true)
            .getModel();

        mockContext(after);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(before, after),
            Collections.emptyList()
        );

        assertThat(errors).containsExactlyInAnyOrder(new ModelValidationError(after.getId(),
            ModelValidationError.ErrorType.TRANSITION_ERROR,
            ModelValidationError.ErrorSubtype.TRANSITION_MISSING,
            true)
            .addLocalizedMessagePattern("Для удаления SKU %{MODEL_ID} необходимо создать переезды типа SKU.")
            .addParam(ModelStorage.ErrorParamName.MODEL_ID, after.getId())
        );
    }

    @Test
    public void testIsSkuUncheckNoTransition() {
        CommonModel before = getGuruBuilder().id(1)
            .withIsSkuParam(true)
            .getModel();
        CommonModel after = getGuruBuilder().id(1)
            .withIsSkuParam(false)
            .getModel();

        mockContext(after);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(before, after),
            Collections.emptyList()
        );

        assertThat(errors).containsExactlyInAnyOrder(new ModelValidationError(after.getId(),
            ModelValidationError.ErrorType.TRANSITION_ERROR,
            ModelValidationError.ErrorSubtype.TRANSITION_MISSING,
            true)
            .addLocalizedMessagePattern("Для удаления SKU %{MODEL_ID} необходимо создать переезды типа SKU.")
            .addParam(ModelStorage.ErrorParamName.MODEL_ID, after.getId())
        );
    }

    @Test
    public void testDuplicateNotPrimary() {
        CommonModel guru1 = getGuruBuilder().id(1)
            .setDeleted(true)
            .getModel();
        CommonModel guru2 = getGuruBuilder().id(2)
            .getModel();

        mockContext(guru1, guru2);

        List<ModelTransition> transitions = ImmutableList.of(
            new ModelTransition()
                .setType(ModelTransitionType.DUPLICATE)
                .setEntityType(EntityType.MODEL)
                .setReason(ModelTransitionReason.DUPLICATE_REMOVAL)
                .setOldEntityId(guru1.getId())
                .setNewEntityId(guru2.getId())
                .setOldEntityDeleted(true)
        );

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru1),
            transitions
        );

        assertThat(errors).containsExactlyInAnyOrder(new ModelValidationError(guru1.getId(),
            ModelValidationError.ErrorType.TRANSITION_ERROR,
            ModelValidationError.ErrorSubtype.TRANSITION_INVALID,
            true)
            .addLocalizedMessagePattern("Переезд с дублирующейся модели %{MODEL_ID} должен быть основным.")
            .addParam(ModelStorage.ErrorParamName.MODEL_ID, guru1.getId())
        );
    }

    private ModelValidationError createInvalidTransitionError(long modelId, String message) {
        return new ModelValidationError(modelId,
            ModelValidationError.ErrorType.TRANSITION_ERROR,
            ModelValidationError.ErrorSubtype.TRANSITION_INVALID,
            true)
            .addLocalizedMessagePattern(message)
            .addParam(ModelStorage.ErrorParamName.MODEL_ID, modelId);
    }

    private void mockContext(CommonModel... models) {
        ModelSaveGroup group = ModelSaveGroup.fromModels(models);
        when(context.getModelFromSaveGroupOrCache(anyLong()))
            .thenAnswer(i -> {
                Long modelId = i.getArgument(0);
                return Optional.ofNullable(group.getById(modelId));
            });
    }

    private ModelChanges getModelChanges(CommonModel after) {
        CommonModel before = new CommonModel(after);
        before.setDeleted(false);
        return new ModelChanges(before, after, ModelChanges.Operation.UPDATE);
    }

    private ModelChanges getModelChanges(CommonModel before, CommonModel after) {
        return new ModelChanges(before, after, ModelChanges.Operation.UPDATE);
    }
}
