package ru.yandex.autotests.directmonitoring.tests.pagesAvailability;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.directmonitoring.tests.BaseDirectPageAvailabilityTest;
import ru.yandex.autotests.directmonitoring.tests.Project;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.Collection;

/**
 * User: buhter
 * Date: 01.04.13
 * Time: 13:08
 */
@Aqua.Test
@Feature(Project.Feature.DIRECT_MONITORING)
@Stories(Project.Story.AVAILABILITY)
@Title("Проверка доступности страниц новостей Директа")
@RunWith(Parameterized.class)
public class NewsAvailabilityTest extends BaseDirectPageAvailabilityTest {

    public NewsAvailabilityTest(String url, String host) {
        super(url, host);
    }

    @Parameterized.Parameters(name = "{index}: url = {0}")
    public static Collection data() {
        Object[][] data = new Object[][] {
            {"http://direct.yandex.ru/archive.html", "https://passport.yandex.ru/"},
            {"http://direct.yandex.ru/news.rss", "https://passport.yandex.ru/"}
        };

        return Arrays.asList(data);
    }

}

