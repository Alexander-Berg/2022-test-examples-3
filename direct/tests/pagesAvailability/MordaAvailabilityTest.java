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
@Title("Проверка доступности морд Директа")
@RunWith(Parameterized.class)
public class MordaAvailabilityTest extends BaseDirectPageAvailabilityTest {

    public MordaAvailabilityTest(String url, String host) {
        super(url, host);
    }

    @Parameterized.Parameters(name = "{index}: url = {0}")
    public static Collection data() {
        Object[][] data = new Object[][] {
            {"http://direct.yandex.ru", "https://passport.yandex.ru/"},
            {"http://direct.yandex.ua", "https://passport.yandex.ua/"},
            {"http://direct.yandex.kz", "https://passport.yandex.kz/"},
            {"http://direct.yandex.com.tr", "https://passport.yandex.com.tr/"},
            {"http://direct.yandex.com", "https://passport.yandex.com/"},
        };

        return Arrays.asList(data);
    }
}

