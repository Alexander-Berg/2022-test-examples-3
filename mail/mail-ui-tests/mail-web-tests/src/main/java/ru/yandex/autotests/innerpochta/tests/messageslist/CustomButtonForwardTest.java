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
@Title("Тест на создание кнопки для переадресации")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.CUSTOM_BUTTONS)
public class CustomButtonForwardTest extends BaseTest {

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
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .removesToolbarCustomButton(onCustomButtons().overview().deleteForwardButton());
        user.defaultSteps().shouldNotSee(onMessagePage().toolbar().sendOnButtonIcon())
            .onMouseHoverAndClick(onMessagePage().toolbar().configureCustomButtons());
    }

    @Test
    @Title("Создание кнопки для переадресации")
    @TestCaseId("1493")
    public void testCreateCustomForwardButton() {
        user.defaultSteps().clicksOn(onCustomButtons().overview().forward())
            .inputsTextInElement(
                onCustomButtons().configureForwardButton().emailInput(),
                lock.firstAcc().getSelfEmail()
            )
            .clicksOn(onMessagePage().sudgest())
            .clicksOn(onCustomButtons().configureForwardButton().saveButton())
            .clicksOn(onCustomButtons().overview().saveChangesButton());
        user.messagesSteps().selectMessageWithSubject(subject);
        user.defaultSteps().shouldSeeThatElementTextEquals(
            onMessagePage().toolbar().sendOnButton(),
            lock.firstAcc().getSelfEmail()
        )
            .clicksOn(onMessagePage().toolbar().sendOnButton());
        user.messagesSteps().shouldSeeMessageWithSubjectAndPrefix(subject);
    }
}
