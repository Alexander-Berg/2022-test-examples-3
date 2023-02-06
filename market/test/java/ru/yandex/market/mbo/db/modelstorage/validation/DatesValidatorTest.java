package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.db.modelstorage.validation.DatesValidator.CREATION;
import static ru.yandex.market.mbo.db.modelstorage.validation.DatesValidator.DELETED;
import static ru.yandex.market.mbo.db.modelstorage.validation.DatesValidator.MODIFICATION;

@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.class)
public class DatesValidatorTest {

    @Mock
    private ModelValidationContext context;

    private DatesValidator validator;

    @Before
    public void setup() {
        validator = new DatesValidator();
    }

    @Test
    public void testMissingCreateUpdateDatesYieldsCritOnNewModel() {
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model(0, null, null, new Date())),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(0L, ModelValidationError.ErrorType.EMPTY_DATE, true)
                .addLocalizedMessagePattern("Дата создания не заполнена.")
                .addParam(ModelStorage.ErrorParamName.MODEL_OPERATION_TYPE, CREATION),

            new ModelValidationError(0L, ModelValidationError.ErrorType.EMPTY_DATE, true)
                .addLocalizedMessagePattern("Дата редактирования не заполнена.")
                .addParam(ModelStorage.ErrorParamName.MODEL_OPERATION_TYPE, MODIFICATION)
        );
    }

    @Test
    public void testMissingCreateUpdateDatesYieldsCritOnUpdatedModel() {
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(model(6, new Date(), new Date(), new Date()), model(6, null, null, new Date())),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(6L, ModelValidationError.ErrorType.EMPTY_DATE, true)
                .addLocalizedMessagePattern("Дата создания не заполнена.")
                .addParam(ModelStorage.ErrorParamName.MODEL_OPERATION_TYPE, CREATION),

            new ModelValidationError(6L, ModelValidationError.ErrorType.EMPTY_DATE, true)
                .addLocalizedMessagePattern("Дата редактирования не заполнена.")
                .addParam(ModelStorage.ErrorParamName.MODEL_OPERATION_TYPE, MODIFICATION)
        );
    }

    @Test
    public void testMissingCreateUpdateDatesYieldsWarnOnSameNulls() {
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(model(6, null, null, null), model(6, null, null, new Date())),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(6L, ModelValidationError.ErrorType.EMPTY_DATE, false)
                .addLocalizedMessagePattern("Дата создания не заполнена.")
                .addParam(ModelStorage.ErrorParamName.MODEL_OPERATION_TYPE, CREATION),

            new ModelValidationError(6L, ModelValidationError.ErrorType.EMPTY_DATE, false)
                .addLocalizedMessagePattern("Дата редактирования не заполнена.")
                .addParam(ModelStorage.ErrorParamName.MODEL_OPERATION_TYPE, MODIFICATION)
        );
    }

    @Test
    public void testMissingDeleteDateYieldsCritOnUpdatedModel() {
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(rmModel(6, new Date(), new Date(), new Date()), rmModel(6, new Date(), new Date(), null)),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(6L, ModelValidationError.ErrorType.EMPTY_DATE, true)
                .addLocalizedMessagePattern("Дата удаления не заполнена.")
                .addParam(ModelStorage.ErrorParamName.MODEL_OPERATION_TYPE, DELETED)
        );
    }

    @Test
    public void testMissingDeleteDateYieldsWarnOnSameNull() {
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(rmModel(6L, new Date(), new Date(), null), rmModel(6, new Date(), new Date(), null)),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            new ModelValidationError(6L, ModelValidationError.ErrorType.EMPTY_DATE, false)
                .addLocalizedMessagePattern("Дата удаления не заполнена.")
                .addParam(ModelStorage.ErrorParamName.MODEL_OPERATION_TYPE, DELETED)
        );
    }

    private CommonModel model(long id, Date created, Date updated, Date deleted) {
        CommonModel model = new CommonModel();
        model.setId(id);
        if (created != null) {
            model.setCreatedDate(created);
        }
        if (updated != null) {
            model.setModificationDate(updated);
        }
        if (deleted != null) {
            model.setDeletedDate(deleted);
        }
        return model;
    }

    private CommonModel rmModel(long id, Date created, Date updated, Date deleted) {
        CommonModel model = model(id, created, updated, deleted);
        model.setDeleted(true);
        return model;
    }
}
