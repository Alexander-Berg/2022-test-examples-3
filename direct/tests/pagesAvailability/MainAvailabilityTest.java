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
@Title("Проверка доступности основных страниц Директа")
@RunWith(Parameterized.class)
public class MainAvailabilityTest extends BaseDirectPageAvailabilityTest {

    public MainAvailabilityTest(String url, String host) {
        super(url, host);
    }

    @Parameterized.Parameters(name = "{index}: url = {0}")
    public static Collection data() {
        Object[][] data = new Object[][] {
            {"https://direct.yandex.ru/registered/main.pl", "https://passport.yandex.ru/"},
            {"https://direct.yandex.ru/registered/main.pl?cmd=newCamp&mediaType=text", "https://passport.yandex.ru/"},
            {"https://direct.yandex.ru/registered/main.pl?cmd=showCamps&tab=all", "https://passport.yandex.ru/"},
            {"https://direct.yandex.ua/registered/main.pl?cmd=showCamps&tab=all", "https://passport.yandex.ua/"},
            {"https://direct.yandex.kz/registered/main.pl?cmd=showCamps&tab=all", "https://passport.yandex.kz/"}
        };

        return Arrays.asList(data);
    }

}

