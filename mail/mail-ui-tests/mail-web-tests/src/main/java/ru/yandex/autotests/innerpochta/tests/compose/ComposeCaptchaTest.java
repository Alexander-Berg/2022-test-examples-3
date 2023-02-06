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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * Created by mabelpines on 16.03.15.
 */

@Aqua.Test
@Title("Новый композ - Проверка капчи при отправке письма")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
public class ComposeCaptchaTest extends BaseTest {

    private static final String MESSAGE_BODY = "XJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-STANDARD-ANTI-UBE-TEST-EMAIL*C.34X";
    private String subject = Utils.getRandomName();

    public AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setup() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        String mid = user.apiMessagesSteps().prepareDraft(
            lock.firstAcc().getSelfEmail(),
            subject, MESSAGE_BODY
        );
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE_MSG_FRAGMENT.fragment(mid));
    }

    @Test
    @Title("Тест на проверку капчи при отправке писма")
    @TestCaseId("1208")
    public void composeCaptcha() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn())
            .shouldSee(
                onComposePopup().captchaPopup().captchaForm(),
                onComposePopup().captchaPopup().closeBtn(),
                onComposePopup().captchaPopup().sendBtn(),
                onComposePopup().captchaPopup().captchaInput(),
                onComposePopup().captchaPopup().captchaImg(),
                onComposePopup().captchaPopup().refreshCaptcha()
            );
    }
}
