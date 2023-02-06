package ru.yandex.autotests.innerpochta.tests.advertisement;


import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
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
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ENABLED_ADV;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Тесты на рекламу")
@Features(FeaturesConst.ADVERTISEMENT)
@Tag(FeaturesConst.ADVERTISEMENT)
@RunWith(DataProviderRunner.class)
public class AdvertisementTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем рекламу",
            of(
                SHOW_ADVERTISEMENT, ENABLED_ADV
            )
        );
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Показываем рекламу в промо на Done")
    @TestCaseId("2534")
    public void shouldSeeAdOnDonePromo() {
        user.advertisementSteps().goToDone();
        user.defaultSteps().shouldSee(onMessagePage().directDone());
    }

    @Test
    @Title("Показывем рекламу в контактах")
    @TestCaseId("2533")
    public void shouldSeeAdInContacts() {
        user.defaultSteps().opensFragment(QuickFragments.CONTACTS)
            .shouldSee(
                user.pages().MessagePage().directLine(),
                user.pages().MessagePage().directLeft()
            );
    }

    @Test
    @Title("Показываем рекламу в компактной шапке и компактном списке писем")
    @TestCaseId("3635")
    public void shouldSeeAdInCompactView() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем компактную шапку и компактный список писем",
            of(
                LIZA_MINIFIED_HEADER, STATUS_ON,
                LIZA_MINIFIED, STATUS_ON
            )
        );
        user.advertisementSteps().shouldSeeAllAd();
        user.composeSteps().goToDone(lock.firstAcc().getSelfEmail());
        user.defaultSteps().shouldSee(user.pages().MessagePage().directDone());
    }

    @Test
    @Title("В компактной ЛК нет рекламы")
    @TestCaseId("3637")
    public void shouldNotSeeAdvertInCompactLeftPanel() {
        user.defaultSteps().clicksOn(onMessagePage().toolbar().layoutSwitchBtn())
            .turnTrue(onMessagePage().layoutSwitchDropdown().compactLeftColumnSwitch())
            .shouldSee(onMessagePage().compactLeftPanel())
            .shouldNotSee(onHomePage().advertiseBanner())
            .clicksOn(onMessagePage().toolbar().layoutSwitchBtn())
            .deselects(onMessagePage().layoutSwitchDropdown().compactLeftColumnSwitch())
            .shouldNotSee(onMessagePage().compactLeftPanel())
            .shouldSee(onHomePage().advertiseBanner());
    }

    @Test
    @Title("Показывем рекламу в просмотре письма на отдельной странице")
    @TestCaseId("4306")
    public void shouldSeeAdInMessageView() {
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().shouldSee(user.pages().MessagePage().directLine());
    }
}
