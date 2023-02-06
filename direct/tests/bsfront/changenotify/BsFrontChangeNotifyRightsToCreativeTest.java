package ru.yandex.autotests.directintapi.tests.bsfront.changenotify;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontChangeNotifyResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.Creative;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

/**
 * Created by pavryabov on 04.09.15.
 * https://st.yandex-team.ru/TESTIRT-6894
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BS_FRONT)
@Description("Проверка прав доступа оператора к клиенту в BsFront.change_notify. Позитивные проверки")
@Issue("https://st.yandex-team.ru/DIRECT-43716")
@RunWith(Parameterized.class)
public class BsFrontChangeNotifyRightsToCreativeTest {

    private static final Long CREATIVE_ID = 123L;

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameter(value = 0)
    public String description;

    @Parameterized.Parameter(value = 1)
    public String operatorLogin;

    @Parameterized.Parameter(value = 2)
    public String clientLogin;

    @Parameterized.Parameters(name = "{0}")
    public static Collection strategies() {
        Object[][] data = new Object[][]{
                {"супер - клиент", Logins.SUPER_LOGIN, Logins.CLIENT_FOR_RUB},
                {"саппорт - клиент", Logins.SUPPORT, Logins.CLIENT_FOR_RUB},
                {"менеджер - сервисируемый клиент", Logins.MANAGER_DEFAULT, Logins.LOGIN_FOR_RUB},
                {"тимлидер - сервисируемый клиент", Logins.TEAMLEADER, Logins.LOGIN_FOR_RUB},
                {"представитель агентства - субклиент", Logins.AGENCY_YE_DEFAULT_REP, Logins.CLIENT_FREE_YE_DEFAULT},
                {"агентство - субклиент", Logins.AGENCY_CAMPAIGNS, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS},
                {"агентство - субклиент без прав редактирования",
                        Logins.AGENCY_CAMPAIGNS, Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS},
                {"клиент - клиент", Logins.CLIENT_FOR_RUB, Logins.CLIENT_FOR_RUB},
                {"представитель клиента - клиент",
                        Logins.CLIENT_FREE_YE_DEFAULT_REP, Logins.CLIENT_FREE_YE_DEFAULT},
                {"представитель клиента - представитель клиента",
                        Logins.CLIENT_FREE_YE_DEFAULT_REP, Logins.CLIENT_FREE_YE_DEFAULT_REP},
                {"клиент - представитель клиента",
                        Logins.CLIENT_FREE_YE_DEFAULT, Logins.CLIENT_FREE_YE_DEFAULT_REP},
                {"сервисируемый клиент - сервисируемый клиент", Logins.LOGIN_FOR_RUB, Logins.LOGIN_FOR_RUB},
                {"легкий клиент - легкий клиент", Logins.CLIENT_LIGHT, Logins.CLIENT_LIGHT},
                {"субклиент с правами редактирования - субклиент с правами редактирования",
                        Logins.SUB_CLIENT_WITH_EDIT_RIGHTS, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS},
                {"клиент", Logins.CLIENT_FOR_RUB, null},
                {"сервисируемый клиент", Logins.LOGIN_FOR_RUB, null},
                {"легкий клиент", Logins.CLIENT_LIGHT, null},
                {"субклиент с правами редактирования",
                        Logins.SUB_CLIENT_WITH_EDIT_RIGHTS, null}
        };
        return Arrays.asList(data);
    }

    private Integer uid;

    @Before
    public void getUid() {
        uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(operatorLogin).getPassportID());
    }

    @Test
    public void checkResponse() {
        List<BsFrontChangeNotifyResponse> response = api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .changeNotify(
                        new BsFrontRequest()
                                .withOperatorUid(uid)
                                .withClientLogin(clientLogin)
                                .withCreatives(new Creative().withId(CREATIVE_ID)));
        BsFrontChangeNotifyResponse expectedResponse = new BsFrontChangeNotifyResponse();
        expectedResponse.setId(CREATIVE_ID);
        expectedResponse.setResult(0);
        expectedResponse.setErrorMessage(BsFrontChangeNotifyResponse.CREATIVE_NOT_FOUND);
        assertThat("получен правильный ответ от BsFront.change_notify",
                response, beanDiffer(Arrays.asList(expectedResponse)));
    }
}
