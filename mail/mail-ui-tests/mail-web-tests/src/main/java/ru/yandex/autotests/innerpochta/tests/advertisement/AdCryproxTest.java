package ru.yandex.autotests.innerpochta.tests.advertisement;


import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.AD_CRYPROX;
import static ru.yandex.autotests.innerpochta.util.MailConst.OLD_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ENABLED_ADV;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_DONE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_PAGE_AFTER_SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Тесты на рекламу под блокировщиком")
@Features(FeaturesConst.ADVERTISEMENT)
@Tag(FeaturesConst.ADVERTISEMENT)
public class AdCryproxTest extends BaseTest {

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
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensDefaultUrlWithPostFix(AD_CRYPROX);
    }

    @Test
    @Title("Должны видеть блок рекламы в левой колонке под блокировщиком")
    @TestCaseId("2535")
    public void shouldSeeAdWithCryproxLeft() {
        user.defaultSteps().shouldSee(user.pages().MessagePage().directLeftCryprox());
        shouldSeeThatElementHasNotEmptySize(user.pages().MessagePage().directLeftCryprox());
    }

    @Test
    @Title("Должны видеть блок рекламы над списком писем под блокировщиком")
    @TestCaseId("5144")
    public void shouldSeeAdWithCryproxLine() {
        user.defaultSteps().shouldSee(user.pages().MessagePage().directLineCryprox());
        shouldSeeThatElementHasNotEmptySize(user.pages().MessagePage().directLineCryprox());
    }

    @Test
    @Title("Должны видеть блок рекламы на промо на Done под блокировщиком")
    @TestCaseId("5145")
    public void shouldSeeAdWithCryproxDone() {
        user.advertisementSteps().goToDone();
        user.defaultSteps().shouldSee(user.pages().MessagePage().directDone());
        shouldSeeThatElementHasNotEmptySize(user.pages().MessagePage().directDone());
    }

    @Test
    @Title("Должны видеть блоки рекламы в адресной книге под блокировщиком")
    @TestCaseId("4290")
    public void shouldSeeAdWithCryproxContacts() {
        user.defaultSteps().opensFragment(QuickFragments.CONTACTS)
            .shouldSee(
                user.pages().MessagePage().directLeftCryproxContacts(),
                user.pages().MessagePage().directLineCryprox()
            );
        shouldSeeThatElementHasNotEmptySize(user.pages().MessagePage().directLeftCryproxContacts());
        shouldSeeThatElementHasNotEmptySize(user.pages().MessagePage().directLineCryprox());
    }

    @Step("Размеры блока элемента «{0}» должны быть больше 0")
    public void shouldSeeThatElementHasNotEmptySize(MailElement element) {
        System.out.println(element.getAttribute("id"));
        System.out.println(element.getSize());
        assertThat("Не содержит нужный текст", element.getSize().height, not(0));
        assertThat("Не содержит нужный текст", element.getSize().width, not(0));
    }
}
