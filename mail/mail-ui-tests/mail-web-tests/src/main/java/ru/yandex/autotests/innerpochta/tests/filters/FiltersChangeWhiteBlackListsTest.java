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

import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllEmailsFromBlacklistRule.removeAllEmailsFromBlacklist;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllEmailsFromWhitelistRule.removeAllEmailsFromWhitelist;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тест на добавление и удаление адресов из белого и черного списков")
@Stories(FeaturesConst.EDIT_FILTERS)
@Features(FeaturesConst.FILTERS)
@Tag(FeaturesConst.FILTERS)
public class FiltersChangeWhiteBlackListsTest extends BaseTest {

    private static final String WHITE_LIST_EMAIL = "test@test.ru";
    private static final String BLACK_LIST_EMAIL = "yandex-team-mailt-25@yandex.ru";
    private static final String SENDER_EMAIL = "BlackListEmail";

    private AccLockRule lock = AccLockRule.use().className();
    private AccLockRule lock2 = AccLockRule.use().names(SENDER_EMAIL);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock2);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(lock2)
        .around(auth)
        .around(auth2)
        .around(removeAllEmailsFromWhitelist(user))
        .around(removeAllEmailsFromBlacklist(user))
        .around(removeAllMessages(() -> user, INBOX));


    @Before
    public void setUp() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FILTERS);
    }

    @Test
    @Title("Удалить адрес из белого списка")
    @TestCaseId("2519")
    public void shouldRemoveContactFromWhiteList() {
        user.defaultSteps().inputsTextInElement(
                onFiltersCreationPage().whiteListBlock().emailField(),
                WHITE_LIST_EMAIL
            )
            .clicksOn(onFiltersCreationPage().whiteListBlock().submitButton())
            .shouldSee(onFiltersCreationPage().whiteListBlock().whitedAddressBlock().get(0))
            .shouldSeeElementInList(
                onFiltersCreationPage().whiteListBlock().whitedAddressBlock(),
                WHITE_LIST_EMAIL
            );
        user.defaultSteps().clicksOn(onFiltersCreationPage().whiteListBlock().whitedAddressBlock().get(0))
            .clicksOn(onFiltersCreationPage().whiteListBlock().deleteButton())
            .shouldNotSee(onFiltersCreationPage().whiteListBlock().whitedAddressesBlock());
    }

    @Test
    @Title("Письмо с адреса из черного списка не приходит")
    @TestCaseId("2513")
    public void shouldAddContactToBlackList() {
        String msgSubject = Utils.getRandomString();
        String blackListMsgSubject = Utils.getRandomString();
        user.defaultSteps().inputsTextInElement(
                onFiltersCreationPage().blackListBlock().emailField(),
                BLACK_LIST_EMAIL
            )
            .clicksOn(onFiltersCreationPage().blackListBlock().submitButton())
            .shouldSee(onFiltersCreationPage().blackListBlock().blockedAddressBlock().get(0))
            .shouldSeeElementInList(
                onFiltersCreationPage().blackListBlock().blockedAddressBlock(),
                BLACK_LIST_EMAIL
            );
        user.apiMessagesSteps().withAuth(auth2).sendMail(
                lock.firstAcc().getSelfEmail(),
                blackListMsgSubject,
                ""
            )
            .withAuth(auth).sendMail(
                lock.firstAcc().getSelfEmail(),
                msgSubject,
                ""
            );
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject)
            .shouldNotSeeMessageWithSubject(blackListMsgSubject);
    }

    @Test
    @Title("Письмо приходит после удаления адреса из черного списка")
    @TestCaseId("2518")
    public void shouldRemoveContactFromBlackList() {
        String msgSubject = Utils.getRandomString();
        user.defaultSteps().inputsTextInElement(
                onFiltersCreationPage().blackListBlock().emailField(),
                BLACK_LIST_EMAIL
            )
            .clicksOn(onFiltersCreationPage().blackListBlock().submitButton())
            .shouldSee(onFiltersCreationPage().blackListBlock().blockedAddressBlock().get(0));
        user.defaultSteps().shouldSeeElementInList(
                onFiltersCreationPage().blackListBlock().blockedAddressBlock(),
                BLACK_LIST_EMAIL
            );
        user.defaultSteps().clicksOn(onFiltersCreationPage().blackListBlock().blockedAddressBlock().get(0))
            .clicksOn(onFiltersCreationPage().blackListBlock().deleteButton())
            .shouldNotSee(onFiltersCreationPage().blackListBlock().blockedAddressesBlock());
        user.apiMessagesSteps().withAuth(auth2).sendMail(
            lock.firstAcc().getSelfEmail(),
            msgSubject,
            ""
        );
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject);
    }
}

