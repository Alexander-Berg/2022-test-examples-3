package ru.yandex.market.tsum.spok.validation;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.config.PipelineScanConfiguration;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestBeansConfig;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.ReflectionsSourceCodeProvider;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.SourceCodeServiceImpl;
import ru.yandex.market.tsum.validation.PipelineDefinitionsFactory;
import ru.yandex.market.tsum.validation.PipelineValidator;
import ru.yandex.market.tsum.validation.PipelineValidator.PipelineDefinition;
import ru.yandex.market.tsum.validation.ValidationError;
import ru.yandex.market.tsum.validation.ValidationErrorType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        TestBeansConfig.class,
        PipeServicesConfig.class,
        PipelineScanConfiguration.class,
        MockCuratorConfig.class,
        SpokPipelineTestConfiguration.class

})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpokPipelineValidationTest {

    @Qualifier("spokPipelinesToValidate")
    @Autowired
    List<Pipeline> spokPipelinesToValidate;
    private SourceCodeService sourceCodeService = new SourceCodeServiceImpl(
            new ReflectionsSourceCodeProvider(ReflectionsSourceCodeProvider.SOURCE_CODE_PACKAGE)
    );
    private PipelineDefinitionsFactory pipelineDefinitionsFactory = PipelineDefinitionsFactory.newInstance(
            sourceCodeService
    );

    @Test
    public void validateSpokPipelinesFromTestConfiguration() {
        spokPipelinesToValidate
                .forEach(this::validate);
    }

    private void validate(Pipeline pipeline) {
        PipelineDefinition pipelineDefinition = pipelineDefinitionsFactory
                .createFromSinglePipeline(pipeline);


        List<ValidationError> errors = PipelineValidator.validate(pipelineDefinition, sourceCodeService)
                .stream()
                // todo: https://st.yandex-team.ru/MARKETINFRA-5422
                .filter(x -> !x.getType().equals(ValidationErrorType.MULTIPLE_TO_SINGLE_RESOURCE))
                .collect(Collectors.toList());

        Assert.assertEquals(
                "Errors: " + errors.stream().map(ValidationError::getMessage).collect(Collectors.joining("\n")),
                0, errors.size()
        );
    }

}
