package ru.yandex.direct.core.entity.client.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import com.google.common.collect.Sets;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.agency.repository.AgencyRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbschema.ppc.enums.AgencyCurrenciesCurrency;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.AGENCY_CURRENCIES;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AgencyRepositoryGetAdditionalCurrenciesTest {
    private static final Set<CurrencyCode> EXPECTED_CURRENCIES = Sets.newHashSet(CurrencyCode.RUB, CurrencyCode.BYN);

    @Autowired
    private Steps steps;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private AgencyRepository agencyRepository;

    private ClientInfo agencyInfo;

    @Before
    public void setUp() {
        agencyInfo = steps.clientSteps().createDefaultClient();

        LocalDateTime now = LocalDateTime.now();

        Field<LocalDate> serverDateField = DSL.currentLocalDate().as("now");
        LocalDate serverDate = dslContextProvider.ppc(agencyInfo.getShard())
                .select(serverDateField)
                .fetchOne(serverDateField);

        LocalDate actualNowDate = serverDate.plusDays(1);
        LocalDate expiredNowDate = serverDate.minusDays(1);

        dslContextProvider.ppc(agencyInfo.getShard())
                .insertInto(AGENCY_CURRENCIES)
                .columns(
                        AGENCY_CURRENCIES.CLIENT_ID,
                        AGENCY_CURRENCIES.EXPIRATION_DATE,
                        AGENCY_CURRENCIES.CURRENCY,
                        AGENCY_CURRENCIES.LAST_CHANGE)
                // То что актуально в настоящий момент
                .values(
                        agencyInfo.getClientId().asLong(),
                        actualNowDate,
                        AgencyCurrenciesCurrency.RUB,
                        now)
                .values(
                        agencyInfo.getClientId().asLong(),
                        actualNowDate,
                        AgencyCurrenciesCurrency.BYN,
                        now)
                // То что было актуально в прошлом, нужно чтобы понять что ничего лишнего не отбирается
                .values(
                        agencyInfo.getClientId().asLong(),
                        expiredNowDate,
                        AgencyCurrenciesCurrency.USD,
                        now)
                .execute();
    }

    @Test
    public void test() {
        Set<CurrencyCode> actualCurrencies = agencyRepository.getAdditionalCurrencies(
                agencyInfo.getShard(), agencyInfo.getClientId());
        assertThat(actualCurrencies).isEqualTo(EXPECTED_CURRENCIES);
    }
}
