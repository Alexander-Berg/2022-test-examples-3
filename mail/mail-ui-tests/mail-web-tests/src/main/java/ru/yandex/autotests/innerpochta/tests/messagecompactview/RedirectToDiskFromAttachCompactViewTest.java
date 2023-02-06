package ru.yandex.autotests.innerpochta.tests.messagecompactview;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.autotests.innerpochta.util.MailConst.DISK_URL;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Тест на переход в диск при просмотре аттача в письме в списке писем")
@Description("У пользователя должны быть письма с дисковым и обычным аттачем в инбоксе")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.COMPACT_VIEW)
@UseCreds({RedirectToDiskFromAttachCompactViewTest.CREDS})
@RunWith(DataProviderRunner.class)
public class RedirectToDiskFromAttachCompactViewTest extends BaseTest {

    public static final String CREDS = "RedirectToDiskFromAttachTest";
    private static final String MESSAGE_WITH_ATTACHES_SUBJECT = "ATTACH";

    private static final int ARCHIVE_INDEX = 0;
    private static final int VIDEO_INDEX = 1;
    private static final int IMAGE_INDEX = 2;
    private static final int AUDIO_INDEX = 3;
    private static final int EXE_INDEX = 4;

    private AccLockRule lock = AccLockRule.use().annotation();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth);

    @DataProvider
    public static Object[] attachmentsIndexes() {
        return new Object[]{ARCHIVE_INDEX, VIDEO_INDEX, IMAGE_INDEX, AUDIO_INDEX, EXE_INDEX};
    }

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем открытие письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().clicksOnMessageWithSubject(MESSAGE_WITH_ATTACHES_SUBJECT);
        user.defaultSteps().shouldSee(onMessageView().attachments());
    }

    @Test
    @Title("Редирект в диск при попытке скачать дисковый аттач")
    @TestCaseId("3281")
    @UseDataProvider("attachmentsIndexes")
    public void shouldBeRedirectToDiskFromDownloadAttachTest(int index) {
        user.defaultSteps().onMouseHover(onMessageView().attachments().list().get(index))
            .clicksOn(onMessageView().attachments().list().get(index).download())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(startsWith(DISK_URL));
    }

    @Test
    @Title("Редирект в диск при попытке посмотреть дисковый аттач")
    @TestCaseId("3281")
    @UseDataProvider("attachmentsIndexes")
    public void shouldBeRedirectToDiskFromShowAttachTest(int index) {
        user.defaultSteps().onMouseHover(onMessageView().attachments().list().get(index))
            .clicksOn(onMessageView().attachments().list().get(index).show())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(startsWith(DISK_URL));
    }
}
