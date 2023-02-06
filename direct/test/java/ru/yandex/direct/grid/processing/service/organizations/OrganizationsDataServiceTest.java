package ru.yandex.direct.grid.processing.service.organizations;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.altay.model.language.LanguageOuterClass;
import ru.yandex.direct.core.entity.clientphone.ClientPhoneService;
import ru.yandex.direct.core.entity.organizations.service.OrganizationService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.organization.GdOrganizationsContainer;
import ru.yandex.direct.grid.processing.model.organizations.GdOrganization;
import ru.yandex.direct.grid.processing.model.organizations.GdOrganizationAccess;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.service.organizations.loader.CanOverridePhoneDataLoader;
import ru.yandex.direct.grid.processing.service.organizations.loader.OperatorCanEditDataLoader;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;
import ru.yandex.direct.organizations.swagger.OrganizationInfo;
import ru.yandex.direct.organizations.swagger.OrganizationsClient;
import ru.yandex.direct.organizations.swagger.OrganizationsClientException;
import ru.yandex.direct.organizations.swagger.model.MetrikaData;
import ru.yandex.direct.rbac.RbacService;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.altay.model.language.LanguageOuterClass.Language.RU;
import static ru.yandex.direct.core.testing.data.TestUsers.defaultUser;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;

@ParametersAreNonnullByDefault
public class OrganizationsDataServiceTest {

    private static final Long REPR_UID = RandomUtils.nextLong(0L, 100000L);

    private OrganizationsClient organizationsClient;
    private OrganizationsDataService organizationsDataService;
    private ClientPhoneService clientPhoneService;
    private CanOverridePhoneDataLoader canOverridePhoneDataLoader;

    @Before
    public void before() throws Exception {
        organizationsClient = mock(OrganizationsClient.class);
        clientPhoneService = mock(ClientPhoneService.class);
        RbacService rbacService = mock(RbacService.class);
        when(rbacService.getClientRepresentativesUids(any())).thenReturn(Collections.singleton(REPR_UID));
        User user = new User().withUid(REPR_UID).withClientId(ClientId.fromLong(RandomUtils.nextLong(0L, 100000L)));
        GridGraphQLContext graphQLContext = new GridGraphQLContext(user, user);
        GridContextProvider gridContextProvider = new GridContextProvider();
        gridContextProvider.setGridContext(graphQLContext);
        var organizationService = new OrganizationService(null, organizationsClient, null, rbacService, null);
        canOverridePhoneDataLoader = new CanOverridePhoneDataLoader(gridContextProvider, organizationService);
        organizationsDataService =
                new OrganizationsDataService(
                        organizationService,
                        clientPhoneService,
                        mock(GridValidationService.class),
                        mock(OperatorCanEditDataLoader.class),
                        canOverridePhoneDataLoader);
    }

    @Test
    public void getOnlineOrganization() {
        Long permalinkId = nextPositiveLong();
        OrganizationInfo organizationInfo = createOrganizationInfo();
        organizationInfo.withIsOnline(true).withPermalinkId(permalinkId);
        List<Long> permalinkIds = List.of(permalinkId);
        when(organizationsClient
                .getOrganizationsInfo(anyList(), any(LanguageOuterClass.Language.class), anyString(), anySet()))
                .thenReturn(List.of(organizationInfo));
        when(organizationsClient.getOrganizationsUidsWithModifyPermission(anyCollection(), anyCollection(), anyString(),
                anyString())).thenReturn(Map.of(permalinkId, List.of(REPR_UID)));
        GdOrganizationsContainer input = new GdOrganizationsContainer().withPermalinkIds(permalinkIds).withLanguage(RU);
        List<GdOrganization> results = organizationsDataService.getOrganizations(defaultUser(), input,
                GdOrganization.allModelProperties());
        fillAccessFields(results);
        assertThat(results.size(), equalTo(1));
        compare(results.get(0), organizationInfo, true);
    }

