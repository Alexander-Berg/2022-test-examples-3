package ru.yandex.direct.core.entity.user.service.validation;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.model.request.FindClientRequest;
import ru.yandex.direct.balance.client.model.request.ListPaymentMethodsSimpleRequest;
import ru.yandex.direct.balance.client.model.response.FindClientResponseItem;
import ru.yandex.direct.balance.client.model.response.ListPaymentMethodsSimpleResponseItem;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.PassportClientStub;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.user.service.validation.UserDefectIds.Gen.BALANCE_USER_ASSOCIATED_WITH_ANOTHER_CLIENT;
import static ru.yandex.direct.core.entity.user.service.validation.UserDefectIds.Gen.USER_HAS_CARD_PAYMENT_METHOD;
import static ru.yandex.direct.core.entity.user.service.validation.UserDefectIds.Gen.USER_HAS_NOT_VALID_EMAIL;
import static ru.yandex.direct.core.entity.user.service.validation.UserDefectIds.Gen.USER_HAS_NOT_VALID_NAME;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Gen.MUST_NOT_CONTAIN_DUPLICATED_ELEMENTS;
import static ru.yandex.direct.validation.result.DefectIds.INCONSISTENT_STATE_ALREADY_EXISTS;
import static ru.yandex.direct.validation.result.DefectIds.MUST_BE_VALID_LOGIN;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddUserValidationServiceTest {

    private static final int FAKE_SERVICE_ID = 667;
    private static final String FAKE_SERVICE_TOKEN = "fake_token";
    private static final Long FAKE_CLIENT_ID = 0x7ffffffeL;

    @Autowired
    AddUserValidationService addUserValidationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private PassportClientStub passportClientStub;

    private List<FindClientResponseItem> findClientResponse = emptyList();
    private Map<String, Map<String, Object>> paymentMethods = emptyMap();

    private User startUser;

    @Before
    public void setUp() throws Exception {
        BalanceClient balanceClient = mock(BalanceClient.class);


        BalanceService balanceService = new BalanceService(balanceClient, FAKE_SERVICE_ID, FAKE_SERVICE_TOKEN);
        addUserValidationService = new AddUserValidationService(userRepository, balanceService);

        when(balanceClient.findClient(any(FindClientRequest.class))).then(l -> findClientResponse);
        when(balanceClient.listPaymentMethodsSimple(any(ListPaymentMethodsSimpleRequest.class)))
                .then(l -> new ListPaymentMethodsSimpleResponseItem()
                        .withPaymentMethods(paymentMethods));

        ClientInfo clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT);
        Long uid = passportClientStub.generateNewUserUid();
        String login = passportClientStub.getLoginByUid(uid);
        startUser = new User()
                .withUid(uid)
                .withLogin(login)
                .withEmail(login + "@yandex.ru")
                .withFio("Василий Пупкин")
                .withRole(RbacRole.CLIENT)
                .withRepType(RbacRepType.MAIN)
                .withClientId(clientInfo.getClientId())
                .withChiefUid(clientInfo.getUid())
                .withLang(Language.EN);
    }

    @Test
    public void validate_success() {
        ValidationResult<List<User>, Defect> validationResult =
                addUserValidationService.validate(singletonList(startUser));

        assertThat(validationResult.hasAnyErrors()).as("hasAnyErrors").isFalse();
    }

    @Test
    public void validate_whenThereIsClientInBalance_failure() {
        FindClientResponseItem clientItem = new FindClientResponseItem().withClientId(FAKE_CLIENT_ID);
        findClientResponse = singletonList(clientItem);

        ValidationResult<List<User>, Defect> validationResult =
                addUserValidationService.validate(singletonList(startUser));

        assertThat(validationResult.flattenErrors().get(0).getDefect().defectId())
                .isEqualTo(BALANCE_USER_ASSOCIATED_WITH_ANOTHER_CLIENT);
    }

    @Test
    public void validate_whenThereIsCardPaymentMethod_failure() {
        paymentMethods = singletonMap("Method#1", singletonMap("type", "card"));

        ValidationResult<List<User>, Defect> validationResult =
                addUserValidationService.validate(singletonList(startUser));

        assertThat(validationResult.flattenErrors().get(0).getDefect().defectId())
                .isEqualTo(USER_HAS_CARD_PAYMENT_METHOD);
    }

    @Test
    public void validate_whenUserIdAlreadyExist_failure() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT);
        UserInfo userInfo = steps.userSteps().createUser(clientInfo, RbacRepType.MAIN);
        User existingUser = userInfo.getUser();

        ValidationResult<List<User>, Defect> validationResult =
                addUserValidationService.validate(singletonList(existingUser));

        assertThat(validationResult.flattenErrors().get(0).getDefect().defectId())
                .isEqualTo(INCONSISTENT_STATE_ALREADY_EXISTS);
    }

    @Test
    public void validate_whenUserLoginAlreadyExist_failure() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT);
        UserInfo userInfo = steps.userSteps().createUser(clientInfo, RbacRepType.MAIN);
        startUser.setLogin(userInfo.getUser().getLogin());

        ValidationResult<List<User>, Defect> validationResult =
                addUserValidationService.validate(singletonList(startUser));

        assertThat(validationResult.flattenErrors().get(0).getDefect().defectId())
                .isEqualTo(INCONSISTENT_STATE_ALREADY_EXISTS);
    }

    @Test
    public void validate_whenContainDuplicatedUsers_failure() {
        ValidationResult<List<User>, Defect> validationResult =
                addUserValidationService.validate(List.of(startUser, startUser));

        assertThat(validationResult.flattenErrors().get(0).getDefect().defectId())
                .isEqualTo(MUST_NOT_CONTAIN_DUPLICATED_ELEMENTS);
    }

    @Test
    public void validate_whenBlankLogin_failure() {
        startUser.setLogin("   ");

        ValidationResult<List<User>, Defect> validationResult =
                addUserValidationService.validate(singletonList(startUser));

        assertThat(validationResult.flattenErrors().get(0).getDefect().defectId())
                .isEqualTo(MUST_BE_VALID_LOGIN);
    }

    @Test
    public void validate_whenBlankFio_failure() {
        startUser.setFio("   ");

        ValidationResult<List<User>, Defect> validationResult =
                addUserValidationService.validate(singletonList(startUser));

        assertThat(validationResult.flattenErrors().get(0).getDefect().defectId())
                .isEqualTo(USER_HAS_NOT_VALID_NAME);
    }

    @Test
    public void validate_whenWrongEmail_failure() {
        startUser.setEmail("https://yandex.ru/");

        ValidationResult<List<User>, Defect> validationResult =
                addUserValidationService.validate(singletonList(startUser));

        assertThat(validationResult.flattenErrors().get(0).getDefect().defectId())
                .isEqualTo(USER_HAS_NOT_VALID_EMAIL);
    }

}
