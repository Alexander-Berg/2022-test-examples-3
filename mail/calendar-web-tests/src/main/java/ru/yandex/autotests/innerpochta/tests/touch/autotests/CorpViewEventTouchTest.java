package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ru.yandex.autotests.innerpochta.cal.rules.AddLayerIfNeedRule.addLayerIfNeed;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("[Тач][Корп] Отображение занятости переговорок/участников")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.CORP)
public class CorpViewEventTouchTest {

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().names(CREDS, OTHER_USER_CREDS));
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock();

    private static final String CREDS = "CorpViewEventTest";
    private static final String OTHER_USER_CREDS = "CorpViewEventTestDifferent";
    private static final String NIGHT_TIME = "T23:00:00";
    private String startDate;
    private String date;

    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(addLayerIfNeed(() -> steps.user()));

    @Before
    public void setUp() {
        steps.user().loginSteps().forAcc(lock.firstAcc()).loginsToCorp();
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
        date = dateFormat.format(LocalDateTime.now());
        startDate = date + NIGHT_TIME;
        Long layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(getRandomName())
            .withStartTs(date + "T00:00:00")
            .withEndTs(date + "T23:00:00");
        steps.user().apiCalSettingsSteps().createNewEvent(event);
        steps.user().loginSteps().forAcc(lock.acc(OTHER_USER_CREDS)).loginsToCorp();
    }

    @Test
    @Title("Отображаем занятость участников в ябблах и в саджесте")
    @TestCaseId("1213")
    public void shouldSeeBusyMembers() {
        steps.user().defaultSteps().clicksOn(
            steps.pages().cal().touchHome().addEventButton(),
            steps.user().pages().calTouch().eventPage().changeParticipants(),
            steps.user().pages().calTouch().editParticipantsPage().input()
        )
            .inputsTextInElement(
                steps.user().pages().calTouch().editParticipantsPage().input(),
                lock.acc(CREDS).getSelfEmail()
            )
            .clicksOn(steps.pages().cal().touchHome().editParticipantsPage().busyMember())
            .clicksOn(steps.pages().cal().touchHome().editParticipantsPage().save())
            .shouldSee(steps.pages().cal().touchHome().eventPage().busyMember());
        steps.user().calTouchCreateEventSteps().setDate(
            steps.pages().cal().touchHome().eventPage().startDateInput(),
            startDate
        );
        steps.user().defaultSteps().shouldSee(steps.pages().cal().touchHome().eventPage().freeMember());
    }
}
