package ru.yandex.direct.logicprocessor.processors.moderation.writers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.IntStreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.log.service.ModerationLogService;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.text.TextBannerModerationRequest;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.ess.common.logbroker.LogbrokerClientFactoryFacade;
import ru.yandex.direct.ess.common.logbroker.LogbrokerProducerProperties;
import ru.yandex.direct.ess.common.logbroker.LogbrokerProducerPropertiesImpl;
import ru.yandex.direct.logicprocessor.processors.moderation.BannerModerationRequestLogEntryCreator;
import ru.yandex.direct.logicprocessor.processors.moderation.CampaignIdPartitionGroupComputer;
import ru.yandex.direct.logicprocessor.processors.moderation.VoidModerationRequestFilter;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
class ModerationRequestsWriterImplTest {

    @Mock(stubOnly = true)
    private ModerationLogService moderationLogService;

    @Mock
    private LogbrokerClientFactoryFacade logbrokerClientFactory;

    private TestModerationRequestWriter writer;
    private List<TestLogbrokerAsyncProducer> asyncProducers;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void createWriter(List<LogbrokerProducerProperties> producersProperties) {
        asyncProducers = new ArrayList<>();

        when(logbrokerClientFactory.createProducerSupplier(any(), any()))
                .thenReturn(() -> {
                    var asyncProducer = new TestLogbrokerAsyncProducer();
                    asyncProducers.add(asyncProducer);
                    return CompletableFuture.completedFuture(asyncProducer);
                });

        writer = new TestModerationRequestWriter(logbrokerClientFactory, producersProperties);
        writer.initialize("processor",1);
    }

    @AfterEach
    public void finishWriter() {
        writer.finish();
    }

    @Test
    void write_TwoReqs_OnePartition() {
        createWriter(List.of(
                createLogbrokerProducerProperties(1)
        ));

        var result = writer.writeRequests(List.of(createRequest(1), createRequest(2)));

        assertThat(result).isEqualTo(2);
        assertThat(asyncProducers).hasSize(1);

        assertThat(asyncProducers.get(0).writeCallsCount).isEqualTo(1);
        assertThat(asyncProducers.get(0).writtenData).hasSize(2);
    }

    @Test
    void write_TwoReqs_TwoPartition() {
        createWriter(List.of(
                createLogbrokerProducerProperties(1), createLogbrokerProducerProperties(2)
        ));

        var result = writer.writeRequests(List.of(createRequest(1), createRequest(2)));

        assertThat(result).isEqualTo(2);
        assertThat(asyncProducers).hasSize(2);

        var firstProducer = asyncProducers.get(0);
        assertThat(firstProducer.writeCallsCount).isEqualTo(1);
        assertThat(firstProducer.writtenData).hasSize(1);

        var secondProducer = asyncProducers.get(1);
        assertThat(secondProducer.writeCallsCount).isEqualTo(1);
        assertThat(secondProducer.writtenData).hasSize(1);
    }

    @Test
    void write_TwoReqs_ThreePartition() {
        createWriter(List.of(
                createLogbrokerProducerProperties(1),
                createLogbrokerProducerProperties(2),
                createLogbrokerProducerProperties(3)
        ));

        var result = writer.writeRequests(List.of(createRequest(1), createRequest(2)));

        assertThat(result).isEqualTo(2);
        assertThat(asyncProducers).hasSize(2);

        var firstProducer = asyncProducers.get(0);
        assertThat(firstProducer.writeCallsCount).isEqualTo(1);
        assertThat(firstProducer.writtenData).hasSize(1);

        var secondProducer = asyncProducers.get(1);
        assertThat(secondProducer.writeCallsCount).isEqualTo(1);
        assertThat(secondProducer.writtenData).hasSize(1);
    }

    @Test
    void write_TenReqs_ThreePartition() {
        createWriter(List.of(
                createLogbrokerProducerProperties(1),
                createLogbrokerProducerProperties(2),
                createLogbrokerProducerProperties(3)
        ));

        var result = writer.writeRequests(
                IntStreamEx.range(0, 10)
                        .boxed()
                        .map(i -> createRequest(i + 1))
                        .toList()
        );

        assertThat(result).isEqualTo(10);
        assertThat(asyncProducers).hasSize(3);

        var firstProducer = asyncProducers.get(0);
        assertThat(firstProducer.writeCallsCount).isEqualTo(1);
        assertThat(firstProducer.writtenData).hasSize(3);

        var secondProducer = asyncProducers.get(1);
        assertThat(secondProducer.writeCallsCount).isEqualTo(1);
        assertThat(secondProducer.writtenData).hasSize(4);

        var thirdProducer = asyncProducers.get(2);
        assertThat(thirdProducer.writeCallsCount).isEqualTo(1);
        assertThat(thirdProducer.writtenData).hasSize(3);
    }

    private LogbrokerProducerProperties createLogbrokerProducerProperties(Integer group) {
        return LogbrokerProducerPropertiesImpl.newBuilder()
                .setHost("foo")
                .setWriteTopic("bar")
                .setTimeoutSec(42L)
                .setRetries(0)
                .setGroup(group)
                .build();
    }

    private TextBannerModerationRequest createRequest(long campaignId) {
        var meta = new BannerModerationMeta();
        meta.setCampaignId(campaignId);
        var request = new TextBannerModerationRequest();
        request.setMeta(meta);
        return request;
    }

    public class TestLogbrokerAsyncProducer implements AsyncProducer {
        private long writeCallsCount = 0;
        private List<String> writtenData = new ArrayList<>();

        @Override
        public CompletableFuture<ProducerInitResponse> init() {
            return CompletableFuture.completedFuture(
                    new ProducerInitResponse(writeCallsCount, "test-topic", 100500, "session-id"));
        }

        @Override
        public CompletableFuture<ProducerWriteResponse> write(byte[] data, long seqNo, long timestamp) {
            writeCallsCount += 1;
            writtenData.addAll(Arrays.asList(new String(data, UTF_8).split("\n")));

            return CompletableFuture.completedFuture(new ProducerWriteResponse(seqNo, 42L, false));
        }

        @Override
        public CompletableFuture<Void> closeFuture() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void close() {

        }
    }

    private class TestModerationRequestWriter
            extends ModerationRequestsWriterImpl<TextBannerModerationRequest> {

        protected TestModerationRequestWriter(LogbrokerClientFactoryFacade logbrokerClientFactory,
                                              List<LogbrokerProducerProperties> producersProperties) {
            super(EnvironmentType.DEVELOPMENT,
                    new VoidModerationRequestFilter(),
                    logbrokerClientFactory,
                    new BannerModerationRequestLogEntryCreator(),
                    moderationLogService,
                    producersProperties,
                    new CampaignIdPartitionGroupComputer(),
                    null,
                    null);
        }
    }

}
