package ru.yandex.travel.grpc.interceptors;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.grpc.util.MutableHandlerRegistry;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.travel.commons.concurrent.FutureUtils;
import ru.yandex.travel.credentials.UserCredentials;
import ru.yandex.travel.credentials.UserCredentialsAuthValidatorStubImpl;
import ru.yandex.travel.credentials.UserCredentialsBuilder;
import ru.yandex.travel.credentials.UserCredentialsPassportExtractorStubImpl;
import ru.yandex.travel.credentials.UserCredentialsValidator;
import ru.yandex.travel.test.fake.proto.FakeServiceGrpc;
import ru.yandex.travel.test.fake.proto.TTestMethodReq;
import ru.yandex.travel.test.fake.proto.TTestMethodRsp;

public class UserCredentialsClientInterceptorContextPreserveTest {
    @Rule
    public GrpcCleanupRule cleanupRule = new GrpcCleanupRule();
    private MutableHandlerRegistry serviceRegistry;

    @Before
    public void setUp() {
        serviceRegistry = new MutableHandlerRegistry();
    }

    @Test
    public void testContextPreserved() throws Exception {
        UserCredentials uc = new UserCredentials("sess1", "foo", "42", "bar", "baz", "127.0.0.1", false, false);
        Context context = Context.current().withValue(UserCredentials.KEY, uc);
        // we collect all observed user credentials. adding null will lead to null pointer exception
        ConcurrentLinkedQueue<UserCredentials> queue = new ConcurrentLinkedQueue<>();
        FakeServiceGrpc.FakeServiceImplBase fakeService = new FakeServiceGrpc.FakeServiceImplBase() {
            @Override
            public void testMethod(TTestMethodReq request, StreamObserver<TTestMethodRsp> responseObserver) {
                queue.add(UserCredentials.get());
                responseObserver.onNext(TTestMethodRsp.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
        serviceRegistry.addService(fakeService);
        ManagedChannel channel = createServerAndChannel();

        FakeServiceGrpc.FakeServiceFutureStub futureStub = FakeServiceGrpc.newFutureStub(channel)
                .withInterceptors(new UserCredentialsClientInterceptor());

        CompletableFuture<TTestMethodRsp> result = context.call(() -> {
            CompletableFuture<TTestMethodRsp> firstResult =
                    FutureUtils.buildCompletableFuture(futureStub.testMethod(TTestMethodReq.newBuilder().build()));
            CompletableFuture<TTestMethodRsp> secondResult =
                    firstResult.thenCompose(ignored -> FutureUtils.buildCompletableFuture(futureStub.testMethod(TTestMethodReq.newBuilder().build())));
            return secondResult.thenCompose(ignored -> FutureUtils.buildCompletableFuture(futureStub.testMethod(TTestMethodReq.newBuilder().build())));
        });
        Assertions.assertThatCode(result::get).doesNotThrowAnyException();
        Assertions.assertThat(queue.size()).isEqualTo(3); // is equal to number of times we called the service

        // choosing unique user credentials
        Set<String> uniqueYandexUids = queue.stream().map(UserCredentials::getYandexUid).collect(Collectors.toSet());
        Assertions.assertThat(uniqueYandexUids).containsOnly(uc.getYandexUid());
    }

    private ManagedChannel createServerAndChannel() {
        try {
            String serverName = InProcessServerBuilder.generateName();
            cleanupRule.register(
                    InProcessServerBuilder.forName(serverName).fallbackHandlerRegistry(serviceRegistry)
                            .executor(Executors.newFixedThreadPool(3))
                            .intercept(new UserCredentialsServerInterceptor(
                                    new UserCredentialsBuilder(),
                                    new UserCredentialsPassportExtractorStubImpl(),
                                    new UserCredentialsValidator(new UserCredentialsAuthValidatorStubImpl())
                            ))
                            .build().start()
            );
            return cleanupRule.register(InProcessChannelBuilder.forName(serverName).executor(
                    Executors.newFixedThreadPool(3, new ThreadFactoryBuilder().setNameFormat("channel-%s").build())).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
