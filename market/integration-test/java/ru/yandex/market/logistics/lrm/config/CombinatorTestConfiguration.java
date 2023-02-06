package ru.yandex.market.logistics.lrm.config;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import yandex.market.combinator.v0.CombinatorGrpc;

@Configuration
public class CombinatorTestConfiguration {

    @MockBean
    private CombinatorGrpc.CombinatorImplBase combinatorImplBase;

    @Bean
    public String combinatorGrpcServerName() {
        return InProcessServerBuilder.generateName();
    }

    @Bean
    public ManagedChannel combinatorGrpcManagedChannel(String combinatorGrpcServerName) {
        return InProcessChannelBuilder.forName(combinatorGrpcServerName).directExecutor().build();
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server combinatorGrpcServer(String combinatorGrpcServerName) {
        return InProcessServerBuilder.forName(combinatorGrpcServerName)
            .directExecutor()
            .addService(combinatorImplBase)
            .build();
    }

    @Bean
    public CombinatorGrpc.CombinatorBlockingStub combinatorBlockingStub(ManagedChannel combinatorGrpcManagedChannel) {
        return CombinatorGrpc.newBlockingStub(combinatorGrpcManagedChannel);
    }
}
