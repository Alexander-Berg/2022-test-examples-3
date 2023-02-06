package ru.yandex.direct.core.entity.organizations.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.altay.model.language.LanguageOuterClass.Language;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.geosearch.GeosearchClient;
import ru.yandex.direct.organizations.swagger.OrganizationApiInfo;
import ru.yandex.direct.organizations.swagger.OrganizationsClient;
import ru.yandex.direct.organizations.swagger.model.MetrikaData;
import ru.yandex.direct.organizations.swagger.model.PubApiCompany;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultOrganizationApiInfo;
import static ru.yandex.direct.core.testing.steps.OrganizationsSteps.mockOrganizationsClient;

@CoreTest
@ParametersAreNonnullByDefault
@RunWith(SpringJUnit4ClassRunner.class)
public class OrganizationServiceApiTest {

    @Autowired
    private Steps steps;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private RbacService rbacService;

    private OrganizationsClient organizationsClient;
    private GeosearchClient geosearchClient;
    private OrganizationService organizationService;

    private Language language = Language.RU;
    private ClientId clientId;
    private long chiefUid;
    private long permalinkId = 10101;
    private long otherPermalinkId = 20202;
    private long permalinkIdNotInDb = 30303;

    @Before
    public void before() throws Exception {
        organizationsClient = mock(OrganizationsClient.class);
        geosearchClient = mock(GeosearchClient.class);
        organizationService = new OrganizationService(organizationRepository, organizationsClient, geosearchClient, null,
                shardHelper);

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        chiefUid = rbacService.getChiefByClientId(clientId);
        steps.organizationSteps().createClientOrganization(clientId, permalinkId);
        steps.organizationSteps().createClientOrganization(clientId, otherPermalinkId);
    }

    // *** Проверка получения всех организаций клиента ***

    /**
     * Запрос всех организаций клиента.
     * Предусловия:
     * <ol>
     * <li>У клиента есть организации в БД Директа с пермалинками {@link #permalinkId} и {@link #otherPermalinkId}.</li>
     * <li>Справочник возвращает пустой список организаций клиента.</li>
     * <li>Геопоиск не возвращает инфорамацию по склеенным организациям.</li>
     * <li>Справочник не возвращает никакой информации по организациям {@link #permalinkId} и
     * {@link #otherPermalinkId}.</li>
     * </ol>
     * Результат: список организаций клиента пуст.
     */
    @Test
    public void getApiClientOrganizations_getAllOrganizations_organizationFromDb_noInfoFromTycoon_emptyResult() {
        List<OrganizationApiInfo> organizations = organizationService
                .getApiClientOrganizations(clientId, chiefUid, language, null);
        assertThat(organizations).isEmpty();
    }

    /**
     * Запрос всех организаций клиента.
     * Предусловия:
     * <ol>
     * <li>У клиента есть организации в БД Директа с пермалинками {@link #permalinkId} и {@link #otherPermalinkId}.</li>
     * <li>Справочник возвращает пустой список организаций клиента.</li>
     * <li>Геопоиск не возвращает инфорамацию по склеенным организациям.</li>
     * <li>Справочник возвращает информацию по {@link #permalinkId} и не возвращает информацию по
     * {@link #otherPermalinkId}</li>
     * </ol>
     * Результат: список организаций клиента содержит {@link #permalinkId}.
     */
    @Test
    public void getApiClientOrganizations_getAllOrganizations_organizationFromDb_infoFromTycoon() {
        mockOrganizationsClient(organizationsClient, Set.of(permalinkId, otherPermalinkId), emptyList(),
                List.of(new PubApiCompany().id(permalinkId)));
        List<OrganizationApiInfo> organizations = organizationService
                .getApiClientOrganizations(clientId, chiefUid, language, null);

        SoftAssertions.assertSoftly(softly -> {
            assertThat(organizations).hasSize(1);
            assertThat(organizations.get(0)).isEqualTo(defaultOrganizationApiInfo(permalinkId));
        });
    }

