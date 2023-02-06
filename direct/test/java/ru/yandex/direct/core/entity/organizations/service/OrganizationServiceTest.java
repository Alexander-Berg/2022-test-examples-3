package ru.yandex.direct.core.entity.organizations.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.organizations.swagger.OrganizationsClient;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish.PUBLISHED;
import static ru.yandex.direct.core.testing.steps.OrganizationsSteps.mockOrganizationsClient;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
public class OrganizationServiceTest {

    private ClientId clientId = ClientId.fromLong(1L);
    private long permalinkId = 123L;
    private OrganizationsClient client;
    private OrganizationService organizationService;

    @Before
    public void setUp() throws Exception {
        client = mock(OrganizationsClient.class);
        organizationService = new OrganizationService(null, client, null, null, null);
    }

    @Test
    public void getClientOrganizations_EmptyPermalinkIdsPassed_ZeroInvocations() {
        organizationService.getClientOrganizations(emptyList(), clientId);
        verifyZeroInteractions(client);
    }

    @Test
    public void getClientOrganizations_NullPermalinkId_ZeroInvocations() {
        organizationService.getClientOrganizations(singletonList(null), clientId);
        verifyZeroInteractions(client);
    }

    @Test
    public void getClientOrganizations_OnePermalinkId_OrganizationReturned() {
        mockOrganizationsClient(client, List.of(permalinkId));

        Map<Long, Organization> availableOrganizationByPermalinkId =
                organizationService.getClientOrganizations(singleton(permalinkId), clientId);

        assumeThat(availableOrganizationByPermalinkId.entrySet(), hasSize(1));

        Organization organization = availableOrganizationByPermalinkId.get(permalinkId);
        Organization expectedOrganization = new Organization()
                .withPermalinkId(permalinkId)
                .withClientId(clientId)
                .withStatusPublish(PUBLISHED);

        assertThat(organization, beanDiffer(expectedOrganization));
    }

    @Test
    public void getClientOrganizations_TwoEqualPermalinkIds_OneOrganizationReturned() {
        mockOrganizationsClient(client, List.of(permalinkId));

        Map<Long, Organization> availableOrganizationByPermalinkId =
                organizationService.getClientOrganizations(List.of(permalinkId, permalinkId), clientId);

        assumeThat(availableOrganizationByPermalinkId.entrySet(), hasSize(1));

        Organization organization = availableOrganizationByPermalinkId.get(permalinkId);
        Organization expectedOrganization = new Organization()
                .withPermalinkId(permalinkId)
                .withClientId(clientId)
                .withStatusPublish(PUBLISHED);

        assertThat(organization, beanDiffer(expectedOrganization));
    }

    @Test
    public void getClientOrganizations_TwoPermalinkIds_OnePermalinkIdIsNull_OneOrganizationReturned() {
        mockOrganizationsClient(client, List.of(permalinkId));

        Map<Long, Organization> availableOrganizationByPermalinkId =
                organizationService.getClientOrganizations(asList(permalinkId, null), clientId);

        assumeThat(availableOrganizationByPermalinkId.entrySet(), hasSize(1));

        Organization organization = availableOrganizationByPermalinkId.get(permalinkId);
        Organization expectedOrganization = new Organization()
                .withPermalinkId(permalinkId)
                .withClientId(clientId)
                .withStatusPublish(PUBLISHED);

        assertThat(organization, beanDiffer(expectedOrganization));
    }

    @Test
    public void getAccessibleOrganizations_TwoPermalinkIds_OnePermalinkNotFound_OneOrganizationReturned() {
        long someOtherPermalink = permalinkId + 1;
        mockOrganizationsClient(client, List.of(permalinkId));

        Map<Long, Organization> availableOrganizationByPermalinkId =
                organizationService.getClientOrganizations(List.of(someOtherPermalink, permalinkId), clientId);

        assumeThat(availableOrganizationByPermalinkId.entrySet(), hasSize(1));

        Organization organization = availableOrganizationByPermalinkId.get(permalinkId);
        Organization expectedOrganization = new Organization()
                .withPermalinkId(permalinkId)
                .withClientId(clientId)
                .withStatusPublish(PUBLISHED);

        assertThat(organization, beanDiffer(expectedOrganization));
    }

    @Test
    public void getAvailablePermalinkIds_emptyPermalinks_returnEmpty() {
        Set<Long> result = organizationService.getAvailablePermalinkIds(emptyList());
        assertThat(result, empty());
        verifyZeroInteractions(client);
    }

    @Test
    public void getAvailablePermalinkIds_notFoundPermalinkId_returnEmpty() {
        Set<Long> result = organizationService.getAvailablePermalinkIds(List.of(permalinkId));
        assertThat(result, empty());
    }

    @Test
    public void getAvailablePermalinkIds_nullsInPermalinks_returnEmpty() {
        Set<Long> result = organizationService.getAvailablePermalinkIds(singletonList(null));
        assertThat(result, empty());
        verifyZeroInteractions(client);
    }

    @Test
    public void getAvailablePermalinkIds_publishedPermalink_returnPermalinkWithOneUid() {
        mockOrganizationsClient(client, List.of(permalinkId));
        Set<Long> result = organizationService.getAvailablePermalinkIds(List.of(permalinkId));
        assertThat(result, hasSize(1));
        assertThat(result.iterator().next(), equalTo(permalinkId));
    }
}
