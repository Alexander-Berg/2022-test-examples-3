package ru.yandex.autotests.innerpochta.tests.messagefullview;

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
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.startsWith;
import static ru.yandex.autotests.innerpochta.util.MailConst.DISK_URL;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тест на переход в диск при просмотре аттача при открытии письма на отдельной странице")
@Description("У пользователя должно быть одно письмо с дисковым аттачем в инбоксе")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.FULL_VIEW)
@RunWith(DataProviderRunner.class)
public class RedirectToDiskFromAttachTest extends BaseTest {

    private static final String MESSAGE_WITH_ATTACHES_SUBJECT = "ATTACH";

    private static final int ARCHIVE_INDEX = 0;
    private static final int VIDEO_INDEX = 1;
    private static final int IMAGE_INDEX = 2;
    private static final int AUDIO_INDEX = 3;
    private static final int EXE_INDEX = 4;
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private final AccLockRule lock = AccLockRule.use().className();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @DataProvider
    public static Object[] attachmentsIndexes() {
        return new Object[]{ARCHIVE_INDEX, VIDEO_INDEX, IMAGE_INDEX, AUDIO_INDEX, EXE_INDEX};
    }

    @Before
    public void logIn() throws IOException {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем открытие письма на отдельной странице",
            of(SETTINGS_OPEN_MSG_LIST, EMPTY_STR)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().clicksOnMessageWithSubject(MESSAGE_WITH_ATTACHES_SUBJECT);
        user.defaultSteps().shouldSee(onMessageView().attachments());
    }

    @Test
    @Title("Редирект в диск при попытке скачать дисковый аттач")
    @TestCaseId("1081")
    @UseDataProvider("attachmentsIndexes")
    public void shouldBeRedirectToDiskFromDownloadAttachTest(int index) {
        user.defaultSteps().onMouseHover(onMessageView().attachments().list().get(index))
            .clicksOn(onMessageView().attachments().list().get(index).download())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(startsWith(DISK_URL));
    }

    @Test
    @Title("Редирект в диск при попытке посмотреть дисковый аттач")
    @TestCaseId("3290")
    @UseDataProvider("attachmentsIndexes")
    public void shouldBeRedirectToDiskFromShowAttachTest(int index) {
        user.defaultSteps().onMouseHover(onMessageView().attachments().list().get(index))
            .clicksOn(onMessageView().attachments().list().get(index).show())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(startsWith(DISK_URL));
    }
}
