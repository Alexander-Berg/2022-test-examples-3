package ru.yandex.travel.grpc.interceptors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import lombok.RequiredArgsConstructor;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.travel.test.fake.proto.FakeServiceGrpc;
import ru.yandex.travel.test.fake.proto.TTestMethodReq;
import ru.yandex.travel.test.fake.proto.TTestMethodRsp;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class CheckServerInterceptorCallSequence {

    private static final Context.Key<List<String>> CALLED_INTERCEPTORS = Context.key("CALLED_INTERCEPTORS");

    @RequiredArgsConstructor
    private static final class TrackingServerInterceptor implements ServerInterceptor {

        private final String name;

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                     ServerCallHandler<ReqT, RespT> next) {
            if (CALLED_INTERCEPTORS.get() == null) {
                List<String> l = new ArrayList<>();
                l.add(name);
                return Contexts.interceptCall(Context.current().withValue(CALLED_INTERCEPTORS, l), call, headers, next);
            } else {
                CALLED_INTERCEPTORS.get().add(name);
                return next.startCall(call, headers);
            }
        }

        public String getName() {
            return name;
        }
    }

    @Rule
    public GrpcCleanupRule cleanupRule = new GrpcCleanupRule();

    // A simple test demonstrating, that grpc interceptors are invoked in the revers order
    @Test
    public void testServerInterceptorsCallSequence() {
        TrackingServerInterceptor first = new TrackingServerInterceptor("first");
        TrackingServerInterceptor second = new TrackingServerInterceptor("second");
        TrackingServerInterceptor third = new TrackingServerInterceptor("third");


        FakeServiceGrpc.FakeServiceBlockingStub fakeServiceClient = createServerAndBlockingStub(interceptors -> {
            assertThat(interceptors).containsSequence("third", "second", "first");
        }, List.of(first, second, third));
        fakeServiceClient.testMethod(TTestMethodReq.newBuilder().build());
    }

    private FakeServiceGrpc.FakeServiceBlockingStub createServerAndBlockingStub(Consumer<List<String>> checker,
                                                                                List<ServerInterceptor> interceptors) {
        try {

            String serverName = InProcessServerBuilder.generateName();


            cleanupRule.register(
                    InProcessServerBuilder.forName(serverName)
                            .directExecutor()
                            .addService(
                                    ServerInterceptors.intercept(
                                            new FakeServiceGrpc.FakeServiceImplBase() {
                                                @Override
                                                public void testMethod(TTestMethodReq request,
                                                                       StreamObserver<TTestMethodRsp> responseObserver) {
                                                    checker.accept(CALLED_INTERCEPTORS.get());
                                                    responseObserver.onNext(TTestMethodRsp.newBuilder().build());
                                                    responseObserver.onCompleted();
                                                }
                                            }, interceptors
                                    )
                            ).build()
                            .start()
            );

            InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(serverName);

            return FakeServiceGrpc.newBlockingStub(
                    cleanupRule.register(
                            channelBuilder.build()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
