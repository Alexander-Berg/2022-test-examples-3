package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsCurrency;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class CampaignMappingCurrencyTest {
    @Parameterized.Parameters(name = "model format: {0}, db format: {1}")
    public static Collection<Object[]> params() {
        return asList(
                new Object[]{CurrencyCode.RUB, CampaignsCurrency.RUB},
                new Object[]{CurrencyCode.UAH, CampaignsCurrency.UAH},
                new Object[]{CurrencyCode.KZT, CampaignsCurrency.KZT},
                new Object[]{CurrencyCode.USD, CampaignsCurrency.USD},
                new Object[]{CurrencyCode.EUR, CampaignsCurrency.EUR},
                new Object[]{CurrencyCode.YND_FIXED, CampaignsCurrency.YND_FIXED},
                new Object[]{CurrencyCode.CHF, CampaignsCurrency.CHF},
                new Object[]{CurrencyCode.TRY, CampaignsCurrency.TRY},
                new Object[]{CurrencyCode.BYN, CampaignsCurrency.BYN},
                new Object[]{null, null}
        );
    }

    private CurrencyCode modelStatus;
    private CampaignsCurrency dbStatus;

    public CampaignMappingCurrencyTest(CurrencyCode modelStatus, CampaignsCurrency dbStatus) {
        this.modelStatus = modelStatus;
        this.dbStatus = dbStatus;
    }

    @Test
    public void testToDbFormat() {
        assertThat("Конвертация типа в формат базы должна быть однозначной",
                CampaignMappings.currencyCodeToDb(modelStatus),
                is(dbStatus));
    }

    @Test
    public void testFromDbFormat() {
        assertThat("Конвертация типа в формат модели должна быть однозначной",
                CampaignMappings.currencyCodeFromDb(dbStatus),
                is(modelStatus));
    }
}
