package ru.yandex.market.pers.tms.yt.yql;

import ru.yandex.market.pers.yt.yqlgen.YqlLoader;
import ru.yandex.yt.yqltest.YqlTestScript;
import ru.yandex.yt.yqltest.spring.AbstractYqlTest;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 06.09.2021
 */
public class AbstractPersYqlTest extends AbstractYqlTest {
    public YqlTestScript loadScript(String path) {
        // в персах почти все запросы обогащаются доп. функциями.
        // поэтому загружаем их отдельно через YqlLoader.readYqlWithLibForTests
        return YqlTestScript.simple(YqlLoader.readYqlWithLib(path));
    }
}
