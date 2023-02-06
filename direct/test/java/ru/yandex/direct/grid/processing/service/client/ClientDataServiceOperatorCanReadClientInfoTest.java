package ru.yandex.direct.grid.processing.service.client;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.security.SecurityTranslations;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.exception.GdExceptions;
import ru.yandex.direct.grid.processing.exception.GridPublicException;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class ClientDataServiceOperatorCanReadClientInfoTest {
    private static final Long OPERATOR_UID = RandomNumberUtils.nextPositiveLong();
    private static final Long CLIENT_ID = RandomNumberUtils.nextPositiveLong();
    private static final long CLIENT_UID = RandomNumberUtils.nextPositiveLong();

    @Mock
    private RbacService rbacService;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Spy
    @InjectMocks
    private ClientDataService clientDataService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);
        doReturn(CLIENT_UID).when(rbacService).getChiefByClientId(eq(ClientId.fromLong(CLIENT_ID)));
    }

    @SuppressWarnings("unused")
    private Object[] parametrizedTestData() {
        return new Object[][]{
                {" Has clientId, can read, is limited for agency", CLIENT_ID, true, true, null},
                {" Has clientId, can read, isn't limited for agency", CLIENT_ID, true, false, null},
                {" Has clientId, can't read, is limited for agency", CLIENT_ID, false, true, null},
                {" Has clientId, can't read, isn't limited for agency", CLIENT_ID, false, false,
                        getNoAccessException()},
                {" No clientId, can read, is limited for agency", null, true, true, getNoClientIdException()},
        };
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{0}")
    public void checkIsLimitedAgencyRepresentativeRequestsAgencyInfo_limitedRepForSelfAgency(
            @SuppressWarnings("unused") String description,
            @Nullable Long clientId,
            Boolean canRead,
            Boolean limitedAgencyRep,
            @Nullable GridPublicException expectedException) {
        doReturn(canRead).when(rbacService).canRead(eq(OPERATOR_UID), eq(CLIENT_UID));
        doReturn(limitedAgencyRep).when(clientDataService)
                .isLimitedAgencyRepresentativeRequestsAgencyInfo(eq(OPERATOR_UID), eq(CLIENT_UID));
        doCallRealMethod().when(clientDataService).checkOperatorCanReadClientInfo(any(), any());

        if (expectedException != null) {
            thrown.expect(expectedException.getClass());
            thrown.expectMessage(expectedException.getMessage());
        }

        clientDataService.checkOperatorCanReadClientInfo(OPERATOR_UID, clientId);
    }

    private static GridPublicException getNoClientIdException() {
        return new GridPublicException(GdExceptions.ACCESS_DENIED, "Client id is not defined",
                SecurityTranslations.INSTANCE.accessDenied());
    }

    private static GridPublicException getNoAccessException() {
        return new GridPublicException(GdExceptions.ACCESS_DENIED,
                String.format("Operator with uid %s cannot access client with id %s", OPERATOR_UID,
                        ClientId.fromLong(CLIENT_ID)),
                SecurityTranslations.INSTANCE.accessDenied());
    }
}
