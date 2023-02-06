package ru.yandex.travel.grpc.interceptors;


import java.util.Collections;
import java.util.List;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import lombok.AllArgsConstructor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.travel.test.fake.proto.FakeServiceGrpc;
import ru.yandex.travel.test.fake.proto.TTestMethodReq;
import ru.yandex.travel.test.fake.proto.TTestMethodRsp;
import ru.yandex.travel.tvm.ServiceTicketCheck;
import ru.yandex.travel.tvm.ServiceTicketCheckStatus;
import ru.yandex.travel.tvm.TvmWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TvmHeaderServerInterceptorTest {

    private static final class FakeService extends FakeServiceGrpc.FakeServiceImplBase {
        @Override
        public void testMethod(TTestMethodReq request, StreamObserver<TTestMethodRsp> responseObserver) {
            responseObserver.onNext(TTestMethodRsp.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    private FakeService fakeService;

    private TvmHeaderServerInterceptor tvmHeaderServerInterceptor;

    private FakeServiceGrpc.FakeServiceBlockingStub fakeServiceClient;

    private TvmWrapper tvm2;

    private final Integer validSrcId = 1000;

    private final List<String> validSrcAliases = Collections.singletonList(validSrcId + "");

    @Rule
    public GrpcCleanupRule cleanupRule = new GrpcCleanupRule();

    @Before
    public void setUp() {
        tvm2 = mock(TvmWrapper.class);
        tvmHeaderServerInterceptor = new TvmHeaderServerInterceptor(tvm2, validSrcAliases);
        fakeService = new FakeService();
        fakeService = spy(fakeService);
    }

    @Test
    public void testErrorWhenNoServiceMetadataProvided() {
        fakeServiceClient = createServerAndBlockingStub(null);
        StatusRuntimeException exception = catchThrowableOfType(
                () -> fakeServiceClient.testMethod(TTestMethodReq.newBuilder().build()),
                StatusRuntimeException.class
        );

        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        verify(fakeService, never()).testMethod(any(), any());
    }

    @Test
    public void testErrorWhenNotValidServiceTicketProvided() {
        when(tvm2.checkServiceTicket(any(), any())).thenReturn(ServiceTicketCheck.invalid(ServiceTicketCheckStatus.UNEXPECTED_ERROR, null));
        String tvmTicket = "ignored_in_test";
        fakeServiceClient = createServerAndBlockingStub(
                new MetadataKeySettingClientInterceptor(TvmHeaders.METADATA_SERVICE_TICKET_HEADER, tvmTicket)
        );
        StatusRuntimeException exception = catchThrowableOfType(
                () -> fakeServiceClient.testMethod(TTestMethodReq.newBuilder().build()),
                StatusRuntimeException.class
        );
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        verify(tvm2, times(1)).checkServiceTicket(eq(tvmTicket), any());
        verify(fakeService, never()).testMethod(any(), any());
    }

    private FakeServiceGrpc.FakeServiceBlockingStub createServerAndBlockingStub(ClientInterceptor headerInterceptor) {
        try {

            String serverName = InProcessServerBuilder.generateName();

            cleanupRule.register(
                    InProcessServerBuilder.forName(serverName)
                            .directExecutor()
                            .addService(
                                    ServerInterceptors.intercept(fakeService, tvmHeaderServerInterceptor)
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

    @AllArgsConstructor
    private static final class MetadataKeySettingClientInterceptor implements ClientInterceptor {
        private final Metadata.Key<String> key;
        private final String value;

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
            return new MetadataKeySettingCall<>(next.newCall(method, callOptions));
        }

        private class MetadataKeySettingCall<ReqT, RespT> extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

            MetadataKeySettingCall(ClientCall<ReqT, RespT> delegate) {
                super(delegate);
            }

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(key, value);
                super.start(responseListener, headers);
            }
        }
    }


}
