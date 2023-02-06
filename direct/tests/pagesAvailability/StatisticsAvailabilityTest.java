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
@Title("Проверка доступности страниц статистики Директа")
@RunWith(Parameterized.class)
public class StatisticsAvailabilityTest extends BaseDirectPageAvailabilityTest {

    public StatisticsAvailabilityTest(String url) {
        super(url, "https://passport.yandex.ru/");
    }

    @Parameterized.Parameters(name = "{index}: url = {0}")
    public static Collection data() {
        Object[][] data = new Object[][] {
            {"https://direct.yandex.ru/registered/main.pl?cmd=showCampStat&cid=1847186&detail=Yes&types=days"},
            {"https://direct.yandex.ru/registered/main.pl?cmd=showCampStat&cid=1847186&types=total&target_all=1"},
            {"https://direct.yandex.ru/registered/main.pl?cmd=showCampStat&cid=1847186&phrasedate=Yes&target_all=1"},
            {"https://direct.yandex.ru/registered/main.pl?cmd=showCampStat&cid=1847186&stat_type=geo&target_all=1"},
            {"https://direct.yandex.ru/registered/main.pl?cmd=showCampStat&cid=1847186&stat_type=pages&target_all=1"},
            {"https://direct.yandex.ru/registered/main.pl?cmd=showCampStat&cid=1847186&stat_type=custom&target_all=1"},
            {"https://direct.yandex.ru/registered/main.pl?cmd=showCampStat;cid=1847186;stat_type=custom;target_all=1"+
                    "&print=yes"},
            {"https://direct.yandex.ru/registered/main.pl?cmd=showCampStat&stat_type=campdate&"}
        };

        return Arrays.asList(data);
    }

}
