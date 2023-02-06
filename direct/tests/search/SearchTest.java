package ru.yandex.autotests.directmonitoring.tests.search;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.directmonitoring.tests.BaseDirectMonitoringTest;
import ru.yandex.autotests.directmonitoring.tests.Project;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Random;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
* User: buhter
* Date: 28.10.12
* Time: 0:05
*/
@Aqua.Test
@Feature(Project.Feature.DIRECT_MONITORING)
@Stories(Project.Story.SEARCH)
@Title("Проверка поиска по объявлениям Директа")
public class SearchTest extends BaseDirectMonitoringTest {

    private static final String ERROR = "Сервис временно недоступен";

    private String query;
    private String location;
    private String message;

    private String[] data = new String[] {
            "доставка пиццы круглосуточно",
            "доставка цветов",
            "пластиковые окна",
            "жалюзи",
            "карнавальные костюмы",
            "одежда детская",
            "свадебное платье",
            "банкетный зал",
            "пошив штор",
            "пошив костюмов",
            "ремонт кухни",
            "машина на прокат",
            "кредит наличными",
            "турфирма нева",
            "горячие туры"
    };

    @Override
    public void additionalActions() {
        query = data[new Random().nextInt(data.length)];

        user.inBrowserAddressBar().openSearchPage();
        user.onSearchPage().search(query);
    }

    @Test
    @Title("Проверка наличия карты в результатах поиска")
    public void mapIsVisibleInSearchResulsTest() {
        user.onSearchPage().shouldSeeMap();
    }

    @Test
    @Title("Проверка наличия и работоспособности пагинатора в результатах поиска")
    public void paginatorIsVisibleAndAvailableInSearchResultsTest() {
        location = "результатов поиска по баннерам";
        message = String.format("На странице %s получена ошибка %s", location, ERROR);

        user.inOperatingSystem().waitForPageTitle(not(containsString(ERROR)));
    }

    @Test
    @Title("Проверка наличия и работоспособности пагинатора в результатах поиска на второй странице")
    public void paginatorIsVisibleAndAvailableInSearchResultsOnSecondPageTest() {
        location = "результатов поиска по баннерам, 2";
        message = String.format("На странице %s получена ошибка %s", location, ERROR);

        user.onSearchPage().goToPage("2");

        user.inOperatingSystem().waitForPageTitle(not(containsString(ERROR)));
        user.onSearchPage().shouldSeeResultListStartsWith("21");
    }

}
