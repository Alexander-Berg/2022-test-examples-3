package ru.yandex.market.adv.promo.swagger;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.adv.promo.FunctionalTest;

/**
 * Тест, проверяющий swagger спецификацию с помощью утилиты swaggerlint
 *
 * https://github.com/antonk52/swaggerlint
 */
class SwaggerLintTest extends FunctionalTest {
    private static final Logger log = LoggerFactory.getLogger(SwaggerLintTest.class);

    @Test
    void validateSwaggerApiSpec() throws Exception {
        String swaggerSpecUrl = baseUrl() + "/v3/api-docs";
        String swaggerLintConfigFileName = this.getClass().
                getResource("/ru/yandex/market/adv/promo/swagger/swagger_lint_test/swaggerlint.config.js").getFile();

        Runtime rt = Runtime.getRuntime();
        String[] commands = {"swaggerlint", swaggerSpecUrl, "--config=" + swaggerLintConfigFileName};
        log.debug("swaggerlint command: {}", String.join(" ", commands));
        Process proc = rt.exec(commands);
        proc.waitFor(1, TimeUnit.MINUTES);

        String swaggerLintStdOut = IOUtils.readInputStream(proc.getInputStream());
        String swaggerLintStdErr = IOUtils.readInputStream(proc.getErrorStream());
        String message = "swaggerlint std output:\n" + swaggerLintStdOut +
                "swaggerlint error output:\n" + swaggerLintStdErr;

        Assertions.assertEquals(0, proc.exitValue(), message);
    }
}
