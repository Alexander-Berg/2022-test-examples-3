package ru.yandex.direct.grid.processing.service.welcome;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.HashMultimap;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.agency.service.AgencyService;
import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.currency.service.CurrencyDictCache;
import ru.yandex.direct.core.entity.currency.service.CurrencyService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.geo.repository.FakeGeoRegionRepository;
import ru.yandex.direct.core.entity.geo.service.CurrentGeoService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.welcome.GdGetWelcomePageDataPayload;
import ru.yandex.direct.grid.processing.service.validation.GridValidationResultConversionService;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.user.service.validation.UserDefects.userAssociatedWithAnotherClient;
import static ru.yandex.direct.core.entity.user.service.validation.UserDefects.userHasNoAvailableCurrencies;
import static ru.yandex.direct.core.entity.user.service.validation.UserDefects.userNotFound;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class WelcomePageDataServiceTest {
    @Autowired
    private Steps steps;
    @Autowired
    private RbacService rbacService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private ClientGeoService clientGeoService;
    @Autowired
    private AgencyClientRelationService agencyClientRelationService;
    @Autowired
    private GridValidationResultConversionService validationResultConverter;
    @Autowired
    private CurrencyDictCache currencyDictCache;
    @Autowired
    private AgencyService agencyService;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private BalanceService balanceService;
    private WelcomePageDataService welcomePageDataService;

    private User user;

    @Before
    public void before() {
        final var geoRegionRepository = new FakeGeoRegionRepository();

        user = steps.userSteps().createUserInBlackboxStub().withRole(RbacRole.EMPTY);

        balanceService = mock(BalanceService.class);
        final var currentGeoService = mock(CurrentGeoService.class);
        when(currentGeoService.getCurrentCountryRegionId()).thenReturn(Optional.of(RUSSIA_REGION_ID));

        final var currencyService = new CurrencyService(currencyDictCache, agencyService,
                balanceService, featureService, ppcPropertiesSupport, geoRegionRepository);

        final var blackboxUserService = mock(BlackboxUserService.class);
        welcomePageDataService = new WelcomePageDataService(rbacService, clientService, balanceService,
                currencyService, clientGeoService, currentGeoService,
                blackboxUserService, agencyClientRelationService, validationResultConverter, geoRegionRepository);
    }

    @Test
    public void newUserAndClient() {
        var payload = welcomePageDataService.getWelcomePageData(user);
        assertSuccessPayload(payload);
    }

    @Test
    public void emptyUser() {
        var payload = welcomePageDataService.getWelcomePageData(new User().withUid(0L));
        assertErrorPayload(payload, userNotFound());
    }

    @Test
    public void wrongRepresentative() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        when(balanceService.findClientIdByUid(eq(user.getUid())))
                .thenReturn(Optional.of(clientInfo.getClientId()));
        var payload = welcomePageDataService.getWelcomePageData(user);
        assertErrorPayload(payload, userAssociatedWithAnotherClient());
    }

    @Test
    public void noCurrenciesAvailable() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient(user);
        when(balanceService.findClientIdByUid(eq(user.getUid())))
                .thenReturn(Optional.of(clientInfo.getClientId()));
        when(balanceService.getClientCountryCurrencies(eq(clientInfo.getClientId()), eq(null)))
                .thenReturn(HashMultimap.create());
        var payload = welcomePageDataService.getWelcomePageData(user);
        assertErrorPayload(payload, userHasNoAvailableCurrencies());
    }

    @Test
    public void balanceCountryOutOfGeoTree() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient(user);
        when(balanceService.findClientIdByUid(eq(user.getUid())))
                .thenReturn(Optional.of(clientInfo.getClientId()));
        HashMultimap<Long, CurrencyCode> balanceCountryCurrencies = HashMultimap.create();
        balanceCountryCurrencies.put(20992L, CurrencyCode.EUR);
        balanceCountryCurrencies.put(20992L, CurrencyCode.USD);
        balanceCountryCurrencies.put(RUSSIA_REGION_ID, CurrencyCode.RUB);
        when(balanceService.getClientCountryCurrencies(eq(clientInfo.getClientId()), eq(null)))
                .thenReturn(balanceCountryCurrencies);
        var payload = welcomePageDataService.getWelcomePageData(user);
        assertSuccessPayload(payload);
    }

    private void assertSuccessPayload(GdGetWelcomePageDataPayload payload) {
        assertThat(payload.getValidationResult()).isNull();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(payload.getCurrenciesByCountry()).isNotEmpty();
            softly.assertThat(payload.getCountryCurrencyChooseEnabled()).isTrue();
            softly.assertThat(payload.getClientCountry()).isEqualTo(RUSSIA_REGION_ID);
            softly.assertThat(payload.getMainCountries()).isNotEmpty();
            softly.assertThat(payload.getCountries()).isNotEmpty();
            softly.assertThat(payload.getClientCurrency()).isEqualTo(CurrencyCode.RUB);
            softly.assertThat(payload.getEmails()).isEqualTo(List.of(user.getEmail()));
        });
    }

    private void assertErrorPayload(GdGetWelcomePageDataPayload payload, Defect defect) {
        assertThat(payload.getValidationResult()).isNotNull();
        assertThat(payload.getValidationResult().getErrors()).hasSize(1);
        assertThat(payload.getValidationResult().getErrors().get(0).getCode())
                .isEqualTo(defect.defectId().getCode());
    }
}
