package ru.yandex.autotests.directintapi.tests.bsfront.auth;

import org.hamcrest.Matcher;
import org.junit.After;
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
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.Creative;
import ru.yandex.autotests.directapi.darkside.tags.StageTag;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Tag(StageTag.RELEASE)
@Features(FeatureNames.BS_FRONT)
@Description("Проверка прав и шаблонов при запросе с operator_uid, client_login и creatives")
@Issue("https://st.yandex-team.ru/DIRECT-60472")
@RunWith(Parameterized.class)
public class BsFrontAuthByCreativesFieldTest {

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
    public int shard;
    public Long creativeId;
    private BsFrontAuthResponse bsFrontAuthResponse;

    @Parameterized.Parameters(name = "{0}")
    public static Collection strategies() {
        Object[][] data = new Object[][]{
                {"супер", Logins.SUPER_LOGIN,
                        Logins.CLIENT_FOR_RUB, Logins.CLIENT_FOR_RUB,
                        BsFrontAuthResponse.allActions, BsFrontAuthResponse.getAllTemplates()},
                {"субклиент без прав редактирования",
                        Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS, Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS,
                        Logins.SUB_CLIENT_WITHOUT_EDIT_RIGHTS,
                        BsFrontAuthResponse.getActions, BsFrontAuthResponse.genericTemplates},
        };
        return Arrays.asList(data);
    }

    @Before
    public void callBsFront() {
        shard = api.userSteps.getDarkSideSteps().getClientFakeSteps().getUserShard(client);
        Integer uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(operator).getPassportID());
        Long clientId = Long.parseLong(api.userSteps.clientFakeSteps().getClientData(creativeOwner).getClientID());
        creativeId = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .perfCreativesSteps().saveDefaultPerfCreative(clientId);
        bsFrontAuthResponse =
                api.userSteps.getDarkSideSteps().getBsFrontSteps().auth(
                        new BsFrontRequest()
                                .withOperatorUid(uid)
                                .withClientLogin(client)
                                .withCreatives(new Creative().withId(creativeId)));
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
