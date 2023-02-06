package ru.yandex.market.jmf.blackbox.support.test;

import java.io.InputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.util.Resources;
import ru.yandex.market.jmf.blackbox.support.AuthInfoParser;
import ru.yandex.market.jmf.blackbox.support.response.BlackBoxResponse;

public class AuthInfoParserTest {

    @Test
    public void parseResponse() {
        AuthInfoParser parser = new AuthInfoParser();
        // вызов системы
        InputStream is = Resources.readAsInputStream("/blackbox/support/blackBoxResponse_1.json");
        BlackBoxResponse result = parser.parse(is);
        // проверка утверждений
        Assertions.assertEquals("tesseract", result.getLogin());

        Assertions.assertEquals(0, result.getStatus().getCode());
        Assertions.assertEquals("VALID", result.getStatus().name());

        Assertions.assertEquals(Long.valueOf(1120000000016875L), result.getUid().getValue());
        Assertions.assertEquals("OK", result.getError());
        Assertions.assertNull(result.getDisplayName());
        Assertions.assertEquals(
                "3:user:CLYNEJC2zc4FGigKCQjrg5mRpdT-ARDrg5mRpdT-ARoMYmI6c2Vzc2lvbmlkIPqJeigC:BBd3rXXrBVhsGqh27iCk8a" +
                        "-BWv2LMusaFW60WH9C0cEJLrPBN0QxOmdYL7lzzZDDrvfqqupIL3aynJ4oMbGWNe_RWVVaqkgtS8NvU0uy5HpJc" +
                        "FMROzgmsXEehSALjN-xWt-79NP522kXzh7PnNs0RgjGFNakTgJQFrE8-TZpe10",
                result.getUserTicket());
        Assertions.assertNull(result.getException());
    }
}
