package ru.yandex.direct.core.entity.client.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.bs.resync.queue.repository.BsResyncQueueRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.model.CreateAgencySubclientStatus;
import ru.yandex.direct.core.entity.client.repository.ClientManagersRepository;
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.currency.repository.CurrencyConvertQueueRepository;
import ru.yandex.direct.core.entity.currency.repository.ForceCurrencyConvertRepository;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.regions.GeoTreeFactory;

import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClientServiceCreateAgencySubclientsTest {
    @Test
    public void testCreateAgencySubclients() {
        Long operatorUid = 10L;
        UidAndClientId agency = UidAndClientId.of(10L, ClientId.fromLong(1L));
        long minClientUidAndClientId = 2;
        long maxClientUidAndClientId = 4;
        Set<UidAndClientId> newClients = LongStream.rangeClosed(minClientUidAndClientId, maxClientUidAndClientId)
                .boxed()
                .map(l -> UidAndClientId.of(10 * l, ClientId.fromLong(l)))
                .collect(Collectors.toSet());

        RbacService rbacService = mock(RbacService.class);
        AgencyClientRelationService agencyClientRelationService = mock(AgencyClientRelationService.class);

        when(agencyClientRelationService.getAllowableToBindClients(
                any(ClientId.class), anySet()))
                .thenReturn(
                        LongStream.rangeClosed(minClientUidAndClientId, maxClientUidAndClientId - 1)
                                .boxed()
                                .map(ClientId::fromLong)
                                .collect(Collectors.toSet()));

        when(rbacService.bindClientsToAgency(
                any(Long.class), anyCollection(), anySet()))
                .thenReturn(
                        LongStream.rangeClosed(minClientUidAndClientId, maxClientUidAndClientId - 2)
                                .boxed()
                                .map(ClientId::fromLong)
                                .collect(Collectors.toSet()));

        ClientService clientService = new ClientService(
                mock(ShardHelper.class),
                mock(UserRepository.class),
                mock(GeoTreeFactory.class), mock(ClientRepository.class),
                mock(ClientOptionsRepository.class),
                mock(ClientManagersRepository.class), rbacService,
                agencyClientRelationService,
                mock(BsResyncQueueRepository.class),
                mock(CampaignRepository.class),
                mock(CurrencyConvertQueueRepository.class),
                mock(ForceCurrencyConvertRepository.class),
                mock(DirectConfig.class));

        Map<ClientId, CreateAgencySubclientStatus> actual = clientService.createAgencySubclients(
                operatorUid, Set.of(agency), newClients, emptySet());

        assertThat(actual, allOf(
                hasEntry(ClientId.fromLong(minClientUidAndClientId), CreateAgencySubclientStatus.OK),
                hasEntry(ClientId.fromLong(minClientUidAndClientId + 1),
                        CreateAgencySubclientStatus.RBAC_CANT_BIND_TO_AGENCY),
                hasEntry(ClientId.fromLong(minClientUidAndClientId + 2),
                        CreateAgencySubclientStatus.NOT_ALLOWED_BIND_TO_AGENCY)));
    }
}
