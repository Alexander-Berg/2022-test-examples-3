package ru.yandex.chemodan.grpc;

import grpc.health.v1.HealthGrpc;
import grpc.health.v1.HealthOuterClass;
import org.junit.Assert;
import org.junit.Test;

public class HealthCheckTest extends AbstractGrpcBaseTest {

    @Test
    public void testHealthCheck() {
        HealthGrpc.HealthBlockingStub healthBlockingStub = HealthGrpc.newBlockingStub(createManagedChannel());
        HealthOuterClass.HealthCheckResponse response = healthBlockingStub.check(
                HealthOuterClass.HealthCheckRequest.newBuilder().build());
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), HealthOuterClass.HealthCheckResponse.ServingStatus.SERVING);
    }
}
