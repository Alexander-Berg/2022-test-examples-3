package ru.yandex.autotests.innerpochta.tests.messageslist;


import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMPORTANT_LABEL_NAME_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на создание кнопки для установки существующей метки")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class LabelCustomButtonForExistingLabelTest extends BaseTest {

    private static final String CUSTOM_LABEL_NAME = "label";

    private String subject;
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws IOException {
        user.apiLabelsSteps().addNewLabel(CUSTOM_LABEL_NAME, LABELS_PARAM_GREEN_COLOR);
        subject = user.apiMessagesSteps().sendMail(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().removesToolbarCustomButton(onCustomButtons().overview().deleteLabelButton());
    }

    @Test
    @Title("Создание кнопки установки для существующей метки")
    @TestCaseId("1515")
    public void testCreateAutoLabelButtonForExistingLabel() {
        createsButtonForLabel(CUSTOM_LABEL_NAME);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().autoLabelButton());
        user.messagesSteps().shouldSeeThatMessageIsLabeledWith(CUSTOM_LABEL_NAME, subject);
    }

    @Test
    @Title("Создание кнопки установки метки Важное")
    @TestCaseId("1516")
    public void testCreateAutoLabelButtonForImportantLabel() {
        createsButtonForLabel(IMPORTANT_LABEL_NAME_RU);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().autoLabelButton());
        user.messagesSteps().shouldSeeThatMessageIsImportant(subject);
    }

    @Test
    @Title("Удаление кнопки с меткой из тулбара")
    @TestCaseId("3945")
    public void shouldDeleteCustomLabelButton() {
        createsButtonForLabel(IMPORTANT_LABEL_NAME_RU);
        user.messagesSteps().removesToolbarCustomButton(onCustomButtons().overview().deleteLabelButton());
        user.defaultSteps().shouldNotSee(onMessagePage().toolbar().autoLabelButton());
    }

    private void createsButtonForLabel(String label) {
        user.defaultSteps().shouldNotSee(onMessagePage().toolbar().autoLabelButtonIcon())
            .onMouseHoverAndClick(onMessagePage().toolbar().configureCustomButtons())
            .clicksOn(onCustomButtons().overview().label())
            .selectsOption(onCustomButtons().configureLabelButton().labelSelect(), label)
            .clicksOn(onCustomButtons().configureLabelButton().saveButton())
            /* ждем, когда закончится анимация смены блока */
            .waitInSeconds(1)
            .shouldNotSee(onCustomButtons().configureLabelButton())
            .clicksOn(onCustomButtons().overview().saveChangesButton());
        user.messagesSteps().selectMessageWithSubject(subject);
        user.defaultSteps().shouldSeeThatElementTextEquals(onMessagePage().toolbar().autoLabelButton(), label);
    }
}
