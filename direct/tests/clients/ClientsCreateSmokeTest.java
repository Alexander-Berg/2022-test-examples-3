package ru.yandex.autotests.directintapi.tests.clients;

import com.yandex.direct.api.v5.clients.ClientFieldEnum;
import com.yandex.direct.api.v5.clients.GetResponse;
import com.yandex.direct.api.v5.general.CurrencyEnum;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.model.RegionIDValues;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.ClientsCreateRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.json.ClientsCreateResponse;
import ru.yandex.autotests.directapi.darkside.steps.ClientsSteps;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.api5.clients.GetRequestMap;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/**
 * Created by hmepas on 27.03.17
 */
@Aqua.Test(title = "clients - IntAPI метод для создания клиентов в Директе")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Issue("https://st.yandex-team.ru/DIRECT-64544")
@Features(FeatureNames.CLIENTS)
public class ClientsCreateSmokeTest {
    private static final String ALREADY_EXISTS_LOGIN = Logins.CLIENT_SELF;
    private static final String ALREADY_EXISTS_UID = User.get(ALREADY_EXISTS_LOGIN).getPassportUID();
    private static final int RUSSIA_COUNTRY_ID = RegionIDValues.RUSSIA_COUNTRY.getId();

    private static final String ERROR_MESSAGE_FORBIDDEN_PREFIX = "Login can't start with 'yndx' or 'yandex-team' prefixes, it's reserved for manually created internal users: DIRECT-64544";
    private static final String ERROR_MESSAGE_NAME_CANT_BE_EMPTY = "name can't be empty";
    private static final String ERROR_MESSAGE_AGENCY_AND_MANAGER_ARE_MUTUAL_EXCLUSIVE = "agency_login and manager_login are mutually exclusive parameters";
    private static final String ERROR_MESSAGE_METHOD_ADD_NOT_FOUND = "method /add not found";
    private static final String ERROR_MESSAGE_LOGIN_ALREADY_EXISTS_IN_PASSPORT = "login " + ALREADY_EXISTS_LOGIN
            + " already exists in passport with uid: " + ALREADY_EXISTS_UID;

    @ClassRule
    public static ApiSteps api = new ApiSteps();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    final DarkSideSteps darkSideSteps = api.userSteps.getDarkSideSteps();

    @Test
    public void randomLoginCreation() {
        final ClientsCreateResponse createResponse = darkSideSteps.getClientsSteps().createRandomRubClientNoErrors();
        final GetResponse getResponse = api.userSteps.clientsStepsV5().clientsGet(
                new GetRequestMap().withFieldNames(ClientFieldEnum.LOGIN, ClientFieldEnum.CURRENCY, ClientFieldEnum.COUNTRY_ID),
                createResponse.getLogin());
        assumeThat("Clients.get вернул результат", getResponse.getClients(), hasSize(1));
        assertThat("клиент " + createResponse.getLogin() + "создан", getResponse.getClients().get(0).getLogin(), equalToIgnoringCase(
                createResponse.getLogin()
        ));
        assertThat("клиент " + createResponse.getLogin() + "создан в валюте " + CurrencyEnum.RUB,
                getResponse.getClients().get(0).getCurrency(), equalTo(CurrencyEnum.RUB));
        assertThat("клиент " + createResponse.getLogin() + "создан cо страной " + RUSSIA_COUNTRY_ID,
                getResponse.getClients().get(0).getCountryId(), equalTo(RUSSIA_COUNTRY_ID));
    }

    @Test
    public void alreadyExistsInPassportUserCreationFailsOk() {
        final String errorMessage = darkSideSteps.getClientsSteps().createClientExpectBadRequest(
                ClientsCreateRequest.getDefaultClientCreateRequest().withLogin(ALREADY_EXISTS_LOGIN));
        assertThat("вернулась правильная ошибка", errorMessage,
                equalTo(ERROR_MESSAGE_LOGIN_ALREADY_EXISTS_IN_PASSPORT));
    }

    @Test
    public void wrongMethodFailsOk() {
        final String errorMessage = darkSideSteps.getClientsSteps().createClientExpectMethodNotFound(
                ClientsSteps.getServiceName() + "/add", new ClientsCreateRequest());
        assertThat("вернулась правильная ошибка", errorMessage, equalTo(ERROR_MESSAGE_METHOD_ADD_NOT_FOUND));
    }

    @Test
    public void withAgencyAndManagerLoginFailsOk() {
        final String errorMessage = darkSideSteps.getClientsSteps().createClientExpectBadRequest(
                ClientsCreateRequest.getDefaultClientCreateRequest()
                        .withManagerLogin("yndx-test-manager").withAgencyLogin("yndx-test-agency")
        );
        assertThat("вернулась правильная ошибка", errorMessage, equalTo(ERROR_MESSAGE_AGENCY_AND_MANAGER_ARE_MUTUAL_EXCLUSIVE));
    }

    @Test
    public void emptyRequestFailsOk() {
        final String errorMessage = darkSideSteps.getClientsSteps().createClientExpectBadRequest(
                new ClientsCreateRequest()
        );
        assertThat("вернулась правильная ошибка", errorMessage, equalTo(ERROR_MESSAGE_NAME_CANT_BE_EMPTY));
    }

    @Test
    public void internalUserPrefixCreateFailsOk() {
        final String errorMessage = darkSideSteps.getClientsSteps().createClientExpectBadRequest(
                ClientsCreateRequest.getDefaultClientCreateRequest().withLogin("yndx-test-user")
        );
        assertThat("вернулась правильная ошибка", errorMessage, equalTo(ERROR_MESSAGE_FORBIDDEN_PREFIX));
    }
}
