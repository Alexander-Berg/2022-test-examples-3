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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 07.09.12
 * Time: 15:47
 */

@Aqua.Test
@Title("Тест на создание фильтра")
@Features(FeaturesConst.FILTERS)
@Tag(FeaturesConst.FILTERS)
@Stories(FeaturesConst.GENERAL)
public class FiltersStoryCreateNewFilterTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    private String ifCondition;

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws IOException {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FILTERS);
        user.defaultSteps().clicksOn(onFiltersOverview().createNewFilterButton());
        ifCondition = user.filtersSteps().chooseRandomIfConditionForNewFilter();
    }

    @Test
    @Title("Тест на создание фильтра на удаление писем")
    @TestCaseId("1308")
    public void testCreateNewFilterForDeletingMail() {
        user.defaultSteps().turnTrue(onFiltersCreationPage().setupFiltersCreate().blockSelectAction().deleteCheckBox());
        user.filtersSteps().submitsFilter(lock.firstAcc())
            .shouldSeeSelectedConditionInFilter(ifCondition)
            .shouldSeeSelectedActionInFilter("— удалить письмо");
    }

    @Test
    @Title("Тест на создание фильтра на то, как помечать письма прочитанными")
    @TestCaseId("1309")
    public void testCreateNewFilterForMarkAsRead() {
        user.defaultSteps().deselects(onFiltersCreationPage().setupFiltersCreate().blockSelectAction()
                .moveToFolderCheckBox())
            .turnTrue(onFiltersCreationPage().setupFiltersCreate().blockSelectAction().markAsReadCheckBox());
        user.filtersSteps().submitsFilter(lock.firstAcc())
            .shouldSeeSelectedConditionInFilter(ifCondition)
            .shouldSeeSelectedActionInFilter("— пометить письмо как прочитанное");
    }

    @Test
    @Title("Тест на создание фильтра для пользовательской метки")
    @TestCaseId("1310")
    public void testCreateNewFilterForNewMark() {
        String mark = user.filtersSteps().chooseToPutRandomlyCreatedMark();
        user.filtersSteps().submitsFilter(lock.firstAcc())
            .shouldSeeSelectedConditionInFilter(ifCondition)
            .shouldSeeSelectedActionInFilter("— пометить письмо меткой «" + mark + "»");

    }

    @Test
    @Title("Тест на создание фильтра для пользовательской папки")
    @TestCaseId("1311")
    public void testCreateNewFilterForNewFolder() {
        String folder = user.filtersSteps().chooseToMoveInRandomlyCreatedFolder();
        user.filtersSteps().submitsFilter(lock.firstAcc())
            .shouldSeeSelectedConditionInFilter(ifCondition)
            .shouldSeeSelectedActionInFilter("— переместить письмо в папку «" + folder + "»");
    }

    @Test
    @Title("Тест на создание фильтра для ответа с текстом")
    @TestCaseId("1312")
    public void testCreateNewFilterForReplyingWithText() {
        user.filtersSteps().replyWithText("text")
            .submitsFilter(lock.firstAcc())
            .shouldSeeSelectedConditionInFilter(ifCondition)
            .shouldSeeSelectedActionInFilter("— автоматический ответ «text»");
    }

    @Test
    @Title("Отображаем письма с аттачами в правилах обработки почты")
    @TestCaseId("4752")
    public void createFilterForMsgWithAttach() {
        String msgSbj = getRandomString();
        user.defaultSteps().opensDefaultUrl();
        user.composeSteps().sendMsgWithAttach(lock.firstAcc().getSelfEmail(), msgSbj);
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FILTERS)
            .clicksOn(onFiltersOverview().createNewFilterButton())
            .clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate()
                .blockApplyConditionFor().withAttachConditionDropdown())
            .clicksOn(user.pages().SettingsPage().selectConditionDropdown().conditionsList().get(1));
        user.filtersSteps().shouldOpenFromDropdown(0);
        user.defaultSteps().clicksOn(user.pages().SettingsPage().selectConditionDropdown().conditionsList().get(4))
            .inputsTextInElement(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(0).inputCondition(), msgSbj)
            .clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().previewButton())
            .shouldSeeElementsCount(
                user.pages().FiltersCreationSettingsPage().setupFiltersCreate().previewMessagesListWithAttach(),
                2
            )
            .shouldSeeElementsCount(
                user.pages().FiltersCreationSettingsPage().setupFiltersCreate().previewMessagesList(),
                2
            );
    }
}
