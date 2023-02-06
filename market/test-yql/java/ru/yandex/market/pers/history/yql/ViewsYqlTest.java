package ru.yandex.market.pers.history.yql;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.yt.yqlgen.YqlLoader;
import ru.yandex.yt.yqltest.YqlTestScript;
import ru.yandex.yt.yqltest.spring.AbstractYqlTest;

public class ViewsYqlTest extends AbstractYqlTest {

    @Test
    public void testImportViews() {
        runTest(
            YqlTestScript.simple(YqlLoader.readYqlWithLib("/yql/socialecom/se_views.sql")),
            "/views/views_import_expected.json",
            "/views/views.mock"
        );
    }
}
