package ru.yandex.downloader.tests;

import org.junit.Before;
import ru.yandex.misc.log.log4j.Log4jUtils;

/**
 * @author akirakozov
 */
public class DownloaderTestBase {

    @Before
    public void initLogger() {
        Log4jUtils.configureDefault();
    }
}
