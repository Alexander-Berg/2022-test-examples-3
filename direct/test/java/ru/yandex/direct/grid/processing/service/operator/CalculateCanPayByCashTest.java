package ru.yandex.direct.grid.processing.service.operator;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.common.enums.YandexDomain;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class CalculateCanPayByCashTest {

    public static Object[] parametersData() {
        return new Object[][]{
                {YandexDomain.RU, true, RbacRole.CLIENT, true, CurrencyCode.RUB, false, true},
                {YandexDomain.RU, false, RbacRole.CLIENT, true, CurrencyCode.RUB, false, false},

                //кейсы на домен
                {YandexDomain.BY, true, RbacRole.CLIENT, true, CurrencyCode.RUB, false, false},
                {null, true, RbacRole.CLIENT, true, CurrencyCode.RUB, false, false},

                //кейсы на роль оператора
                {YandexDomain.RU, true, RbacRole.AGENCY, false, CurrencyCode.RUB, false, false},
                {YandexDomain.RU, true, RbacRole.CLIENT, true, CurrencyCode.RUB, true, false},

                //кейсы на валюту
                {YandexDomain.RU, true, RbacRole.CLIENT, true, CurrencyCode.YND_FIXED, false, true},
                {YandexDomain.RU, true, RbacRole.CLIENT, true, CurrencyCode.KZT, false, false},
        };
    }


    @Test
    @Parameters(method = "parametersData")
    @TestCaseName("domain = {0}, hasCampaignsSupportCashPay = {1}, operatorRole = {2}, operatorIsClient = {3},"
            + " clientWorkCurrency = {4}, clientIsUnderAgency = {5}, expectedResult = {6}")
    public void checkCalculateCanTransferMoney(YandexDomain yandexDomain, boolean hasCampaignsSupportCashPay,
                                               RbacRole operatorRole, boolean operatorIsClient,
                                               CurrencyCode clientWorkCurrency, boolean clientIsUnderAgency,
                                               boolean expectedResult) {
        boolean result = OperatorClientRelationsHelper
                .calculateCanPayByCash(yandexDomain, hasCampaignsSupportCashPay, operatorRole, operatorIsClient,
                        clientWorkCurrency, clientIsUnderAgency);

        assertThat(result).isEqualTo(expectedResult);
    }

}
