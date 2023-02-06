package ru.yandex.autotests.innerpochta.tests.filters;


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

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_FILTER_NOTIFICATION;

@Aqua.Test
@Title("Тест на создание фильтра при перемещении письма в папку")
@Features(FeaturesConst.FILTERS)
@Tag(FeaturesConst.FILTERS)
@Stories(FeaturesConst.GENERAL)
public class CreateFilterWhenMoveMsgToFolder extends BaseTest {

    private static final String CONDITION_PATTERN_WITHOUT_SUBJECT = "Если\n«От кого» совпадает c «%s»";
    private static final String ACTION_PATTERN = "— переместить письмо в папку «%s»";

    private String subject;
    private String folder = Utils.getRandomString();

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws IOException {
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.apiFoldersSteps().createNewFolder(folder);
        user.apiSettingsSteps().callWithListAndParams(
            "Сбрасываем настройку показа нотификации о создании фильтра",
            of(SHOW_FILTER_NOTIFICATION, EMPTY_STR)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Тест на создание фильтра из статуслайна при перемещении письма в папку")
    @TestCaseId("1301")
    public void testCreateFilterFromDragNDrop() {
        user.messagesSteps().clicksOnMessageCheckBoxWithSubject(subject);
        user.defaultSteps().clicksOn(onMessagePage().toolbar().moveMessageDropDown())
            .clicksOnElementWithText(onMessagePage().moveMessageDropdownMenu().customFolders(), folder)
            .clicksOn(onMessagePage().statusLineBlock().createFilterBtn())
            .opensFragment(QuickFragments.SETTINGS_FILTERS);
        user.filtersSteps().shouldSeeSelectedConditionInFilter(
            String.format(CONDITION_PATTERN_WITHOUT_SUBJECT, lock.firstAcc().getSelfEmail())
        )
            .shouldSeeSelectedActionInFilter(String.format(ACTION_PATTERN, folder));
    }
}
