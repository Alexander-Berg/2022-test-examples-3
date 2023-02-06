package ru.yandex.market.tsum.validation;

import java.util.Collection;
import java.util.stream.Collectors;

public class PipelineDefinitionTestingResult {

    private final StringBuilder errorMessage = new StringBuilder();

    private PipelineDefinitionTestingResult() {

    }

    public static PipelineDefinitionTestingResult empty() {
        return new PipelineDefinitionTestingResult();
    }

    void addErrorsForPipelineId(String pipelineId,
                                Collection<ValidationError> validationErrors) {
        String validationErrorDescription = createErrorDescriptionFromPipelineIdAndValidationErrors(
            pipelineId,
            validationErrors
        );
        addValidationErrorDescription(validationErrorDescription);
    }

    private void addValidationErrorDescription(String validationErrorDescription) {
        errorMessage.append(validationErrorDescription).append("\n\n");
    }

    private String createErrorDescriptionFromPipelineIdAndValidationErrors(
        String pipelineId,
        Collection<ValidationError> validationErrors) {

        return String.format(
            "Pipeline %s has errors:\n%s", pipelineId,
            validationErrors
                .stream()
                .map(validationError ->
                    String.format("%s:%s",
                        validationError.getType(),
                        validationError.getMessage()
                    )
                )
                .collect(Collectors.joining("\n"))
        );
    }

    public String getErrorMessage() {
        return errorMessage.toString();
    }

    public boolean hasErrors() {
        return errorMessage.length() > 0;
    }
}
