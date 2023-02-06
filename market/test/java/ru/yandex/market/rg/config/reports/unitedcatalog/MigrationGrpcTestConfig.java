package ru.yandex.market.rg.config.reports.unitedcatalog;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.business.migration.BusinessMigrationServiceGrpc;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

@Configuration
public class MigrationGrpcTestConfig {
    @Bean
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase mbocGrpcService() {
        return mock(BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase.class,
                delegatesTo(new BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase() {
                }));
    }

    @Bean
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceBlockingStub mbocGrpcClient() {
        return BusinessMigrationServiceGrpc
                .newBlockingStub(mbocManagedChannel());
    }

    @Bean
    public String getMbocGrpcServerName() {
        return InProcessServerBuilder.generateName();
    }

    @Bean
    public ManagedChannel mbocManagedChannel() {
        return InProcessChannelBuilder.forName(getMbocGrpcServerName()).directExecutor().build();
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server mbocGrpcServer() {
        return InProcessServerBuilder
                .forName(getMbocGrpcServerName()).directExecutor().addService(mbocGrpcService())
                .build();
    }

    @Bean
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase mdmGrpcService() {
        return mock(BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase.class,
                delegatesTo(new BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase() {
                }));
    }

    @Bean
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceBlockingStub mdmGrpcClient() {
        return BusinessMigrationServiceGrpc
                .newBlockingStub(mdmManagedChannel());
    }

    @Bean
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase pppGrpcService() {
        return mock(BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase.class,
                delegatesTo(new BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase() {
                }));
    }

    @Bean
    public BusinessMigrationServiceGrpc.BusinessMigrationServiceBlockingStub pppGrpcClient() {
        return BusinessMigrationServiceGrpc
                .newBlockingStub(pppManagedChannel());
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server pppGrpcServer() {
        return InProcessServerBuilder
                .forName(getPppGrpcServerName()).directExecutor().addService(pppGrpcService())
                .build();
    }

    @Bean
    public String getMdmGrpcServerName() {
        return InProcessServerBuilder.generateName();
    }

    @Bean
    public String getPppGrpcServerName() {
        return InProcessServerBuilder.generateName();
    }

    @Bean
    public ManagedChannel mdmManagedChannel() {
        return InProcessChannelBuilder.forName(getMdmGrpcServerName()).directExecutor().build();
    }

    @Bean
    public ManagedChannel pppManagedChannel() {
        return InProcessChannelBuilder.forName(getPppGrpcServerName()).directExecutor().build();
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Server mdmGrpcServer() {
        return InProcessServerBuilder
                .forName(getMdmGrpcServerName()).directExecutor().addService(mdmGrpcService())
                .build();
    }
}
