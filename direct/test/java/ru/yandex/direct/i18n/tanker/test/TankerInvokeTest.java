package ru.yandex.direct.i18n.tanker.test;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.i18n.tanker.Keyset;
import ru.yandex.direct.i18n.tanker.Tanker;
import ru.yandex.direct.i18n.tanker.TankerList;
import ru.yandex.direct.i18n.tanker.TankerResponse;
import ru.yandex.direct.utils.io.FileUtils;

public class TankerInvokeTest {
    @Ignore("Юнит-тест для ручных запусков танкера для дебага проблем")
    @Test
    public void test() throws IOException {
        Tanker tanker = Tanker.forTokenFile(Tanker.TANKER_URL, FileUtils.expandHome("~/.ssh/tanker-direct-java-token"),
                "direct-java", true);
        TankerResponse<TankerList<Keyset>> keysets = tanker.listKeysets("master");
    }
}
