package ru.yandex.direct.core.entity.agency.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.agency.model.AgencyAdditionalCurrency;
import ru.yandex.direct.core.entity.agency.repository.AgencyRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.currency.model.AnyCountryCurrencyCount;
import ru.yandex.direct.core.entity.currency.service.CurrencyDictCache;
import ru.yandex.direct.core.entity.user.model.AgencyLimRep;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestAgencyRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacAgencyLimRepType;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringRunner.class)
public class AgencyServiceTest {
    private static final ClientId AGENCY_CLIENT_ID = ClientId.fromLong(1);
    private static final long REGION1_ID = 1;
    private static final long REGION2_ID = 2;

    private static final Multimap<Long, CurrencyCode> ALLOWED_FOR_PAY_BY_COUNTRIES =
            ImmutableMultimap.<Long, CurrencyCode>builder()
                    .put(REGION1_ID, CurrencyCode.USD)
                    .put(REGION1_ID, CurrencyCode.BYN)
                    .put(REGION2_ID, CurrencyCode.USD)
                    .put(REGION2_ID, CurrencyCode.CHF)
                    .build();

    @Autowired
    private Steps steps;
    @Autowired
    private AgencyService agencyService;
    @Autowired
    private TestAgencyRepository testAgencyRepository;
    @Mock
    private ShardHelper shardHelper;
    @Mock
    private AgencyRepository agencyRepository;
    @Mock
    private ClientService clientService;
    @Mock
    private CurrencyDictCache currencyDictCache;
    @InjectMocks
    private AgencyService agencyServiceMock;

    private ClientInfo agency;
    private ClientInfo client1;
    private ClientInfo client2;

    private AgencyLimRep agencyLimRep1;
    private AgencyLimRep agencyLimRep2;
    private AgencyLimRep agencyLimRep3;

    @Before
    public void setUp() {
        when(agencyRepository.getAdditionalCurrencies(anyInt(), eq(AGENCY_CLIENT_ID)))
                .thenReturn(emptySet());

        when(clientService.getWorkCurrency(eq(AGENCY_CLIENT_ID)))
                .thenReturn(Currencies.getCurrency(CurrencyCode.YND_FIXED));
        when(clientService.getAllowedForPayCurrencies(eq(AGENCY_CLIENT_ID)))
                .thenReturn(ALLOWED_FOR_PAY_BY_COUNTRIES);

        when(currencyDictCache.getAnyCountryCurrencyCount())
                .thenReturn(new AnyCountryCurrencyCount(ALLOWED_FOR_PAY_BY_COUNTRIES.size() + 1, 0));

        agency = steps.clientSteps().createDefaultAgency();
        client1 = steps.clientSteps().createClientUnderAgency(agency);
        client2 = steps.clientSteps().createClientUnderAgency(agency, steps.clientSteps().createDefaultClientAnotherShard());

        agencyLimRep1 = new AgencyLimRep().withRepType(RbacAgencyLimRepType.LEGACY);
        agencyLimRep2 = new AgencyLimRep().withRepType(RbacAgencyLimRepType.CHIEF).withGroupId(RandomNumberUtils.nextPositiveLong());
        agencyLimRep3 = new AgencyLimRep().withRepType(RbacAgencyLimRepType.MAIN).withGroupId(agencyLimRep2.getGroupId());

        Stream.of(agencyLimRep1, agencyLimRep2, agencyLimRep3).forEach(o -> steps.userSteps().createAgencyLimRep(agency, o));

        testAgencyRepository.linkLimRepToClient(client1.getShard(),
                List.of(agencyLimRep1.getUid(), agencyLimRep2.getUid(), agencyLimRep3.getUid()), client1.getClientId().asLong());
        testAgencyRepository.linkLimRepToClient(client2.getShard(), List.of(agencyLimRep2.getUid()), client2.getClientId().asLong());
    }

    @Test
    public void getAllowedCurrencies_AgencyWithCurrencyNotEqualYndFixed() {
        when(clientService.getWorkCurrency(eq(AGENCY_CLIENT_ID)))
                .thenReturn(Currencies.getCurrency(CurrencyCode.RUB));

        Set<CurrencyCode> allowedCurrencies = agencyServiceMock.getAllowedCurrencies(AGENCY_CLIENT_ID);

        assertThat(allowedCurrencies).isEqualTo(singleton(CurrencyCode.RUB));
    }

