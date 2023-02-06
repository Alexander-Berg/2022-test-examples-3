package ui_tests.src.test.java.tests;

import interfaces.other.InfoTest;
import interfaces.testPriorities.Blocker;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pageHelpers.PageHelper;
import pages.Pages;
import rules.BeforeClassRules;
import rules.CustomRuleChain;
import tools.Tools;
import unit.Config;

/**
 * Функциональность Распределение обращений
 */
public class TestsDistributionTicket {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsDistributionTicket.class);

    @InfoTest(descriptionTest = "Проверка вывода ссылки на обращение назначенное на оператора через Play-режим",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-623",
            requireYouToLogInUnderANewUser = true)
    @Category({Blocker.class})
    @Test
    public void ocrm623_ChangeViewLinkFromTicketInWork() {

        // Переходим в статус "готов"
        PageHelper.mainMenuHelper(webDriver).switchUserToStatus("Готов");
        // Ожидаем, пока статус изменится
        Tools.waitElement(webDriver).waitInvisibilityElementTheTime(By.xpath("//*[@class='_1nYbmkbT' and @title='Не готов']"), Config.DEF_TIME_WAIT_LOAD_PAGE);

        // Получаем ссылку на обращение, которое в работе
        String actualLinkOnTicketInWork = webDriver.getCurrentUrl();
        // Получаем ссылку на обращение из основного меню
        String linkOnTicketInWork = PageHelper.mainMenuHelper(webDriver).getLinkOnTicketFromTicketInWork();

        Assert.assertEquals("Ссылка на обращение в работе выводится не корректно. Должны вывести ссылку " + actualLinkOnTicketInWork + " а вывели ссылку " + linkOnTicketInWork, linkOnTicketInWork, actualLinkOnTicketInWork);
    }
}
