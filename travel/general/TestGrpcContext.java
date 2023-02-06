package ru.yandex.travel.orders.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import lombok.Value;
import org.springframework.aop.support.AopUtils;

import ru.yandex.travel.credentials.UserCredentialsAuthValidatorStubImpl;
import ru.yandex.travel.credentials.UserCredentialsBuilder;
import ru.yandex.travel.credentials.UserCredentialsPassportExtractorStubImpl;
import ru.yandex.travel.credentials.UserCredentialsValidator;
import ru.yandex.travel.grpc.GrpcService;
import ru.yandex.travel.grpc.interceptors.RetryingClientInterceptor;
import ru.yandex.travel.grpc.interceptors.UserCredentialsClientInterceptor;
import ru.yandex.travel.grpc.interceptors.UserCredentialsServerInterceptor;

@Value
public class TestGrpcContext {
    private final GrpcCleanupRule cleanupRule;
    private final String serverName;
    private final Server server;

    public static TestGrpcContext createTestServer(GrpcCleanupRule cleanupRule, BindableService... services) {
        String serverName = InProcessServerBuilder.generateName();

        InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(serverName)
                .directExecutor();
        for (BindableService service : services) {
            serverBuilder.addService(ServerInterceptors.intercept(
                    service, buildServiceInterceptors(service)));
        }
        Server server = cleanupRule.register(serverBuilder.build());
        try {
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new TestGrpcContext(cleanupRule, serverName, server);
    }

    // inspired by DefaultGrpcServiceInterceptorConfigurer.getInterceptors
    private static List<ServerInterceptor> buildServiceInterceptors(BindableService service) {
        List<ServerInterceptor> interceptors = new ArrayList<>();

        Class<?> originalClass = AopUtils.getTargetClass(service);
        GrpcService annotation = originalClass.getAnnotation(GrpcService.class);
        Preconditions.checkNotNull(annotation);
        if (annotation.authenticateUser()) {
            interceptors.add(new UserCredentialsServerInterceptor(
                    new UserCredentialsBuilder(),
                    new UserCredentialsPassportExtractorStubImpl(),
                    new UserCredentialsValidator(new UserCredentialsAuthValidatorStubImpl())
            ));
        }

        return interceptors;
    }

    public ManagedChannel createChannel() {
        RetryingClientInterceptor retryingClientInterceptor =
                RetryingClientInterceptor.builder().maxAttempts(4).build();

        return cleanupRule.register(
                InProcessChannelBuilder.forName(serverName)
                        .directExecutor()
                        .intercept(
                                new UserCredentialsClientInterceptor(),
                                retryingClientInterceptor
                        )
                        .build()
        );
    }
}