    @Test
    public void getOrganization_telephonyAllowed_turnOnCalltracking() {
        Long permalinkId = nextPositiveLong();
        MetrikaData metrikaData = new MetrikaData();
        long counterId = nextPositiveLong();
        metrikaData.setPermalink(permalinkId);
        OrganizationInfo organizationInfo = createOrganizationInfo().withMetrikaData(metrikaData);
        metrikaData.setCounter(String.valueOf(counterId));
        organizationInfo.withIsOnline(true).withPermalinkId(permalinkId);
        List<Long> permalinkIds = List.of(permalinkId);
        when(organizationsClient
                .getOrganizationsInfo(anyList(), any(LanguageOuterClass.Language.class), anyString(), anySet()))
                .thenReturn(List.of(organizationInfo));
        GdOrganizationsContainer input = new GdOrganizationsContainer().withPermalinkIds(permalinkIds).withLanguage(RU);
        User subjectUser = defaultUser().withClientId(ClientId.fromLong(1L));
        List<GdOrganization> results = organizationsDataService.getOrganizations(subjectUser, input,
                GdOrganization.allModelProperties());
        fillAccessFields(results);
        assertThat(results.size(), equalTo(1));
        verify(clientPhoneService).getAndSaveTelephonyPhones(any(ClientId.class), anyList());
    }

    @Test
    public void getOrganization_telephonyNotAllowed_turnOnCalltracking() {
        Long permalinkId = nextPositiveLong();
        OrganizationInfo organizationInfo = createOrganizationInfo();
        organizationInfo.withIsOnline(true).withPermalinkId(permalinkId);
        List<Long> permalinkIds = List.of(permalinkId);
        when(organizationsClient
                .getOrganizationsInfo(anyList(), any(LanguageOuterClass.Language.class), anyString(), anySet()))
                .thenReturn(List.of(organizationInfo));
        GdOrganizationsContainer input = new GdOrganizationsContainer().withPermalinkIds(permalinkIds).withLanguage(RU);
        User subjectUser = defaultUser().withClientId(ClientId.fromLong(1L));
        List<GdOrganization> results = organizationsDataService.getOrganizations(subjectUser, input,
                GdOrganization.allModelProperties());
        fillAccessFields(results);
        assertThat(results.size(), equalTo(1));
        verify(clientPhoneService).getAndSaveTelephonyPhones(any(ClientId.class), anyList());
    }

    @Test
    public void getOnlineOrganization_ThrowOnCheckAccess() {
        Long permalinkId = nextPositiveLong();
        OrganizationInfo organizationInfo = createOrganizationInfo();
        organizationInfo.withIsOnline(true).withPermalinkId(permalinkId);
        List<Long> permalinkIds = List.of(permalinkId);
        when(organizationsClient
                .getOrganizationsInfo(anyList(), any(LanguageOuterClass.Language.class), anyString(), anySet()))
                .thenReturn(List.of(organizationInfo));
        when(organizationsClient.getOrganizationsUidsWithModifyPermission(anyList(), anyCollection(), anyString(),
                anyString())).thenThrow(OrganizationsClientException.class);
        GdOrganizationsContainer input = new GdOrganizationsContainer().withPermalinkIds(permalinkIds).withLanguage(RU);
        List<GdOrganization> results = organizationsDataService.getOrganizations(defaultUser(), input,
                GdOrganization.allModelProperties());
        fillAccessFields(results);
        assertThat(results.size(), equalTo(1));
        compare(results.get(0), organizationInfo, false);
    }

    @Test
    public void getNotOnlineOrganization() {
        Long permalinkId = nextPositiveLong();
        OrganizationInfo organizationInfo = createOrganizationInfo();
        organizationInfo.withIsOnline(false).withPermalinkId(permalinkId);
        List<Long> permalinkIds = List.of(permalinkId);
        when(organizationsClient
                .getOrganizationsInfo(anyList(), any(LanguageOuterClass.Language.class), anyString(), anySet()))
                .thenReturn(List.of(organizationInfo));
        GdOrganizationsContainer input = new GdOrganizationsContainer().withPermalinkIds(permalinkIds).withLanguage(RU);
        List<GdOrganization> results = organizationsDataService.getOrganizations(defaultUser(), input,
                GdOrganization.allModelProperties());
        fillAccessFields(results);
        assertThat(results.size(), equalTo(1));
        compare(results.get(0), organizationInfo, false);
    }

