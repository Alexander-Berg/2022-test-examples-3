package ru.yandex.autotests.innerpochta.tests.compose;

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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import static ru.yandex.autotests.innerpochta.util.MailConst.BIG_SIZE;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;


/**
 * Created by eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Тест на алерты")
@Description("Тесты на композ")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.HEAD)
public class NewComposeAlertsTest extends BaseTest {

  private static final String DESCRIPTION_TOO_BIG_ATTACH = "Письмо не может быть отправлено: превышено количество" +
          " пересылаемых писем или их суммарный объём. Вы можете одновременно пересылать не более 30 писем или письма" +
          " суммарным объёмом не более 24 МБ.";
  private static final String TITLE = "Письмо не отправлено";

  private AccLockRule lock = AccLockRule.use().useTusAccount();
  private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
  private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

  private String subj2 = getRandomString();

  @ClassRule
  public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

  @Rule
  public RuleChain rules = RuleChain.outerRule(lock)
          .around(auth)
          .around(clearAcc(() -> user));

  @Before
  public void setUp() {
    user.apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            lock.firstAcc().getSelfEmail(),
            subj2,
            getRandomString(),
            BIG_SIZE
    );
    user.loginSteps().forAcc(lock.firstAcc()).logins();
  }

  @Test
  @Title("Проверяем появление алерта при пересылке письма с большим аттачем")
  @TestCaseId("4753")
  public void shouldSeeTooBigAttachAlert() {
    user.messagesSteps().selectMessageWithSubject(subj2);
    user.defaultSteps().clicksOn(onMessageView().toolbar().forwardButton())
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
            .clicksOn(onComposePopup().expandedPopup().sendBtn())
            .shouldContainText(onComposePopup().confirmClosePopup().title(), TITLE)
            .shouldContainText(onComposePopup().confirmClosePopup().description(), DESCRIPTION_TOO_BIG_ATTACH);
  }
}
