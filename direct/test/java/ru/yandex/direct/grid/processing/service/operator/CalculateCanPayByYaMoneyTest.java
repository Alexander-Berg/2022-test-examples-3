package ru.yandex.direct.grid.processing.service.operator;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.common.enums.YandexDomain;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.grid.model.campaign.GdiBaseCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.regions.Region;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.service.operator.OperatorClientRelationsHelper.DAYS_TO_COUNT_SHOWS_FOR_PAY_YA_MONEY_CHECK;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class CalculateCanPayByYaMoneyTest {

    private static final GdiCampaign LAST_SHOW_CAMPAIGN = new GdiCampaign()
            .withEmpty(false)
            .withLastShowTime(now().minusDays(DAYS_TO_COUNT_SHOWS_FOR_PAY_YA_MONEY_CHECK - 1));

    private static final GdiCampaign NO_LAST_SHOW_CAMPAIGN = new GdiCampaign()
            .withEmpty(false)
            .withLastShowTime(now().minusDays(DAYS_TO_COUNT_SHOWS_FOR_PAY_YA_MONEY_CHECK + 1));

    public static Object[] parametersData() {
        return new Object[][]{
                {"Все условия выполнены", YandexDomain.RU, true, Set.of(LAST_SHOW_CAMPAIGN,
                        NO_LAST_SHOW_CAMPAIGN), Region.RUSSIA_REGION_ID, CurrencyCode.RUB, false, true},
                {"Домен не RU", YandexDomain.BY, true, Set.of(LAST_SHOW_CAMPAIGN), Region.RUSSIA_REGION_ID,
                        CurrencyCode.RUB, false, false},
                {"Оператор не клиент", YandexDomain.RU, false, Set.of(LAST_SHOW_CAMPAIGN),
                        Region.RUSSIA_REGION_ID, CurrencyCode.RUB, false, false},
                {"Не было показов", YandexDomain.RU, true, Set.of(NO_LAST_SHOW_CAMPAIGN), Region.RUSSIA_REGION_ID,
                        CurrencyCode.RUB, false, false},
                {"Клиент без страны", YandexDomain.RU, true, Set.of(LAST_SHOW_CAMPAIGN), null,
                        CurrencyCode.RUB, false, true},
                {"Клиент не из России", YandexDomain.RU, true, Set.of(LAST_SHOW_CAMPAIGN),
                        Region.KAZAKHSTAN_REGION_ID, CurrencyCode.RUB, false, false},
                {"Клиент не рублевый", YandexDomain.RU, true, Set.of(LAST_SHOW_CAMPAIGN), Region.RUSSIA_REGION_ID,
                        CurrencyCode.USD, false, false},
                {"Клиент агентский", YandexDomain.RU, true, Set.of(LAST_SHOW_CAMPAIGN), Region.RUSSIA_REGION_ID,
                        CurrencyCode.RUB, true, false},
        };
    }

    @Test
    @Parameters(method = "parametersData")
    @TestCaseName("{0}")
    public void checkCalculateCanTransferMoney(String description, YandexDomain yandexDomain,
                                               boolean operatorIsClient, Collection<GdiBaseCampaign> campaigns,
                                               @Nullable Long countryRegionId, CurrencyCode clientWorkCurrency,
                                               boolean clientIsUnderAgency, boolean expectedResult) {
        boolean result = OperatorClientRelationsHelper.calculateCanPayByYaMoney(yandexDomain,
                operatorIsClient, campaigns, countryRegionId, clientWorkCurrency, clientIsUnderAgency);
        assertThat(result).isEqualTo(expectedResult);
    }

}