    @Test
    public void getMultipleOrganization() {
        Long onlinePermalinkId = nextPositiveLong();
        Long offlinePermalinkId = nextPositiveLong();
        OrganizationInfo onlineOrganization = createOrganizationInfo();
        onlineOrganization.withIsOnline(true).withPermalinkId(onlinePermalinkId);
        OrganizationInfo offlineOrganization = createOrganizationInfo();
        offlineOrganization.withIsOnline(false).withPermalinkId(offlinePermalinkId);
        List<Long> permalinkIds = List.of(onlinePermalinkId, offlinePermalinkId);
        when(organizationsClient
                .getOrganizationsInfo(eq(permalinkIds), any(LanguageOuterClass.Language.class), anyString(), anySet()))
                .thenReturn(List.of(onlineOrganization, offlineOrganization));
        GdOrganizationsContainer input = new GdOrganizationsContainer().withPermalinkIds(permalinkIds).withLanguage(RU);
        List<GdOrganization> results = organizationsDataService.getOrganizations(defaultUser(), input,
                GdOrganization.allModelProperties());
        fillAccessFields(results);
        assertThat(results.size(), equalTo(2));
        compare(results.get(0), onlineOrganization, false);
        compare(results.get(1), offlineOrganization, false);
    }

    @Test
    public void getOrganizationWithCounter() {
        Long permalinkId = nextPositiveLong();
        long counterId = nextPositiveLong();
        MetrikaData metrikaData = new MetrikaData();
        metrikaData.setPermalink(permalinkId);
        metrikaData.setCounter(String.valueOf(counterId));

        OrganizationInfo organizationInfo = createOrganizationInfo().withMetrikaData(metrikaData);
        organizationInfo.setPermalinkId(permalinkId);
        List<Long> permalinkIds = List.of(permalinkId);
        when(organizationsClient
                .getOrganizationsInfo(anyList(), any(LanguageOuterClass.Language.class), anyString(), anySet()))
                .thenReturn(List.of(organizationInfo));

        GdOrganizationsContainer input = new GdOrganizationsContainer().withPermalinkIds(permalinkIds).withLanguage(RU);
        List<GdOrganization> results = organizationsDataService.getOrganizations(defaultUser(), input,
                GdOrganization.allModelProperties());
        fillAccessFields(results);
        assertThat(results.size(), equalTo(1));
        Long orgCounterId = results.get(0).getCounterId();
        assertThat(orgCounterId, equalTo(counterId));
    }

    private void fillAccessFields(List<GdOrganization> results) {
        results.forEach(t -> t.withAccess(
                new GdOrganizationAccessWithAdditionalFields(t.getAccess(), organizationsDataService)));
        canOverridePhoneDataLoader.get().dispatchAndJoin();
    }

    private void compare(GdOrganization data, OrganizationInfo expected, boolean canOverridePhone) {

        assertThat(data.getPermalinkId(), equalTo(expected.getPermalinkId()));
        assertThat(data.getIsOnline(), equalTo(expected.getIsOnline()));
        assertThat(((GdOrganizationAccessWithAdditionalFields) data.getAccess()).getCanOverridePhone().join(),
                equalTo(canOverridePhone));
    }

    private OrganizationInfo createOrganizationInfo() {
        return new OrganizationInfo();
    }


    class GdOrganizationAccessWithAdditionalFields extends GdOrganizationAccess {
        CompletableFuture<Boolean> canOverridePhone;

        public GdOrganizationAccessWithAdditionalFields(GdOrganizationAccess access,
                                                        OrganizationsDataService organizationsDataService) {
            this.withPermalinkId(access.getPermalinkId());
            this.canOverridePhone = organizationsDataService.getCanOverridePhone(access.getPermalinkId());
        }

        CompletableFuture<Boolean> getCanOverridePhone() {
            return canOverridePhone;
        }
    }

}
