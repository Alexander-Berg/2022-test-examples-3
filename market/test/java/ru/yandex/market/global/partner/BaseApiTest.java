package ru.yandex.market.global.partner;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import static ru.yandex.market.global.common.test.TestUtil.mockRequestAttributes;

public class BaseApiTest extends BaseFunctionalTest {
    @BeforeEach
    public void mockRequest() {
        mockRequestAttributes(Map.of());
    }
}
