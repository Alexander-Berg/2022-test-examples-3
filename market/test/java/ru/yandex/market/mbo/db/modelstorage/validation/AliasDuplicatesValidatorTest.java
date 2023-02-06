package ru.yandex.market.mbo.db.modelstorage.validation;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Collections;
import java.util.List;

/**
 * @author york
 * @since 29.04.2020
 */
public class AliasDuplicatesValidatorTest extends BaseValidatorTestClass {
    private static final Long MODEL_ID = 1L;
    private static final Long ALIAS_PARAM_ID = 100000L;
    private static final Long VC_PARAM_ID = 100001L;
    private static final Long BC_PARAM_ID = 100002L;

    private AliasDuplicatesValidator validator = new AliasDuplicatesValidator();

    @Test
    public void testNoParams() {
        CommonModel model = createModel(MODEL_ID, CommonModel.Source.GURU, cb -> {
            cb.parameterValues(ALIAS_PARAM_ID, XslNames.ALIASES, "al1", "al2");
        });

        List<ModelValidationError> errors =
            validator.validate(context, new ModelChanges(model), Collections.singletonList(model));

        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testDuplicatesInVC() {
        CommonModel model = createModel(MODEL_ID, CommonModel.Source.GURU, cb -> {
            cb.parameterValues(ALIAS_PARAM_ID, XslNames.ALIASES, "al1", "al2", "al3")
                    .parameterValues(VC_PARAM_ID, XslNames.VENDOR_CODE, "vc1", "al2", "al1");
        });

        List<ModelValidationError> errors =
                validator.validate(context, new ModelChanges(model), Collections.singletonList(model));

        Assertions.assertThat(errors).containsExactlyInAnyOrder(
                validator.createError(MODEL_ID, XslNames.VENDOR_CODE, "al1",
                        ModelValidationError.ErrorSubtype.ALIAS_DUPLICATE_IN_VENDOR_CODE),
                validator.createError(MODEL_ID, XslNames.VENDOR_CODE, "al2",
                        ModelValidationError.ErrorSubtype.ALIAS_DUPLICATE_IN_VENDOR_CODE)
        );
    }

    @Test
    public void testDuplicatesInBC() {
        CommonModel model = createModel(MODEL_ID, CommonModel.Source.GURU, cb -> {
            cb.parameterValues(ALIAS_PARAM_ID, XslNames.ALIASES, "al1", "al2", "al3")
                .parameterValues(BC_PARAM_ID, XslNames.BAR_CODE, "bc1", "al2", "al1")
                .parameterValues(VC_PARAM_ID, XslNames.VENDOR_CODE, "vc1", "vc2");
        });

        List<ModelValidationError> errors =
            validator.validate(context, new ModelChanges(model), Collections.singletonList(model));

        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            validator.createError(MODEL_ID, XslNames.BAR_CODE, "al1",
                ModelValidationError.ErrorSubtype.ALIAS_DUPLICATE_IN_BARCODE),
            validator.createError(MODEL_ID, XslNames.BAR_CODE, "al2",
                ModelValidationError.ErrorSubtype.ALIAS_DUPLICATE_IN_BARCODE)
        );
    }

    @Test
    public void testDuplicatesInBCAndVC() {
        CommonModel model = createModel(MODEL_ID, CommonModel.Source.GURU, cb -> {
            cb.parameterValues(ALIAS_PARAM_ID, XslNames.ALIASES, "al1", "al2", "al3")
                    .parameterValues(BC_PARAM_ID, XslNames.BAR_CODE, "bc1", "al2", "al1")
                    .parameterValues(VC_PARAM_ID, XslNames.VENDOR_CODE, "vc1", "vc2", "al2");
        });

        List<ModelValidationError> errors =
                validator.validate(context, new ModelChanges(model), Collections.singletonList(model));

        Assertions.assertThat(errors).containsExactlyInAnyOrder(
                validator.createError(MODEL_ID, XslNames.BAR_CODE, "al1",
                        ModelValidationError.ErrorSubtype.ALIAS_DUPLICATE_IN_BARCODE),
                validator.createError(MODEL_ID, XslNames.BAR_CODE, "al2",
                        ModelValidationError.ErrorSubtype.ALIAS_DUPLICATE_IN_BARCODE),
                validator.createError(MODEL_ID, XslNames.VENDOR_CODE, "al2",
                        ModelValidationError.ErrorSubtype.ALIAS_DUPLICATE_IN_VENDOR_CODE)
        );
    }

}
