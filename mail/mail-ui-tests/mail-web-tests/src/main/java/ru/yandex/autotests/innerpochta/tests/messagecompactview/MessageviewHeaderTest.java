package ru.yandex.autotests.innerpochta.tests.messagecompactview;

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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

@Aqua.Test
@Title("Тест на шапку письма")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.HEAD)
public class MessageviewHeaderTest extends BaseTest {

    private static final String FROM = "От кого";
    private static final String EQUAL_TO = "совпадает c";
    private static final String SUBJECT = "Тема";
    private static final String SENT_EMAIL = "newtestbot6@yandex.ru";
    private static final String URL_FILTERS = "/filters-create?message=";
    private static final String DOMAIN = "@yandex.ru";
    private static final String MEDAL_4LVL_DESCRIPTION = "Отправитель письма подтверждён " +
        "и проверен Спамообороной Яндекса.";
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private Message msg;
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() throws IOException {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Проверяем наличие ссылок при клике по кнопке «еще»")
    @TestCaseId("1617")
    public void messageHeadShowMiscInfo() {
        msg = user.apiMessagesSteps().addCcEmails(SENT_EMAIL)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), getRandomName(), "");
        user.messagesSteps().clicksOnMessageWithSubject(msg.getSubject());
        user.messageViewSteps().expandCcAndBccBlock();
        user.defaultSteps().clicksOn(onMessageView().contentToolbarBlock().moreBtn())
            .shouldSee(
                onMessageView().miscField(),
                onMessageView().miscField().printBtn(),
                onMessageView().miscField().createFilter(),
                onMessageView().miscField().messageInfo()
            );
    }

    @Test
    @Title("Проверяем, что информация о получателе письма сворачивается/разворачивается")
    @TestCaseId("1618")
    public void messageHeadToggleTest() {
        msg = user.apiMessagesSteps().addCcEmails(SENT_EMAIL)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), getRandomName(), "");
        user.messagesSteps().clicksOnMessageWithSubject(msg.getSubject());
        user.messageViewSteps().expandCcAndBccBlock();
        user.defaultSteps().shouldSee(onMessageView().messageHead().contactsInTo().get(0)) // cc field
            .clicksOn(onMessageView().messageHead().showFieldToggler())
            .shouldNotSee(onMessageView().messageHead().contactsInTo().get(0));
    }

    @Test
    @Title("Создание фильтра из шапки")
    @TestCaseId("1622")
    public void messageHeadCreateFilter() {
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomName(), getRandomName());
        user.messagesSteps().clicksOnMessageWithSubject(msg.getSubject());
        user.defaultSteps().clicksOn(onMessageView().contentToolbarBlock().moreBtn())
            .clicksIfCanOn(onMessageView().miscField().createFilter())
            .shouldBeOnUrl(containsString(URL_FILTERS))
            .shouldSeeThatElementTextEquals(onFiltersCreationPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(0).firstConditionDropDown(), FROM)
            .shouldSeeThatElementTextEquals(onFiltersCreationPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(0).secondConditionDropDown(), EQUAL_TO)
            .shouldContainValue(onFiltersCreationPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(0).inputCondition(), lock.firstAcc().getLogin() + DOMAIN)
            .shouldSeeThatElementTextEquals(onFiltersCreationPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(1).firstConditionDropDown(), SUBJECT)
            .shouldSeeThatElementTextEquals(onFiltersCreationPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(1).secondConditionDropDown(), EQUAL_TO)
            .shouldContainValue(onFiltersCreationPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(1).inputCondition(), msg.getSubject())
            .shouldBeSelected(onFiltersCreationPage().setupFiltersCreate().blockSelectAction()
                .moveToFolderCheckBox());
    }

    @Test
    @Title("Закрываем попап замочка крестиком")
    @TestCaseId("4456")
    public void shouldCloseMedalPopupByCross() {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomName(), getRandomName());
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().clicksOn(onMessageView().messageHead().medal())
            .shouldSee(onMessageView().medalPopup())
            .shouldHasText(onMessageView().medalPopup().headerTextMedal().get(0), MEDAL_4LVL_DESCRIPTION)
            .clicksOn(onMessageView().medalPopup().closeMedal())
            .shouldNotSee(onMessageView().medalPopup());
    }
}
