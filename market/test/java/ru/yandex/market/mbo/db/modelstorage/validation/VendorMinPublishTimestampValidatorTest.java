package ru.yandex.market.mbo.db.modelstorage.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import static org.assertj.core.api.Assertions.assertThat;

public class VendorMinPublishTimestampValidatorTest extends BaseValidatorTestClass {

    private static final long CATEGORY_ID = 100500;
    private static final long VENDOR_MIN_PUBLISH_TIMESTAMP_ID = 16451588L;

    private VendorMinPublishTimestampValidator validator;

    @Before
    public void setup() {
        validator = new VendorMinPublishTimestampValidator();
    }

    @Test
    public void whenEmptyParamThenOk() {
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model(0, null)),
            Collections.emptyList()
        );
        assertThat(errors).isEmpty();
    }

    @Test
    public void whenParamHasEmptyStringValueThenOk() {
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model(0, Collections.singletonList(""))),
            Collections.emptyList()
        );
        assertThat(errors).isEmpty();
    }

    @Test
    public void whenParamHasNumberInStringValueThenOk() {
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model(0, Collections.singletonList("123"))),
            Collections.emptyList()
        );
        assertThat(errors).isEmpty();
    }

    @Test
    public void whenParamHasSeveralNumbersInStringValueThenOk() {
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model(0L, Arrays.asList("-123", "567", "0"))),
            Collections.emptyList()
        );
        assertThat(errors).isEmpty();
    }

    @Test
    public void whenParamHasNotANumberInStringValueThenError() {
        String errValue = "abv";
        CommonModel model = model(0L, Collections.singletonList(errValue));
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            validator.createError(context, model, VENDOR_MIN_PUBLISH_TIMESTAMP_ID, errValue)
        );
    }

    @Test
    public void whenParamHasSomeNotANumberInStringValueThenError() {
        String errValue1 = "abv";
        String errValue2 = "gde";
        CommonModel model = model(0L, Arrays.asList("123", errValue1, errValue2));
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.emptyList()
        );
        assertThat(errors).containsExactlyInAnyOrder(
            validator.createError(context, model, VENDOR_MIN_PUBLISH_TIMESTAMP_ID, errValue1),
            validator.createError(context, model, VENDOR_MIN_PUBLISH_TIMESTAMP_ID, errValue2)
        );
    }

    private CommonModel model(long id, List<String> values) {
        CommonModelBuilder<CommonModel> modelBuilder = CommonModelBuilder.newBuilder()
            .id(id)
            .category(CATEGORY_ID);
        if (values != null) {
            modelBuilder.parameterValues(VENDOR_MIN_PUBLISH_TIMESTAMP_ID, XslNames.VENDOR_MIN_PUBLISH_TIMESTAMP,
                ModificationSource.AUTO, values.toArray(new String[]{}));
        }

        return modelBuilder.getModel();
    }
}
