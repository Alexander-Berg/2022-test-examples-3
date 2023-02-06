package ru.yandex.autotests.innerpochta.tests.main;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TIMELINE_COLLAPSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TIMELINE_ENABLE;

/**
 * @author yaroslavna
 */
@Aqua.Test
@Title("Тесты на события в таймлайне")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.TIMELINE)
public class TimelineTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth);

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем и разворачиваем таймлайн",
            of(TIMELINE_ENABLE, TRUE, TIMELINE_COLLAPSE, TRUE)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Должны видеть тултип события по ховеру")
    @TestCaseId("2435")
    public void shouldSeeEventTooltip() {
        user.defaultSteps()
            .shouldSee(onMessagePage().timelineBlock())
            .shouldNotSee(onMessagePage().timelineEventTooltip())
            .onMouseHover(
                user.defaultSteps().shouldSeeElementFromListInViewport(onMessagePage().timelineBlock().events())
            )
            .shouldSee(onMessagePage().timelineEventTooltip());
    }
}