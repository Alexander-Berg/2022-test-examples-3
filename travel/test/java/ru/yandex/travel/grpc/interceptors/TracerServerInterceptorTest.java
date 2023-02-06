package ru.yandex.travel.grpc.interceptors;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.propagation.Format;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.test.fake.proto.FakeServiceGrpc;
import ru.yandex.travel.test.fake.proto.TTestMethodReq;
import ru.yandex.travel.test.fake.proto.TTestMethodRsp;
import ru.yandex.travel.tracing.GrpcMetadataInjectAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TracerServerInterceptorTest {
    private static final class FakeService extends FakeServiceGrpc.FakeServiceImplBase {
        @Override
        public void testMethod(TTestMethodReq request, StreamObserver<TTestMethodRsp> responseObserver) {
            responseObserver.onNext(TTestMethodRsp.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    private FakeService fakeService;

    private MockTracer tracer;

    private TracerServerInterceptor tracerServerInterceptor;

    private LoggingServerInterceptor loggingServerInterceptor;

    @Rule
    public GrpcCleanupRule cleanupRule = new GrpcCleanupRule();

    @Before
    public void setUp() throws UnknownHostException {
        tracer = new MockTracer();
        tracerServerInterceptor = new TracerServerInterceptor(tracer);
        loggingServerInterceptor = new LoggingServerInterceptor(InetAddress.getLocalHost().getCanonicalHostName());
        fakeService = new FakeService();
        fakeService = spy(fakeService);
    }

    @Test
    public void testServerInterceptorCorrectlyExtractSpanContext() {
        FakeServiceGrpc.FakeServiceBlockingStub fakeServiceClient =
                createServerAndBlockingStub(new TracingInjectClientInterceptor(tracer));
        MockSpan mockedSpan = tracer.buildSpan("testSpan").start();
        try (Scope scope = tracer.scopeManager().activate(mockedSpan)) {
            fakeServiceClient.testMethod(TTestMethodReq.newBuilder().build());
            assertThat(tracer.finishedSpans().size()).isEqualTo(1);
            assertThat(tracer.finishedSpans().get(0).parentId()).isEqualTo(mockedSpan.context().spanId());
        } finally {
            mockedSpan.finish();
        }
        assertThat(tracer.finishedSpans().size() == 2);
        verify(fakeService, Mockito.times(1)).testMethod(any(), any());
    }

    private FakeServiceGrpc.FakeServiceBlockingStub createServerAndBlockingStub(ClientInterceptor headerInterceptor) {
        try {

            String serverName = InProcessServerBuilder.generateName();

            cleanupRule.register(
                    InProcessServerBuilder.forName(serverName)
                            .directExecutor()
                            .addService(
                                    ServerInterceptors.intercept(fakeService,
                                            List.of(loggingServerInterceptor, tracerServerInterceptor))
                            )
                            .build()
                            .start()
            );

            InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(serverName);
            if (headerInterceptor != null) {
                channelBuilder = channelBuilder.intercept(headerInterceptor);
            }

            return FakeServiceGrpc.newBlockingStub(
                    cleanupRule.register(
                            channelBuilder.build()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class TracingInjectClientInterceptor implements ClientInterceptor {
        private final Tracer tracer;

        TracingInjectClientInterceptor(Tracer tracer) {
            this.tracer = tracer;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                                   CallOptions callOptions, Channel next) {
            return new MetadataKeySettingCall<>(next.newCall(method, callOptions));
        }

        private class MetadataKeySettingCall<ReqT, RespT> extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

            MetadataKeySettingCall(ClientCall<ReqT, RespT> delegate) {
                super(delegate);
            }

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                tracer.inject(tracer.activeSpan().context(), Format.Builtin.HTTP_HEADERS,
                        new GrpcMetadataInjectAdapter(headers));
                super.start(responseListener, headers);
            }
        }
    }

}
