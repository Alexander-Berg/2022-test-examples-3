package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DeletedModelValidator}.
 *
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:magicNumber")
@RunWith(MockitoJUnitRunner.class)
public class DeletedModelValidatorTest extends BaseValidatorTestClass {

    private DeletedModelValidator validator;

    @Mock
    private ModelValidationContext context;

    private CommonModel model;

    @Before
    public void setup() {
        validator = new DeletedModelValidator();

        model = createModel(1, m -> {
            m.setDeleted(true);
        });
        storage.saveModel(model, saveContext);
    }

    @Test
    public void testModifyDeleted() {
        when(context.getIndexedModelCategory(anyLong())).thenReturn(CATEGORY_ID);
        when(context.isForcedModifyDeleted()).thenReturn(false);

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(model), Arrays.asList(model));

        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(
                model.getId(),
                ModelValidationError.ErrorType.MODEL_IS_DELETED,
                true,
                true)
                .addLocalizedMessagePattern("Модель удалена. Подтверждаете редактирование удаленной модели?")
        );
    }

    @Test
    public void testModifyDeletedForce() {
        when(context.getIndexedModelCategory(anyLong())).thenReturn(CATEGORY_ID);
        when(context.isForcedModifyDeleted()).thenReturn(true);

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(model), Arrays.asList(model));

        assertThat(errors).isEmpty();
    }

    @Test
    public void testModifyDeletedForceNotAllowed() {
        when(context.getIndexedModelCategory(anyLong())).thenReturn(1L);

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(model), Arrays.asList(model));

        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(
                model.getId(),
                ModelValidationError.ErrorType.MODEL_IS_DELETED,
                true,
                false)
                .addLocalizedMessagePattern(
                    "Модель перенесена в категорию %{CATEGORY_ID}. Редактирование возможно только в новой категории.")
                .addParam(ModelStorage.ErrorParamName.CATEGORY_ID, 1L)
        );
    }
}
