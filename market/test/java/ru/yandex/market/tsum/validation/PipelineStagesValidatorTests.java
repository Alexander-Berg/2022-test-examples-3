package ru.yandex.market.tsum.validation;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.tsum.entity.pipeline.PipelineConfigurationType;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.stage.Stage;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageGroup;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.InMemorySourceCodeProvider;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.SourceCodeServiceImpl;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 21.02.18
 */
@SuppressWarnings("Duplicates")
public class PipelineStagesValidatorTests {
    StageGroup stages;

    private static final String FIRST_STAGE = "first";
    private static final String SECOND_STAGE = "second";
    private static final String THIRD_STAGE = "third";

    private Stage firstStage;
    private Stage secondStage;
    private Stage thirdStage;

    JobBuilder jobA;
    JobBuilder jobB;
    JobBuilder jobC;
    JobBuilder jobD;
    JobBuilder jobE;

    PipelineBuilder pipelineBuilder = PipelineBuilder.create();
    private SourceCodeService sourceCodeService;

    @Before
    public void setup() {
        InMemorySourceCodeProvider provide = new InMemorySourceCodeProvider();

        //noinspection unchecked
        provide.add(
            DummyJob.class
        );

        sourceCodeService = new SourceCodeServiceImpl(provide);

        stages = new StageGroup(FIRST_STAGE, SECOND_STAGE, THIRD_STAGE);
        firstStage = stages.getStage(FIRST_STAGE);
        secondStage = stages.getStage(SECOND_STAGE);
        thirdStage = stages.getStage(THIRD_STAGE);

        jobA = jobBuilder();
        jobB = jobBuilder();
        jobC = jobBuilder();
        jobD = jobBuilder();
        jobE = jobBuilder();

        jobB.withUpstreams(jobA);
        jobC.withUpstreams(jobB);
        jobD.withUpstreams(jobB);
        jobE.withUpstreams(jobC, jobD);

        /*
                C
        A - B -   - E
                D
         */
    }

    @Test
    public void invalidForSkippedStage() {
        jobA.beginStage(firstStage);
        jobB.beginStage(secondStage);

        List<ValidationError> errors =
            PipelineStagesValidator.validate(
                new PipelineValidator.PipelineDefinition(build(pipelineBuilder), PipelineConfigurationType.CD,
                    sourceCodeService)
            );

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.STAGE, errors.get(0).getType());
    }

    private JobBuilder jobBuilder() {
        return pipelineBuilder.withJob(DummyJob.class);
    }

    @Test
    public void validForDiamondPipeline() {
        jobA.beginStage(firstStage);
        jobB.beginStage(secondStage);
        jobE.beginStage(thirdStage);

        List<ValidationError> errors =
            PipelineStagesValidator.validate(
                new PipelineValidator.PipelineDefinition(build(pipelineBuilder), PipelineConfigurationType.CD,
                    sourceCodeService)
            );

        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void detectsParallelStages() {
        jobA.beginStage(firstStage);
        jobC.beginStage(secondStage);
        jobE.beginStage(thirdStage);

        List<ValidationError> errors =
            PipelineStagesValidator.validate(
                new PipelineValidator.PipelineDefinition(build(pipelineBuilder), PipelineConfigurationType.CD,
                    sourceCodeService)
            );

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.STAGE, errors.get(0).getType());
    }

    @Test
    public void detectsMultipleEntriesStages() {
        jobA.beginStage(firstStage);
        jobC.beginStage(secondStage);
        jobD.beginStage(secondStage);
        jobE.beginStage(thirdStage);

        List<ValidationError> errors =
            PipelineStagesValidator.validate(
                new PipelineValidator.PipelineDefinition(build(pipelineBuilder), PipelineConfigurationType.CD,
                    sourceCodeService)
            );

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.STAGE, errors.get(0).getType());
    }

    @Test
    public void detectsJobWithoutStage() {
        jobB.beginStage(secondStage);

        List<ValidationError> errors =
            PipelineStagesValidator.validate(
                new PipelineValidator.PipelineDefinition(build(pipelineBuilder), PipelineConfigurationType.CD,
                    sourceCodeService)
            );

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.STAGE, errors.get(0).getType());
    }

    @Test
    public void detectsWrongStageOrder() {
        jobA.beginStage(thirdStage);
        jobB.beginStage(secondStage);
        jobE.beginStage(firstStage);

        List<ValidationError> errors =
            PipelineStagesValidator.validate(
                new PipelineValidator.PipelineDefinition(build(pipelineBuilder), PipelineConfigurationType.CD,
                    sourceCodeService)
            );

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.STAGE, errors.get(0).getType());
    }

    @Test
    public void detectsJobWithoutDownstreamInNotLastStage() {
        jobE = jobBuilder();
        jobE.withUpstreams(jobC).beginStage(thirdStage);
        jobA.beginStage(firstStage);
        jobB.beginStage(secondStage);

        List<ValidationError> errors =
            PipelineStagesValidator.validate(
                new PipelineValidator.PipelineDefinition(build(pipelineBuilder), PipelineConfigurationType.CD,
                    sourceCodeService)
            );

        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.STAGE, errors.get(0).getType());
    }

    private Pipeline build(PipelineBuilder builder) {
        Pipeline pipeline = builder.withStages(stages.getStages()).build();
        pipeline.setBeanName("id");
        return pipeline;
    }
}
