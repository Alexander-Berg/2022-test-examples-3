package ru.yandex.direct.api.v5.entity.businesses.delegate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.businesses.BusinessFieldEnum;
import com.yandex.direct.api.v5.businesses.BusinessGetItem;
import com.yandex.direct.api.v5.businesses.GetRequest;
import com.yandex.direct.api.v5.businesses.GetResponse;
import com.yandex.direct.api.v5.general.IdsCriteria;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.organizations.service.OrganizationService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.geosearch.GeosearchClient;
import ru.yandex.direct.organizations.swagger.model.CompanyRubric;
import ru.yandex.direct.organizations.swagger.model.CompanyUrl;
import ru.yandex.direct.organizations.swagger.model.LocalizedString;
import ru.yandex.direct.organizations.swagger.model.PubApiCompaniesData;
import ru.yandex.direct.organizations.swagger.model.PubApiCompany;
import ru.yandex.direct.organizations.swagger.model.TycoonRubricDefinition;

import static com.yandex.direct.api.v5.general.YesNoEnum.NO;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.organizations.swagger.model.PubApiUserAccessLevel.ANY;
import static ru.yandex.direct.organizations.swagger.model.PubApiUserAccessLevel.OWNED;

@ParametersAreNonnullByDefault
@Api5Test
@RunWith(SpringRunner.class)
public class GetBusinessesDelegateTest {
    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private Steps steps;
    @Autowired
    private OrganizationsClientStub organizationsClient;
    @Autowired
    private GeosearchClient geosearchClient;

    private GenericApiService genericApiService;
    private GetBusinessesDelegate delegate;

    private ClientId clientId;
    private long uid;
    private Long permalinkId;
    private PubApiCompaniesData mockCompaniesData;

    private static final String URL_1 = "URL_1";
    private static final String URL_2 = "URL_2";
    private static final Long RUBRIC_ID = 123L;
    private static final String RUBRIC_NAME = "RUBRIC";

    @Before
    public void before() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        uid = client.getUid();
        permalinkId = nextLong();
        clientId = client.getClientId();

