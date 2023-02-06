package ru.yandex.autotests.direct.intapi.java.tests.clients;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.UsersRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppcdict.tables.records.ApiFinanceTokensRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.java.steps.ClientsControllerSteps;
import ru.yandex.autotests.direct.intapi.models.ClientsAddOrGetRequest;
import ru.yandex.autotests.direct.intapi.models.ClientsAddOrGetResponse;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.apiclient.config.Semaphore;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.direct.intapi.models.ClientsAddOrGetRequest.CurrencyEnum.RUB;
import static ru.yandex.autotests.directapi.darkside.Logins.LOGIN_MAIN;

@Aqua.Test
@Description("Проверка работы Clients.addOrGet")
@Stories(TestFeatures.Clients.ADD_OR_GET)
@Features(TestFeatures.CLIENTS)
@Tag(Tags.CLIENTS)
@Tag(TagDictionary.TRUNK)
@Issue("DIRECT-91703")
public class AddOrGetClientTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(AddOrGetClientTest.class);

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public Trashman trasher = new Trashman(api);

    private static ClientsControllerSteps clientsControllerSteps;

    private String login;
    private Long uid;

    @Before
    public void before() {
        clientsControllerSteps = directClassRule.intapiSteps().clientsControllerSteps();
    }

    @Test
    public void addNewClientWithLoginTest() throws IOException {
        registerNewUser();

        ClientsAddOrGetResponse response = clientsControllerSteps.addOrGet(new ClientsAddOrGetRequest()
                .withLogin(login)
                .withFio("test test")
                .withCountry(225)
                .withCurrency(RUB));
        checkNewClientCreated(response);
    }

    @Test
    public void addExitingClientWithLoginTest() {
        String token = getExistingUserFinanceToken();
        ClientsAddOrGetResponse response = clientsControllerSteps.addOrGet(new ClientsAddOrGetRequest()
                .withLogin(login)
                .withFio("test test")
                .withCountry(225)
                .withCurrency(RUB));
        checkExistingClientReturned(token, response);
    }

    @Test
    public void addNewClientWithUidTest() throws IOException {
        registerNewUser();
        ClientsAddOrGetResponse response = clientsControllerSteps.addOrGet(new ClientsAddOrGetRequest()
                .withUid(uid)
                .withFio("test test")
                .withCountry(225)
                .withCurrency(RUB));
        checkNewClientCreated(response);
    }

    @Test
    public void addExitingClientWithUidTest() {
        String token = getExistingUserFinanceToken();
        ClientsAddOrGetResponse response = clientsControllerSteps.addOrGet(new ClientsAddOrGetRequest()
                .withUid(uid)
                .withFio("test test")
                .withCountry(225)
                .withCurrency(RUB));
        checkExistingClientReturned(token, response);
    }

    private void registerNewUser() throws IOException {
        // НЕ ИСПОЛЬЗОВАТЬ ЭТУ РУЧКУ НИГДЕ КРОМЕ ЭТОГО МЕСТА
        // Ручка временная пока паспорт не сделает нормальное создание тестовых клиентов из sandbox
        List<String> errors;
        try (InputStream is = new URL("https://beta6.direct.yandex.ru/5fcaf9f2").openStream()) {
            Map<String, Object> result = mapper
                    .reader(new TypeReference<Map<String, Object>>() {
                    })
                    .readValue(is);
            login = (String) result.get("login");
            uid = ((Integer) result.get("uid")).longValue();
            errors = (List<String>) result.get("errors");
        }

        if (login == null) {
            throw new RuntimeException(Arrays.toString(errors.toArray()));
        }
        logger.info(String.format("Created passport client with login %s, uid %s", login, uid));
    }

    @NotNull
    private String getExistingUserFinanceToken() {
        login = "at-tester-account-text";
        uid = Long.valueOf(User.get(login).getPassportUID());
        DirectJooqDbSteps dbSteps = api.userSteps.getDirectJooqDbSteps().useShardForLogin(login);
        return dbSteps.apiFinanceTokensSteps().get(uid).map(ApiFinanceTokensRecord::getMasterToken)
                .orElseThrow(() -> new IllegalStateException("Test user should have finance token"));
    }

    private void checkNewClientCreated(ClientsAddOrGetResponse response) {
        assertThat(response.getSuccess(), is(true));
        assertThat(response.getUserId(), is(uid));
        assertThat(response.getFinanceToken(), not(isEmptyString()));
        DirectJooqDbSteps dbSteps = api.userSteps.getDirectJooqDbSteps().useShardForLogin(login);
        UsersRecord usersRecord = dbSteps.usersSteps().getUsers(uid);
        assertThat(usersRecord, notNullValue());
    }

    private void checkExistingClientReturned(String token, ClientsAddOrGetResponse response) {
        assertThat(response.getUserId(), is(uid));
        assertThat(response.getFinanceToken(), is(token));
        assertThat(response.getClientId(), is(Long.valueOf(User.get(login).getClientID())));
    }
}
