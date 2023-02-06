package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.text.ParseException;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.ping.CheckResult;

public class CheckResultJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void shouldSerialize() throws IOException, ParseException {
        CheckResult result = new CheckResult(CheckResult.Level.OK, "asdasd");

        String json = write(result);

        checkJson(json, "$." + Names.CheckResult.LEVEL, "OK");
        checkJson(json, "$." + Names.CheckResult.MESSAGE, "asdasd");
    }
}
