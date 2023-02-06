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
 * Time: 14:15
 */
@Aqua.Test
@Feature(Project.Feature.DIRECT_MONITORING)
@Stories(Project.Story.AVAILABILITY)
@Title("Проверка доступности страниц помощи Директа")
@RunWith(Parameterized.class)
public class HelpAvailabilityTest extends BaseDirectPageAvailabilityTest {

    public HelpAvailabilityTest(String url, String host) {
        super(url, host);
    }

    @Parameterized.Parameters(name = "{index}: url = {0}")
    public static Collection data() {
        Object[][] data = new Object[][] {
            {"http://direct.yandex.ru/help/", "https://passport.yandex.ru/"},
            {"http://direct.yandex.ru/help/?id=904209", "https://passport.yandex.ru/"},
            {"http://direct.yandex.ru/help/?id=990404", "https://passport.yandex.ru/"},
            {"http://direct.yandex.ru/help/?id=992858", "https://passport.yandex.ru/"},
            {"http://direct.yandex.ru/help/?id=990407", "https://passport.yandex.ru/"},
            {"http://direct.yandex.ru/help/?id=1003967", "https://passport.yandex.ru/"},
            {"http://direct.yandex.ru/help/?id=1030028&region=213", "https://passport.yandex.ru/"},
            {"http://direct.yandex.ua/help/", "https://passport.yandex.ua/"},
            {"http://direct.yandex.kz/help/", "https://passport.yandex.kz/"}
        };

        return Arrays.asList(data);
    }
}
