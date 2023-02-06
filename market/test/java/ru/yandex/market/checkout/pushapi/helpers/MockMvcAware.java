package ru.yandex.market.checkout.pushapi.helpers;

import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.market.checkout.util.PushApiTestSerializationService;

public abstract class MockMvcAware {
    protected final MockMvc mockMvc;
    protected final PushApiTestSerializationService testSerializationService;

    public MockMvcAware(MockMvc mockMvc,
                        PushApiTestSerializationService testSerializationService) {
        this.mockMvc = mockMvc;
        this.testSerializationService = testSerializationService;
    }
}
