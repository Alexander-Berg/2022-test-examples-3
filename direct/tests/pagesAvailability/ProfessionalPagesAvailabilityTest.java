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
* Date: 26.10.12
* Time: 16:00
*/
@Aqua.Test
@Feature(Project.Feature.DIRECT_MONITORING)
@Stories(Project.Story.AVAILABILITY)
@Title("Проверка доступности страниц в профессиональном интерфейсе")
@RunWith(Parameterized.class)
public class ProfessionalPagesAvailabilityTest extends BaseDirectPageAvailabilityTest {

    public ProfessionalPagesAvailabilityTest(String url, String host) {
        super(url, host);
    }

    @Parameterized.Parameters(name = "{index}: url = {0}")
    public static Collection data() {
        Object[][] data = new Object[][] {
            {
                "https://direct.yandex.ru/registered/main.pl?cmd=servicingRequest&cid=1847186#request",
                "https://passport.yandex.ru/"
            },
            {
                "https://direct.yandex.ru/registered/main.pl?cmd=servicingRequest&cid=1847186",
                "https://passport.yandex.ru/"
            },
            {
                "http://direct.yandex.ru/servicing_request.pl",
                "https://passport.yandex.ru/"
            },
            {
                "https://direct.yandex.ru/registered/main.pl?cmd=editCamp&cid=1847186&retpath=%2Fregistered%2Fmain.pl%3F",
                "https://passport.yandex.ru/"
            },
            {
                "https://direct.yandex.ru/registered/main.pl?cmd=editCamp&cid=1847186",
                "https://passport.yandex.ru/"
            },
            {
                "https://direct.yandex.ru/registered/main.pl?cmd=reportPayment",
                "https://passport.yandex.ru/"
            },
            {
                "https://direct.yandex.ru/registered/main.pl?cmd=userSettings",
                "https://passport.yandex.ru/"
            },
            {
                "https://direct.yandex.ru/registered/main.pl?cmd=newPdfReport&cid_1847186=1",
                "https://passport.yandex.ru/"
            },
            {
                "https://direct.yandex.ru/registered/main.pl?cmd=newPdfReport",
                "https://passport.yandex.ru/"
            },
            {
                "http://wordstat.yandex.ru/?direct=1",
                "https://passport.yandex.ru/"
            },
            {
                "https://direct.yandex.ru/registered/main.pl?cmd=ForecastByWords",
                "https://passport.yandex.ru/"
            }
        };

        return Arrays.asList(data);
    }

}
