package ui_tests.src.test.java.tests;


import entity.Entity;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import interfaces.testPriorities.Normal;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;


import java.util.List;

/**
 * Функциональность Таблицы
 */

public class TestsTables {

    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsTables.class);

    @InfoTest(descriptionTest = "Проверка поиска тикета по gid",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-762",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-368")
    @Category({Normal.class})
    @Test
    public void ocrm762_SearchTicketWithGid() {
        // Перейти в "Письменная коммуникация"-> "Обращения".
        Pages.navigate(webDriver).openServicesBeruQuestion();

        // Открыть любое обращение
        PageHelper.tableHelper(webDriver).openRandomEntity();

        // Скопировать gid открывшегося тикета
        String ticketGid = Tools.other().getGidFromCurrentPageUrl(webDriver);

        // Вернуться в список обращений
        Pages.navigate(webDriver).openLVAllTickets();

        // Сделать быстрый поиск по gid
        PageHelper.tableHelper(webDriver).quicklyFindEntity(ticketGid);

        // Открыть найденный тикет (1 в результатах поиска)
        Assert.assertEquals("В результатах поиска 0 или больше 1 тикета", 1, Entity.entityTable(webDriver).footer().getTotalEntity());
        PageHelper.tableHelper(webDriver).openRandomEntity();

        // Проверить, что gid тикета совпадает со скопированным ранее
        Assert.assertEquals("Открывшийся тикет имеет gid отличный от того, по которому производился поиск",
                ticketGid, Tools.other().getGidFromCurrentPageUrl(webDriver));
    }

    @Test

    @InfoTest(descriptionTest = "Поиск очереди по названию",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1351",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-404")
    @Category(Blocker.class)
    public void ocrm1351_SearchForAServiceByTitle() {
        String code = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def service = api.db.of('service').withFilters{\n" +
                        "}.limit(1).get().title\n"
        );
        Pages.navigate(webDriver).openPageByMetaClassAndID("root@1?tabBar=2");
        PageHelper.tableHelper(webDriver).quicklyFindEntity(code);
        List<String> titlesFromPage = Entity.entityTable(webDriver).content().getTitlesEntityOnPage();

        Assert.assertTrue("Среди найденных записей нет той которую мы искали. \n" +
                "Мы искали - " + code + "\n" +
                "А нашлось - " + titlesFromPage, titlesFromPage.contains(code));
    }
  @Test
    @InfoTest(descriptionTest = "Поиск очереди по коду",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1352",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-405")
    @Category(Blocker.class)
    public void ocrm1352_SearchForAServiceByTitle() {
        String[] code = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "def service = api.db.of('service').withFilters{\n" +
                        "}.limit(1).get()\n" +
                        "return \\\"${service.code},${service.title}\\\""
        ).split(",");
        Pages.navigate(webDriver).openPageByMetaClassAndID("root@1?tabBar=2");
        PageHelper.tableHelper(webDriver).quicklyFindEntity(code[0]);
        List<String> titlesFromPage = Entity.entityTable(webDriver).content().getTitlesEntityOnPage();

        Assert.assertTrue("Среди найденных записей нет той которую мы искали. \n" +
                "Мы искали - " + code[1] + "\n" +
                "А нашлось - " + titlesFromPage, titlesFromPage.contains(code[1]));


    }
}
