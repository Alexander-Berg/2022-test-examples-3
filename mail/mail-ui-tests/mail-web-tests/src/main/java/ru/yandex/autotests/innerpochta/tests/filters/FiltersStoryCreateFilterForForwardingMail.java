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

import static java.lang.String.format;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllFiltersRule.removeAllFiltersRule;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 07.09.12
 * Time: 15:47
 */

@Aqua.Test
@Title("Тест на создание фильтра для пересылки писем")
@Features({FeaturesConst.FILTERS, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.FILTERS)
@Stories(FeaturesConst.GENERAL)
public class FiltersStoryCreateFilterForForwardingMail extends BaseTest {

    public static final String CREDS = "FilterForwardTest";
    private static final String ALT_CREDS = "ForwardToTest";
    private static final String ALT_CREDS2 = "FilterForReplying";
    private static final String FWD_MSG_TO_EMAIL_PATTERN = "— переслать письмо по адресу «%s»";
    private static final String FWD_ADDRESS_FROM_ABOOK = "sendtotestmail@yandex.ru";
    private static final String NOTIFY_BY_ADDRESS_PATTERN = "— уведомить по адресу «%s»";
    private static final String MESSAGE_SUBJECT = "Подтверждение адреса для получения уведомлений";

    public AccLockRule lock = AccLockRule.use().names(CREDS, ALT_CREDS, ALT_CREDS2);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(removeAllFiltersRule(user));

    @Before
    public void logIn() throws IOException {
        user.loginSteps().forAcc(lock.acc(CREDS)).logins(QuickFragments.SETTINGS_FILTERS);
        user.defaultSteps().clicksOn(onFiltersOverview().createNewFilterButton())
            .inputsTextInElement(
                onFiltersCreationPage().setupFiltersCreate().blockCreateConditions().conditionsList().get(0)
                    .inputCondition(),
                "forward back"
            );
    }

    @Test
    @Title("Создание фильтра по пересылке на другой адрес")
    @TestCaseId("1303")
    public void testForwardingFilter() throws InterruptedException {
        user.filtersSteps().chooseToForwardToAddress(lock.acc(ALT_CREDS).getSelfEmail())
            .submitsFilter(lock.acc(CREDS))
            .shouldSeeSelectedActionInFilter(format(FWD_MSG_TO_EMAIL_PATTERN, lock.acc(ALT_CREDS).getSelfEmail()))
            .shouldSeeThatFilterIsWaitingForConfirmation(0);
        user.defaultSteps().logsOut();
        user.loginSteps().forAcc(lock.acc(ALT_CREDS)).logins(QuickFragments.INBOX);
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().clicksOn(onMessageView().messageTextBlock().messageHref().get(0))
            .switchOnJustOpenedWindow();
        String url = user.defaultSteps().getsCurrentUrl();
        user.defaultSteps().switchOnWindow(0)
            .logsOut();
        user.loginSteps().forAcc(lock.acc(CREDS)).logins(QuickFragments.INBOX);
        user.defaultSteps().opensUrl(url)
            .clicksOn(onFiltersOverview().blockConfirmFilter().turnOnForwardingButton())
            .shouldNotSee(onFiltersOverview().blockConfirmFilter().turnOnForwardingButton())
            .opensFragment(QuickFragments.SETTINGS_FILTERS)
            .shouldSee(onFiltersOverview().createNewFilterButton());
        user.filtersSteps().shouldSeeThatFilterIsConfirmed(0);
    }

    @Test
    @Title("Создание фильтра нотификации на другой адрес")
    @TestCaseId("1304")
    public void testCreateNewFilterForNotifyingAddress() {
        user.filtersSteps().chooseToNotifyAddress(lock.acc(ALT_CREDS2).getSelfEmail())
            .submitsFilter(lock.acc(CREDS))
            .shouldSeeSelectedActionInFilter(format(NOTIFY_BY_ADDRESS_PATTERN, lock.acc(ALT_CREDS2).getSelfEmail()))
            .shouldSeeThatFilterIsWaitingForConfirmation(0);
        user.defaultSteps().logsOut();
        user.loginSteps().forAcc(lock.acc(ALT_CREDS2)).logins(QuickFragments.INBOX);
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().clicksOn(onMessageView().messageTextBlock().messageHref().get(0))
            .switchOnJustOpenedWindow();
        String url = user.defaultSteps().getsCurrentUrl();
        user.defaultSteps().switchOnWindow(0)
            .logsOut();
        user.loginSteps().forAcc(lock.acc(CREDS)).logins(QuickFragments.INBOX);
        user.defaultSteps().opensUrl(url)
            .clicksOn(onFiltersOverview().blockConfirmFilter().turnOnForwardingButton())
            .shouldNotSee(onFiltersOverview().blockConfirmFilter().turnOnForwardingButton())
            .opensFragment(QuickFragments.SETTINGS_FILTERS);
        user.filtersSteps().shouldSeeThatFilterIsConfirmed(0);
    }

    @Test
    @Title("Создание фильтра по пересылке на адрес из адресной книги")
    @TestCaseId("1305")
    public void testForwardingFilterForContactFromAbook() throws InterruptedException {
        user.filtersSteps().chooseToForwardToAddress(FWD_ADDRESS_FROM_ABOOK)
            .submitsFilter(lock.acc(CREDS))
            .shouldSeeSelectedActionInFilter(String.format(FWD_MSG_TO_EMAIL_PATTERN, FWD_ADDRESS_FROM_ABOOK))
            .shouldSeeThatFilterIsWaitingForConfirmation(0);
    }
}
