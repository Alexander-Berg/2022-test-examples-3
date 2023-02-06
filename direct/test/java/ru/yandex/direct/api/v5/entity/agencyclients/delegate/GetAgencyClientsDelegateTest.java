package ru.yandex.direct.api.v5.entity.agencyclients.delegate;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.google.common.collect.Sets;
import com.yandex.direct.api.v5.agencyclients.AgencyClientsSelectionCriteria;
import com.yandex.direct.api.v5.general.RepresentativeRoleEnum;
import com.yandex.direct.api.v5.generalclients.ClientGetItem;
import com.yandex.direct.api.v5.generalclients.Representative;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.GenericGetRequest;
import ru.yandex.direct.api.v5.entity.agencyclients.service.AgencyClientDataFetcher;
import ru.yandex.direct.api.v5.entity.agencyclients.service.RequestedField;
import ru.yandex.direct.api.v5.security.utils.ApiAuthenticationSourceMockBuilder;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.user.repository.ApiUserRepository;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@Api5Test
@RunWith(SpringRunner.class)
public class GetAgencyClientsDelegateTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Autowired
    private Steps steps;

    @Mock
    private UserService userService;

    @Autowired
    private ApiUserRepository apiUserRepository;

    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private RbacService rbacService;
    @Autowired
    private AgencyClientRelationService agencyClientRelationService;
    @Autowired
    private AgencyClientDataFetcher clientDataFetcher;

    private GetAgencyClientsDelegate getAgencyClientsDelegate;

    private UserInfo agency;
    private UserInfo user1;
    private UserInfo readonlyRep;
    private UserInfo user2;

    private static GenericGetRequest<RequestedField, AgencyClientsSelectionCriteria> defaultGetRequest() {
        return new GenericGetRequest<>(
                EnumSet.of(RequestedField.CLIENT_ID),
                new AgencyClientsSelectionCriteria(),
                new LimitOffset(10, 0));
    }

    @Before
    public void setUp() {
        agency = steps.userSteps().createDefaultUser();
        user1 = steps.userSteps().createDefaultUser();
        readonlyRep = steps.userSteps().createReadonlyRepresentative(user1.getClientInfo());
        user2 = steps.userSteps().createDefaultUser();

        doReturn(Sets.newHashSet(user1.getUid(), user2.getUid()))
                .when(userService)
                .getUserUidsWithoutHavingOnlyGeoOrMcbCampaigns(anyCollection());

        rbacService = spy(rbacService);

        doReturn(Arrays.asList(user1.getUid(), user2.getUid()))
                .when(rbacService).getAgencySubclients(eq(agency.getUid()));

        getAgencyClientsDelegate = new GetAgencyClientsDelegate(
                new ApiAuthenticationSourceMockBuilder()
                        .withOperator(apiUserRepository.fetchByUid(agency.getShard(), agency.getUid()))
                        .toApiAuthenticationSource(),
                shardHelper,
                rbacService,
                userService,
                agencyClientRelationService,
                clientDataFetcher);
    }

    @Test
    public void testAllSubclientsDontExcludeByMcbOrGeoCampaignsFilter() {
        List<ClientGetItem> actual = getAgencyClientsDelegate.get(
                new GenericGetRequest<>(
                        EnumSet.of(RequestedField.CLIENT_ID),
                        new AgencyClientsSelectionCriteria(),
                        new LimitOffset(10, 0)));

        assertThat(actual).extracting(ClientGetItem::getClientId)
                .containsOnly(user1.getClientInfo().getClientId().asLong(),
                        user2.getClientInfo().getClientId().asLong());
    }

    @Test
    public void testReadonlyRep() {
        List<ClientGetItem> actual = getAgencyClientsDelegate.get(
                new GenericGetRequest<>(
                        EnumSet.of(RequestedField.CLIENT_ID, RequestedField.REPRESENTATIVES),
                        new AgencyClientsSelectionCriteria(),
                        new LimitOffset(10, 0)));

        var expected = List.of(
                new ClientGetItem()
                        .withClientId(user1.getUser().getClientId().asLong())
                        .withRepresentatives(
                                List.of(
                                        new Representative()
                                                .withEmail(readonlyRep.getUser().getEmail())
                                                .withLogin(readonlyRep.getUser().getLogin())
                                                .withRole(RepresentativeRoleEnum.READONLY),
                                        new Representative()
                                                .withEmail(user1.getUser().getEmail())
                                                .withLogin(user1.getUser().getLogin())
                                                .withRole(RepresentativeRoleEnum.CHIEF)

                                )
                        ),
                new ClientGetItem()
                        .withClientId(user2.getUser().getClientId().asLong())
                        .withRepresentatives(
                                List.of(
                                        new Representative()
                                                .withEmail(user2.getUser().getEmail())
                                                .withLogin(user2.getUser().getLogin())
                                                .withRole(RepresentativeRoleEnum.CHIEF)

                                )
                        )
        );

        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(expected);
    }

    @Test
    public void testUser2ExcludeByMcbOrGeoCampaignsFilter() {
        doReturn(singleton(user1.getUid()))
                .when(userService)
                .getUserUidsWithoutHavingOnlyGeoOrMcbCampaigns(anyCollection());

        List<ClientGetItem> actual = getAgencyClientsDelegate.get(defaultGetRequest());

        assertThat(actual).extracting(ClientGetItem::getClientId)
                .containsOnly(user1.getClientInfo().getClientId().asLong());
    }
}
