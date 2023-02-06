package ru.yandex.market.tsum.pipe.engine.runtime;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.annotation.PersistenceConstructor;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.common.UpstreamType;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res2;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res3;

import java.util.UUID;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 03.10.17
 */
public class TestUpstreamsPipeline {
    public static final String CONSUME_RES1_ID = "consumeRes1";

    public static Pipeline noResourcePipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder produceRes1 = builder.withJob(ProduceRes1.class)
            .withResources(new StringResource("Res1"));

        JobBuilder produceRes2 = builder.withJob(ProduceRes2.class)
            .withResources(new StringResource("Res2"));

        JobBuilder consumeRes123 = builder.withJob(ConsumeRes123.class)
            .withId(CONSUME_RES1_ID)
            .withUpstreams(UpstreamType.NO_RESOURCES, produceRes2);

        return builder.build();
    }

    public static Pipeline directResourceSequencePipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder produceRes1 = builder.withJob(ProduceRes1.class)
            .withResources(new StringResource("Res1"));

        JobBuilder produceRes2 = builder.withJob(ProduceRes2.class)
            .withResources(new StringResource("Res2"))
            .withUpstreams(produceRes1);

        JobBuilder consumeRes123 = builder.withJob(ConsumeRes123.class)
            .withId(CONSUME_RES1_ID)
            .withUpstreams(UpstreamType.DIRECT_RESOURCES, produceRes2);

        return builder.build();
    }

    public static Pipeline directResourceParallelPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder produceRes1 = builder.withJob(ProduceRes1.class)
            .withResources(new StringResource("Res1"));

        JobBuilder produceRes2 = builder.withJob(ProduceRes2.class)
            .withUpstreams(produceRes1)
            .withResources(new StringResource("Res2"));

        JobBuilder produceRes3 = builder.withJob(ProduceRes3.class)
            .withUpstreams(produceRes1)
            .withResources(new StringResource("Res3"));

        JobBuilder consumeRes123 = builder.withJob(ConsumeRes123.class)
            .withId(CONSUME_RES1_ID)
            .withUpstreams(UpstreamType.NO_RESOURCES, produceRes2)
            .withUpstreams(UpstreamType.DIRECT_RESOURCES, produceRes3);

        return builder.build();
    }

    public static Pipeline directResourceSequenceWithDownstreamPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder produceRes1 = builder.withJob(ProduceRes1.class)
            .withResources(new StringResource("Res1"));
        JobBuilder produceRes2 = builder.withJob(ProduceRes2.class)
            .withResources(new StringResource("Res2"))
            .withUpstreams(UpstreamType.DIRECT_RESOURCES, produceRes1);
        JobBuilder consumeRes123 = builder.withJob(ConsumeRes123.class)
            .withId(CONSUME_RES1_ID)
            .withUpstreams(produceRes2);
        return builder.build();
    }

    public static Pipeline allResourcePipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder produceRes1 = builder.withJob(ProduceRes1.class)
            .withResources(new StringResource("Res1"));
        JobBuilder produceRes2 = builder.withJob(ProduceRes2.class)
            .withResources(new StringResource("Res2"))
            .withUpstreams(produceRes1);
        JobBuilder consumeRes123 = builder.withJob(ConsumeRes123.class)
            .withId(CONSUME_RES1_ID)
            .withUpstreams(UpstreamType.ALL_RESOURCES, produceRes2);
        return builder.build();
    }


    @Produces(single = Res1.class)
    public static class ProduceRes1 implements JobExecutor {
        @WiredResource
        private StringResource message;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("ad2dcc27-7eb1-46b7-9b04-4c1064f82f63");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new Res1(message.getString()));
        }
    }

    @Produces(single = Res2.class)
    public static class ProduceRes2 implements JobExecutor {
        @WiredResource
        private StringResource message;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("199ef7b3-ba3b-4248-b3c6-ed7a42a161ee");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new Res2(message.getString()));
        }
    }

    @Produces(single = Res3.class)
    public static class ProduceRes3 implements JobExecutor {
        @WiredResource
        private StringResource message;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("1830da61-01d2-4f44-bf9b-a61420eaba13");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new Res3(message.getString()));
        }
    }

    public static class ConsumeRes123 implements JobExecutor {
        @WiredResource(optional = true)
        private Res1 resource1;

        @WiredResource(optional = true)
        private Res2 resource2;

        @WiredResource(optional = true)
        private Res3 resource3;

        private static final Logger log = LogManager.getLogger();

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("66d5eb7d-40fc-4a02-a165-e4acdad71488");
        }

        @Override
        public void execute(JobContext context) throws Exception {
        }
    }

    public static class StringResource implements Resource {
        private final String string;

        @PersistenceConstructor
        @JsonCreator
        public StringResource(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("f0910edd-e871-4ab8-a63f-d2b3addc88a4");
        }
    }
}
