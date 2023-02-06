package ru.yandex.direct.core.entity.client.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.feature.FeatureName.BALANCE_LIMITED_AGENCY_REP;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AgencyLimitedRepsServiceTest {

    @Autowired
    private Steps steps;
    @Autowired
    private RbacService rbacService;

    private BalanceService balanceService;
    private AgencyLimitedRepsService agencyLimitedRepsService;
    private FeatureService featureService;

    private ClientId agencyClientId;
    private Long chiefAgencyRepUid;
    private UserInfo limitedAgencyRep;
    private UserInfo limitedAgencyRep2;
    private Long operatorUid;

    private ClientId subclientId1;
    private ClientId subclientId2;


    @Before
    public void before() {
        // создаем агентство и главного представителя агентства
        ClientInfo agency = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        agencyClientId = agency.getClientId();
        chiefAgencyRepUid = agency.getUid();
        operatorUid = agency.getUid();

        // создаем ограниченных представителей агентства
        limitedAgencyRep = steps.userSteps().createUser(new UserInfo()
                .withUser(generateNewUser().withRepType(RbacRepType.LIMITED))
                .withClientInfo(agency));
        limitedAgencyRep2 = steps.userSteps().createUser(new UserInfo()
                .withUser(generateNewUser().withRepType(RbacRepType.LIMITED))
                .withClientInfo(agency));

        // создаем субклиентов агентства
        subclientId1 =
                steps.clientSteps().createClientUnderAgency(limitedAgencyRep, new ClientInfo()).getClientId();
        subclientId2 =
                steps.clientSteps().createClientUnderAgency(limitedAgencyRep, new ClientInfo()).getClientId();
        steps.clientSteps().createClientUnderAgency(agency, new ClientInfo());

        // включаем фичу для агентства
        featureService = mock(FeatureService.class);
        when(featureService.isEnabledForClientIds(
                any(), eq(BALANCE_LIMITED_AGENCY_REP.getName()))
        ).thenReturn(singletonMap(agencyClientId, Boolean.TRUE));

        balanceService = mock(BalanceService.class);
        agencyLimitedRepsService = new AgencyLimitedRepsService(balanceService, featureService, rbacService);
    }


    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateAgencyLimitedRepsSubclientsInBalance_success() {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        doNothing().when(balanceService).setAgencyLimitedRepSubclients(
                eq(operatorUid),
                eq(UidAndClientId.of(limitedAgencyRep.getUid(), agencyClientId)),
                argumentCaptor.capture());

        agencyLimitedRepsService.updateAgencyLimitedRepsSubclientsInBalance(
                operatorUid, singletonList(limitedAgencyRep.getUid()));

        // проверяем что баланс вызывается с правильными параметрами
        List<Long> actualSubclientIds = (List<Long>) argumentCaptor.getValue();
        assertThat(actualSubclientIds, containsInAnyOrder(subclientId1.asLong(), subclientId2.asLong()));
    }

    @Test
    public void testUpdateAgencyLimitedRepsSubclientsInBalance_forChiefAgencyRep() {
        agencyLimitedRepsService.updateAgencyLimitedRepsSubclientsInBalance(
                operatorUid, singletonList(chiefAgencyRepUid));

        // проверяем что баланс не вызывается для главного представителя
        verify(balanceService, never()).setAgencyLimitedRepSubclients(anyLong(), any(), any());
    }

    @Test
    public void testUpdateAgencyLimitedRepsSubclientsInBalance_whenNoSubclients() {
        agencyLimitedRepsService.updateAgencyLimitedRepsSubclientsInBalance(
                operatorUid, singletonList(limitedAgencyRep2.getUid()));

        // проверяем что баланс не вызывается если нет клиентов, к которым представитель имеет доступ
        verify(balanceService, never()).setAgencyLimitedRepSubclients(anyLong(), any(), any());
    }

    @Test
    public void testUpdateAgencyLimitedRepsSubclientsInBalance_whenFeatureIsDisabled() {
        when(featureService.isEnabledForClientIds(
                any(), eq(BALANCE_LIMITED_AGENCY_REP.getName()))
        ).thenReturn(emptyMap());

        agencyLimitedRepsService.updateAgencyLimitedRepsSubclientsInBalance(
                operatorUid, singletonList(limitedAgencyRep.getUid()));

        // проверяем что баланс не вызывается когда фича выключена
        verify(balanceService, never()).setAgencyLimitedRepSubclients(anyLong(), any(), any());
    }
}