    /**
     * Запрос всех организаций клиента.
     * Предусловия:
     * <ol>
     * <li>У клиента есть организации в БД Директа с пермалинками {@link #permalinkId} и {@link #otherPermalinkId}.</li>
     * <li>Справочник возвращает список организаций клиента с организацией {@link #permalinkId}.</li>
     * <li>Геопоиск не возвращает инфорамацию по склеенным организациям.</li>
     * <li>Справочник возвращает информацию по {@link #otherPermalinkId}</li>
     * </ol>
     * Результат: список организаций клиента содержит {@link #permalinkId} и {@link #otherPermalinkId}.
     */
    @Test
    public void getApiClientOrganizations_getAllOrganizations_organizationFromTycoon_OrganizationInDb() {
        mockOrganizationsClient(organizationsClient, emptyList(), List.of(chiefUid),
                List.of(new PubApiCompany().id(permalinkId)));
        mockOrganizationsClient(organizationsClient, Set.of(otherPermalinkId), emptyList(),
                List.of(new PubApiCompany().id(otherPermalinkId)));
        List<OrganizationApiInfo> organizations = organizationService
                .getApiClientOrganizations(clientId, chiefUid, language, null);

        SoftAssertions.assertSoftly(softly -> {
            assertThat(organizations).hasSize(2);
            assertThat(organizations).containsExactlyInAnyOrder(
                    defaultOrganizationApiInfo(permalinkId),
                    defaultOrganizationApiInfo(otherPermalinkId)
            );
        });
    }

    /**
     * Запрос всех организаций клиента.
     * Предусловия:
     * <ol>
     * <li>У клиента есть организации в БД Директа с пермалинками {@link #permalinkId} и {@link #otherPermalinkId}.</li>
     * <li>Справочник возвращает список организаций клиента с организацией {@link #permalinkIdNotInDb}.</li>
     * <li>Геопоиск не возвращает инфорамацию по склеенным организациям.</li>
     * <li>Справочник возвращает информацию по {@link #permalinkId} и по {@link #otherPermalinkId}</li>
     * </ol>
     * Результат: список организаций клиента содержит {@link #permalinkIdNotInDb},
     * {@link #permalinkId} и {@link #otherPermalinkId}.
     */
    @Test
    public void getApiClientOrganizations_getAllOrganizations_organizationFromTycoon_OrganizationNotInDb() {
        mockOrganizationsClient(organizationsClient, emptyList(), List.of(chiefUid),
                List.of(new PubApiCompany().id(permalinkIdNotInDb)));
        mockOrganizationsClient(organizationsClient, Set.of(permalinkId, otherPermalinkId), emptyList(),
                List.of(new PubApiCompany().id(permalinkId), new PubApiCompany().id(otherPermalinkId)));
        List<OrganizationApiInfo> organizations = organizationService
                .getApiClientOrganizations(clientId, chiefUid, language, null);

        SoftAssertions.assertSoftly(softly -> {
            assertThat(organizations).hasSize(3);
            assertThat(organizations).containsExactlyInAnyOrder(
                    defaultOrganizationApiInfo(permalinkIdNotInDb),
                    defaultOrganizationApiInfo(permalinkId),
                    defaultOrganizationApiInfo(otherPermalinkId)
            );
        });
    }

    /**
     * Запрос всех организаций клиента.
     * Предусловия:
     * <ol>
     * <li>У клиента есть организации в БД Директа с пермалинками {@link #permalinkId} и {@link #otherPermalinkId}.</li>
     * <li>Справочник возвращает список организаций клиента с организацией {@link #permalinkId}.</li>
     * <li>Геопоиск возвращает головной пермалинк {@link #permalinkId} cо склеенным пермалинком
     * {@link #otherPermalinkId}</li>
     * <li>Справочник возвращает информацию по {@link #otherPermalinkId}.</li>
     * </ol>
     * Результат: список организаций клиента содержит {@link #permalinkId} со списком
     * {@code #MergedIds = [mergedPermalinkId]}.
     */
    @Test
    public void getApiClientOrganizations_getAllOrganizations_infoFromGeosearch_OrganizationInDb_resultMergedOrganization() {
        mockOrganizationsClient(organizationsClient, emptyList(), List.of(chiefUid),
                List.of(new PubApiCompany().id(permalinkId)));
        when(geosearchClient.getMergedPermalinks(eq(Set.of(otherPermalinkId))))
                .thenReturn(Map.of(permalinkId, Set.of(otherPermalinkId)));
        mockOrganizationsClient(organizationsClient, Set.of(otherPermalinkId), emptyList(),
                List.of(new PubApiCompany().id(otherPermalinkId)));
        List<OrganizationApiInfo> organizations = organizationService
                .getApiClientOrganizations(clientId, chiefUid, language, null);

        SoftAssertions.assertSoftly(softly -> {
            assertThat(organizations).hasSize(1);
            assertThat(organizations.get(0)).isEqualTo(
                    defaultOrganizationApiInfo(permalinkId)
                            .withMergedPermalinks(Set.of(otherPermalinkId))
            );
        });
    }

