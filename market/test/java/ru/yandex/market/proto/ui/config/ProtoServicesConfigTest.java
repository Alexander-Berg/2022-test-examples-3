package ru.yandex.market.proto.ui.config;

import org.junit.Test;
import ru.yandex.market.proto.ui.proto.configuration.ProtoConfig;

public class ProtoServicesConfigTest {

    @Test
    public void testProtoConfig() {
        ProtoServicesConfig config = new ProtoServicesConfig();
        // check no exceptions are thrown
        ProtoConfig protoConfig = config.protoConfig();
    }
}
