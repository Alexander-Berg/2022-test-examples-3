package ui_tests.src.test.java.tests;

import Classes.ticketCommentTemplate.TicketCommentTemplate;
import entity.Entity;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
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

/**
 * Функциональность Очереди
 */
public class TestsServices {

    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsServices.class);

    @InfoTest(descriptionTest = "Проверка создания шаблона в очереди",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-761")
    @Category({Blocker.class})
    @Test
    public void ocrm761_CreateTicketCommentTemplate() {
        // Создать объект-переменную "Шаблон"
        TicketCommentTemplate expectedTemplate = new TicketCommentTemplate();

        // Задать атрибуты переменной
        expectedTemplate.setTitle(Tools.other().getRandomText())
                .setCode(String.valueOf(Tools.other().getRandomNumber(100, 99999999)))
                .setText(Tools.other().getRandomText());

        // Перейти в очередь
        Pages.navigate(webDriver).openServicesBeruQuestion();
        // Открыть вкладку "Шаблоны"
        PageHelper.tableHelper(webDriver).openTab("Шаблоны");

        // Нажать кнопку "Добавить"
        Entity.entityTable(webDriver).toolBar().clickOnAddTicketButton();

        // Создать шаблон
        Pages.createTicketCommentTemplatePage(webDriver)
                .setName(expectedTemplate.getTitle())
                .setCode(expectedTemplate.getCode())
                .setText(expectedTemplate.getText())
                .saveButtonClick();

        // Найти созданный шаблон (через api вернуть git)
        String templateGid = PageHelper.otherHelper(webDriver)
                .runningScriptFromAdministrationPage
                        (String.format("api.db.of('ticketCommentTemplate').withFilters{ eq('title', '%s') }.get()",
                                expectedTemplate.getTitle()));

        // Открыть карточку шаблона
        Pages.navigate(webDriver).openPageByMetaClassAndID(templateGid);

        // Создать объект-переменную "Шаблон" из значений на странице
        TicketCommentTemplate templateFromPage = new TicketCommentTemplate();

        templateFromPage.setTitle(Pages.ticketCommentTemplatePage(webDriver).header().getSubject())
                .setCode(Pages.ticketCommentTemplatePage(webDriver).properties().getCode())
                .setText(Pages.ticketCommentTemplatePage(webDriver).properties().getText());

        // Архивировать созданный шаблон
        PageHelper.otherHelper(webDriver).archivedARecordThroughScripts(templateGid, true);

        // Сравнить переменные "Ожидаемый шаблон" и "Шаблон, созданный из значений на странице"
        Assert.assertEquals("Сохранившиеся данные в шаблоне отличаются от введённых в форму",
                expectedTemplate, templateFromPage);
    }
}
