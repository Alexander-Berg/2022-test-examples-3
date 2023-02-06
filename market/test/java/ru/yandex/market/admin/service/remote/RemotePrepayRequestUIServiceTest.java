package ru.yandex.market.admin.service.remote;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.contract.UIContract;

/**
 * Тесты для {@link RemotePrepayRequestUIService}.
 *
 * @author Vadim Lyalin
 */
public class RemotePrepayRequestUIServiceTest extends FunctionalTest {
    @Autowired
    private RemotePrepayRequestUIService service;

    @Test
    void testGetContract() {
        UIContract contract = service.getContract(1);
    }
}