    /**
     * Запрос всех организаций клиента.
     * Предусловия:
     * <ol>
     * <li>У клиента есть организации в БД Директа с пермалинками {@link #permalinkId} и {@link #otherPermalinkId}.</li>
     * <li>Справочник возвращает список организаций клиента с организацией {@link #permalinkIdNotInDb}.</li>
     * <li>Геопоиск возвращает головной пермалинк {@link #permalinkIdNotInDb} cо склеенными пермалинками
     * {@link #permalinkId} и {@link #otherPermalinkId}</li>
     * <li>Справочник возвращает информацию по {@link #permalinkId} и {@link #otherPermalinkId}.</li>
     * </ol>
     * Результат: список организаций клиента содержит {@link #permalinkId} со списком
     * {@code MergedIds = [permalinkId, mergedPermalinkId]}.
     */
    @Test
    public void getApiClientOrganizations_getAllOrganizations_infoFromGeosearch_OrganizationNotInDb_resultMergedOrganization() {
        mockOrganizationsClient(organizationsClient, emptyList(), List.of(chiefUid),
                List.of(new PubApiCompany().id(permalinkIdNotInDb)));
        when(geosearchClient.getMergedPermalinks(eq(Set.of(permalinkId, otherPermalinkId))))
                .thenReturn(Map.of(permalinkIdNotInDb, Set.of(permalinkId, otherPermalinkId)));
        mockOrganizationsClient(organizationsClient, Set.of(permalinkId, otherPermalinkId), emptyList(),
                List.of(new PubApiCompany().id(permalinkId), new PubApiCompany().id(otherPermalinkId)));
        List<OrganizationApiInfo> organizations = organizationService
                .getApiClientOrganizations(clientId, chiefUid, language, null);

        SoftAssertions.assertSoftly(softly -> {
            assertThat(organizations).hasSize(1);
            assertThat(organizations.get(0)).isEqualTo(
                    defaultOrganizationApiInfo(permalinkIdNotInDb)
                            .withMergedPermalinks(Set.of(permalinkId, otherPermalinkId)));
        });
    }

    // *** Проверка получения конкретных организаций клиента ***

    @Test
    public void getApiClientOrganizations_EmptyPermalinkIdsPassed_ZeroInvocations() {
        organizationService.getApiClientOrganizations(clientId, chiefUid, language, emptyList());
        verifyZeroInteractions(organizationsClient);
    }

    @Test
    public void getApiClientOrganizations_NullPermalinkId_ZeroInvocations() {
        organizationService.getApiClientOrganizations(clientId, chiefUid, language, singletonList(null));
        verifyZeroInteractions(organizationsClient);
    }

    /**
     * Запрос определённых организаций клиента.
     * Предусловия:
     * <ol>
     * <li>У клиента есть организации в БД Директа с пермалинками {@link #permalinkId} и {@link #otherPermalinkId}.</li>
     * <li>Запрашиваем пустой список организаций</li>
     * </ol>
     * Результат: пустой список организаций.
     */
    @Test
    public void getApiClientOrganizations_getSomeOrganizations_emptyPermalinkIds_emptyResult() {
        List<OrganizationApiInfo> organizations = organizationService
                .getApiClientOrganizations(clientId, chiefUid, language, emptyList());
        assertThat(organizations).isEmpty();
    }

    /**
     * Запрос определённых организаций клиента.
     * Предусловия:
     * <ol>
     * <li>У клиента есть организации в БД Директа с пермалинками {@link #permalinkId} и {@link #otherPermalinkId}.</li>
     * <li>Запрашиваем организацию {@link #permalinkId}</li>
     * <li>Справочник не возвращает никакой информации по организации {@link #permalinkId}.</li>
     * </ol>
     * Результат: пустой список организаций.
     */
    @Test
    public void getApiClientOrganizations_getSomeOrganizations_organizationInDb_noInfoFromTycoon_emptyResult() {
        List<OrganizationApiInfo> organizations = organizationService
                .getApiClientOrganizations(clientId, chiefUid, language, List.of(permalinkId));
        assertThat(organizations).isEmpty();
    }

