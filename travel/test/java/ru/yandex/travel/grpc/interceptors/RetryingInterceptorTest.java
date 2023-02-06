package ru.yandex.travel.grpc.interceptors;

import java.util.concurrent.atomic.AtomicInteger;

import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.travel.test.fake.proto.FakeServiceGrpc;
import ru.yandex.travel.test.fake.proto.TTestMethodReq;
import ru.yandex.travel.test.fake.proto.TTestMethodRsp;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class RetryingInterceptorTest {
    @Rule
    public GrpcCleanupRule cleanupRule = new GrpcCleanupRule();

    private InlineFakeService fakeService;

    @Before
    public void setUp() throws ClassNotFoundException {
        fakeService = new InlineFakeService();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCorrectHeaders() {
        FakeServiceGrpc.FakeServiceBlockingStub fakeServiceBlockingStub =
                createServerAndBlockingStub()
                        .withInterceptors(new UserCredentialsClientInterceptor());
        fakeServiceBlockingStub.testMethod(TTestMethodReq.newBuilder().build());
        assertThat(fakeService.callCount.get()).isEqualTo(4);
    }

    @SuppressWarnings("Unchecked")
    private FakeServiceGrpc.FakeServiceBlockingStub createServerAndBlockingStub(ServerInterceptor... captors) {
        try {

            String serverName = InProcessServerBuilder.generateName();


            cleanupRule.register(
                    InProcessServerBuilder.forName(serverName)
                            .directExecutor()
                            .addService(ServerInterceptors.intercept(fakeService, captors))
                            .build()
                            .start()
            );

            RetryingClientInterceptor retryingClientInterceptor = RetryingClientInterceptor.builder().maxAttempts(4)
                    .build();
            return FakeServiceGrpc.newBlockingStub(
                    cleanupRule.register(
                            InProcessChannelBuilder
                                    .forName(serverName)
                                    .intercept(retryingClientInterceptor)
                                    .build()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class InlineFakeService extends FakeServiceGrpc.FakeServiceImplBase {

        public final AtomicInteger callCount = new AtomicInteger(0);

        @Override
        public void testMethod(TTestMethodReq request, StreamObserver<TTestMethodRsp> responseObserver) {

            int currentCall = callCount.getAndIncrement();

            if (currentCall < 3) {
                responseObserver.onError(Status.UNAVAILABLE.withDescription("Call count: " + currentCall).asException());

            } else {
                responseObserver.onNext(TTestMethodRsp.newBuilder().build());
                responseObserver.onCompleted();
            }
        }
    }
}
