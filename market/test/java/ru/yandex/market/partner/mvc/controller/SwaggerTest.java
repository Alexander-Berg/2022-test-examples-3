package ru.yandex.market.partner.mvc.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@ParametersAreNonnullByDefault
class SwaggerTest extends FunctionalTest {
    private static final Logger log = LoggerFactory.getLogger(SwaggerTest.class);

    /**
     * Работоспособность Swagger'а.
     * <p>
     * Валидируем генерируемую OpenAPI-спеку.
     * {@see} https://wiki.yandex-team.ru/mbi/development/howto/swaggerlint/
     */
    @Test
    void validateSwaggerApiSpec() throws Exception {
        String swaggerSpec = readSwaggerSpec();
        checkSpecWithParser(swaggerSpec);
        checkSpecWithSwaggerLint(swaggerSpec);
    }

    private void checkSpecWithSwaggerLint(String swaggerSpec) throws Exception {
        File file = new File(System.getProperty("java.io.tmpdir") + "/swagger.json");
        FileUtils.writeStringToFile(file, swaggerSpec, StandardCharsets.UTF_8);

        Runtime rt = Runtime.getRuntime();
        String[] commands = {"swaggerlint",
                System.getProperty("java.io.tmpdir") + "/swagger.json",
                "--config=" + this.getClass().getResource("/mvc/swaggerlint.config.js").getFile()};
        log.debug("command: {}", String.join(" ", commands));
        Process proc = rt.exec(commands);
        try (
                BufferedReader std =
                        new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8));
                BufferedReader stdErr =
                        new BufferedReader(new InputStreamReader(proc.getErrorStream(), StandardCharsets.UTF_8))
        ) {
            proc.waitFor(1, TimeUnit.MINUTES);
            StringBuilder sb = new StringBuilder("Swaggerlint std output:\n");
            while (std.ready()) {
                sb.append(std.readLine()).append("\n");
            }
            sb.append("Error output\n");
            while (stdErr.ready()) {
                sb.append(stdErr.readLine()).append("\n");
            }
            Assertions.assertEquals(0, proc.exitValue(), sb.toString());
        }
    }

    private void checkSpecWithParser(String swaggerSpec) {
        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(swaggerSpec, true);
        List<String> messageList = result.getMessages();

        for (String message : messageList) {
            log.error("{}", message);
        }

        MatcherAssert.assertThat(messageList, Matchers.empty());
    }

    private String readSwaggerSpec() {
        String location = baseUrl + "/v2/api-docs";
        return FunctionalTestHelper.get(location).getBody();
    }

}
