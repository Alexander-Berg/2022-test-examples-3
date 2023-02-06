package ru.yandex.market.abo.api.client;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.abo.api.entity.ShopPlacement;
import ru.yandex.market.abo.api.entity.forecast.ShopForecast;
import ru.yandex.market.abo.api.entity.problem.partner.ShopPartnerProblems;
import ru.yandex.market.abo.api.entity.spark.CompanyExtendedReport;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author kukabara
 */
@Disabled
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:appContext.xml")
public class AboPublicRestClientTest {
    private static final String TEST_OGRN_OOO = "5555555555555";

    @Autowired
    private AboPublicRestClient aboPublicClient;

    @Test
    public void testGetShopDown() throws Exception {
        List<Long> shopDown = aboPublicClient.getShopsDown();
        assertNotNull(shopDown);
    }

    @Test
    public void getShopForecast() throws Exception {
        ShopForecast shopForecast = aboPublicClient.getShopForecast(774, ShopPlacement.CPA);
        assertNotNull(shopForecast);
    }

    @Test
    public void getShopProblems() throws Exception {
        ShopPartnerProblems shopProblems = aboPublicClient.getShopProblems(10216833, ShopPlacement.CPA, false);
        assertNotNull(shopProblems);
    }

    @Test
    public void getSparkOgrnInfo() throws Exception {
        CompanyExtendedReport companyExtendedReport = aboPublicClient.getOgrnInfo(TEST_OGRN_OOO);
        assertNotNull(companyExtendedReport);
    }
}
