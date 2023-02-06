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

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на создание кнопки для установки новой метки")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class LabelCustomButtonForNewLabelTest extends BaseTest {

    private static final String NEW_LABEl = "Новая метка…";

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
        subject = user.apiMessagesSteps().sendMail(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Создание пользовательской кнопки для установки новой метки")
    @TestCaseId("1517")
    public void testCreateAutoLabelButtonForNewLabel() {
        /* Ждем, пока заработает «Шестеренка редактирования пользовательских кнопок» */
        user.defaultSteps().waitInSeconds(2)
            .onMouseHoverAndClick(onMessagePage().toolbar().configureCustomButtons())
            .clicksOn(onCustomButtons().overview().label())
            .selectsOption(onCustomButtons().configureLabelButton().labelSelect(), NEW_LABEl);
        String label = Utils.getRandomString();
        user.defaultSteps().inputsTextInElement(onCustomButtons().configureLabelButton().labelNameInput(), label);
        String color = user.messagesSteps().selectsRandomColorForLabel();
        user.defaultSteps().clicksOn(onCustomButtons().configureLabelButton().saveButton())
            /* ждем, когда закончится анимация смены блока */
            .waitInSeconds(1)
            .shouldNotSee(onCustomButtons().configureLabelButton())
            .clicksOn(onCustomButtons().overview().saveChangesButton())
            .shouldNotSee(onCustomButtons().overview().saveChangesButton());
        user.messagesSteps().selectMessageWithSubject(subject);
        user.defaultSteps().shouldSeeThatElementTextEquals(onMessagePage().toolbar().autoLabelButton(), label)
            .clicksOn(onMessagePage().toolbar().autoLabelButton());
        user.messagesSteps().shouldSeeThatMessageIsLabeledWith(label, subject);
        user.messagesSteps().shouldSeeThatLabelOnFirstMessageHasColor(color);
    }
}
