package ru.yandex.direct.core.entity.user.repository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.dbschema.ppc.Tables.API_SPECIAL_USER_OPTIONS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ApiUserRepositoryTest {
    private static final int SHARD = 1;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Mock
    private RbacService rbacService;

    private ApiUserRepository apiUserRepository;
    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        apiUserRepository = new ApiUserRepository(dslContextProvider, rbacService);

        clientInfo = steps.clientSteps().createDefaultClient();

        dslContextProvider.ppc(clientInfo.getShard())
                .insertInto(API_SPECIAL_USER_OPTIONS,
                        API_SPECIAL_USER_OPTIONS.CLIENT_ID, API_SPECIAL_USER_OPTIONS.KEYNAME,
                        API_SPECIAL_USER_OPTIONS.VALUE)
                .values(clientInfo.getClientId().asLong(), "concurrent_calls", 128L)
                .execute();
    }

    @Test
    public void fetchByUid_readConcurrentCalls() {
        when(rbacService.getUidRole(anyLong())).thenReturn(RbacRole.CLIENT);

        ApiUser apiUser = apiUserRepository.fetchByUid(clientInfo.getShard(), clientInfo.getUid());

        org.junit.Assert.assertThat(apiUser.getConcurrentCalls(), is(128L));
    }

    @Test
    public void fetchByUid_existInRbac() {
        when(rbacService.getUidRole(anyLong())).thenReturn(RbacRole.CLIENT);

        ApiUser apiUser = apiUserRepository.fetchByUid(clientInfo.getShard(), clientInfo.getUid());

        assertThat(apiUser).isNotNull();
    }

    @Test
    public void fetchByUid_notExistInRbac() {
        when(rbacService.getUidRole(anyLong())).thenReturn(null);

        ApiUser apiUser = apiUserRepository.fetchByUid(clientInfo.getShard(), clientInfo.getUid());

        assertThat(apiUser).isNull();
    }
}
