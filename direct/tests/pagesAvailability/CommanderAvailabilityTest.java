package ru.yandex.autotests.directmonitoring.tests.pagesAvailability;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.autotests.directmonitoring.tests.BaseDirectPageAvailabilityTest;
import ru.yandex.autotests.directmonitoring.tests.Project;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 20.01.15
 *         https://st.yandex-team.ru/TESTIRT-3559
 */

@Aqua.Test
@Features(Project.Feature.DIRECT_MONITORING)
@Stories(Project.Story.AVAILABILITY)
@Title("Проверка доступности промо-страниц коммандера")
@RunWith(Parameterized.class)
public class CommanderAvailabilityTest extends BaseDirectPageAvailabilityTest {

    public CommanderAvailabilityTest(String url, String host) {
        super(url, host);
    }

    @Parameterized.Parameters(name = "{index}: url = {0}")
    public static Collection data() {
        Object[][] data = new Object[][] {
                {"https://direct.yandex.ru/commander/", "https://passport.yandex.ru/"}
                , {"https://direct.yandex.ua/commander/", "https://passport.yandex.ua/"}
                , {"https://direct.yandex.kz/commander/", "https://passport.yandex.kz/"}
                , {"http://direct.yandex.com.tr/commander/", "https://passport.yandex.com.tr/"}
                , {"http://direct.yandex.com/commander/", "https://passport.yandex.com/"}
        };
        return Arrays.asList(data);
    }
}
