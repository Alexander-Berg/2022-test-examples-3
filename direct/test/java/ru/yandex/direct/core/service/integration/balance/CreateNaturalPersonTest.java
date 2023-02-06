package ru.yandex.direct.core.service.integration.balance;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.model.request.CreatePersonRequest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class CreateNaturalPersonTest {

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    private static final int FAKE_SERVICE_ID = 667;
    private static final String FAKE_SERVICE_TOKEN = "fake_token";
    private static final Long OPERATOR_UID = RandomNumberUtils.nextPositiveLong();
    private static final Long PERSON_ID = RandomNumberUtils.nextPositiveLong();
    private static final String EMAIL = RandomStringUtils.randomAlphanumeric(10);
    private static final String PHONE = RandomStringUtils.randomAlphanumeric(10);

    private BalanceService balanceService;
    private BalanceClient balanceClient;

    @Autowired
    private Steps steps;

    @Before
    public void setUp() {
        balanceClient = mock(BalanceClient.class);
        when(balanceClient.createPerson(any())).thenReturn(PERSON_ID);
        balanceService = new BalanceService(balanceClient, FAKE_SERVICE_ID, FAKE_SERVICE_TOKEN);
    }

    public Object[] testData() {
        return new Object[][]{
                {"Вася Пупкин", "Вася", "Пупкин"},
                {"", "-", "Фамилия"},
                {"     ", "-", "Фамилия"},
                {"тыква", "-", "тыква"},
                {"   Володя", "-", "Володя"},
                {"   Володя    ", "-", "Володя"},
                {"Володя ", "-", "Володя"},
                {"   Володя    Кузьмин    ", "Володя", "Кузьмин"}
        };
    }

    @Test
    @Parameters(method = "testData")
    public void test(String userFio, String expectedFirstName, String expectedLastName) {

        UserInfo userInfo = steps.userSteps().createUser(
                generateNewUser()
                        .withFio(userFio)
                        .withEmail(EMAIL)
                        .withPhone(PHONE)
        );

        Long personId = balanceService.createNaturalPerson(userInfo.getUser(), OPERATOR_UID);

        assertEquals(PERSON_ID, personId);

        verify(balanceClient).createPerson(
                new CreatePersonRequest()
                        .withOperatorUid(OPERATOR_UID)
                        .withClientId(userInfo.getClientId().toString())
                        .withType("ph")
                        .withFirstName(expectedFirstName)
                        .withMiddlename("-")
                        .withLastName(expectedLastName)
                        .withPhone(PHONE)
                        .withEmail(EMAIL)
        );
    }
}
