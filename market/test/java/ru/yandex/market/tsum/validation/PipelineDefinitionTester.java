package ru.yandex.market.tsum.validation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.validation.PipelineValidator.PipelineDefinition;

public class PipelineDefinitionTester {

    private final SourceCodeService sourceCodeService;

    private PipelineDefinitionTester(SourceCodeService sourceCodeService) {
        this.sourceCodeService = sourceCodeService;
    }

    public static PipelineDefinitionTester newInstance(SourceCodeService sourceCodeService) {
        return new PipelineDefinitionTester(sourceCodeService);
    }

    public PipelineDefinitionTestingResult testSinglePipelineDefinition(PipelineDefinition pipelineDefinition) {
        return testPipelineDefinitions(Collections.singleton(pipelineDefinition));
    }

    public PipelineDefinitionTestingResult testPipelineDefinitions(Collection<PipelineDefinition> pipelineDefinitions) {

        PipelineDefinitionTestingResult result = PipelineDefinitionTestingResult.empty();

        pipelineDefinitions.forEach(pipelineDefinition -> {
            List<ValidationError> validationErrors = validatePipelineDefinition(pipelineDefinition);
            if (!validationErrors.isEmpty()) {
                result.addErrorsForPipelineId(pipelineDefinition.id, validationErrors);
            }
        });
        return result;
    }

    List<ValidationError> validatePipelineDefinition(PipelineDefinition pipelineDefinition) {
        return PipelineValidator.validate(pipelineDefinition, sourceCodeService);
    }

}

