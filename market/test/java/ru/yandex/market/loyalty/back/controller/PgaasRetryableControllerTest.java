package ru.yandex.market.loyalty.back.controller;

import org.junit.Test;

import ru.yandex.market.loyalty.core.utils.CommonTestUtils;

public class PgaasRetryableControllerTest {
    @Test
    public void allControllersRetryable() {
        CommonTestUtils.checkControllers("ru.yandex.market.loyalty.back.controller", LoyaltyProgramsController.class);
    }
}
