package ru.yandex.market.global.checkout;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import static ru.yandex.market.global.common.test.TestUtil.mockRequestAttributes;

public abstract class BaseApiTest extends BaseFunctionalTest {
    @BeforeEach
    public void mockRequest() {
        mockRequestAttributes(Map.of());
    }
}
