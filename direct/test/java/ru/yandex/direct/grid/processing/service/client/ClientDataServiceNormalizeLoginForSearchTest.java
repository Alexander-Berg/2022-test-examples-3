package ru.yandex.direct.grid.processing.service.client;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.dbutil.sharding.ShardSupport;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClientSearchRequest;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class ClientDataServiceNormalizeLoginForSearchTest {


    @Mock
    private ShardSupport shardSupport;

    @Mock
    private BlackboxUserService blackboxUserService;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Spy
    @InjectMocks
    private ClientDataService clientDataService;

    private GridGraphQLContext context;

    @SuppressWarnings("unused")
    private Object[] parametrizedTestData() {
        return new Object[][]{
                {emptyList(), emptySet()},
                {List.of("login"), Set.of("login")},
                {List.of("login-with-minus"), Set.of("login-with-minus")},
                {List.of("login.with.dots"), Set.of("login-with-dots")},
                {List.of("vErY.cOmPlEx-LoGiN"), Set.of("very-complex-login")},
        };
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        context = new GridGraphQLContext(new User());
        doReturn(emptyMap()).when(shardSupport).getValuesMap(any(), any(), any(), any());
        doReturn(Optional.empty()).when(blackboxUserService).getUidByLogin(anyString());
        doReturn(emptyList()).when(clientDataService).getClientInfo(eq(context), eq(emptySet()));
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{0}")
    public void checkLoginNormalizing(List<String> input, Set<String> expected) {
        clientDataService.getClientInfo(context, (List<GdClientSearchRequest>) mapList(input,
                l -> new GdClientSearchRequest().withLogin(l)));
        verify(clientDataService).getClientInfo(context, emptySet(), emptySet(), expected);
    }
}
