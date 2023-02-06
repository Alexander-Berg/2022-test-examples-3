package ru.yandex.autotests.innerpochta.tests.compose;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.steps.DefaultSteps.sign;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Подписи")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
public class NewComposeSignatureTest extends BaseTest {

  private String sign_1 = getRandomString();
  private String sign_2 = getRandomString();
  private String sbj = getRandomString();

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
  public void logIn() {
    user.apiSettingsSteps().changeSignsWithTextAndAmount(
            sign(sign_1),
            sign(sign_2)
    );
    user.apiSettingsSteps().callWithListAndParams(
            "Выключаем треды у пользователя",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
    );
    user.loginSteps().forAcc(lock.firstAcc()).logins();
  }

  @Test
  @Title("Меняем подпись в композе и отправляем письмо")
  @TestCaseId("5895")
  public void shouldSendLetterWithChangedSignature() {
    user.defaultSteps().clicksOn(onHomePage().composeButton())
            .shouldContainText(onComposePopup().signatureBlock(), sign_2)
            .onMouseHover(onComposePopup().signatureBlock())
            .clicksOn(onComposePopup().signatureChooser())
            .clicksOn(onComposePopup().signaturesPopup().signaturesList().get(2))
            .shouldNotSee(onComposePopup().signaturesPopup())
            .shouldContainText(onComposePopup().signatureBlock(), sign_1)
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), sbj)
            .clicksOn(onComposePopup().expandedPopup().sendBtn())
            .opensFragment(QuickFragments.INBOX);
    user.messagesSteps().clicksOnMessageWithSubject(sbj);
    user.defaultSteps().shouldContainText(onMessageView().messageTextBlock(), sign_1);
  }

}
