package ru.yandex.direct.grid.processing.service.organizations;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.clientphone.ClientPhoneService;
import ru.yandex.direct.core.entity.organizations.service.OrganizationService;
import ru.yandex.direct.grid.processing.model.organization.GdOrganizationsByUrlInput;
import ru.yandex.direct.grid.processing.model.organizations.GdOrganization;
import ru.yandex.direct.grid.processing.service.organizations.loader.CanOverridePhoneDataLoader;
import ru.yandex.direct.grid.processing.service.organizations.loader.OperatorCanEditDataLoader;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.organizations.swagger.OrganizationInfo;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.utils.FunctionalUtils.flatMapToSet;

@ParametersAreNonnullByDefault
public class OrganizationsDataServiceGetByUrlTest {
    private static final Set<ModelProperty<?, ?>> DEFAULT_FIELDS_TO_FETCH = GdOrganization.allModelProperties();

    @Mock
    private OrganizationService organizationService;
    private OrganizationsDataService organizationsDataService;

    @Captor
    private ArgumentCaptor<String> textForSearchCaptor;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        organizationsDataService = new OrganizationsDataService(
                organizationService,
                mock(ClientPhoneService.class),
                mock(GridValidationService.class),
                mock(OperatorCanEditDataLoader.class),
                mock(CanOverridePhoneDataLoader.class));
    }

    @Test
    public void test_NotSocialNetworkDomain_SameUrl_OneOrganization() {
        OrganizationInfo organizationInfo = createOrganizationInfo("https://test.ru/", "https://other-url.ru");
        when(organizationService.getOrganizationsInfoByText(anyString(), any(), anySet()))
                .thenReturn(List.of(organizationInfo));
        List<GdOrganization> organizationsByUrl = organizationsDataService.getOrganizationsByUrl(
                new GdOrganizationsByUrlInput().withUrl("https://www.test.ru/"),
                DEFAULT_FIELDS_TO_FETCH);
        verify(organizationService).getOrganizationsInfoByText(textForSearchCaptor.capture(), any(), anySet());

        assertThat(organizationsByUrl.size()).isEqualTo(1);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(textForSearchCaptor.getValue())
                    .isEqualTo("test.ru/*");
            softAssertions.assertThat(organizationsByUrl.get(0).getUrls())
                    .isEqualTo(organizationInfo.getUrls());
        });
    }

    @Test
    public void test_NotSocialNetworkDomain_SameHost_OneOrganization() {
        OrganizationInfo organizationInfo = createOrganizationInfo("https://other-url.ru", "https://test.ru");
        when(organizationService.getOrganizationsInfoByText(anyString(), any(), anySet()))
                .thenReturn(List.of(organizationInfo));
        List<GdOrganization> organizationsByUrl = organizationsDataService.getOrganizationsByUrl(
                new GdOrganizationsByUrlInput().withUrl("https://test.ru/bla/bla-bla"),
                DEFAULT_FIELDS_TO_FETCH);
        verify(organizationService).getOrganizationsInfoByText(textForSearchCaptor.capture(), any(), anySet());

        assertThat(organizationsByUrl.size()).isEqualTo(1);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(textForSearchCaptor.getValue())
                    .isEqualTo("test.ru/*");
            softAssertions.assertThat(organizationsByUrl.get(0).getUrls())
                    .isEqualTo(organizationInfo.getUrls());
        });
    }

    @Test
    public void test_NotSocialNetworkDomain_SameShortUrl_MultipleOrganizations() {
        List<OrganizationInfo> organizations = List.of(
                createOrganizationInfo("https://oteli96.ru/berezovaya-roshcha/"),
                createOrganizationInfo("https://oteli96.ru/"),
                createOrganizationInfo("https://oteli96.ru/priozere/"));
        when(organizationService.getOrganizationsInfoByText(anyString(), any(), anySet())).thenReturn(organizations);
        List<GdOrganization> organizationsByUrl = organizationsDataService.getOrganizationsByUrl(
                new GdOrganizationsByUrlInput().withUrl("https://oteli96.ru/"),
                DEFAULT_FIELDS_TO_FETCH);
        verify(organizationService).getOrganizationsInfoByText(textForSearchCaptor.capture(), any(), anySet());

        assertThat(organizationsByUrl.size()).isEqualTo(1);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(textForSearchCaptor.getValue())
                    .isEqualTo("oteli96.ru/*");
            softAssertions.assertThat(organizationsByUrl.get(0).getUrls())
                    .isEqualTo(List.of("https://oteli96.ru/"));
        });
    }

    @Test
    public void test_NotSocialNetworkDomain_SameLongUrl_MultipleOrganizations() {
        List<OrganizationInfo> organizations = List.of(
                createOrganizationInfo("https://oteli96.ru/berezovaya-roshcha/"),
                createOrganizationInfo("https://oteli96.ru/"),
                createOrganizationInfo("https://oteli96.ru/priozere/"));
        when(organizationService.getOrganizationsInfoByText(anyString(), any(), anySet())).thenReturn(organizations);
        List<GdOrganization> organizationsByUrl = organizationsDataService.getOrganizationsByUrl(
                new GdOrganizationsByUrlInput().withUrl("https://oteli96.ru/priozere/"),
                DEFAULT_FIELDS_TO_FETCH);
        verify(organizationService).getOrganizationsInfoByText(textForSearchCaptor.capture(), any(), anySet());

        assertThat(organizationsByUrl.size()).isEqualTo(1);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(textForSearchCaptor.getValue())
                    .isEqualTo("oteli96.ru/*");
            softAssertions.assertThat(organizationsByUrl.get(0).getUrls())
                    .isEqualTo(List.of("https://oteli96.ru/priozere/"));
        });
    }

    @Test
    public void test_NotSocialNetworkDomain_SameHost_MultipleOrganizations() {
        List<OrganizationInfo> organizations = List.of(
                createOrganizationInfo("https://oteli96.ru/berezovaya-roshcha/"),
                createOrganizationInfo("https://oteli96.ru/"),
                createOrganizationInfo("https://oteli96.ru/priozere/"));
        when(organizationService.getOrganizationsInfoByText(anyString(), any(), anySet())).thenReturn(organizations);
        List<GdOrganization> organizationsByUrl = organizationsDataService.getOrganizationsByUrl(
                new GdOrganizationsByUrlInput().withUrl("https://oteli96.ru/other-path"),
                DEFAULT_FIELDS_TO_FETCH);
        verify(organizationService).getOrganizationsInfoByText(textForSearchCaptor.capture(), any(), anySet());

        assertThat(organizationsByUrl.size()).isEqualTo(3);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(textForSearchCaptor.getValue())
                    .isEqualTo("oteli96.ru/*");
            Set<String> resultUrls = flatMapToSet(organizationsByUrl, GdOrganization::getUrls);
            Set<String> organizationUrls = flatMapToSet(organizations, OrganizationInfo::getUrls);
            softAssertions.assertThat(resultUrls)
                    .isEqualTo(organizationUrls);
        });
    }

    @Test
    public void test_SocialNetworkDomain_SameUrl_OneOrganization() {
        OrganizationInfo organizationInfo =
                createOrganizationInfo("https://vk.com/test", "https://other-url.ru");
        when(organizationService.getOrganizationsInfoByText(anyString(), any(), anySet()))
                .thenReturn(List.of(organizationInfo));
        List<GdOrganization> organizationsByUrl = organizationsDataService.getOrganizationsByUrl(
                new GdOrganizationsByUrlInput().withUrl("https://www.vk.com/test/"),
                DEFAULT_FIELDS_TO_FETCH);
        verify(organizationService).getOrganizationsInfoByText(textForSearchCaptor.capture(), any(), anySet());

        assertThat(organizationsByUrl.size()).isEqualTo(1);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(textForSearchCaptor.getValue())
                    .isEqualTo("vk.com/test");
            softAssertions.assertThat(organizationsByUrl.get(0).getUrls())
                    .isEqualTo(organizationInfo.getUrls());
        });
    }

    @Test
    public void test_CyrillicDomain_SameUrl_OneOrganization() {
        OrganizationInfo organizationInfo =
                createOrganizationInfo("http://ременьвподарок.рф", "https://other-url.ru");
        when(organizationService.getOrganizationsInfoByText(anyString(), any(), anySet()))
                .thenReturn(List.of(organizationInfo));
        List<GdOrganization> organizationsByUrl = organizationsDataService.getOrganizationsByUrl(
                new GdOrganizationsByUrlInput().withUrl("https://www.ременьвподарок.рф/"),
                DEFAULT_FIELDS_TO_FETCH);
        verify(organizationService).getOrganizationsInfoByText(textForSearchCaptor.capture(), any(), anySet());

        assertThat(organizationsByUrl.size()).isEqualTo(1);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(textForSearchCaptor.getValue())
                    .isEqualTo("ременьвподарок.рф/*");
            softAssertions.assertThat(organizationsByUrl.get(0).getUrls())
                    .isEqualTo(organizationInfo.getUrls());
        });
    }

    private OrganizationInfo createOrganizationInfo(String... urls) {
        var organizationInfo = new OrganizationInfo().withUrls(Arrays.asList(urls));
        organizationInfo.withPermalinkId(RandomNumberUtils.nextPositiveLong());
        return organizationInfo;
    }
}
