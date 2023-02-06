package ru.yandex.chemodan.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.chemodan.grpc.server.GrpcServerConfiguration;
import ru.yandex.chemodan.util.test.AbstractTest;

@ContextConfiguration(classes = {
        GrpcServerConfiguration.class,
})
public abstract class AbstractGrpcBaseTest extends AbstractTest {

    @Value("${grpc.port:-26118}")
    private int port;

    protected ManagedChannel createManagedChannel() {
        return ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
    }
}
