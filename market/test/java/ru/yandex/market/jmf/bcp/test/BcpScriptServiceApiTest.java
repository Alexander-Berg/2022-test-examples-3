package ru.yandex.market.jmf.bcp.test;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.bcp.impl.BcpScriptServiceApi;

@SpringJUnitConfig(InternalBcpTestConfiguration.class)
public class BcpScriptServiceApiTest {
    private final BcpScriptServiceApi bcpScriptServiceApi;

    public BcpScriptServiceApiTest(BcpScriptServiceApi bcpScriptServiceApi) {
        this.bcpScriptServiceApi = bcpScriptServiceApi;
    }

    @Test
    public void editWithUnexpectedObjectTypeThrowsEx() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> bcpScriptServiceApi.edit(new Object(), Map.of()));
    }
}
