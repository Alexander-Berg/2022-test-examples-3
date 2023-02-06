package ru.yandex.market.helpers;

import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.market.util.TestSerializationService;

public abstract class MockMvcAware {
    protected final MockMvc mockMvc;
    protected final TestSerializationService testSerializationService;


    public MockMvcAware(MockMvc mockMvc, TestSerializationService testSerializationService) {
        this.mockMvc = mockMvc;
        this.testSerializationService = testSerializationService;
    }
}
