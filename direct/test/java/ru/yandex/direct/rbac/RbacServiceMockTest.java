package ru.yandex.direct.rbac;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.rbac.RbacService.INTERNAL_AD_UID_PRODUCTS_FOR_GET_METRIKA_COUNTERS;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RbacServiceMockTest {

    @Mock
    private ShardHelper shardHelper;
    @Mock
    private DslContextProvider dslContextProvider;
    @Mock
    private PpcRbac ppcRbac;
    @Mock
    private RbacClientsRelations rbacClientsRelations;

    @Spy
    @InjectMocks
    private RbacService rbacService;

    private static final ClientId CLIENT_ID = ClientId.fromLong(123);
    // yndx-unikiod
    private static final ClientId BAD_CLIENT_ID = ClientId.fromLong(40734869);

    @BeforeEach
    public void before() {
        Map<ClientId, Optional<ClientPerminfo>> permissionsWithNullChiefId = new HashMap<>();
        permissionsWithNullChiefId.put(BAD_CLIENT_ID, Optional.of(new ClientPerminfo(BAD_CLIENT_ID, null,
                RbacRole.EMPTY, null, null, null, null, null, null, true, Set.of(), null, null)));

        doReturn(permissionsWithNullChiefId)
                .when(ppcRbac).getClientsPerminfo(anyCollection());
    }

    @Test
    public void getChiefsByClientIds_NullChiefId_NotCrushed() {
        assertNull(rbacService.getChiefsByClientIds(Collections.singletonList(BAD_CLIENT_ID)).get(BAD_CLIENT_ID));
    }

    @Test
    public void getClientRepresentativesUidsForGetMetrikaCounters() {
        doReturn(false)
                .when(rbacService).isInternalAdProduct(CLIENT_ID);
        List<Long> expectedUids = List.of(RandomNumberUtils.nextPositiveLong());
        doReturn(expectedUids)
                .when(rbacService).getClientRepresentativesUids(CLIENT_ID);

        List<Long> result = rbacService.getClientRepresentativesUidsForGetMetrikaCounters(CLIENT_ID);
        assertThat(result)
                .isEqualTo(expectedUids);

        verify(rbacService).getClientRepresentativesUids(CLIENT_ID);
    }

    @Test
    public void getClientRepresentativesUidsForGetMetrikaCounters_WhenIsInternalAdProduct() {
        doReturn(true)
                .when(rbacService).isInternalAdProduct(CLIENT_ID);

        List<Long> result = rbacService.getClientRepresentativesUidsForGetMetrikaCounters(CLIENT_ID);
        assertThat(result)
                .isEqualTo(List.of(INTERNAL_AD_UID_PRODUCTS_FOR_GET_METRIKA_COUNTERS));

        verify(rbacService, never()).getClientRepresentativesUids(any());
    }

}
