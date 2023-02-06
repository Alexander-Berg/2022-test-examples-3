package ru.yandex.market.checkout.carter.mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.storage.curator.ZooCurator;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

public class MockFactory {

    @Autowired
    private WebApplicationContext wac;

    public ZooCurator getZooCuratorMock() throws Exception {
        ZooCurator result = mock(ZooCurator.class);
        when(result.callWithLock(anyString(), any())).thenAnswer(invocation -> {
            Runnable runnable = (Runnable) invocation.getArguments()[1];
            runnable.run();
            return true;
        });
        return result;
    }

    public MockMvc getMockMvc() {
        return MockMvcBuilders.webAppContextSetup(this.wac).alwaysDo(log()).build();
    }
}
