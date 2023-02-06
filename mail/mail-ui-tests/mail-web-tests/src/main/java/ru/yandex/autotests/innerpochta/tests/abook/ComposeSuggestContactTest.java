package ru.yandex.autotests.innerpochta.tests.abook;


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
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author arttimofeev
 */
@Aqua.Test
@Title("Саджест групповых контактов при написании письма")
@Features(FeaturesConst.ABOOK)
@Tag(FeaturesConst.ABOOK)
@Stories(FeaturesConst.GENERAL)
public class ComposeSuggestContactTest extends BaseTest {

    private static final String RECIPIENTS_COUNT = "%s и ещё 1";
    private static final String GROUP_PREFIX = "группа %s";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    private Contact contact1, contact2;
    private String groupName;

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        contact1 = user.abookSteps().createDefaultContact();
        contact2 = user.abookSteps().createDefaultContact();
        groupName = getRandomString();
        user.apiAbookSteps().addNewContacts(contact1, contact2);
        user.apiAbookSteps().addNewAbookGroupWithContacts(
            groupName,
            user.apiAbookSteps().getPersonalContacts().get(0),
            user.apiAbookSteps().getPersonalContacts().get(1)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.COMPOSE);
    }

    @Test
    @Title("Отправляем письмо группе контактов из саджеста")
    @TestCaseId("1085")
    public void shouldSendMsgToGroupOfContacts() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().popupTo());
        user.composeSteps().inputsAddressInFieldTo(groupName)
            .shouldSeeSuggestedGroup(groupName);
        user.defaultSteps().clicksOn(onComposePopup().suggestList().get(0));
        user.composeSteps().shouldSeeSendToAreaHas(String.format(GROUP_PREFIX, groupName))
            .inputsSubject(getRandomString())
            .clicksOnSendBtn();
        user.defaultSteps().opensFragmentWithRefresh(QuickFragments.SENT);
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().shouldSeeThatElementTextEquals(
                onMessageView().messageHead().recipientsCount(),
                String.format(RECIPIENTS_COUNT, user.abookSteps().getFullName(contact1))
            );
    }

}
