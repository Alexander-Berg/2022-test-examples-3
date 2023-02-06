package manual.ru.yandex.market.psku.postprocessor.bazinga.deduplication;

import io.grpc.netty.NettyChannelBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.grpc.trace.TraceClientInterceptor;
import ru.yandex.market.markup3.api.Markup3Api;
import ru.yandex.market.markup3.api.Markup3ApiTaskServiceGrpc;
import ru.yandex.market.markup3.api.Markup3ApiTaskServiceGrpc.Markup3ApiTaskServiceBlockingStub;
import ru.yandex.market.request.trace.Module;

@Ignore
public class TolokaReceivingTest {

    private Markup3ApiTaskServiceBlockingStub markup3ApiTaskServiceBlockingStub;

    @Before
    public void setUp() throws Exception {
        markup3ApiTaskServiceBlockingStub = Markup3ApiTaskServiceGrpc.newBlockingStub(
                NettyChannelBuilder.forAddress("markup3.vs.market.yandex.net", 8080)
                        .usePlaintext()
                        .maxInboundMessageSize(100 * 1024 * 1024)
                        .userAgent("psku-post-processor")
                        .intercept(new TraceClientInterceptor(Module.MBO_MARKUP))
                        .build()
        );

    }

    @Test
    public void test() {
        Markup3Api.TasksResultPollResponse results = markup3ApiTaskServiceBlockingStub.pollResults(
                Markup3Api.TasksResultPollRequest.newBuilder()
                        .setTaskTypeIdentity(
                                Markup3Api.TaskTypeIdentity.newBuilder()
                                        .setGroupKey("deduplication_toloka_mapping_moderation")
                                        .setType(Markup3Api.TaskType.TOLOKA_MAPPING_MODERATION)
                                        .build()
                        )
                        .setCount(10)
                        .build()
        );

        System.out.println(results);
    }
}
