package ru.yandex.autotests.directintapi.tests.bsfront.getclientfeeds;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.GetClientFeedsResponse;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.model.User;
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

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BS_FRONT)
@Description("Проверка прав доступа оператора к клиенту в BsFront.get_client_feeds. Возвращение пустого списка фидов")
@Issue("https://st.yandex-team.ru/DIRECT-56625")
@RunWith(Parameterized.class)
public class GetClientFeedsRightsNegativeTest {

    private static final String CLIENT = ru.yandex.autotests.directapi.darkside.Logins.LOGIN_MAIN;

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
                //Нет прав оператора на клиента
                {"менеджер - клиент", Logins.MANAGER_DEFAULT, Logins.CLIENT_FOR_RUB},
                {"агентство - сервисируемый клиент", Logins.AGENCY_CAMPAIGNS, Logins.LOGIN_FOR_RUB},
                {"агентство - клиент", Logins.AGENCY_CAMPAIGNS, Logins.CLIENT_FOR_RUB},
                {"агентство - чужой субклиент", Logins.AGENCY_YE_DEFAULT, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS},
                {"клиент - другой клиент", Logins.CLIENT_FOR_RUB, Logins.LOGIN_FOR_RUB},
                {"клиент - субклиент", Logins.CLIENT_FOR_RUB, Logins.SUB_CLIENT_WITH_EDIT_RIGHTS}
        };
        return Arrays.asList(data);
    }

    private Integer uid;
    private Long feedId;
    private int shard;

    @Before
    public void getUid() {
        shard = api.userSteps.getDarkSideSteps().getClientFakeSteps().getUserShard(CLIENT);
        feedId = api.userSteps.getDirectJooqDbSteps().useShard(shard)
                .feedsSteps().createDefaultFeed(User.get(clientLogin).getClientID());
        uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(operatorLogin).getPassportID());
    }

    @Test
    public void checkRights() {
        List<GetClientFeedsResponse> response = api.userSteps.getDarkSideSteps()
                .getBsFrontSteps().getClientFeeds(uid, clientLogin);
        assertThat("получили пустой список", response, hasSize(0));
    }

    @After
    public void after() {
        if (feedId != null) {
            api.userSteps.getDirectJooqDbSteps().useShard(shard).feedsSteps().deleteFeedById(feedId);
        }
    }
}
