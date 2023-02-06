package ru.yandex.market.marketId.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import ru.yandex.market.marketId.FunctionalTest;
import ru.yandex.market.marketId.config.MarketIdConfigurationTest;

public class GrpcFunctionalTest extends FunctionalTest {
    protected static ManagedChannel channel;

    @BeforeAll
    static void initChannel() {
        channel = ManagedChannelBuilder.forAddress("localhost",
                MarketIdConfigurationTest.TESTING_PORT)
                .usePlaintext()
                .build();
    }

    @AfterAll
    static void shutdown() {
        channel.shutdown();
    }
}
