package ru.yandex.market.checkout.checkouter.tasks.v2.returns;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.returns.AbstractReturnTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class ReturnApplicationFallbackGeneratorTaskV2Test extends AbstractReturnTestBase {

    @Autowired
    private ReturnApplicationFallbackGeneratorTaskV2 generatorTaskV2;

    @Test
    public void taskWorksSuccessfully() {
        var saved = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), (r, o) -> {
            r.setApplicationUrl(null);
            return r;
        });
        assertNull(saved.getSecond().getApplicationUrl());
        var batch = generatorTaskV2.prepareBatch();
        assertEquals(1, batch.size());
        batch.forEach(r -> generatorTaskV2.processItem(r));
        var returnAfter = returnService.findReturnById(saved.second.getId(), false, ClientInfo.SYSTEM);
        assertNotNull(returnAfter.getApplicationUrl());
    }

    @Test
    public void taskWorksByTms() {
        var saved = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), (r, o) -> {
            r.setApplicationUrl(null);
            return r;
        });
        assertNull(saved.getSecond().getApplicationUrl());
        tmsTaskHelper.runReturnApplicationFallbackGeneratorTaskV2();
        var returnAfter = returnService.findReturnById(saved.second.getId(), false, ClientInfo.SYSTEM);
        assertNotNull(returnAfter.getApplicationUrl());
    }

}
