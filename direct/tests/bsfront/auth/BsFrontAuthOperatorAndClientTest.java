package ru.yandex.autotests.directintapi.tests.bsfront.auth;

import org.hamcrest.Matcher;
import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by pavryabov on 28.08.15.
 * https://st.yandex-team.ru/TESTIRT-6894
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BS_FRONT)
@Description("Проверка прав и шаблонов при запросе с operator_uid и client_login")
@Issue("https://st.yandex-team.ru/DIRECT-43716")
@RunWith(Parameterized.class)
public class BsFrontAuthOperatorAndClientTest {

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

    @Parameterized.Parameter(value = 3)
    public List<String> expectedActions;

    @Parameterized.Parameter(value = 4)
    public List<Integer> expectedTemplates;

    @Parameterized.Parameters(name = "{0}")
    public static Collection strategies() {
        Object[][] data = new Object[][]{
                {"супер - агентство", Logins.SUPER_LOGIN, Logins.AGENCY_YE_DEFAULT,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"супер - клиент", Logins.SUPER_LOGIN, Logins.CLIENT_FOR_RUB,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.getAllTemplates()},
                {"саппорт - клиент", Logins.SUPPORT, Logins.CLIENT_FOR_RUB,
                        BsFrontAuthResponse.allActionsWithoutDelete, BsFrontAuthResponse.genericTemplates},
                {"суперридер - клиент", Logins.SUPER_READER, Logins.CLIENT_FOR_RUB,
                        BsFrontAuthResponse.getActions, BsFrontAuthResponse.genericTemplates},
                {"вешальщик - клиент", Logins.PLACER, Logins.CLIENT_FOR_RUB,
                        BsFrontAuthResponse.getActions, BsFrontAuthResponse.genericTemplates},
                {"тимлидер - клиент", Logins.TEAMLEADER, Logins.CLIENT_FOR_RUB,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"тимлидер - сервисируемый клиент", Logins.TEAMLEADER, Logins.LOGIN_FOR_RUB,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"медиа - клиент", Logins.MEDIA, Logins.CLIENT_FOR_RUB,
                        BsFrontAuthResponse.getActions, BsFrontAuthResponse.genericTemplates},
                {"медиа - сервисируемый клиент", Logins.MEDIA, Logins.LOGIN_FOR_RUB,
                        BsFrontAuthResponse.getActions, BsFrontAuthResponse.genericTemplates},
                {"менеджер - менеджер", Logins.MANAGER_DEFAULT, Logins.MANAGER_DEFAULT,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"менеджер - агентство", Logins.MANAGER_DEFAULT, Logins.AGENCY_YE_DEFAULT,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"менеджер - клиент", Logins.MANAGER_DEFAULT, Logins.CLIENT_FOR_RUB,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"менеджер - сервисируемый клиент", Logins.MANAGER_DEFAULT, Logins.LOGIN_FOR_RUB,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"менеджер - субклиент", Logins.MANAGER_DEFAULT, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"агентство - агентство", Logins.AGENCY_CAMPAIGNS, Logins.AGENCY_CAMPAIGNS,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"агентство - сервисируемый клиент", Logins.AGENCY_CAMPAIGNS, Logins.LOGIN_FOR_RUB,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"агентство - клиент", Logins.AGENCY_CAMPAIGNS, Logins.CLIENT_FOR_RUB,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"представитель агентства - субклиент", Logins.AGENCY_YE_DEFAULT_REP, Logins.CLIENT_FREE_YE_DEFAULT,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"агентство - субклиент", Logins.AGENCY_CAMPAIGNS, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"агентство - субклиент без прав редактирования",
                        Logins.AGENCY_CAMPAIGNS, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"агентство - чужой субклиент", Logins.AGENCY_YE_DEFAULT, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"клиент - клиент", Logins.CLIENT_FOR_RUB, Logins.CLIENT_FOR_RUB,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"представитель клиента - клиент", Logins.CLIENT_FREE_YE_DEFAULT_REP, Logins.CLIENT_FREE_YE_DEFAULT,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"представитель клиента - представитель клиента",
                        Logins.CLIENT_FREE_YE_DEFAULT_REP, Logins.CLIENT_FREE_YE_DEFAULT_REP,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"клиент - представитель клиента", Logins.CLIENT_FREE_YE_DEFAULT, Logins.CLIENT_FREE_YE_DEFAULT_REP,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"клиент - другой клиент", Logins.CLIENT_FOR_RUB, Logins.LOGIN_FOR_RUB,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"клиент - субклиент", Logins.CLIENT_FOR_RUB, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"сервисируемый клиент - сервисируемый клиент", Logins.LOGIN_FOR_RUB, Logins.LOGIN_FOR_RUB,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"легкий клиент - легкий клиент", Logins.CLIENT_LIGHT, Logins.CLIENT_LIGHT,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"субклиент с правами редактирования - субклиент с правами редактирования",
                        Logins.SUB_CLIENT_WITH_EDIT_RIGHTS, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"субклиент с правами редактирования - субклиент без прав редактирования",
                        Logins.SUB_CLIENT_WITH_EDIT_RIGHTS, Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"субклиент без прав редактирования - субклиент с правами редактирования",
                        Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"субклиент без прав редактирования - субклиент без прав редактирования",
                        Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS, Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS,
                        BsFrontAuthResponse.getActions, BsFrontAuthResponse.genericTemplates}
        };
        return Arrays.asList(data);
    }

    private BsFrontAuthResponse bsFrontAuthResponse;

    @Before
    public void callBsFront() {
        Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(operatorLogin).getPassportID());
        bsFrontAuthResponse =
                api.userSteps.getDarkSideSteps().getBsFrontSteps().auth(
                        new BsFrontRequest().withOperatorUid(uid).withClientLogin(clientLogin));
    }

    @Test
    public void checkAvailableActions() {
        assertThat("список прав соответствует ожидаемому",
                bsFrontAuthResponse.getAvailableActions(),
                containsInAnyOrder(expectedActions.toArray(new String[expectedActions.size()])));
    }

    @Test
    public void checkAvailableTemplates() {
        assertThat("список шаблонов соответствует ожидаемому",
                bsFrontAuthResponse.getAvailableTemplates(), hasSize(sizeMatcher()));
    }

    private Matcher<Integer> sizeMatcher() {
        if (expectedTemplates.size() == 0) {
            return equalTo(0);
        } else {
            return greaterThanOrEqualTo(expectedTemplates.size());
        }
    }

}
