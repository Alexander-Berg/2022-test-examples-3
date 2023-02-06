package ru.yandex.travel.api.config.common;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import io.grpc.ManagedChannelBuilder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.travel.api.services.hotels.searcher.SearcherClient;
import ru.yandex.travel.grpc.interceptors.DefaultTimeoutClientInterceptor;
import ru.yandex.travel.grpc.interceptors.LoggingClientInterceptor;
import ru.yandex.travel.hotels.proto.OfferSearchServiceV1Grpc;
import ru.yandex.travel.hotels.proto.TPingRpcReq;
import ru.yandex.travel.orders.client.ChannelState;
import ru.yandex.travel.orders.client.ChannelSupplier;
import ru.yandex.travel.orders.client.GrpcChannelSupplierFactory;
import ru.yandex.travel.orders.client.HAGrpcChannelFactory;
import ru.yandex.travel.orders.client.LabeledChannel;

import static ru.yandex.travel.commons.concurrent.FutureUtils.buildCompletableFuture;

@Configuration
@ConditionalOnProperty("test-context.enabled")
@EnableConfigurationProperties(TestContextConfigurationProperties.class)
@RequiredArgsConstructor
@Slf4j
public class TestContextConfiguration {
    private final TestContextConfigurationProperties properties;

    @Bean("SearcherSupplier")
    @ConditionalOnMissingBean(name = "SearcherSupplier")
    public ChannelSupplier getChannelSupplier() {
        return new GrpcChannelSupplierFactory(properties.getSearcher()).getChannelSupplier();
    }

    @Bean("SearcherGrpcChannelFactory")
    public HAGrpcChannelFactory haGrpcChannelFactory(@Qualifier("SearcherSupplier") ChannelSupplier channelSupplier) {

        return HAGrpcChannelFactory.Builder.newBuilder().withPingProducer(
                channel -> buildCompletableFuture(OfferSearchServiceV1Grpc
                        .newFutureStub(channel)
                        .ping(TPingRpcReq.newBuilder().build())
                ).thenApply((rsp) -> rsp.getIsReady() ? ChannelState.READY : ChannelState.NOT_READY))
                .withFailureDetectorProperties(properties.getSearcher().getFailureDetection())
                .withChannelSupplier(channelSupplier)
                .withChannelBuilder(this::createChannel)
                .build();
    }

    @Bean
    public SearcherClient searcherClient(@Qualifier("SearcherGrpcChannelFactory") HAGrpcChannelFactory factory) {
        return new SearcherClient(factory);
    }

    @SneakyThrows
    private LabeledChannel createChannel(String target) {
        var clientFqdn = Objects.requireNonNull(InetAddress.getLocalHost().getCanonicalHostName());
        var loggingClientInterceptor = new LoggingClientInterceptor(clientFqdn, target,
                Set.of(OfferSearchServiceV1Grpc.getPingMethod().getFullMethodName()));
        var defaultTimeoutClientInterceptor =
                new DefaultTimeoutClientInterceptor(properties.getSearcher().getTimeout());
        return new LabeledChannel(target,
                ManagedChannelBuilder
                        .forTarget(target)
                        .intercept(Arrays.asList(loggingClientInterceptor, defaultTimeoutClientInterceptor))
                        .usePlaintext()
                        .maxInboundMessageSize(properties.getSearcher().getMaxMessageSize())
                        .build());
    }
}
