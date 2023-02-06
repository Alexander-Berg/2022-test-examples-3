package ru.yandex.autotests.directintapi.tests.bsfront.auth;

import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.Creative;
import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontAuthResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontRequest;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

/**
 * Created by pavryabov on 28.08.15.
 * https://st.yandex-team.ru/TESTIRT-6894
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BS_FRONT)
@Description("Проверка ошибок в ответе BsFront.auth")
@Issue("https://st.yandex-team.ru/DIRECT-43716")
public class BsFrontAuthErrorsTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Test
    public void checkNoDirectClientAsOperator() {
        Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(Logins.NO_DIRECT_LOGIN).getPassportID());
        api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .authExpectError(new BsFrontRequest().withOperatorUid(uid),
                        String.format(BsFrontAuthResponse.OPERATOR_UID_NOT_FOUND, uid));
    }

    @Test
    public void checkNoDirectClientAsClient() {
        Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(Logins.SUPER_LOGIN).getPassportID());
        api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .authExpectError(new BsFrontRequest().withOperatorUid(uid).withClientLogin(Logins.NO_DIRECT_LOGIN),
                        String.format(BsFrontAuthResponse.CLIENT_LOGIN_NOT_FOUND, Logins.NO_DIRECT_LOGIN));
    }

    @Test
    public void callWithoutOperatorUid() {
        api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .authExpectError(new BsFrontRequest().withClientLogin(Logins.CLIENT_FOR_RUB),
                        BsFrontAuthResponse.INCORRECT_OPERATOR_UID);
    }

    @Test
    public void callWithNegativeCreativeId() {
        Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(Logins.SUPER_LOGIN).getPassportID());
        api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .authExpectError(new BsFrontRequest()
                                .withOperatorUid(uid)
                                .withClientLogin(Logins.CLIENT_FOR_RUB)
                                .withCreativeId(-1l),
                        BsFrontAuthResponse.INCORRECT_CREATIVE_ID);
    }

    @Test
    public void callWithZeroCreativeId() {
        Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(Logins.SUPER_LOGIN).getPassportID());
        api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .authExpectError(new BsFrontRequest()
                                .withOperatorUid(uid)
                                .withClientLogin(Logins.CLIENT_FOR_RUB)
                                .withCreativeId(0l),
                        BsFrontAuthResponse.INCORRECT_CREATIVE_ID);
    }

    @Test
    public void cannotRequestWithCreativesAndCreativeIdAtTheSameTime() {
        Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(Logins.SUPER_LOGIN).getPassportID());
        api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .authExpectError(new BsFrontRequest()
                                .withOperatorUid(uid)
                                .withClientLogin(Logins.CLIENT_FOR_RUB)
                                .withCreativeId(1l)
                                .withCreatives(new Creative().withId(2L)),
                        BsFrontAuthResponse.PARAMS_AMBIGUOUS);
    }
}
