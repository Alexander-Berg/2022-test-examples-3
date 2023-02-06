package ru.yandex.direct.core.entity.client.service;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.bs.resync.queue.repository.BsResyncQueueRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.model.ClientWithOptions;
import ru.yandex.direct.core.entity.client.repository.ClientManagersRepository;
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.currency.repository.CurrencyConvertQueueRepository;
import ru.yandex.direct.core.entity.currency.repository.ForceCurrencyConvertRepository;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.regions.GeoTreeFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class ClientServiceSaveNewSubclientTest {
    private static final int SHARD = 1;

    private static final Long OPERATOR_UID = 1L;
    private static final UidAndClientId AGENCY = UidAndClientId.of(2L, ClientId.fromLong(3L));
    private static final UidAndClientId SUBCLIENT = UidAndClientId.of(4L, ClientId.fromLong(5L));

    private UserRepository userRepository;
    private ClientRepository clientRepository;
    private ClientService clientService;

    @Before
    public void setUp() {
        ShardHelper shardHelper = mock(ShardHelper.class);
        when(shardHelper.allocShardForNewClient(any(), any(), any()))
                .thenReturn(SHARD);

        userRepository = mock(UserRepository.class);
        clientRepository = mock(ClientRepository.class);

        RbacService rbacService = mock(RbacService.class);

        clientService = new ClientService(
                shardHelper,
                userRepository,
                mock(GeoTreeFactory.class), clientRepository,
                mock(ClientOptionsRepository.class),
                mock(ClientManagersRepository.class), rbacService,
                mock(AgencyClientRelationService.class),
                mock(BsResyncQueueRepository.class),
                mock(CampaignRepository.class),
                mock(CurrencyConvertQueueRepository.class),
                mock(ForceCurrencyConvertRepository.class),
                mock(DirectConfig.class));
    }

    @Test
    public void saveNewSubclient() {
        ClientWithOptions subclient = new ClientWithOptions()
                .withRole(RbacRole.CLIENT)
                .withUid(SUBCLIENT.getUid())
                .withClientId(SUBCLIENT.getClientId())
                .withAllowEditCampaigns(true)
                .withAllowTransferMoney(true)
                .withAllowImportXls(true);

        clientService.saveNewClient(subclient, false);

        verify(userRepository).addClient(eq(SHARD), argThat(beanDiffer(subclient)));
        verify(clientRepository).addClient(eq(SHARD), argThat(beanDiffer(subclient)), eq(false));
    }

    @Test
    public void setSubclientGrants() {
        ClientWithOptions subclient = new ClientWithOptions()
                .withRole(RbacRole.CLIENT)
                .withUid(SUBCLIENT.getUid())
                .withClientId(SUBCLIENT.getClientId())
                .withAllowEditCampaigns(true)
                .withAllowTransferMoney(true)
                .withAllowImportXls(true);
        clientService.setClientGrants(subclient, 0);
    }
}
