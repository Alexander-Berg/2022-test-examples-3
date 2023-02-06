package ru.yandex.travel.grpc.interceptors;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Deadline;
import io.grpc.MethodDescriptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import lombok.Getter;
import org.junit.Test;

import ru.yandex.travel.test.fake.proto.FakeServiceGrpc;
import ru.yandex.travel.test.fake.proto.TTestMethodReq;
import ru.yandex.travel.test.fake.proto.TTestMethodRsp;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class DefaultTimeoutClientInterceptorTest {

    private static final class FakeService extends FakeServiceGrpc.FakeServiceImplBase {
        @Override
        public void testMethod(TTestMethodReq request, StreamObserver<TTestMethodRsp> responseObserver) {
            responseObserver.onNext(TTestMethodRsp.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Test
    public void testNoDeadlinePresentWhenNoDeadlineSet() {
        DeadlineCaptor deadlineCaptor = new DeadlineCaptor();
        FakeServiceGrpc.FakeServiceBlockingStub stub =
                createServerAndBlockingStub(deadlineCaptor);
        stub.testMethod(TTestMethodReq.newBuilder().build());
        assertThat(deadlineCaptor.getDeadline()).isNull();
    }

    @Test
    public void testDefaultTimeoutSetWhenNoDeadlinePresent() {
        DeadlineCaptor deadlineCaptor = new DeadlineCaptor();
        DefaultTimeoutClientInterceptor defaultTimeoutClientInterceptor = new DefaultTimeoutClientInterceptor(Duration.ofMillis(1000));
        FakeServiceGrpc.FakeServiceBlockingStub stub =
                createServerAndBlockingStub(deadlineCaptor, defaultTimeoutClientInterceptor);
        stub.testMethod(TTestMethodReq.newBuilder().build());
        assertThat(deadlineCaptor.getDeadline()).isNotNull();
    }

    @Test
    public void testDeadlineNotChangedWhenPresent() {
        DeadlineCaptor deadlineCaptor = new DeadlineCaptor();
        DefaultTimeoutClientInterceptor defaultTimeoutClientInterceptor = new DefaultTimeoutClientInterceptor(Duration.ofMillis(1000));
        FakeServiceGrpc.FakeServiceBlockingStub stub =
                createServerAndBlockingStub(deadlineCaptor, defaultTimeoutClientInterceptor);
        Deadline deadline = Deadline.after(300000, TimeUnit.MILLISECONDS);
        stub.withDeadline(deadline).testMethod(TTestMethodReq.newBuilder().build());
        assertThat(deadlineCaptor.getDeadline()).isNotNull();
        assertThat(deadlineCaptor.getDeadline()).isEqualTo(deadline);
    }

    private final FakeService fakeService = new FakeService();

    public GrpcCleanupRule cleanupRule = new GrpcCleanupRule();

    private FakeServiceGrpc.FakeServiceBlockingStub createServerAndBlockingStub(ClientInterceptor... clientInterceptors) {
        try {

            String serverName = InProcessServerBuilder.generateName();


            cleanupRule.register(
                    InProcessServerBuilder.forName(serverName)
                            .directExecutor()
                            .addService(ServerInterceptors.intercept(fakeService))
                            .build()
                            .start()
            );

            return FakeServiceGrpc.newBlockingStub(
                    cleanupRule.register(
                            InProcessChannelBuilder.forName(serverName).intercept(
                                    clientInterceptors
                            ).build()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class DeadlineCaptor implements ClientInterceptor {

        @Getter
        private Deadline deadline;

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
            deadline = callOptions.getDeadline();
            return next.newCall(method, callOptions);
        }
    }
}
