package ru.yandex.direct.core.entity.payment.service;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class CanUseAutopayTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private AutopayService autopayService;

    public Object[] testData() {
        return new Object[][]{
                {CurrencyCode.RUB, Region.RUSSIA_REGION_ID, false, true},
                {CurrencyCode.RUB, Region.RUSSIA_REGION_ID, true, false},
                {CurrencyCode.RUB, Region.RUSSIA_REGION_ID, true, true},
                {CurrencyCode.RUB, Region.UKRAINE_REGION_ID, false, false},
                {CurrencyCode.RUB, Region.KAZAKHSTAN_REGION_ID, false, true},
                {CurrencyCode.RUB, Region.CRIMEA_REGION_ID, true, false},
                {CurrencyCode.RUB, Region.BY_REGION_ID, true, true},
                {CurrencyCode.USD, Region.RUSSIA_REGION_ID, false, false},
                {CurrencyCode.TRY, Region.RUSSIA_REGION_ID, false, true},
                {CurrencyCode.YND_FIXED, Region.RUSSIA_REGION_ID, true, false},
                {CurrencyCode.EUR, Region.RUSSIA_REGION_ID, true, true},
                {CurrencyCode.CHF, Region.RUSSIA_REGION_ID, false, false},
                {CurrencyCode.KZT, Region.UZBEKISTAN_REGION_ID, false, true},
                {CurrencyCode.UAH, Region.MOSCOW_REGION_ID, true, false},
                {CurrencyCode.BYN, Region.KYIV_OBLAST_REGION_ID, true, true}
        };
    }

    @Test
    @Parameters(method = "testData")
    public void testNegative(CurrencyCode currencyCode, Long regionId, boolean campaignAgency, boolean isAgency) {
        Client client = defaultClient()
                .withWorkCurrency(currencyCode)
                .withCountryRegionId(regionId);

        ClientInfo clientInfo;

        if (isAgency) {
            clientInfo = steps.clientSteps().createClient(client.withRole(RbacRole.AGENCY));
        } else {
            clientInfo = steps.clientSteps().createClient(client);
        }

        steps.campaignSteps().createCampaign(
                newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withAgencyId(campaignAgency ? RandomNumberUtils.nextPositiveInteger() : 0L),
                clientInfo);

        assertFalse(autopayService.canUseAutopay(clientInfo.getShard(), clientInfo.getUid(), clientInfo.getClientId()));
    }

    @Test
    public void testPositive() {
        Client client = defaultClient()
                .withWorkCurrency(CurrencyCode.RUB)
                .withCountryRegionId(Region.RUSSIA_REGION_ID);

        ClientInfo clientInfo = steps.clientSteps().createClient(client);

        steps.campaignSteps().createCampaign(
                newTextCampaign(clientInfo.getClientId(), clientInfo.getUid()),
                clientInfo);

        assertTrue(autopayService.canUseAutopay(clientInfo.getShard(), clientInfo.getUid(), clientInfo.getClientId()));
    }
}