        ApiUser apiUser = new ApiUser();
        apiUser.withChiefUid(uid).withClientId(clientId);
        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getChiefSubclient()).thenReturn(apiUser);

        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                mock(RequestCampaignAccessibilityCheckerProvider.class));

        delegate = new GetBusinessesDelegate(auth, organizationService, mock(PropertyFilter.class));

        PubApiCompany mockOrganization = new PubApiCompany()
                .id(permalinkId)
                .isOnline(true)
                .addUrlsItem(new CompanyUrl().value(URL_1))
                .addUrlsItem(new CompanyUrl().value(URL_2))
                .addRubricsItem(new CompanyRubric().rubricId(RUBRIC_ID));
        mockCompaniesData = new PubApiCompaniesData().addCompaniesItem(mockOrganization);
        mockCompaniesData.setRubricDefs(Map.of(
                RUBRIC_ID.toString(),
                new TycoonRubricDefinition()
                        .id(RUBRIC_ID)
                        .addNamesItem(new LocalizedString()
                                .locale("ru")
                                .value("RU")
                        )
                        .addNamesItem(new LocalizedString()
                                .locale("en")
                                .value(RUBRIC_NAME)
                        )
                        .addNamesItem(new LocalizedString()
                                .locale("ua")
                                .value("UA")
                        )
                )
        );
        doReturn(mockCompaniesData).when(organizationsClient).getMultipleOrganizationsInfo(anyCollection(),
                eq(List.of(uid)), anyCollection(), anyString(), anyString(), eq(OWNED));
        doReturn(mockCompaniesData).when(organizationsClient).getMultipleOrganizationsInfo(eq(Set.of(permalinkId)),
                eq(emptyList()), anyCollection(), anyString(), anyString(), eq(ANY));
    }

    @Test
    public void get_WithoutSelectionCriteria_hasOwned_Success() {
        GetRequest request = new GetRequest()
                .withFieldNames(BusinessFieldEnum.ID);
        GetResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getBusinesses()).hasSize(1);
            softly.assertThat(response.getBusinesses().get(0).getId()).isEqualTo(permalinkId);
            softly.assertThat(response.getBusinesses().get(0).getRubric()).isEqualTo(RUBRIC_NAME);
            softly.assertThat(response.getBusinesses().get(0).getUrls().getValue().getItems()).hasSize(2);
            softly.assertThat(response.getBusinesses().get(0).getUrls().getValue().getItems().get(0)).isEqualTo(URL_1);
            softly.assertThat(response.getBusinesses().get(0).getUrls().getValue().getItems().get(1)).isEqualTo(URL_2);
            softly.assertThat(response.getBusinesses().get(0).getHasOffice()).isEqualTo(NO);
        });
    }

    @Test
    public void get_WithoutSelectionCriteria_inDb_Success() {
        doReturn(new PubApiCompaniesData()).when(organizationsClient).getMultipleOrganizationsInfo(anyCollection(),
                eq(List.of(uid)), anyCollection(), anyString(), anyString(), eq(OWNED));

        doReturn(mockCompaniesData).when(organizationsClient).getMultipleOrganizationsInfo(anyCollection(),
                eq(emptyList()), anyCollection(), anyString(), anyString(), eq(ANY));

        steps.organizationSteps().createClientOrganization(clientId, permalinkId);

        GetRequest request = new GetRequest()
                .withFieldNames(BusinessFieldEnum.ID);
        GetResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getBusinesses()).hasSize(1);
            softly.assertThat(response.getBusinesses().get(0).getId()).isEqualTo(permalinkId);
            softly.assertThat(response.getBusinesses().get(0).getRubric()).isEqualTo(RUBRIC_NAME);
            softly.assertThat(response.getBusinesses().get(0).getUrls().getValue().getItems()).hasSize(2);
            softly.assertThat(response.getBusinesses().get(0).getUrls().getValue().getItems().get(0)).isEqualTo(URL_1);
            softly.assertThat(response.getBusinesses().get(0).getUrls().getValue().getItems().get(1)).isEqualTo(URL_2);
            softly.assertThat(response.getBusinesses().get(0).getHasOffice()).isEqualTo(NO);
        });
    }

    @Test
    public void get_WithSelectionCriteria_oneOrganizaion_Success() {
        GetRequest request = new GetRequest()
                .withFieldNames(BusinessFieldEnum.ID)
                .withSelectionCriteria(new IdsCriteria().withIds(permalinkId));
        GetResponse response = genericApiService.doAction(delegate, request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getBusinesses()).hasSize(1);
            softly.assertThat(response.getBusinesses().get(0).getId()).isEqualTo(permalinkId);
        });
    }

    @Test
    public void get_WithSelectionCriteria_twoOrganizations_Success() {
        long someOtherPermalinkId = nextLong();
        PubApiCompany someOtherOrganization = new PubApiCompany()
                .id(someOtherPermalinkId);

        doReturn(mockCompaniesData.addCompaniesItem(someOtherOrganization)).when(organizationsClient)
                .getMultipleOrganizationsInfo(eq(Set.of(someOtherPermalinkId, permalinkId)), eq(emptyList()),
                        anyCollection(), anyString(), anyString(), eq(ANY));

        GetRequest request = new GetRequest()
                .withFieldNames(BusinessFieldEnum.ID)
                .withSelectionCriteria(new IdsCriteria().withIds(someOtherPermalinkId, permalinkId));
        GetResponse response = genericApiService.doAction(delegate, request);

        Map<Long, BusinessGetItem> responseMap = StreamEx.of(response.getBusinesses())
                .mapToEntry(BusinessGetItem::getId, identity())
                .toMap();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(responseMap).hasSize(2);
            softly.assertThat(responseMap).containsKey(permalinkId);
            softly.assertThat(responseMap).containsKey(someOtherPermalinkId);
        });
    }

    @Test
    public void get_WithSelectionCriteria_twoOrganizations_oneInDb_Success() {
        long someOtherPermalinkId = nextLong();
        PubApiCompany someOtherOrganization = new PubApiCompany()
                .id(someOtherPermalinkId)
                .uidsWithModifyPermission(emptyList());
        steps.organizationSteps().createClientOrganization(clientId, someOtherPermalinkId);

        doReturn(mockCompaniesData.addCompaniesItem(someOtherOrganization)).when(organizationsClient)
                .getMultipleOrganizationsInfo(eq(Set.of(someOtherPermalinkId, permalinkId)), eq(emptyList()),
                        anyCollection(), anyString(), anyString(), eq(ANY));

        GetRequest request = new GetRequest()
                .withFieldNames(BusinessFieldEnum.ID)
                .withSelectionCriteria(new IdsCriteria().withIds(someOtherPermalinkId, permalinkId));
        GetResponse response = genericApiService.doAction(delegate, request);

        Map<Long, BusinessGetItem> responseMap = StreamEx.of(response.getBusinesses())
                .mapToEntry(BusinessGetItem::getId, identity())
                .toMap();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(responseMap).hasSize(2);
            softly.assertThat(responseMap).containsKey(permalinkId);
            softly.assertThat(responseMap).containsKey(someOtherPermalinkId);
        });
    }
}
