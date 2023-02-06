package ru.yandex.market.tsum.validation;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.tsum.validation.ValidationErrorType.INVALID_RESOURCE;

public class PipelineDefinitionTestingResultTest {

    @Test
    public void verifyErrorMessageContent() {

        PipelineDefinitionTestingResult result = PipelineDefinitionTestingResult.empty();
        String invalidPipelineName = "I'm invalid pipeline";
        String validationErrorMessage = "In that pipeline invalid resource";
        ValidationErrorType validationErrorType = INVALID_RESOURCE;
        ValidationError validationError = new ValidationError(validationErrorType, validationErrorMessage);

        String expectedErrorMessage = String.format("Pipeline %s has errors:\n%s:%s\n\n",
            invalidPipelineName,
            validationErrorType,
            validationErrorMessage);

        result.addErrorsForPipelineId(invalidPipelineName, Collections.singletonList(validationError));

        assertTrue(result.hasErrors());
        assertEquals(expectedErrorMessage, result.getErrorMessage());
    }
}