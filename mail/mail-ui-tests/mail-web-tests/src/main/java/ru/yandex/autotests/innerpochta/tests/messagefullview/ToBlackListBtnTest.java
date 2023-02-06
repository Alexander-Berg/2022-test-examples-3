package ru.yandex.autotests.innerpochta.tests.messagefullview;

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

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * Created by mabelpines on 14.10.15.
 */

@Aqua.Test
@Title("Проверяем кнопку «В черный список» в карточке контакта в просмотре письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.CONTACT_CARD)
public class ToBlackListBtnTest extends BaseTest {

    private static final String MSG_SUBJ = "testLetter";
    private static final String SENDER_EMAIL = "SenderBot";
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final AccLockRule lock2 = AccLockRule.use().names(SENDER_EMAIL);
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock2);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(lock2)
        .around(auth)
        .around(auth2)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiMessagesSteps().withAuth(auth2).sendMail(
            lock.firstAcc().getSelfEmail(),
            MSG_SUBJ,
            Utils.getRandomString()
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Проверяем кнопку «В черный список».")
    @TestCaseId("2045")
    public void shouldAddContactToBlacklist() {
        user.messagesSteps().clicksOnMessageWithSubject(MSG_SUBJ);
        user.defaultSteps().clicksOn(onMessageView().messageHead().fromName())
            .shouldSee(onMessageView().mailCard())
            .clicksOn(onMessageView().mailCard().toBlackListBtn())
            .shouldSee(onMessagePage().statusLineBlock().textBox());
        user.apiFiltersSteps().shouldContainAdressInBlackList(lock2.firstAcc().getSelfEmail());
    }
}