    /**
     * Запрос определённых организаций клиента.
     * Предусловия:
     * <ol>
     * <li>У клиента есть организации в БД Директа с пермалинками {@link #permalinkId} и {@link #otherPermalinkId}.</li>
     * <li>Запрашиваем организацию {@link #permalinkId}</li>
     * <li>Справочник возвращает информацию по организации {@link #permalinkId}.
     * </li>
     * </ol>
     * Результат: список организаций клиента содержит {@link #permalinkId}.
     */
    @Test
    public void getApiClientOrganizations_getSomeOrganizations_organizationInDb_infoFromTycoon() {
        mockOrganizationsClient(organizationsClient, Set.of(permalinkId), emptyList(),
                List.of(new PubApiCompany().id(permalinkId)));
        List<OrganizationApiInfo> organizations = organizationService
                .getApiClientOrganizations(clientId, chiefUid, language, List.of(permalinkId));

        SoftAssertions.assertSoftly(softly -> {
            assertThat(organizations).hasSize(1);
            assertThat(organizations.get(0)).isEqualTo(
                    defaultOrganizationApiInfo(permalinkId)
            );
        });
    }

    /**
     * Запрос определённых организаций клиента.
     * Предусловия:
     * <ol>
     * <li>У клиента есть организации в БД Директа с пермалинками {@link #permalinkId} и {@link #otherPermalinkId}.</li>
     * <li>Запрашиваем организацию {@link #permalinkIdNotInDb}</li>
     * <li>Справочник возвращает информацию по организации {@link #permalinkIdNotInDb}.</li>
     * </ol>
     * Результат: список организаций клиента содержит {@link #permalinkId}.
     */
    @Test
    public void getApiClientOrganizations_getSomeOrganizations_organizationNotInDb_infoFromTycoon_emptyResult() {
        mockOrganizationsClient(organizationsClient, Set.of(permalinkIdNotInDb), emptyList(),
                List.of(new PubApiCompany().id(permalinkIdNotInDb)));
        List<OrganizationApiInfo> organizations = organizationService
                .getApiClientOrganizations(clientId, chiefUid, language, List.of(permalinkIdNotInDb));
        SoftAssertions.assertSoftly(softly -> {
            assertThat(organizations).hasSize(1);
            assertThat(organizations.get(0)).isEqualTo(
                    defaultOrganizationApiInfo(permalinkIdNotInDb)
            );
        });
    }

    /**
     * Запрос счетчика для организации.
     * У организации нет счетчика.
     * Результат: пустая мапа организаций
     */
    @Test
    public void getMetrikaCountersByOrganizationsIds_OrganizationWithoutCounter() {
        MetrikaData metrikaData = new MetrikaData()
                .permalink(RandomNumberUtils.nextPositiveLong())
                .counter(null);
        when(organizationsClient.getOrganizationsCountersData(anyCollection(), anyString(), anyString()))
                .thenReturn(List.of(metrikaData));

        Map<Long, Long> metrikaCountersByOrganizationsIds =
                organizationService.getMetrikaCountersByOrganizationsIds(language, Set.of(metrikaData.getPermalink()));

        assertThat(metrikaCountersByOrganizationsIds).isEmpty();
    }

    @Test
    public void getApiClientOrganizationsWithMetrikaData() {
        organizationService.getApiClientOrganizations(clientId, chiefUid, language, List.of(permalinkId));
        ArgumentCaptor<List<String>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(organizationsClient).getMultipleOrganizationsInfo(anyCollection(), argumentCaptor.capture(),
                anyString(), anyString());
        List<String> fields = argumentCaptor.getValue();
        assertThat(fields).contains("metrika_data");
    }

    @Test
    public void getApiClientOrganizationsWithoutMetrikaData() {
        organizationService.getApiClientOrganizationsWithoutMetrikaData(clientId, chiefUid,
                language, List.of(permalinkId));
        ArgumentCaptor<List<String>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(organizationsClient).getMultipleOrganizationsInfo(anyCollection(), argumentCaptor.capture(),
                anyString(), anyString());
        List<String> fields = argumentCaptor.getValue();
        assertThat(fields).doesNotContain("metrika_data");
    }

}
