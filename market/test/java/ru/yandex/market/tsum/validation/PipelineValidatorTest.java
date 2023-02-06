package ru.yandex.market.tsum.validation;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.tsum.entity.pipeline.PipelineConfigurationType;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.common.CanRunWhen;
import ru.yandex.market.tsum.pipe.engine.definition.common.UpstreamType;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.Features;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobFeature;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipe.engine.definition.resources.forms.ResourceInfo;
import ru.yandex.market.tsum.pipe.engine.definition.resources.forms.inputs.NumberField;
import ru.yandex.market.tsum.pipe.engine.definition.resources.forms.inputs.TextField;
import ru.yandex.market.tsum.pipe.engine.definition.stage.Stage;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageGroup;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.InMemorySourceCodeProvider;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.SourceCodeServiceImpl;
import ru.yandex.market.tsum.pipelines.common.resources.PerCommitLaunchParams;
import ru.yandex.market.tsum.pipelines.common.resources.PerCommitPullRequestParams;
import ru.yandex.market.tsum.pipelines.common.resources.PipelineEnvironment;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 04.05.18
 */
@SuppressWarnings("Duplicates")
public class PipelineValidatorTest {
    private SourceCodeService sourceCodeService;

    @Before
    public void setup() {
        InMemorySourceCodeProvider provide = new InMemorySourceCodeProvider();

        //noinspection unchecked
        provide.add(
            ProducesTestExtendedResourceJob.class,
            ProducesTestResourceFeature.class,
            ConsumesTestResourceFeature.class,
            TestResourceConsumer.class,
            TestExtendedResource.class,
            TestFeatureResource.class,
            TestResource.class,
            ProducesTestResourceJob.class,
            EmptyJob.class,
            TestResourceAndFeatureConsumer.class,
            TestFeatureResourceConsumer.class,
            TestResourceWithFields.class,
            TestResourceFieldsJob.class,
            TestMultitestingResources.class,
            TestPerCommitResources.class,
            PipelineEnvironment.class,
            PerCommitLaunchParams.class,
            PerCommitPullRequestParams.class,
            ProducesMultipleResourceJob.class,
            AbstractChildTestResource.class,
            AbstractTestResource.class,
            AbstractResourceProducer.class,
            AbstractResourceConsumer.class
        );

        sourceCodeService = new SourceCodeServiceImpl(provide);
    }

