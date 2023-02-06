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
 * @author: Alex Samokhin (alex-samo@yandex-team.ru)
 * Date: 27.01.14
 */

@Aqua.Test
@Feature(Project.Feature.DIRECT_MONITORING)
@Stories(Project.Story.AVAILABILITY)
@Title("Проверка доступности страниц статистики по ключевым словам http://direct.yandex.ru/spros")
@RunWith(Parameterized.class)
public class SprosAvailabilityTest extends BaseDirectPageAvailabilityTest {

    public SprosAvailabilityTest(String url, String host) {
        super(url, host);
    }

    @Parameterized.Parameters(name = "{index}: url = {0}")
    public static Collection data() {
        Object[][] data = new Object[][] {
                {"http://direct.yandex.ru/spros", "https://passport.yandex.ru/"}
                , {"http://direct.yandex.ua/spros", "https://passport.yandex.ua/"}
                , {"http://direct.yandex.kz/spros", "https://passport.yandex.kz/"}
                , {"http://direct.yandex.com.tr/spros", "https://passport.yandex.com.tr/"}
                , {"http://direct.yandex.com/spros", "https://passport.yandex.com/"}
        };
        return Arrays.asList(data);
    }
}