    @Test
    public void getAllowedCurrencies_AgencyWithYndFixedAndFromBellorus() {
        when(clientService.getCountryRegionIdByClientId(eq(AGENCY_CLIENT_ID)))
                .thenReturn(Optional.of(Region.BY_REGION_ID));

        Set<CurrencyCode> allowedCurrencies = agencyServiceMock.getAllowedCurrencies(AGENCY_CLIENT_ID);

        assertThat(allowedCurrencies).isEqualTo(singleton(CurrencyCode.BYN));
    }

    @Test
    public void getAllowedCurrencies_AgencyWithYndFixedAndNotFromBellorus() {
        Set<CurrencyCode> allowedCurrencies = agencyServiceMock.getAllowedCurrencies(AGENCY_CLIENT_ID);

        assertThat(allowedCurrencies).isEqualTo(new HashSet<>(ALLOWED_FOR_PAY_BY_COUNTRIES.values()));
    }

    @Test
    public void getAllowedCurrencies_AgencyWithYndFixedAndNotFromBellorusAndAllCountryCurrencies() {
        when(currencyDictCache.getAnyCountryCurrencyCount())
                .thenReturn(new AnyCountryCurrencyCount(ALLOWED_FOR_PAY_BY_COUNTRIES.size(), 0));

        Set<CurrencyCode> allowedCurrencies = agencyServiceMock.getAllowedCurrencies(AGENCY_CLIENT_ID);

        assertThat(allowedCurrencies).isEqualTo(emptySet());
    }

    @Test
    public void getAllowedCurrencies_CheckAdditionalCurrenciesAppended() {
        Set<CurrencyCode> additionalCurrencies = Sets.newSet(CurrencyCode.EUR, CurrencyCode.KZT);
        when(agencyRepository.getAdditionalCurrencies(anyInt(), eq(AGENCY_CLIENT_ID)))
                .thenReturn(additionalCurrencies);

        Set<CurrencyCode> allowedCurrencies = agencyServiceMock.getAllowedCurrencies(AGENCY_CLIENT_ID);

        assertThat(allowedCurrencies)
                .isEqualTo(
                        Stream.of(ALLOWED_FOR_PAY_BY_COUNTRIES.values(), additionalCurrencies)
                                .flatMap(Collection::stream)
                                .collect(toSet()));
    }

    @Test
    public void addAdditionalCurrencies_OneCurrency_WithoutErrors() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        agencyService.addAdditionalCurrencies(singletonList(new AgencyAdditionalCurrency()
                .withClientId(client.getClientId().asLong())
                .withCurrencyCode(CurrencyCode.RUB)
                .withExpirationDate(LocalDate.now())));
    }

    @Test
    public void getAgencyLimRepsByUidsTest() {
        var clientsAgencyLimReps = agencyService.getAgencyLimRepsByUids(
                List.of(agencyLimRep1.getUid(), agencyLimRep2.getUid(), agencyLimRep3.getUid()));
        assertThat(clientsAgencyLimReps).is(matchedBy(beanDiffer(
                Map.ofEntries(
                        Map.entry(agencyLimRep1.getUid(), agencyLimRep1),
                        Map.entry(agencyLimRep2.getUid(), agencyLimRep2),
                        Map.entry(agencyLimRep3.getUid(), agencyLimRep3)
                )
        )));
    }

    @Test
    public void getAgencyLimRepsByClientIdsTest() {
        var clientsAgencyLimReps = agencyService.getAgencyLimRepsByClientIds(
                List.of(client1.getClientId().asLong(), client2.getClientId().asLong()));
        assertThat(clientsAgencyLimReps).is(matchedBy(beanDiffer(
                Map.ofEntries(
                        Map.entry(client1.getClientId().asLong(),
                                Set.of(agencyLimRep1, agencyLimRep2, agencyLimRep3)),
                        Map.entry(client2.getClientId().asLong(), Set.of(agencyLimRep2)))
            )
        ));
    }
}
