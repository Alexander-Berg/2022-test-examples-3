package ru.yandex.autotests.directintapi.tests.bsfront.auth;

import org.hamcrest.Matcher;
import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.*;
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
@Description("Проверка прав и шаблонов при запросе с operator_uid, client_login и creative_id")
@Issue("https://st.yandex-team.ru/DIRECT-43716")
@RunWith(Parameterized.class)
public class BsFrontAuthWithAllFieldsTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps().version(104);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    @Parameterized.Parameter(value = 0)
    public String description;

    @Parameterized.Parameter(value = 1)
    public String operator;

    @Parameterized.Parameter(value = 2)
    public String client;

    @Parameterized.Parameter(value = 3)
    public String creativeOwner;

    @Parameterized.Parameter(value = 4)
    public List<String> expectedActions;

    @Parameterized.Parameter(value = 5)
    public List<Integer> expectedTemplates;

    @Parameterized.Parameters(name = "{0}")
    public static Collection strategies() {
        Object[][] data = new Object[][]{
                {"супер", Logins.SUPER_LOGIN,
                        Logins.CLIENT_FOR_RUB, Logins.CLIENT_FOR_RUB,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.getAllTemplates()},
                {"менеджер", Logins.MANAGER_DEFAULT,
                        Logins.LOGIN_FOR_RUB, Logins.LOGIN_FOR_RUB,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"агентство, субклиент и агентский креатив", Logins.AGENCY_CAMPAIGNS,
                        Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS, Logins.AGENCY_CAMPAIGNS,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"агентство и субклиент", Logins.AGENCY_CAMPAIGNS,
                        Logins.SUB_CLIENT_WITH_EDIT_RIGHTS, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"агентство, субклиент и креатив другого субклиента", Logins.AGENCY_CAMPAIGNS,
                        Logins.SUB_CLIENT_WITH_EDIT_RIGHTS, Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"клиент", Logins.CLIENT_FOR_RUB,
                        Logins.CLIENT_FOR_RUB, Logins.CLIENT_FOR_RUB,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"клиент и чужой креатив", Logins.CLIENT_FOR_RUB,
                        Logins.CLIENT_FOR_RUB, Logins.LOGIN_FOR_RUB,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"сервисируемый клиент", Logins.LOGIN_FOR_RUB,
                        Logins.LOGIN_FOR_RUB, Logins.LOGIN_FOR_RUB,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"легкий клиент", Logins.CLIENT_LIGHT,
                        Logins.CLIENT_LIGHT, Logins.CLIENT_LIGHT,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"субклиент с правами редактирования", Logins.SUB_CLIENT_WITH_EDIT_RIGHTS,
                        Logins.SUB_CLIENT_WITH_EDIT_RIGHTS,
                        Logins.SUB_CLIENT_WITH_EDIT_RIGHTS,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates},
                {"субклиент с правами редактирования и чужой креатив",
                        Logins.SUB_CLIENT_WITH_EDIT_RIGHTS, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS,
                        Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS,
                        new ArrayList<String>(), new ArrayList<Integer>()},
                {"субклиент без прав редактирования",
                        Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS, Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS,
                        Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS,
                        BsFrontAuthResponse.getActions, BsFrontAuthResponse.genericTemplates},
                {"агентство и субклиент без прав редактирования",
                        Logins.AGENCY_CAMPAIGNS, Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS,
                        Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.genericTemplates}
        };
        return Arrays.asList(data);
    }

    private BsFrontAuthResponse bsFrontAuthResponse;
    public int shard;
    public Long creativeId;

    @Before
    public void callBsFront() {
        shard = api.userSteps.getDarkSideSteps().getClientFakeSteps().getUserShard(client);
        Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(operator).getPassportID());
        Long clientId = Long.parseLong(api.userSteps.clientFakeSteps().getClientData(creativeOwner).getClientID());
        creativeId = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .perfCreativesSteps().saveDefaultPerfCreative(clientId);
        bsFrontAuthResponse =
                api.userSteps.getDarkSideSteps().getBsFrontSteps().auth(
                        new BsFrontRequest().withOperatorUid(uid).withClientLogin(client).withCreativeId(creativeId));
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

    @After
    public void deleteCreative() {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).perfCreativesSteps().deletePerfCreatives(creativeId);
    }

    private Matcher<Integer> sizeMatcher() {
        if (expectedTemplates.size() == 0) {
            return equalTo(0);
        } else {
            return greaterThanOrEqualTo(expectedTemplates.size());
        }
    }
}
