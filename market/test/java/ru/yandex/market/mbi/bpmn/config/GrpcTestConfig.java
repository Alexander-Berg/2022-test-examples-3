package ru.yandex.market.mbi.bpmn.config;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import ru.yandex.market.business.migration.BusinessMigrationServiceGrpc;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

@TestConfiguration
public class GrpcTestConfig {

    @Bean
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase mbocBusinessMigrationService() {
        return mock(BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase.class,
                delegatesTo(new BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase() {
                }));
    }

    @Bean
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceBlockingStub mbocGrpcClient() {
        return BusinessMigrationServiceGrpc.newBlockingStub(mbocManagedChannel());
    }

    @Bean
    public String mbocGrpcServerName() {
        return InProcessServerBuilder.generateName();
    }

    @Bean
    public ManagedChannel mbocManagedChannel() {
        return InProcessChannelBuilder.forName(mbocGrpcServerName()).directExecutor().build();
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server mboServer() {
        return InProcessServerBuilder
                .forName(mbocGrpcServerName()).directExecutor().addService(mbocBusinessMigrationService())
                .build();
    }

    @Bean
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase mdmBusinessMigrationService() {
        return mock(BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase.class,
                delegatesTo(new BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase() {
                }));
    }

    @Bean
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceBlockingStub mdmGrpcClient() {
        return BusinessMigrationServiceGrpc.newBlockingStub(mdmManagedChannel());
    }

    @Bean
    public String mdmGrpcServerName() {
        return InProcessServerBuilder.generateName();
    }

    @Bean
    public ManagedChannel mdmManagedChannel() {
        return InProcessChannelBuilder.forName(mdmGrpcServerName()).directExecutor().build();
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server mdmServer() {
        return InProcessServerBuilder
                .forName(mdmGrpcServerName()).directExecutor().addService(mdmBusinessMigrationService())
                .build();
    }

    @Bean
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase pppBusinessMigrationService() {
        return mock(BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase.class,
                delegatesTo(new BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase() {
                }));
    }

    @Bean
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceBlockingStub pppGrpcClient() {
        return BusinessMigrationServiceGrpc.newBlockingStub(pppManagedChannel());
    }

    @Bean
    public String pppGrpcServerName() {
        return InProcessServerBuilder.generateName();
    }

    @Bean
    public ManagedChannel pppManagedChannel() {
        return InProcessChannelBuilder.forName(pppGrpcServerName()).directExecutor().build();
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server pppServer() {
        return InProcessServerBuilder
                .forName(pppGrpcServerName()).directExecutor().addService(pppBusinessMigrationService())
                .build();
    }
}
