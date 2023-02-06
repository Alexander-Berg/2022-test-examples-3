package ru.yandex.market.mbo.db.modelstorage.validation.processing;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorSubtype.DEFINITION_MISMATCH;
import static ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorSubtype.INVALID_OPTION_ID;
import static ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorSubtype.OUT_OF_BOUNDS_VALUE;
import static ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorSubtype.STRING_VALUE_EMPTY;
import static ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorType.DUPLICATE_BARCODE;
import static ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorType.INVALID_PARAMETER_VALUE;

/**
 * @author york
 * @since 25.03.2019
 */
public class ValidationErrorProcessingServiceTest {

    private ValidationErrorProcessingService validationErrorProcessingService;

    @Before
    public void setUp() {
        validationErrorProcessingService = new ValidationErrorProcessingService(Arrays.asList(
            new OutOfBoundsParamValueErrorProcessor(),
            new EraseBrokenParamValuesProcessor(),
            new EraseInvalidParamOptionIdErrorProcessor()
        ));

    }

    @Test
    public void testAllNotRepairable() {
        ModelSaveContext context = new ModelSaveContext(0);
        List<ModelValidationError> errors = Arrays.asList(
            new ModelValidationError(1L, DUPLICATE_BARCODE),
            new ModelValidationError(1L, INVALID_PARAMETER_VALUE, OUT_OF_BOUNDS_VALUE, true, true)
        );

        assertFalse(validationErrorProcessingService.allRepairable(context, errors));
    }

    @Test
    public void testNotAllRepairable2() {
        ModelSaveContext context = new ModelSaveContext(0);
        context.setEraseBrokenParams(true);
        context.setEraseParamsWithInvalidOptionIds(true);
        List<ModelValidationError> errors = Arrays.asList(
            new ModelValidationError(1L, INVALID_PARAMETER_VALUE, OUT_OF_BOUNDS_VALUE, true, true),
            new ModelValidationError(1L, INVALID_PARAMETER_VALUE, DEFINITION_MISMATCH, true, true),
            new ModelValidationError(1L, INVALID_PARAMETER_VALUE, INVALID_OPTION_ID, true, true)
        );

        assertFalse(validationErrorProcessingService.allRepairable(context, errors));
    }

    @Test
    public void testAllRepairable1() {
        ModelSaveContext context = new ModelSaveContext(0);
        context.setEraseBrokenParams(true);
        List<ModelValidationError> errors = Arrays.asList(
            new ModelValidationError(1L, DUPLICATE_BARCODE, true, true),
            new ModelValidationError(1L, INVALID_PARAMETER_VALUE, DEFINITION_MISMATCH)
        );

        assertTrue(validationErrorProcessingService.allRepairable(context, errors));
    }

    @Test
    public void testAllRepairable2() {
        ModelSaveContext context = new ModelSaveContext(0);
        context.setEraseParamsWithInvalidOptionIds(true);
        List<ModelValidationError> errors = Arrays.asList(
            new ModelValidationError(1L, DUPLICATE_BARCODE, true, true),
            new ModelValidationError(1L, INVALID_PARAMETER_VALUE, INVALID_OPTION_ID)
        );

        assertTrue(validationErrorProcessingService.allRepairable(context, errors));
    }

    @Test
    public void testAllRepairable3() {
        ModelSaveContext context = new ModelSaveContext(0);
        context.setForceAll(true);
        List<ModelValidationError> errors = Arrays.asList(
            new ModelValidationError(1L, DUPLICATE_BARCODE),
            new ModelValidationError(1L, INVALID_PARAMETER_VALUE, DEFINITION_MISMATCH, true, true),
            new ModelValidationError(1L, INVALID_PARAMETER_VALUE, INVALID_OPTION_ID)
        );

        assertTrue(validationErrorProcessingService.allRepairable(context, errors));
    }

    @Test
    public void testMinorErrorsIsRepairable() {
        ModelSaveContext context = new ModelSaveContext(0);
        List<ModelValidationError> errors = Arrays.asList(
            new ModelValidationError(1L, INVALID_PARAMETER_VALUE, OUT_OF_BOUNDS_VALUE, false, true),
            new ModelValidationError(1L, INVALID_PARAMETER_VALUE, STRING_VALUE_EMPTY, false, true)
        );

        assertTrue(validationErrorProcessingService.allRepairable(context, errors));
    }

    @Test
    public void testMinorErrorsProcess() {
        ModelValidationError minorError = new ModelValidationError(1L, INVALID_PARAMETER_VALUE,
            STRING_VALUE_EMPTY, false, true);
        ModelValidationError criticalError = new ModelValidationError(1L, INVALID_PARAMETER_VALUE,
            STRING_VALUE_EMPTY, true, true);

        assertTrue(validationErrorProcessingService.process(null, null, null, minorError).isSuccess());
        assertFalse(validationErrorProcessingService.process(null, null, null, criticalError).isSuccess());
    }
}
