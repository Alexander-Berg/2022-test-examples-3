package ui_tests.src.test.java.tests.testsTickets;

import entity.Entity;
import interfaces.other.InfoTest;
import interfaces.testPriorities.Minor;
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

import java.util.Arrays;
import java.util.List;

public class TestsCategories {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsCategories.class);

    @Test
    @InfoTest(descriptionTest = "Очистка всех чекбоксов в инпуте категорий",
            linkFromTestCaseAutoTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-1312",
            linkFromTestCaseSanityTest = "https://testpalm2.yandex-team.ru/testcase/ocrm-836")
    @Category(Minor.class)
    public void ocrm7111_ClearFieldCategories() {
        String gid = PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.of('ticket$b2b').withFilters{\n" +
                        "  not(eq('categories',null))\n" +
                        "}\n" +
                        ".limit(1)\n" +
                        ".get()");

        Pages.navigate(webDriver).openPageByMetaClassAndID(gid + "/edit");

        Entity.properties(webDriver).clearPropertiesOfMultiSuggestTypeField("", "categories");

        Pages.ticketPage(webDriver).header().clickOnSaveTicketButton();
        byte countCategory = Byte.parseByte(PageHelper.otherHelper(webDriver).runningScriptFromAdministrationPage(
                "api.db.'" + gid + "'.categories.size()"
        ));
        Assert.assertEquals("Поле с чекбоксами вручную не очистилось ", 0, countCategory);
    }
}
