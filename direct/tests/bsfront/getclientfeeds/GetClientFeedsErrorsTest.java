package ru.yandex.autotests.directintapi.tests.bsfront.getclientfeeds;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.rules.ExpectedExceptionRule;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontChangeNotifyResponse;
import ru.yandex.autotests.directapi.darkside.exceptions.DarkSideException;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BS_FRONT)
@Description("Проверка ошибок в ответе BsFront.get_client_feeds")
@Issue("https://st.yandex-team.ru/DIRECT-56625")
public class GetClientFeedsErrorsTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Rule
    public ExpectedExceptionRule expectedException =
            ExpectedExceptionRule.none().expect(DarkSideException.class);

    @Test
    public void checkNoDirectClientAsOperator() {
        Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps()
                .getClientData(Logins.NO_DIRECT_LOGIN).getPassportID());
        expectedException.expectMessage(
                String.format(BsFrontChangeNotifyResponse.OPERATOR_UID_NOT_FOUND, uid));

        api.userSteps.getDarkSideSteps().getBsFrontSteps().getClientFeeds(uid, null);
    }

    @Test
    public void checkNoDirectClientAsClient() {
        Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps()
                .getClientData(Logins.SUPER_LOGIN).getPassportID());
        expectedException.expectMessage(
                String.format(BsFrontChangeNotifyResponse.CLIENT_LOGIN_NOT_FOUND, Logins.NO_DIRECT_LOGIN));

        api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .getClientFeeds(uid, Logins.NO_DIRECT_LOGIN);
    }
}
