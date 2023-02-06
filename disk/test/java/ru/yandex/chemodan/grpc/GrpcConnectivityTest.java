package ru.yandex.chemodan.grpc;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.junit.Assert;
import org.junit.Test;

public class GrpcConnectivityTest extends AbstractGrpcBaseTest {


    @Test
    public void testConnectivity() {
        ManagedChannel channel = createManagedChannel();
        ConnectivityState state = channel.getState(true);
        Assert.assertNotNull(state);
        Assert.assertNotEquals(ConnectivityState.SHUTDOWN, state);
        Assert.assertNotEquals(ConnectivityState.TRANSIENT_FAILURE, state);
    }
}
