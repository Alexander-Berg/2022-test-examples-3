package ru.yandex.direct.grid.processing.service.operator;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestClients;
import ru.yandex.direct.core.testing.data.TestUsers;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.model.client.GdClientAccess;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.client.GdUserInfo;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.regions.GeoTreeType;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.client.model.PhoneVerificationStatus.VERIFIED;
import static ru.yandex.direct.grid.processing.service.client.ClientDataService.createClientInfo;
import static ru.yandex.direct.grid.processing.service.operator.UserDataConverter.toGdUserInfo;
import static ru.yandex.direct.grid.processing.util.UserHelper.defaultClientNds;
import static ru.yandex.direct.regions.Region.KAZAKHSTAN_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.TURKEY_REGION_ID;

@RunWith(MockitoJUnitRunner.class)
public class OperatorAccessServiceCanUseSupportChatTest extends OperatorAccessServiceBaseTest {
    private static final int TEST_SHARD = RandomNumberUtils.nextPositiveInteger(22);
    private static final Long TEST_OPERATOR_USER_ID = 1L;
    private static final ClientId TEST_OPERATOR_CLIENT_ID = ClientId.fromLong(100L);
    private static final String FEATURE_NAME = "checked_support_chat";

    @Test
    public void operator_noFeature_cannot() {
        GdClientAccess access = operatorAccessService
                .getAccess(
                        operator().withRole(RbacRole.CLIENT),
                        clientInfo().withChiefUser(new GdUserInfo()
                                .withUserId(TEST_OPERATOR_USER_ID)
                                .withClientId(TEST_OPERATOR_CLIENT_ID.asLong())),
                        operator(),
                        Instant.now()
                );
        assertThat(access.getOperatorCanUseSupportChat(), is(false));
    }

    @Test
    public void operator_isFreelancer_can() {
        when(featureService.getEnabledForClientId(any(ClientId.class))).thenReturn(Set.of(FEATURE_NAME));
        when(freelancerService.isFreelancer(TEST_OPERATOR_CLIENT_ID)).thenReturn(true);
        GdClientAccess access = operatorAccessService
                .getAccess(
                        operator().withRole(RbacRole.CLIENT),
                        clientInfo().withChiefUser(new GdUserInfo()
                                .withUserId(TEST_OPERATOR_USER_ID)
                                .withClientId(TEST_OPERATOR_CLIENT_ID.asLong())),
                        operator(),
                        Instant.now()
                );
        assertThat(access.getOperatorCanUseSupportChat(), is(true));
    }

    @Test
    public void operator_freeClient_fromRussia_can() {
        when(featureService.getEnabledForClientId(any(ClientId.class))).thenReturn(Set.of(FEATURE_NAME));
        GdClientAccess access = operatorAccessService
                .getAccess(
                        operator().withRole(RbacRole.CLIENT),
                        clientInfo()
                                .withChiefUser(new GdUserInfo()
                                        .withUserId(TEST_OPERATOR_USER_ID)
                                        .withClientId(TEST_OPERATOR_CLIENT_ID.asLong()))
                                .withCountryRegionId(RUSSIA_REGION_ID),
                        operator(),
                        Instant.now()
                );
        assertThat(access.getOperatorCanUseSupportChat(), is(true));
    }

    @Test
    public void operator_freeClient_fromKazakhstan_can() {
        when(featureService.getEnabledForClientId(any(ClientId.class))).thenReturn(Set.of(FEATURE_NAME));
        GdClientAccess access = operatorAccessService
                .getAccess(
                        operator().withRole(RbacRole.CLIENT),
                        clientInfo()
                                .withChiefUser(new GdUserInfo()
                                        .withUserId(TEST_OPERATOR_USER_ID)
                                        .withClientId(TEST_OPERATOR_CLIENT_ID.asLong()))
                                .withCountryRegionId(KAZAKHSTAN_REGION_ID),
                        operator(),
                        Instant.now()
                );
        assertThat(access.getOperatorCanUseSupportChat(), is(true));
    }

    @Test
    public void operator_freeClient_notFromKUB_cannot() {
        when(featureService.getEnabledForClientId(any(ClientId.class))).thenReturn(Set.of(FEATURE_NAME));
        GdClientAccess access = operatorAccessService
                .getAccess(
                        operator().withRole(RbacRole.CLIENT),
                        clientInfo()
                                .withChiefUser(new GdUserInfo()
                                        .withUserId(TEST_OPERATOR_USER_ID)
                                        .withClientId(TEST_OPERATOR_CLIENT_ID.asLong()))
                                .withCountryRegionId(TURKEY_REGION_ID),
                        operator(),
                        Instant.now()
                );
        assertThat(access.getOperatorCanUseSupportChat(), is(false));
    }

