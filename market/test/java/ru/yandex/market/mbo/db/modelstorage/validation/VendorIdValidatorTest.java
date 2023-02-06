package ru.yandex.market.mbo.db.modelstorage.validation;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Collections;
import java.util.List;

/**
 * @author s-ermakov
 */
public class VendorIdValidatorTest {

    private VendorIdValidator validator;
    private ModelValidationContextStub context;

    @Before
    public void setUp() throws Exception {
        validator = new VendorIdValidator();
        context = new ModelValidationContextStub(null);
    }

    @Test
    public void ifModelDoesNotContainVendorThenReturnError() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2).endModel();
        ModelChanges modelChanges = new ModelChanges(null, model);

        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges,
            Collections.singleton(model));

        Assertions.assertThat(actualErrors)
            .containsExactly(
                new ModelValidationError(model.getId(), ModelValidationError.ErrorType.EMPTY_VENDOR)
                    .addLocalizedMessagePattern("Вендор не заполнен.")
            );
    }

    @Test
    public void ifBadModelDidntHaveVendoreBeforeThenWarning() {
        CommonModel model = CommonModelBuilder.newBuilder(1, 2).endModel();
        ModelChanges modelChanges = new ModelChanges(model, model);

        List<ModelValidationError> actualErrors = validator.validate(context, modelChanges,
            Collections.singleton(model));

        Assertions.assertThat(actualErrors)
            .containsExactly(
                new ModelValidationError(model.getId(), ModelValidationError.ErrorType.EMPTY_VENDOR, null, false)
                    .addLocalizedMessagePattern("Вендор не заполнен.")
            );
    }
}
