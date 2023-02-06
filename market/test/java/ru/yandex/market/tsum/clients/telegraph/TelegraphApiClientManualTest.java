package ru.yandex.market.tsum.clients.telegraph;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 21.11.2018
 */
@Ignore
public class TelegraphApiClientManualTest {
    private final TelegraphApiClient client = new TelegraphApiClient(
        "https://telegraph.yandex-team.ru/",
        "AQAD-qJSJrVbAAAIWY0jJL7UpUy_h421V7iFCeE"
    );

    @Test
    public void name() throws IOException {
        client.setAndCheckRedirect(21939, 5540931);
    }
}
