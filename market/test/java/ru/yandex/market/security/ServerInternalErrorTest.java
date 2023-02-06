package ru.yandex.market.security;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.security.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

class ServerInternalErrorTest extends FunctionalTest {
    @Autowired
    private String baseUrl;

    @Autowired
    private CheckStaticAuthorityServantlet checkStaticAuthoritySpyServantlet;

    @Test
    void testServerInternalError() {
        doThrow(new RuntimeException()).when(checkStaticAuthoritySpyServantlet).process(any(), any());
        Exception e = Assertions.assertThrows(HttpServerErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/checkStaticAuthoritySpy", null));
        Assertions.assertTrue(e.getMessage().contains("500"));
    }
}
