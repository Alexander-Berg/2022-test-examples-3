package ru.yandex.autotests.directintapi.tests.bsfront.getclientfeeds;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.steps.FeedsSteps;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.GetClientFeedsResponse;
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

import static java.util.Collections.emptyMap;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.BS_FRONT)
@Description("Получение фидов клиента методом BsFront.get_client_feeds")
@Issue("https://st.yandex-team.ru/DIRECT-56625")
@RunWith(Parameterized.class)
public class GetClientFeedsTest {

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
                {"представитель клиента - клиент",
                        Logins.CLIENT_FREE_YE_DEFAULT_REP, Logins.CLIENT_FREE_YE_DEFAULT},
                {"представитель клиента - представитель клиента",
                        Logins.CLIENT_FREE_YE_DEFAULT_REP, Logins.CLIENT_FREE_YE_DEFAULT_REP},
                {"клиент - представитель клиента",
                        Logins.CLIENT_FREE_YE_DEFAULT, Logins.CLIENT_FREE_YE_DEFAULT_REP},
                {"сервисируемый клиент - сервисируемый клиент", Logins.LOGIN_FOR_RUB, Logins.LOGIN_FOR_RUB},
                {"клиент", Logins.CLIENT_FOR_RUB, null},
                {"сервисируемый клиент", Logins.LOGIN_FOR_RUB, null}
        };
        return Arrays.asList(data);
    }

    private Integer uid;
    private Long firstFeedId;
    private Long secondFeedId;
    private String client;
    private String clientId;
    private int shard;

    @Before
    public void before() {
        client = clientLogin == null ? operatorLogin : clientLogin;
        shard = api.userSteps.getDarkSideSteps().getClientFakeSteps().getUserShard(client);
        uid = Integer.parseInt(api.userSteps.clientFakeSteps().getClientData(operatorLogin).getPassportID());
        clientId = User.get(client).getClientID();

        firstFeedId = createFeed();
    }

    @Test
    public void getOneFeed() {
        List<GetClientFeedsResponse> response = api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .getClientFeeds(uid, clientLogin);

        assertThat("получен правильный ответ от BsFront.get_client_feeds",
                response, hasItem(beanDiffer(getExpectedResponse(firstFeedId))));
    }

    @Test
    public void getTwoFeeds() {
        secondFeedId = createFeed();
        List<GetClientFeedsResponse> response = api.userSteps.getDarkSideSteps().getBsFrontSteps()
                .getClientFeeds(uid, clientLogin);

        assertThat("получен правильный ответ от BsFront.get_client_feeds",
                response, hasItems(beanDiffer(getExpectedResponse(firstFeedId)),
                        beanDiffer(getExpectedResponse(secondFeedId))));
    }

    private GetClientFeedsResponse getExpectedResponse(Long feedId) {
        return new GetClientFeedsResponse()
                .withFeedId(feedId)
                .withName(FeedsSteps.DEFAULT_FEED_NAME)
                .withBusinessType("retail")
                .withOffers(emptyMap());
    }

    private Long createFeed() {
        return api.userSteps.getDirectJooqDbSteps().useShard(shard).feedsSteps().createDefaultFeed(clientId);
    }

    private void deleteFeedById(Long feedId) {
        api.userSteps.getDirectJooqDbSteps().useShard(shard).feedsSteps().deleteFeedById(feedId);
    }

    @After
    public void after() {
        if (firstFeedId != null) {
            deleteFeedById(firstFeedId);
        }

        if (secondFeedId != null) {
            deleteFeedById(secondFeedId);
        }
    }
}
