package ru.yandex.autotests.innerpochta.tests.contextmenu;

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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Проверяем пункт “Настроить фильтр“ для писем/тредов")
@Description("Проверяем пункт “Настроить фильтр“ для писем/тредов")
@Features(FeaturesConst.CONTEXT_MENU)
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories(FeaturesConst.GENERAL)
public class ContextFilterTest extends BaseTest {

    private static final String FROM = "От кого";
    private static final String EQUAL_TO = "совпадает c";
    private static final String SUBJECT = "Тема";
    private static final String FILTER_SETTINGS_LABEL = "Настроить фильтр";

    private String subject;

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
    public void login() {
        subject = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Создаём фильтр")
    @Description("Отправляем себе письмо, вызываем КМ, выбираем пункт “Настроить фильтр“, оказываемся на страничке" +
        "“Создать правило“. Проверяем, что поля “От кого“, “Тема“, “Переложить в папку“ заполнены корректно.")
    @TestCaseId("1235")
    public void createFilter() {
        user.messagesSteps().rightClickOnMessageWithSubject(subject)
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps()
            .clicksOnElementWithText(onMessagePage().allMenuListInMsgList().get(0).itemListInMsgList(), FILTER_SETTINGS_LABEL)
            .shouldBeOnUrl(containsString("/filters-create?message="))
            .shouldSeeThatElementTextEquals(onFiltersCreationPage().setupFiltersCreate()
                .blockCreateConditions().conditionsList().get(0).firstConditionDropDown(), FROM)
            .shouldSeeThatElementTextEquals(onFiltersCreationPage().setupFiltersCreate()
                .blockCreateConditions().conditionsList().get(0).secondConditionDropDown(), EQUAL_TO)
            .shouldContainValue(onFiltersCreationPage().setupFiltersCreate()
                .blockCreateConditions().conditionsList().get(0).inputCondition(), lock.firstAcc().getSelfEmail())
            .shouldSeeThatElementTextEquals(onFiltersCreationPage().setupFiltersCreate()
                .blockCreateConditions().conditionsList().get(1).firstConditionDropDown(), SUBJECT)
            .shouldSeeThatElementTextEquals(onFiltersCreationPage().setupFiltersCreate()
                .blockCreateConditions().conditionsList().get(1).secondConditionDropDown(), EQUAL_TO)
            .shouldContainValue(onFiltersCreationPage().setupFiltersCreate()
                .blockCreateConditions().conditionsList().get(1).inputCondition(), subject)
            .shouldBeSelected(onFiltersCreationPage().setupFiltersCreate().blockSelectAction().moveToFolderCheckBox())
            .shouldSee(onFiltersCreationPage().setupFiltersCreate().submitFilterButton());
    }
}
