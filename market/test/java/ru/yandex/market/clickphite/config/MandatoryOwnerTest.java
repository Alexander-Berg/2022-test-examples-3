package ru.yandex.market.clickphite.config;

import org.junit.Test;

public class MandatoryOwnerTest {

    FailureValidationTestTool tool = new FailureValidationTestTool("./");

    @Test
    public void test() {
        tool.shouldFail("noOwnerInConfig")
            .hasMessageContaining("object has missing required properties ([\\\"owner\\\"])");
    }

}
