package ru.yandex.market.tsum.validation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.config.PipelineScanConfiguration;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.runtime.PipeProvider;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestBeansConfig;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.ReflectionsSourceCodeProvider;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.SourceCodeServiceImpl;
import ru.yandex.market.tsum.validation.PipelineValidator.PipelineDefinition;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 04.05.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestBeansConfig.class, PipeServicesConfig.class, PipelineScanConfiguration.class,
    MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PipelinesValidationTest {
    @Autowired
    private PipeProvider pipeProvider;

    @Autowired
    private ListableBeanFactory listableBeanFactory;

    private final SourceCodeService sourceCodeService = new SourceCodeServiceImpl(
        new ReflectionsSourceCodeProvider(ReflectionsSourceCodeProvider.SOURCE_CODE_PACKAGE)
    );

    private final PipelineDefinitionsFactory pipelineDefinitionsFactory = PipelineDefinitionsFactory.newInstance(
        sourceCodeService
    );

    private final PipelineDefinitionTester pipelineDefinitionTester = PipelineDefinitionTester.newInstance(
        sourceCodeService
    );

    @Test
    public void validateReleasePipelines() {

        Collection<String> pipelineIds = getPipelineIdsAndAssertTheyAreMoreThanCount(5);

        Collection<PipelineDefinition> pipelineDefinitions = pipelineDefinitionsFactory
            .createFromPipelineIdsWithPipelineSupplier(pipelineIds, pipeProvider::get);

        PipelineDefinitionTestingResult testingResult = pipelineDefinitionTester
            .testPipelineDefinitions(pipelineDefinitions);

        assertFalse(testingResult.getErrorMessage(), testingResult.hasErrors());
    }

    @SuppressWarnings("SameParameterValue")
    private Collection<String> getPipelineIdsAndAssertTheyAreMoreThanCount(int count) {
        List<String> pipelineIds = getSourceCodePipelineIds();
        assertTrue("Too few pipelines. Something is wrong.", pipelineIds.size() > count);
        return pipelineIds;
    }

    private List<String> getSourceCodePipelineIds() {
        return Stream.of(listableBeanFactory.getBeanNamesForType(Pipeline.class))
            .distinct()
            .collect(Collectors.toList());
    }

}
