package ru.yandex.direct.core.entity.user.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import one.util.streamex.IntStreamEx;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.sharding.ShardKey;
import ru.yandex.direct.dbutil.sharding.ShardSupport;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.rbac.model.Representative;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;

public class UserServiceGetAgencyUidAndClientIdTest {
    private RbacService rbacService;
    private UserRepository userRepository;
    private ShardHelper shardHelper;
    private ShardSupport shardSupport;

    @InjectMocks
    private UserService support;


    @Test
    public void getAgencyUidAndClientId_OperatorIsAgency() {
        Long operatorUid = RandomNumberUtils.nextPositiveLong();
        ClientId agencyClientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());

        Client client = defaultClient()
                .withClientId(RandomNumberUtils.nextPositiveLong());

        initMocksInitial();
        doReturn(RbacRole.AGENCY).when(rbacService).getUidRole(eq(operatorUid));
        doReturn(agencyClientId.asLong()).when(shardSupport).getValue(eq(ShardKey.UID), anyLong(),
                eq(ShardKey.CLIENT_ID), eq(Long.class));
        when(userRepository.getUidsByClientIds(anyInt(), anyCollection())).thenReturn(Map.of(
                agencyClientId, List.of(operatorUid)
        ));
        doReturn(emptyList()).when(rbacService).getAgencyLimitedRepresentatives(anyCollection());
        MockitoAnnotations.initMocks(this);

        UidAndClientId agencyUidAndClientId = support.getAgencyUidAndClientId(operatorUid,
                client.getAgencyUserId(), client.getAgencyClientId());

        assertThat(agencyUidAndClientId).isEqualTo(UidAndClientId.of(operatorUid, agencyClientId));
    }

    @Test
    public void getAgencyUidAndClientId_OperatorIsAgencyWithLimitedRepOfClient() {
        ClientId repClientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        long repUid = RandomNumberUtils.nextPositiveLong();

        Long operatorUid = RandomNumberUtils.nextPositiveLong();
        ClientId agencyClientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());

        Client client = defaultClient()
                .withAgencyUserId(repUid)
                .withAgencyClientId(repClientId.asLong())
                .withClientId(RandomNumberUtils.nextPositiveLong());

        initMocksInitial();
        doReturn(RbacRole.AGENCY).when(rbacService).getUidRole(eq(operatorUid));
        doReturn(agencyClientId.asLong()).when(shardSupport).getValue(eq(ShardKey.UID), anyLong(),
                eq(ShardKey.CLIENT_ID), eq(Long.class));

        Representative agencyRep = Representative.create(
                ClientId.fromLong(RandomNumberUtils.nextPositiveLong()),
                RandomNumberUtils.nextPositiveLong(),
                RbacRepType.LIMITED);
        Representative clientAgencyRep = Representative.create(
                repClientId,
                repUid,
                RbacRepType.LIMITED);
        doReturn(List.of(agencyRep, clientAgencyRep))
                .when(rbacService).getAgencyLimitedRepresentatives(any());
        MockitoAnnotations.initMocks(this);

        UidAndClientId agencyUidAndClientId = support.getAgencyUidAndClientId(operatorUid,
                client.getAgencyUserId(), client.getAgencyClientId());

        assertThat(agencyUidAndClientId).isEqualTo(UidAndClientId.of(repUid, repClientId));
    }

    @Test
    public void getAgencyUidAndClientId_OperatorIsAgencyWithLimitedRep() {
        long repUid = RandomNumberUtils.nextPositiveLong();

        Long operatorUid = RandomNumberUtils.nextPositiveLong();
        ClientId agencyClientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());

        Client client = defaultClient()
                .withAgencyUserId(repUid)
                .withAgencyClientId(agencyClientId.asLong())
                .withClientId(RandomNumberUtils.nextPositiveLong());

        initMocksInitial();
        doReturn(RbacRole.AGENCY).when(rbacService).getUidRole(eq(operatorUid));
        doReturn(agencyClientId.asLong()).when(shardSupport).getValue(eq(ShardKey.UID), anyLong(),
                eq(ShardKey.CLIENT_ID), eq(Long.class));

        Representative agencyRep = Representative.create(
                ClientId.fromLong(RandomNumberUtils.nextPositiveLong()),
                RandomNumberUtils.nextPositiveLong(),
                RbacRepType.LIMITED);
        doReturn(List.of(agencyRep))
                .when(rbacService).getAgencyLimitedRepresentatives(any());
        when(userRepository.getUidsByClientIds(anyInt(), anyCollection())).thenReturn(Map.of(
                agencyClientId, List.of(operatorUid, repUid)
        ));
        MockitoAnnotations.initMocks(this);

        UidAndClientId agencyUidAndClientId = support.getAgencyUidAndClientId(operatorUid,
                client.getAgencyUserId(), client.getAgencyClientId());

        assertThat(agencyUidAndClientId).isEqualTo(UidAndClientId.of(operatorUid, agencyClientId));
    }

    @Test
    public void getAgencyUidAndClientId_OperatorIsNotAgencyClientHasAgency() {
        Long operatorUid = RandomNumberUtils.nextPositiveLong();

        ClientId agencyClientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        long agencyUserId = RandomNumberUtils.nextPositiveLong();

        Client client = defaultClient()
                .withAgencyUserId(agencyUserId)
                .withAgencyClientId(agencyClientId.asLong())
                .withClientId(RandomNumberUtils.nextPositiveLong());

        initMocksInitial();
        doReturn(RbacRole.CLIENT).when(rbacService).getUidRole(eq(operatorUid));
        MockitoAnnotations.initMocks(this);

        UidAndClientId agencyUidAndClientId = support.getAgencyUidAndClientId(operatorUid,
                client.getAgencyUserId(), client.getAgencyClientId());

        assertThat(agencyUidAndClientId).isEqualTo(UidAndClientId.of(agencyUserId, agencyClientId));
    }

    @Test
    public void getAgencyUidAndClientId_OperatorIsNotAgencyClientHasNotAgency() {
        Long operatorUid = RandomNumberUtils.nextPositiveLong();
        Client client = defaultClient()
                .withClientId(RandomNumberUtils.nextPositiveLong());

        initMocksInitial();
        doReturn(RbacRole.CLIENT).when(rbacService).getUidRole(eq(operatorUid));
        MockitoAnnotations.initMocks(this);

        UidAndClientId agencyUidAndClientId = support.getAgencyUidAndClientId(operatorUid,
                client.getAgencyUserId(), client.getAgencyClientId());

        assertThat(agencyUidAndClientId).isNull();
    }

    private void initMocksInitial() {
        shardSupport = mock(ShardSupport.class);
        userRepository = mock(UserRepository.class);
        rbacService = mock(RbacService.class);
        shardHelper = mock(ShardHelper.class, withSettings().useConstructor(shardSupport));
        when(shardHelper.getClientIdByUid(anyLong())).thenCallRealMethod();
        when(shardHelper.groupByShard(anyCollection(), any(ShardKey.class), any(Function.class))).thenCallRealMethod();
        when(shardSupport.getShards(any(ShardKey.class), anyList())).thenAnswer((InvocationOnMock invocation) -> {
            int requiredSize = ((List) invocation.getArgument(1)).size();
            return IntStreamEx.range(0, requiredSize)
                    .boxed()
                    .map(t -> 0)
                    .toList();
        });
    }
}
