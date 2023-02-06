package ru.yandex.market.antifraud.orders.test;

import java.util.Collections;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.stream.StreamConsumerConfig;
import ru.yandex.kikimr.persqueue.proxy.ProxyBalancer;
import ru.yandex.market.antifraud.orders.logbroker.JsonDeserializer;
import ru.yandex.market.antifraud.orders.logbroker.entities.CancelOrderRequest;
import ru.yandex.market.volva.logbroker.LbStreamHolder;
import ru.yandex.passport.tvmauth.NativeTvmClient;
import ru.yandex.passport.tvmauth.TvmApiSettings;
import ru.yandex.passport.tvmauth.TvmClient;

/**
 * @author dzvyagin
 */
public class LogbrokerSmokeTest {

    @Test
    @Ignore
    public void readCancelledOrders() throws Exception {
        TvmClient tvm = tvm();
        LogbrokerClientFactory lbFactory = lbFactory();
        StreamConsumerConfig config = config(tvm);
        StreamConsumer consumer = lbFactory.streamConsumer(config);
        var streamHolder = new LbStreamHolder<>("test",
                JsonDeserializer.forClass(CancelOrderRequest.class),
                this::consume,
                () -> consumer);
        streamHolder.start();
        Thread.sleep(30_000);
    }

    private void consume(CancelOrderRequest request){
        System.out.println(request);
        throw new RuntimeException();
    }

    private LogbrokerClientFactory lbFactory() {
        ProxyBalancer balancer = new ProxyBalancer("lbkx.logbroker.yandex.net", 2135);
        return new LogbrokerClientFactory(balancer);
    }

    private TvmClient tvm() {
        TvmApiSettings settings = TvmApiSettings.create();
        settings.setSelfTvmId(2017129);
        settings.enableServiceTicketsFetchOptions(
                "secret",
                new int[]{2001059}
        );
        return new NativeTvmClient(settings);
    }

    private StreamConsumerConfig config(TvmClient tvmClient) {
        return StreamConsumerConfig.builder(Collections.singleton("xurma/market-orders-verdicts-log"), "marketstat/test/market-orders-verdicts-log")
                .setCredentialsProvider(() -> Credentials.tvm(tvmClient.getServiceTicketFor(2001059)))
                .configureReader(builder -> builder.setMaxCount(109).setMaxSize(1024 * 1024 * 4)/*
                .setPartitionsAtOnce(1)*/)
                .configureSession(builder -> builder
                        .setClientSideLocksAllowed(true)
                        .setReadOnlyLocal(true)
                )
                .build();
    }
}
