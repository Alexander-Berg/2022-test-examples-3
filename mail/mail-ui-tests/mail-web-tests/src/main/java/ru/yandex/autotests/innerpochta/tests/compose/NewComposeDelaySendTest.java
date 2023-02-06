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
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import java.time.LocalDateTime;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Отложенная отправка")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
public class NewComposeDelaySendTest extends BaseTest {

  private String msg_body = getRandomString();
  private String sbj = getRandomString();
  private LocalDateTime now = LocalDateTime.now();

  private static String DELAY_TOMORROW_TEXT = "завтра в 12:00";
  private static String TOMORROW = "Завтра";

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
    user.loginSteps().forAcc(lock.firstAcc()).logins();
  }

  @Test
  @Title("Отправляем письмо с отложенной отправкой на завтра")
  @TestCaseId("5933")
  public void shouldSendLetterWithDelayForTomorrow() {
    user.composeSteps().openAndFillComposePopup(lock.firstAcc().getSelfEmail(), sbj, msg_body);
    user.defaultSteps().clicksOn(onComposePopup().expandedPopup().delaySendBtn())
            .shouldSee(onComposePopup().expandedPopup().delaySendPopup())
            .clicksOnElementWithText(onComposePopup().expandedPopup().delaySendPopup().options(), TOMORROW)
            .shouldContainText(onComposePopup().expandedPopup().sendBtnDelayTxt(), DELAY_TOMORROW_TEXT)
            .shouldSee(onComposePopup().expandedPopup().templatesNotif())
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
    checkDelaySendLetter();
  }

  @Test
  @Title("Отправляем письмо с отложенной отправкой через календарь")
  @TestCaseId("5939")
  public void shouldSendLetterWithDelayFromCalendar() {
    user.composeSteps().openAndFillComposePopup(lock.firstAcc().getSelfEmail(), sbj, msg_body);
    user.defaultSteps().clicksOn(onComposePopup().expandedPopup().delaySendBtn())
            .clicksOn(onComposePopup().expandedPopup().delaySendPopup().sendTimeDate())
            .shouldSee(onComposePopup().expandedPopup().calendar())
            .clicksOn(onComposePopup().expandedPopup().calendar().calendarDates().get(0))
            .clicksOn(onComposePopup().expandedPopup().calendar().saveBtn())
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
    checkDelaySendLetter();
  }

  @Test
  @Title("Сбрасываем пресет отложенной отправки")
  @TestCaseId("5855")
  public void shouldSendResetDelaySend() {
    user.composeSteps().openAndFillComposePopup(lock.firstAcc().getSelfEmail(), sbj, msg_body);
    user.defaultSteps().clicksOn(onComposePopup().expandedPopup().delaySendBtn())
            .clicksOn(onComposePopup().expandedPopup().delaySendPopup().options().get(0))
            .clicksOn(onComposePopup().expandedPopup().delaySendBtn())
            .clicksOn(onComposePopup().expandedPopup().delaySendPopup().reset())
            .shouldNotSee(onComposePopup().expandedPopup().templatesNotif())
            .shouldContainText(onComposePopup().expandedPopup().sendBtn(), "Отправить");
  }

  @Step("Проверка письма с отложенной отправкой")
  private void checkDelaySendLetter() {
    user.defaultSteps().opensFragment(QuickFragments.SENT);
    user.messagesSteps().shouldNotSeeMessageWithSubject(sbj);
    user.defaultSteps().opensFragment(QuickFragments.OUTBOX);
    user.messagesSteps().shouldSeeMessageWithSubject(sbj);
    user.defaultSteps().shouldSee(onMessagePage().displayedMessages().list().get(0).delayedBadge());
  }

}
