package tests;

import beforeAndAfter.BeforeAndAfter;
import io.github.artsok.RepeatedIfExceptionsTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import pageHelpers.LeadPageHelper;
import pages.*;
import tools.Const;
import tools.Date;
import tools.Elements;
import tools.Script;

public class LeadTest {

    private static final int retryCount = Const.retryCount;
    private static WebDriver driver;

    @BeforeEach
    void before() {
        driver = BeforeAndAfter.beforeEach();
    }


    @AfterEach
    void after() {
        BeforeAndAfter.afterEach(driver);
    }

    @RepeatedIfExceptionsTest(repeats = retryCount)
    public void id2_ChangeLeadStatus() throws InterruptedException {
        LeadPageHelper.createNewLead(driver);
        Elements.moveToElementAndClickByXpath(LeadPage.appointedStatusOnStatusBar, driver);
        Elements.moveToElementAndClickByXpath(LeadPage.activeSelectedStatusButton, driver);
        Thread.sleep(5000);
        Assertions.assertEquals("Распределены", LeadPageHelper.getCurrentStatusFromCard(driver), "Текущий статус не равен ожидаемому");
    }

    @RepeatedIfExceptionsTest(repeats = retryCount)
    public void id4_LostLead() throws InterruptedException {
        LeadPageHelper.createNewLead(driver);
        Elements.clickElementByXpath(LeadPage.editLostReasonButton, driver);
        Script.scrollPageDown(driver);
        Elements.selectFromPickList(LeadPage.lostReasonPicklist, LeadPage.defaultReason, driver);
        Elements.selectFromPickList(LeadPage.lostExtendedReasonPicklist, LeadPage.defaultExtendedReason, driver);
        Elements.clickElementByXpath(EditPage.saveButtonXpath, driver);
        Elements.moveToElementAndClickByXpath(LeadPage.lostStatusOnStatusBar, driver);
        Elements.moveToElementAndClickByXpath(LeadPage.activeSelectedStatusButton, driver);
        Thread.sleep(5000);
        Assertions.assertEquals("Дисквалифицирован", LeadPageHelper.getCurrentStatusFromCard(driver), "Текущий статус не равен Дисквалифицирован");

    }

    @RepeatedIfExceptionsTest(repeats = retryCount)
    public void id5_CreateMailCaseFromLeadWithDefaultContact() throws InterruptedException {
        String caseTitle = "MailCase" + Date.getCurrentDateTime();

        LeadPageHelper.createNewLead(driver);

        Elements.clickElementByXpath(LeadPage.createMailCaseButton, driver);
        Elements.getElementInModalWindowByXpath(CreateCasePage.caseTheme, driver).sendKeys(caseTitle);
        Elements.clickElementInModalWindowByXpath(CreateCasePage.saveButtonInModal, driver);
        Thread.sleep(3000);
        Elements.clickElementByXpath(LeadPage.communicationTab, driver);
        Elements.clickLinkByTitle(caseTitle, driver);
        Elements.moveToElementAndClickByXpath(CasePage.emailBlockActivateButton, driver);

        Assertions.assertEquals(
                "test@yandex.ru",
                Elements.getElementByXpath(CasePage.emailBlockAdress, driver).getText(),
                "Email в поле 'Кому' не соответствует ожидаемому"
        );
    }

    @RepeatedIfExceptionsTest(repeats = retryCount)
    public void id6_CreateMailCaseFromLeadWithCustomContact() throws InterruptedException {
        String caseTitle = "MailCase" + Date.getCurrentDateTime();
        String testContactTitle = "autotest_id6";
        String testContactEmail = "autotest@id6.ru";

        LeadPageHelper.createNewLead(driver);

        Elements.clickElementByXpath(LeadPage.createMailCaseButton, driver);
        Elements.getElementInModalWindowByXpath(CreateCasePage.caseContact, driver).sendKeys(testContactTitle);
        Elements.clickElementInModalWindowByTitle(testContactTitle, driver);
        Elements.getElementInModalWindowByXpath(CreateCasePage.caseTheme, driver).sendKeys(caseTitle);
        Elements.clickElementInModalWindowByXpath(CreateCasePage.saveButtonInModal, driver);
        Thread.sleep(3000);
        Elements.clickElementByXpath(LeadPage.communicationTab, driver);
        Elements.clickLinkByTitle(caseTitle, driver);
        Elements.moveToElementAndClickByXpath(CasePage.emailBlockActivateButton, driver);

        Assertions.assertEquals(
                testContactEmail,
                Elements.getElementByXpath(CasePage.emailBlockAdress, driver).getText(),
                "Email в поле 'Кому' не соответствует ожидаемому"
        );

    }

    @RepeatedIfExceptionsTest(repeats = retryCount)
    public void id7_CreateTaskFromLead() throws InterruptedException {
        String taskName = Date.getCurrentDateTime();
        String taskNameAfterCreate = "Задача" + taskName;
        String comment = "Comment " + Date.getCurrentDateTime();

        LeadPageHelper.createNewLead(driver);

        Elements.moveToLinkAndClickByTitle("Задача", driver);
        Elements.moveAndSelectFromPickList(LeadPage.taskTypePickList, LeadPage.qualifiedTaskType, driver);
        Elements.getElementByXpath(LeadPage.taskNameField, driver).sendKeys(taskName);
        Elements.getElementByXpath(LeadPage.actionCommentField, driver).sendKeys(comment);
        Elements.moveToElementAndClickByXpath(LeadPage.saveActionButton, driver);
        Elements.moveToLinkAndClickByTitle(taskNameAfterCreate, driver);
        Assertions.assertEquals(
                comment, Elements.getElementByXpath(TaskPage.comment, driver).getText(),
                "Ожидаемый комментарий не соответствует комментарию со страницы задачи");

    }

    @RepeatedIfExceptionsTest(repeats = retryCount)
    public void id8_CreateNoteFromLead() throws InterruptedException {
        String comment = "Comment " + Date.getCurrentDateTime();

        LeadPageHelper.createNewLead(driver);

        //У поля ввода нет нормального локатора, так что активируем его через переключение вкладок
        Elements.moveToLinkAndClickByTitle("Задача", driver);
        Elements.moveToLinkAndClickByTitle("Заметка", driver);
        Elements.getElementByXpath(LeadPage.actionCommentField, driver).sendKeys(comment);
        Elements.moveToElementAndClickByXpath(LeadPage.saveActionButton, driver);
        Elements.moveToElementAndClickByXpath(LeadPage.noteExpandButton, driver);

        Assertions.assertEquals(comment, Elements.getElementByXpath(LeadPage.noteDescription, driver).getText(), "Описание заметки не соответствует ожидаемому");
    }

}