    @Test
    public void validatesCorrectPipeline() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildValidPipeline(), PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(
            errors.stream().map(ValidationError::getMessage).collect(Collectors.joining(",")),
            0, errors.size()
        );
    }

    @Test
    public void validatesCorrectPipelineWithStaticResource() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildValidPipelineWithStaticResource(), PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void validatesCorrectPipelineWithManualResource() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildValidPipelineWithManualResource(), PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void validatesNoResourceProduced() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildPipelineWithNotProducedResource(), PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.MISSING_RESOURCE, errors.get(0).getType());
    }

    @Test
    public void validatesCorrectMultitestingPipeline() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildValidMultitestingPipeline(), PipelineConfigurationType.MT, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(
            errors.stream().map(ValidationError::getMessage).collect(Collectors.joining(",")),
            0, errors.size()
        );
    }

    @Test
    public void validatesCorrectPerCommitPipeline() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildValidPerCommitPipeline(), PipelineConfigurationType.PER_COMMIT, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(
            errors.stream().map(ValidationError::getMessage).collect(Collectors.joining(",")),
            0, errors.size()
        );
    }

    @Test
    public void validatesNoResourceProducedForMultitesting() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildValidMultitestingPipeline(), PipelineConfigurationType.PER_COMMIT, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.MISSING_RESOURCE, errors.get(0).getType());
    }

    @Test
    public void validatesNoResourceProducedForPerCommit() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildValidPerCommitPipeline(), PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.MISSING_RESOURCE, errors.get(0).getType());
    }

    @Test
    public void validatesNoResourceProducedForFeature() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildPipelineWithNotProducedResourceInFeature(false), PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.MISSING_RESOURCE, errors.get(0).getType());
    }

    @Test
    public void skipValidationForDisabledFeature() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildPipelineWithNotProducedResourceInFeature(true), PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void validatesDoubleResources() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildPipelineWithDoubleResources(), PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.DOUBLE_RESOURCE, errors.get(0).getType());
    }

    @Test
    public void validatesNoResourcesLink() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildPipelineWithNoResourcesLink(), PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.MISSING_RESOURCE, errors.get(0).getType());
    }

    @Test
    public void validatesAnyLink() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildPipelineWithAnyLink(), PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.MISSING_RESOURCE, errors.get(0).getType());
    }

    @Test
    public void validatesManualResourcesInStaged() {
        StageGroup stages = new StageGroup("first");
        Stage firstStage = stages.getStage("first");

        PipelineBuilder builder = PipelineBuilder.create();
        builder.withManualResource(TestResource.class);

        JobBuilder first = builder.withJob(ProducesTestResourceJob.class);
        first.beginStage(firstStage);

        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            build(builder), PipelineConfigurationType.CD, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.MANUAL_RESOURCE, errors.get(0).getType());
    }

    @Test
    public void validatesErrorsInTheMiddleOfPipeline() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildPipelineWithMiddleLinkError(), PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.MISSING_RESOURCE, errors.get(0).getType());
    }

    @Test
    public void validatesErrorMultipleToSingleProduce() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildPipelineWithMultipleToSingleError(), PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.MULTIPLE_TO_SINGLE_RESOURCE, errors.get(0).getType());
    }

    @Test
    public void validatesStaticResourceFields() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildPipelineWithJobResourceCheck(), PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.INVALID_RESOURCE, errors.get(0).getType());
        String[] lines = errors.get(0).getMessage().split("\n");
        Assert.assertEquals(4, lines.length);
        Assert.assertEquals("Field incorrectIntValue min value is 2", lines[2]);
        Assert.assertEquals("Field incorrectStringValue min length is 2", lines[3]);

    }

    @Test
    public void validateErrorsInPipelineWithNoJobs() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildPipelineWithNoJobs(), PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(ValidationErrorType.EMPTY_PIPELINE, errors.get(0).getType());
    }

    @Test
    public void validatesCorrectPipelineWithAbstractResourceProducer() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildValidPipelineWithAbstractResourceProducer(),
            PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void validatesCorrectPipelineWithAbstractResourceConsumer() {
        PipelineValidator.PipelineDefinition pipeline = new PipelineValidator.PipelineDefinition(
            buildValidPipelineWithAbstractResourceConsumer(),
            PipelineConfigurationType.RELEASE, sourceCodeService
        );

        List<ValidationError> errors = PipelineValidator.validate(pipeline, sourceCodeService);
        Assert.assertEquals(0, errors.size());
    }

    private Pipeline buildValidPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(ProducesTestExtendedResourceJob.class);
        JobBuilder second = builder.withJob(TestResourceAndFeatureConsumer.class)
            .withUpstreams(first);

        return build(builder);
    }

    private Pipeline buildValidPipelineWithStaticResource() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(TestResourceConsumer.class)
            .withResources(new TestResource());
        return build(builder);
    }

    private Pipeline buildValidPipelineWithManualResource() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withManualResource(TestExtendedResource.class);
        builder.withJob(TestResourceConsumer.class);
        return build(builder);
    }

    private Pipeline buildValidPipelineWithAbstractResourceProducer() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(AbstractResourceProducer.class);
        return build(builder);
    }

    private Pipeline buildValidPipelineWithAbstractResourceConsumer() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withManualResource(AbstractChildTestResource.class);
        builder.withJob(AbstractResourceConsumer.class);
        return build(builder);
    }

    private Pipeline build(PipelineBuilder builder) {
        Pipeline pipeline = builder.build();
        pipeline.setBeanName("id");
        return pipeline;
    }

    private Pipeline buildPipelineWithNoJobs() {
        PipelineBuilder builder = PipelineBuilder.create();
        return build(builder);
    }

    private Pipeline buildValidMultitestingPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(TestMultitestingResources.class);
        return build(builder);
    }

    private Pipeline buildValidPerCommitPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(TestPerCommitResources.class);
        return build(builder);
    }

    private Pipeline buildPipelineWithNotProducedResource() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(EmptyJob.class);
        JobBuilder second = builder.withJob(TestResourceConsumer.class)
            .withUpstreams(first);

        return build(builder);
    }

    private Pipeline buildPipelineWithNotProducedResourceInFeature(boolean disableFeature) {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder job = builder.withJob(TestFeatureResourceConsumer.class);

        if (disableFeature) {
            job.disableFeature(ConsumesTestResourceFeature.class);
        }

        return build(builder);
    }

    private Pipeline buildPipelineWithNoResourcesLink() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(ProducesTestResourceJob.class);
        JobBuilder second = builder.withJob(TestResourceConsumer.class)
            .withUpstreams(UpstreamType.NO_RESOURCES, first);

        return build(builder);
    }

    private Pipeline buildPipelineWithAnyLink() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(ProducesTestResourceJob.class);
        JobBuilder second = builder.withJob(EmptyJob.class);
        JobBuilder third = builder.withJob(TestResourceConsumer.class)
            .withUpstreams(CanRunWhen.ANY_COMPLETED, first, second);

        return build(builder);
    }

    private Pipeline buildPipelineWithMiddleLinkError() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(EmptyJob.class);
        JobBuilder second = builder.withJob(TestResourceConsumer.class)
            .withUpstreams(first);
        JobBuilder third = builder.withJob(EmptyJob.class)
            .withUpstreams(second);

        return build(builder);
    }

    private Pipeline buildPipelineWithMultipleToSingleError() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(ProducesMultipleResourceJob.class);
        JobBuilder second = builder.withJob(TestResourceConsumer.class)
            .withUpstreams(first);

        return build(builder);
    }

    private Pipeline buildPipelineWithDoubleResources() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(ProducesTestResourceJob.class);
        JobBuilder second = builder.withJob(ProducesTestResourceJob.class)
            .withUpstreams(first);
        JobBuilder third = builder.withJob(TestResourceConsumer.class)
            .withUpstreams(second);

        return build(builder);
    }

    private Pipeline buildPipelineWithJobResourceCheck() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(TestResourceFieldsJob.class)
            .withResources(new TestResourceWithFields());

        return build(builder);
    }

    private static class TestResource implements Resource {
        private int field;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("6c3be076-755b-44ae-a232-815750818d2b");
        }

        public int getField() {
            return field;
        }
    }

    private static class TestFeatureResource implements Resource {
        private int field;

        public int getField() {
            return field;
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("caf6c11c-e0cf-4d64-a60e-7bd280f77d21");
        }
    }

    @ResourceInfo
    private static class TestResourceWithFields implements Resource {
        private Integer optionalIntField = null;
        private int requiredIntField = 1;

        @NumberField(min = 2)
        private int incorrectIntValue = 1;

        @TextField(minLength = 2)
        private String incorrectStringValue = "a";

        public Integer getOptionalIntField() {
            return optionalIntField;
        }

        public int getRequiredIntField() {
            return requiredIntField;
        }

        public int getIncorrectIntValue() {
            return incorrectIntValue;
        }

        public String getIncorrectStringValue() {
            return incorrectStringValue;
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("e1aa0e4d-f7a2-4e04-9094-f4704eef40be");
        }
    }

    private static class TestExtendedResource extends TestResource {

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("885e4de2-8970-4328-84fb-a313bb0cfd2f");
        }
    }

    private static class EmptyJob implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("6398cac1-4b7f-4b9e-986d-114617e9ba00");
        }

        @Override
        public void execute(JobContext context) {

        }
    }

    private static class TestResourceFieldsJob implements JobExecutor {
        @WiredResource
        private TestResourceWithFields field;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("604f706e-bee7-456c-b737-f9092174186b");
        }

        @Override
        public void execute(JobContext context) {

        }
    }

    @Produces(single = TestFeatureResource.class)
    private static class ProducesTestResourceFeature implements JobFeature {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("60042372-3daa-46f4-8507-c8c266cf8216");
        }

        @Override
        public void execute(JobContext context) {

        }
    }

    private static class ConsumesTestResourceFeature implements JobFeature {
        @WiredResource
        private TestFeatureResource resource;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("1855fa72-edf8-4f5a-9a2f-bd4666bc6f65");
        }

        @Override
        public void execute(JobContext context) {

        }
    }

    @Produces(single = TestResource.class)
    private static class ProducesTestResourceJob implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("3521c404-bc63-496a-9793-91d55157a2d9");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.features().runAll();
        }
    }

    @Produces(multiple = TestResource.class)
    private static class ProducesMultipleResourceJob implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("fa4743d8-33d4-4f0e-af59-cd80ced7502d");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.features().runAll();
        }
    }

    @Produces(single = TestExtendedResource.class)
    @Features(ProducesTestResourceFeature.class)
    private static class ProducesTestExtendedResourceJob implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("015c557f-af33-4d28-a9a7-e7ff10ec2c71");
        }

        @Override
        public void execute(JobContext context) {

        }
    }

    private static class TestResourceConsumer implements JobExecutor {
        @WiredResource
        private TestResource test;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("1f45d0f8-4f6c-4718-9113-72b546746e66");
        }

        @Override
        public void execute(JobContext context) {

        }
    }

    @Features(ConsumesTestResourceFeature.class)
    private static class TestFeatureResourceConsumer implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("b36547cb-cf70-4975-b6f3-3a993d7c1551");
        }

        @Override
        public void execute(JobContext context) {

        }
    }

    private static class TestResourceAndFeatureConsumer implements JobExecutor {
        @WiredResource
        private TestResource test;

        @WiredResource
        private TestFeatureResource test2;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("460db329-923f-4882-af4a-4ace7e234037");
        }

        @Override
        public void execute(JobContext context) {

        }
    }

    private static class TestMultitestingResources implements JobExecutor {
        @WiredResource
        private PipelineEnvironment pipelineEnvironment;

        @WiredResource
        private PerCommitLaunchParams perCommitLaunchParams;

        @WiredResource
        private PerCommitPullRequestParams perCommitPullRequestParams;

        @Override
        public void execute(JobContext context) throws Exception {

        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("7bfd4b92-8d09-4cb4-954b-d958f4162197");
        }
    }

    private static class TestPerCommitResources implements JobExecutor {
        @WiredResource
        private PerCommitLaunchParams perCommitLaunchParams;

        @WiredResource
        private PerCommitPullRequestParams perCommitPullRequestParams;

        @Override
        public void execute(JobContext context) throws Exception {

        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("e6ccf2d0-9256-4929-8d48-5d8b065b91ae");
        }
    }

    private abstract static class AbstractTestResource implements Resource {
        private int field;

        public int getField() {
            return field;
        }
    }

    private static class AbstractChildTestResource extends AbstractTestResource {

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("441de33e-9dd9-4b1e-820c-9b59e8d1ad33");
        }
    }

    @Produces(single = {AbstractChildTestResource.class})
    private static class AbstractResourceProducer implements JobExecutor {

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("1699dfa8-1cc6-46d3-82b6-7fa7ef669f12");
        }

        @Override
        public void execute(JobContext context) throws Exception {

        }
    }

    private static class AbstractResourceConsumer implements JobExecutor {
        @WiredResource
        AbstractChildTestResource resource;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("04417715-3d43-43d9-ba67-4aacf07c6713");
        }

        @Override
        public void execute(JobContext context) throws Exception {

        }
    }
}
