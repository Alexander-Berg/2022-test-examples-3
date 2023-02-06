package ru.yandex.autotests.innerpochta.tests.settings;

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
import ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static java.lang.String.format;
import static ru.lanwen.diff.uri.core.filters.AnyParamValueFilter.param;
import static ru.lanwen.diff.uri.core.filters.SchemeFilter.scheme;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getUserUid;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGE_UNION_AVATARS;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Не объединять чекбоксы с аватарками")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
public class OtherParametersStoryShowCheckboxTest extends BaseTest {

    private static final String SEARCH_RESULT_URL = "#search?";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    private AddMessageIfNeedRule addMessageIfNeed = AddMessageIfNeedRule.addMessageIfNeed(() -> user,
        () -> lock.firstAcc());

    private Message msg;
    private String url;

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user))
        .around(addMessageIfNeed);

    @Before
    public void setUp() {
        msg = addMessageIfNeed.getFirstMessage();
        url = new StringBuilder(webDriverRule.getBaseUrl())
            .append(format("/?uid=%s", getUserUid(lock.firstAcc().getLogin())))
            .append(SEARCH_RESULT_URL)
            .append("request=")
            .append(lock.firstAcc().getLogin())
            .append("%40")
            .append(lock.firstAcc().getDomain())
            .append("&scope=hdr_from")
            .toString();
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем квадратные чекбоксы",
            of(SETTINGS_PARAM_MESSAGE_UNION_AVATARS, FALSE)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Проверяем поиск по аватарке отправителя")
    @TestCaseId("2384")
    public void shouldOpenSearchBySender() {
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
        user.defaultSteps().clicksOn(onMessagePage().displayedMessages().list().get(0).avatarImg())
            .shouldBeOnUrlNotDiffWith(url, param("ncrnd").ignore(), scheme("http", "https"))
            .shouldHasValue(onMessagePage().mail360HeaderBlock().searchInput(), lock.firstAcc().getSelfEmail());
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
    }
}
