package ru.yandex.autotests.innerpochta.tests.autotests.Advertisement;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDD_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT_TOUCH;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на рекламу")
@Features(FeaturesConst.ADVERTISEMENT)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AdvertisementPddTest {

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount(PDD_USER_TAG));
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();
    private AddFolderIfNeedRule addFolder = AddFolderIfNeedRule.addFolderIfNeed(() -> steps.user());

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(addFolder);

    @Before
    public void prep() {
        steps.user().apiMessagesSteps().sendMailWithNoSaveWithoutCheck(
            accLock.firstAcc().getSelfEmail(),
            getRandomString(),
            ""
        );
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем показ рекламы",
            of(SHOW_ADVERTISEMENT_TOUCH, TRUE)
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Показываем рекламу в ПДД")
    @TestCaseId("618")
    public void shouldSeeAdvertInPDD() {
        steps.user().defaultSteps().opensDefaultUrl().shouldSee(steps.pages().touch().messageList().advertisement());
    }
}
