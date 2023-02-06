package ru.yandex.autotests.innerpochta.tests.settings;

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

import static com.google.common.collect.ImmutableBiMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.steps.DefaultSteps.sign;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SENDER_CHANGE_SIGN_IN_COMPOSE;

@Aqua.Test
@Title("Изменение подписи отправителя")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SenderChangeSignatureTest extends BaseTest {

    private static final String SECOND_USER_SIGNATURE = "Second added signature";
    private static final String FIRST_USER_SIGNATURE = "First added signature.user";

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
        user.apiSettingsSteps()
            .changeSignsWithTextAndAmount(sign(FIRST_USER_SIGNATURE), sign(SECOND_USER_SIGNATURE))
            .callWithListAndParams(
                "Включаем выбор подписи на странице написания письма",
                of(SETTINGS_SENDER_CHANGE_SIGN_IN_COMPOSE, EMPTY_STR)
            );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.COMPOSE);
    }

    @Test
    @Title("Меняем подпись из композа")
    @TestCaseId("1845")
    public void changeSignatureFromCompose() {
        user.defaultSteps()
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), SECOND_USER_SIGNATURE)
            .onMouseHover(onComposePopup().signatureBlock())
            .clicksOn(onComposePopup().signatureChooser())
            .shouldSee(onComposePopup().signaturesPopup())
            .clicksOn(onComposePopup().signaturesPopup().signaturesList().get(2))
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), FIRST_USER_SIGNATURE)
            .shouldNotContainText(onComposePopup().expandedPopup().bodyInput(), SECOND_USER_SIGNATURE);
    }

}
