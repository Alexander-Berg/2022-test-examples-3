package ru.yandex.direct.core.entity.payment;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.model.response.GetClientPersonsResponseItem;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.balance.client.model.response.GetClientPersonsResponseItem.LEGAL_PERSON_TYPE;
import static ru.yandex.direct.balance.client.model.response.GetClientPersonsResponseItem.NATURAL_PERSON_TYPE;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetOrCreatePersonTest {

    private static final Long PERSON_ID_1 = 77L;
    private static final Long PERSON_ID_2 = 13L;

    @Autowired
    private Steps steps;

    @Mock
    private BalanceClient balanceClient;

    private BalanceService balanceService;
    private User user;

    @Before
    public void before() {
        initMocks(this);
        balanceService = new BalanceService(balanceClient, 0, null);

        user = generateNewUser();
        steps.userSteps().createUser(user);
    }

    @Test
    public void emptyTest() {
        when(balanceClient.getClientPersons(user.getClientId().asLong())).thenReturn(Collections.emptyList());

        Long resultLegalPersonId = balanceService.getOrCreatePerson(user, 1L, true);
        assertNull(resultLegalPersonId);

        when(balanceClient.createPerson(any())).thenReturn(PERSON_ID_1);
        Long resultNaturalPersonId = balanceService.getOrCreatePerson(user, 1L, false);
        assertEquals(PERSON_ID_1, resultNaturalPersonId);
    }

    @Test
    public void positiveTest() {
        GetClientPersonsResponseItem legalPerson = new GetClientPersonsResponseItem();
        legalPerson.setId(PERSON_ID_1);
        legalPerson.setType(LEGAL_PERSON_TYPE);
        GetClientPersonsResponseItem naturalPerson = new GetClientPersonsResponseItem();
        naturalPerson.setId(PERSON_ID_2);
        naturalPerson.setType(NATURAL_PERSON_TYPE);
        when(balanceClient.getClientPersons(user.getClientId().asLong()))
                .thenReturn(List.of(legalPerson, naturalPerson));

        Long resultLegalPersonId = balanceService.getOrCreatePerson(user, 1L, true);
        assertEquals(PERSON_ID_1, resultLegalPersonId);

        Long resultNaturalPersonId = balanceService.getOrCreatePerson(user, 1L, false);
        assertEquals(PERSON_ID_2, resultNaturalPersonId);
    }

    @Test
    public void onlyNaturalTest() {
        GetClientPersonsResponseItem naturalPerson = new GetClientPersonsResponseItem();
        naturalPerson.setId(PERSON_ID_2);
        naturalPerson.setType(NATURAL_PERSON_TYPE);
        when(balanceClient.getClientPersons(user.getClientId().asLong())).thenReturn(List.of(naturalPerson));

        Long resultLegalPersonId = balanceService.getOrCreatePerson(user, 1L, true);
        assertNull(resultLegalPersonId);
    }

    @Test
    public void manyNaturalsTest() {
        GetClientPersonsResponseItem naturalPerson1 = new GetClientPersonsResponseItem();
        naturalPerson1.setId(PERSON_ID_1);
        naturalPerson1.setType(NATURAL_PERSON_TYPE);
        GetClientPersonsResponseItem naturalPerson2 = new GetClientPersonsResponseItem();
        naturalPerson2.setId(PERSON_ID_2);
        naturalPerson2.setType(NATURAL_PERSON_TYPE);
        when(balanceClient.getClientPersons(user.getClientId().asLong()))
                .thenReturn(List.of(naturalPerson1, naturalPerson2));

        Long resultNaturalPersonId = balanceService.getOrCreatePerson(user, 1L, false);
        assertEquals(PERSON_ID_1, resultNaturalPersonId);

        Long resultLegalPersonId = balanceService.getOrCreatePerson(user, 1L, true);
        assertNull(resultLegalPersonId);
    }

    @Test
    public void manyLegalsTest() {
        GetClientPersonsResponseItem legalPerson1 = new GetClientPersonsResponseItem();
        legalPerson1.setId(PERSON_ID_1);
        legalPerson1.setType(LEGAL_PERSON_TYPE);
        GetClientPersonsResponseItem legalPerson2 = new GetClientPersonsResponseItem();
        legalPerson2.setId(PERSON_ID_2);
        legalPerson2.setType(LEGAL_PERSON_TYPE);

        when(balanceClient.getClientPersons(user.getClientId().asLong()))
                .thenReturn(List.of(legalPerson1, legalPerson2));

        Long resultLegalPersonId = balanceService.getOrCreatePerson(user, 1L, true);
        assertNull(resultLegalPersonId);
    }
}
