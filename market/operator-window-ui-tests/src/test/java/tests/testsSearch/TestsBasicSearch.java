package ui_tests.src.test.java.tests.testsSearch;

import entity.Entity;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;

public class TestsBasicSearch {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsBasicSearch.class);

    @Ignore("Поиск переработали")
    @InfoTest(descriptionTest = "Поиск клиента и заказа по несуществующим данным (для проверки пустой выдачи)",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-71",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1390")
    @Category({Blocker.class})
    @Test
    public void ocrm1390_SearchWithEmptyResult() {
        // Открыть страницу поиска
        Pages.mainMenuOfPage(webDriver).openSearchPage();
        Entity.toast(webDriver).hideNotificationError();

        // Произвести поиск по несуществующему запросу
        PageHelper.searchHelper(webDriver).useBasicSearch("абракадаба1304211627");

        // Убедиться, что в результатах отображается надпись "Ничего не найдено"
        Assert.assertEquals("Не отобразился пустой результат выдачи",
                0, PageHelper.searchHelper(webDriver).getResults().size());
    }
}
