package ru.yandex.market.mdm.metadata.testutils

import io.grpc.BindableService
import io.grpc.ManagedChannel
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.testing.GrpcCleanupRule

fun createTestManagedChannel(grpcCleanupRule: GrpcCleanupRule, service: BindableService): ManagedChannel {
    val serverName: String = InProcessServerBuilder.generateName()
    grpcCleanupRule.register(
        InProcessServerBuilder
            .forName(serverName)
            .directExecutor()
            .addService(service)
            .build()
            .start()
    )
    return grpcCleanupRule.register(
        InProcessChannelBuilder
            .forName(serverName)
            .directExecutor()
            .build()
    )
}