    @Test
    public void operator_agencyClient_fromRussia_cannot() {
        when(featureService.getEnabledForClientId(any(ClientId.class))).thenReturn(Set.of(FEATURE_NAME));
        GdClientAccess access = operatorAccessService
                .getAccess(
                        operator().withRole(RbacRole.CLIENT),
                        clientInfo()
                                .withChiefUser(new GdUserInfo()
                                        .withUserId(TEST_OPERATOR_USER_ID)
                                        .withClientId(TEST_OPERATOR_CLIENT_ID.asLong()))
                                .withCountryRegionId(RUSSIA_REGION_ID)
                                .withAgencyClientId(1000L),
                        operator(),
                        Instant.now()
                );
        assertThat(access.getOperatorCanUseSupportChat(), is(false));
    }

    @Test
    public void operator_managerClient_fromRussia_can() {
        when(featureService.getEnabledForClientId(any(ClientId.class))).thenReturn(Set.of(FEATURE_NAME));
        GdClientAccess access = operatorAccessService
                .getAccess(
                        operator().withRole(RbacRole.CLIENT),
                        clientInfo()
                                .withChiefUser(new GdUserInfo()
                                        .withUserId(TEST_OPERATOR_USER_ID)
                                        .withClientId(TEST_OPERATOR_CLIENT_ID.asLong()))
                                .withCountryRegionId(RUSSIA_REGION_ID)
                                .withManagerUserId(60L),
                        operator(),
                        Instant.now()
                );
        assertThat(access.getOperatorCanUseSupportChat(), is(true));
    }


    @Test
    public void operator_differentClient_fromRussia_cannot() {
        when(featureService.getEnabledForClientId(any(ClientId.class))).thenReturn(Set.of(FEATURE_NAME));
        GdClientAccess access = operatorAccessService
                .getAccess(
                        operator().withRole(RbacRole.CLIENT),
                        clientInfo()
                                .withChiefUser(new GdUserInfo()
                                        .withUserId(2L)
                                        .withClientId(200L))
                                .withCountryRegionId(RUSSIA_REGION_ID)
                                .withId(200L),
                        operator().withClientId(ClientId.fromLong(200L)),
                        Instant.now()
                );
        assertThat(access.getOperatorCanUseSupportChat(), is(false));
    }

    @Test
    public void operator_manager_fromRussia_cannot() {
        when(featureService.getEnabledForClientId(any(ClientId.class))).thenReturn(Set.of(FEATURE_NAME));
        GdClientAccess access = operatorAccessService
                .getAccess(
                        operator().withRole(RbacRole.MANAGER),
                        clientInfo()
                                .withChiefUser(new GdUserInfo()
                                        .withUserId(TEST_OPERATOR_USER_ID)
                                        .withClientId(TEST_OPERATOR_CLIENT_ID.asLong()))
                                .withCountryRegionId(RUSSIA_REGION_ID),
                        operator(),
                        Instant.now()
                );
        assertThat(access.getOperatorCanUseSupportChat(), is(false));
    }

    private static User operator() {
        return TestUsers.defaultUser()
                .withId(TEST_OPERATOR_USER_ID)
                .withClientId(TEST_OPERATOR_CLIENT_ID);
    }

    private static GdClientInfo clientInfo() {
        return createClientInfo(TEST_SHARD, TestClients.defaultClient()
                        .withId(TEST_OPERATOR_CLIENT_ID.asLong())
                        .withChiefUid(TEST_OPERATOR_USER_ID),
                Map.of(TEST_OPERATOR_USER_ID, chiefGdInfo()), null, Map.of(), Map.of(),
                defaultClientNds(TEST_OPERATOR_CLIENT_ID.asLong()), GeoTreeType.GLOBAL,
                null, emptySet(), emptySet(), false, emptyMap(), emptyMap(), false, VERIFIED, false);
    }

    private static GdUserInfo chiefGdInfo() {
        return toGdUserInfo(
                TestUsers.defaultUser()
                        .withId(TEST_OPERATOR_USER_ID)
                        .withClientId(TEST_OPERATOR_CLIENT_ID)
                        .withChiefUid(TEST_OPERATOR_USER_ID)
        );
    }
}
