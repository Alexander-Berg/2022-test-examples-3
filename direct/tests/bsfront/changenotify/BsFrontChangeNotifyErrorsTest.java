package ru.yandex.autotests.directintapi.tests.bsfront.changenotify;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontChangeNotifyResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.Creative;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.tests.bsfront.CreativesHelper;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

/**
 * Created by pavryabov on 04.09.15.
 * https://st.yandex-team.ru/TESTIRT-6894
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BS_FRONT)
@Description("Проверка ошибок в ответе BsFront.change_notify")
@Issue("https://st.yandex-team.ru/DIRECT-43716")
public class BsFrontChangeNotifyErrorsTest {

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
                .changeNotifyExpectError(
                        new BsFrontRequest().withOperatorUid(uid).withCreatives(new Creative().withId(123l)),
                        String.format(BsFrontChangeNotifyResponse.OPERATOR_UID_NOT_FOUND, uid));
    }

    @Test
    public void checkNoDirectClientAsClient() {
        Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(Logins.SUPER_LOGIN).getPassportID());
        api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .changeNotifyExpectError(
                        new BsFrontRequest()
                                .withOperatorUid(uid)
                                .withClientLogin(Logins.NO_DIRECT_LOGIN)
                                .withCreatives(new Creative().withId(123l)),
                        String.format(BsFrontChangeNotifyResponse.CLIENT_LOGIN_NOT_FOUND, Logins.NO_DIRECT_LOGIN));
    }

    @Test
    public void callWithoutCreatives() {
        api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .changeNotifyExpectError(new BsFrontRequest().withClientLogin(Logins.CLIENT_FOR_RUB),
                        BsFrontChangeNotifyResponse.MISSING_CREATIVES);
    }

    @Test
    public void callWithNegativeCreativeId() {
        api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .changeNotifyExpectError(new BsFrontRequest()
                                .withClientLogin(Logins.CLIENT_FOR_RUB)
                                .withCreatives(new Creative().withId(-1l)),
                        BsFrontChangeNotifyResponse.INCORRECT_CREATIVES);
    }

    @Test
    public void callWithValidAndInvalidCreativeId() {
        api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .changeNotifyExpectError(new BsFrontRequest()
                                .withCreatives(
                                        new Creative().withId(
                                                new CreativesHelper(api).createSomeCreativeInBS().longValue()),
                                        new Creative().withId(-1l)),
                        BsFrontChangeNotifyResponse.INCORRECT_CREATIVES);
    }

    @Test
    public void callWithZeroCreativeId() {
        api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .changeNotifyExpectError(new BsFrontRequest()
                                .withClientLogin(Logins.CLIENT_FOR_RUB)
                                .withCreatives(new Creative().withId(0l)),
                        BsFrontChangeNotifyResponse.INCORRECT_CREATIVES);
    }
}
