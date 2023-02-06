package ru.yandex.direct.core.entity.client.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncItem;
import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncPriority;
import ru.yandex.direct.core.entity.bs.resync.queue.repository.BsResyncQueueRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.repository.ClientManagersRepository;
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.currency.repository.CurrencyConvertQueueRepository;
import ru.yandex.direct.core.entity.currency.repository.ForceCurrencyConvertRepository;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.regions.GeoTreeFactory;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientServiceUpdateTest {
    public static final long CID1 = 18L;
    public static final long CID2 = 28L;
    @Autowired
    private Steps steps;

    @Autowired
    private ShardHelper shardHelper;

    private ClientService clientService;
    private ClientRepository clientRepository;
    private BsResyncQueueRepository bsResyncQueueRepository;
    private ClientInfo clientInfo;
    private CampaignRepository campaignRepository;

    @Before
    public void setUp() {
        clientRepository = mock(ClientRepository.class);
        bsResyncQueueRepository = mock(BsResyncQueueRepository.class);
        campaignRepository = mock(CampaignRepository.class);
        clientService = new ClientService(
                shardHelper, mock(UserRepository.class), mock(GeoTreeFactory.class), clientRepository,
                mock(ClientOptionsRepository.class),
                mock(ClientManagersRepository.class), mock(RbacService.class),
                mock(AgencyClientRelationService.class),
                bsResyncQueueRepository, campaignRepository,
                mock(CurrencyConvertQueueRepository.class),
                mock(ForceCurrencyConvertRepository.class),
                mock(DirectConfig.class));
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void massUpdate_empty() {
        clientService.massUpdate(emptyList());
        verify(clientRepository, never()).update(anyInt(), any(Collection.class));
        verify(bsResyncQueueRepository, never()).addToResync(anyInt(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void massUpdateResyncItemsWhenUpdateHideMarketRating() {
        ModelChanges<Client> modelChanges = new ModelChanges<>(clientInfo.getClient().getId(), Client.class);
        modelChanges.process(true, Client.HIDE_MARKET_RATING);
        AppliedChanges<Client> appliedChanges = modelChanges.applyTo(clientInfo.getClient());
        when(campaignRepository.getCampaignIdsByClientIds(anyInt(), any())).thenReturn(
                ImmutableSet.of(CID1, CID2));

        clientService.massUpdate(singleton(appliedChanges));

        verify(campaignRepository).getCampaignIdsByClientIds(
                eq(clientInfo.getShard()), argThat(v -> contains(clientInfo.getClientId()).matches(v))
        );

        ArgumentCaptor<List<BsResyncItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(bsResyncQueueRepository).addToResync(eq(clientInfo.getShard()), captor.capture());

        List<BsResyncItem> expectedBsResyncItems = Arrays.asList(
                new BsResyncItem(BsResyncPriority.UPDATE_DOMAIN_RATINGS, CID1),
                new BsResyncItem(BsResyncPriority.UPDATE_DOMAIN_RATINGS, CID2)
        );
        assertThat(captor.getValue(), beanDiffer(expectedBsResyncItems));
    }
}
