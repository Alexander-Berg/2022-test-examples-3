package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.ValidationErrorUtils.assertValidationError;

/**
 * @author anmalysh
 */
public class CategoryIdValidatorTest {

    private CategoryIdValidator validator;
    private ModelValidationContext context;

    @Before
    public void setup() {
        validator = new CategoryIdValidator();

        context = mock(ModelValidationContext.class);
        when(context.hasCategory(anyLong())).thenReturn(false);
    }

    @Test
    public void testEmptyCategory() {
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, createModel(CommonModel.NO_ID)),
            Collections.emptyList()
        );
        assertEquals(1, errors.size());
        ModelValidationError error = errors.get(0);
        assertValidationError(error, ModelValidationError.ErrorType.EMPTY_CATEGORY, true, new LinkedHashMap<>());
    }

    @Test
    public void testMissingCategory() {
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, createModel(1L)),
            Collections.emptyList()
        );
        assertEquals(1, errors.size());
        ModelValidationError error = errors.get(0);
        LinkedHashMap<ModelStorage.ErrorParamName, String> params = new LinkedHashMap<>();
        params.put(ModelStorage.ErrorParamName.CATEGORY_ID, "1");
        assertValidationError(error, ModelValidationError.ErrorType.MISSING_CATEGORY, true, params);
    }

    @Test
    public void testUnchangedCategoryWarn() {
        List<ModelValidationError> errors = validator.validate(context,
            new ModelChanges(createModel(0L), createModel(0L)),
            Collections.emptyList()
        );
        assertEquals(1, errors.size());
        ModelValidationError error = errors.get(0);
        assertValidationError(error, ModelValidationError.ErrorType.EMPTY_CATEGORY, false, new LinkedHashMap<>());
    }

    private CommonModel createModel(Long hid) {
        CommonModel model = new CommonModel();
        model.setCategoryId(hid);
        return model;
    }
}
