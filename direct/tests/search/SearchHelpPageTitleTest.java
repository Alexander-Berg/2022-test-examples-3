package ru.yandex.autotests.directmonitoring.tests.search;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.directmonitoring.tests.Project;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.*;

/**
* User: buhter
* Date: 28.10.12
* Time: 0:12
*/
@Aqua.Test
@Feature(Project.Feature.DIRECT_MONITORING)
@Stories(Project.Story.SEARCH)
@Title("Проверка заголовка страницы при поиске в помощи")
public class SearchHelpPageTitleTest extends SearchHelpTestBase {

    @Test
    @Title("Проверка заголовка страницы")
    public void searchResultsTitleTest(){
        user.inOperatingSystem().waitForPageTitle(containsString(query));
    }

    @Test
    @Title("Проверка выдачи по запросу")
    public void emptyDeliveryOnDemandTest() {
        user.inOperatingSystem().waitForPageTitle(not(endsWith("(0)")));
    }
}
