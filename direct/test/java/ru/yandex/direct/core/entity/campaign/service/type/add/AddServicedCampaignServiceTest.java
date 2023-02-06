package ru.yandex.direct.core.entity.campaign.service.type.add;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.campaign.service.type.add.container.AddServicedCampaignInfo;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(MockitoJUnitRunner.class)
@ParametersAreNonnullByDefault
public class AddServicedCampaignServiceTest {

    @Mock
    private ClientService clientService;

    @Mock
    private RbacService rbacService;

    @InjectMocks
    private AddServicedCampaignService service;

    private RestrictedCampaignsAddOperationContainer parametersContainer;
    private Client client;
    private AddServicedCampaignInfo expectedServicedInfo;

    @Before
    public void initTestData() {
        long operatorUid = RandomNumberUtils.nextPositiveLong();
        int shard = RandomNumberUtils.nextPositiveInteger();
        ClientId clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        long chiefUid = RandomNumberUtils.nextPositiveLong();

        parametersContainer = RestrictedCampaignsAddOperationContainer.create(shard, operatorUid, clientId, chiefUid, chiefUid);

        doReturn(RbacRole.CLIENT)
                .when(rbacService).getUidRole(parametersContainer.getOperatorUid());
        client = new Client()
                .withId(clientId.asLong())
                .withIsIdmPrimaryManager(false);
        doReturn(client)
                .when(clientService).getClient(parametersContainer.getClientId());

        expectedServicedInfo = new AddServicedCampaignInfo()
                .withIsServiced(false);
    }


    @Test
    public void getServicedInfoForNewCampaign_whenOperatorIsAgency() {
        doReturn(RbacRole.AGENCY)
                .when(rbacService).getUidRole(parametersContainer.getOperatorUid());

        AddServicedCampaignInfo servicedInfo = service.getServicedInfoForClientCampaigns(parametersContainer);
        assertThat(servicedInfo)
                .is(matchedBy(beanDiffer(expectedServicedInfo)));

        verify(rbacService).getUidRole(parametersContainer.getOperatorUid());
        verifyNoMoreInteractions(rbacService);
        verifyZeroInteractions(clientService);
    }

    @Test
    public void getServicedInfoForNewCampaign_whenClientIsUnderAgency() {
        doReturn(true)
                .when(rbacService).isUnderAgency(parametersContainer.getChiefUid());

        AddServicedCampaignInfo servicedInfo = service.getServicedInfoForClientCampaigns(parametersContainer);
        assertThat(servicedInfo)
                .is(matchedBy(beanDiffer(expectedServicedInfo)));

        verify(rbacService).getUidRole(parametersContainer.getOperatorUid());
        verify(rbacService).isUnderAgency(parametersContainer.getChiefUid());
        verifyNoMoreInteractions(rbacService);
        verifyZeroInteractions(clientService);
    }

    @Test
    public void getServicedInfoForNewCampaign_whenClientHasIdmPrimaryManager() {
        client.setIsIdmPrimaryManager(true);
        client.setPrimaryManagerUid(RandomNumberUtils.nextPositiveLong());

        AddServicedCampaignInfo servicedInfo = service.getServicedInfoForClientCampaigns(parametersContainer);

        expectedServicedInfo
                .withIsServiced(true)
                .withManagerUid(client.getPrimaryManagerUid());
        assertThat(servicedInfo)
                .is(matchedBy(beanDiffer(expectedServicedInfo)));

        verify(rbacService).getUidRole(parametersContainer.getOperatorUid());
        verify(rbacService).isUnderAgency(parametersContainer.getChiefUid());
        verifyNoMoreInteractions(rbacService);
        verify(clientService).getClient(parametersContainer.getClientId());
    }

    @Test
    public void getServicedInfoForNewCampaign_whenNotAgencyAndNotIdmPrimaryManager() {
        AddServicedCampaignInfo servicedInfo = service.getServicedInfoForClientCampaigns(parametersContainer);
        assertThat(servicedInfo)
                .is(matchedBy(beanDiffer(expectedServicedInfo)));

        verify(rbacService).getUidRole(parametersContainer.getOperatorUid());
        verify(rbacService).isUnderAgency(parametersContainer.getChiefUid());
        verifyNoMoreInteractions(rbacService);
        verify(clientService).getClient(parametersContainer.getClientId());
    }

}
