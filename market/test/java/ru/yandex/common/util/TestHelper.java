package ru.yandex.common.util;

import ru.yandex.market.checker.core.CoreCheckerTask;
import ru.yandex.market.checker.zora.util.Platform;

/**
 * @author valeriashanti
 * @date 15/10/2020
 */
public class TestHelper {

    public static CoreCheckerTask createTask(String url) {
        return new CoreCheckerTask(url, 0, 0, 0,Platform.ANY);
    }
}
