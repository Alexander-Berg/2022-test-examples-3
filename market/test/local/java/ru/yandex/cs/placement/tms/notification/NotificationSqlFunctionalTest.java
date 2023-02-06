package ru.yandex.cs.placement.tms.notification;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.cs.billing.campaign.model.ContractType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;
import ru.yandex.vendor.products.model.VendorProduct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/NotificationSqlFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/NotificationSqlFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class NotificationSqlFunctionalTest extends AbstractCsPlacementTmsFunctionalTest {
    @Autowired
    private NotificationAverageService notificationAverageService;

    @Autowired
    private NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate;

    @BeforeEach
    void before() {
        csBillingNamedParameterJdbcTemplate.update("" +
                "INSERT INTO CS_BILLING.CAMPAIGN_CHARGES " +
                "(ID,CS_ID,CAMPAIGN_ID,SRV_TYPE_ID,DT,PRICE,AMOUNT,SUM) " +
                "VALUES (1, 132, 501, 11, trunc(SYSTIMESTAMP) - 3, 100, 1, 100) ", Collections.emptyMap());

        csBillingNamedParameterJdbcTemplate.update("" +
                "INSERT INTO CS_BILLING.CAMPAIGN_CHARGES " +
                "(ID, CS_ID,CAMPAIGN_ID,SRV_TYPE_ID,DT,PRICE,AMOUNT,SUM) " +
                "VALUES (2, 132, 501, 11, trunc(SYSTIMESTAMP) - 3, 100, 1, 150) ", Collections.emptyMap());
    }


    /**
     * Проверяет, что вендор с суммой меньше либо равной нулю на счете не берется в выборку
     */
    @Test
    void testGetAverageChargesPerVendorForTwoVendors() {
        csBillingNamedParameterJdbcTemplate.update("" +
                "INSERT INTO CS_BILLING.CAMPAIGN_CHARGES " +
                "(ID, CS_ID,CAMPAIGN_ID,SRV_TYPE_ID,DT,PRICE,AMOUNT,SUM) " +
                "VALUES (44, 132, 502, 11, trunc(SYSTIMESTAMP) - 4, 100, 1, 0.12345601) ", Collections.emptyMap());
        Map<Long, Double> map = notificationAverageService.preparedAverageSumForVendor(
                VendorProduct.RECOMMENDED_SHOPS,
                ContractType.PREPAID
        );
        assertEquals(250.0, map.get(100L).doubleValue());
        assertEquals(0.12345601, map.get(102L).doubleValue());
    }


    /**
     * Проверяет, что вендор с суммой равной нулю на счете НЕ берется в выборку
     */
    @Test
    void testGetAverageChargesPerVendorForTwoVendorsWithZeroSum() {
        csBillingNamedParameterJdbcTemplate.update("" +
                "INSERT INTO CS_BILLING.CAMPAIGN_CHARGES " +
                "(ID, CS_ID,CAMPAIGN_ID,SRV_TYPE_ID,DT,PRICE,AMOUNT,SUM) " +
                "VALUES (44, 132, 501, 11, trunc(SYSTIMESTAMP) - 4, 100, 1, 0) ", Collections.emptyMap());

        Map<Long, Double> map = notificationAverageService.preparedAverageSumForVendor(
                VendorProduct.RECOMMENDED_SHOPS,
                ContractType.PREPAID
        );
        assertEquals(250.0, map.get(100L).doubleValue());
    }

    /**
     * Проверяет, что у вендора сумма на счете делится на количество активных дней, которых меньше 30
     */
    @Test
    void testGetAverageChargesPerVendor() {
        Map<Long, Double> map = notificationAverageService.preparedAverageSumForVendor(
                VendorProduct.RECOMMENDED_SHOPS,
                ContractType.PREPAID
        );

        assertEquals(250.0, map.get(100L).doubleValue());
    }

    /**
     * Проверяет, что если нет такого типа договора, то ничего не возвращается
     */
    @Test
    void testGetAverageChargesPerVendorOnEmptyContractType() {
        Map<Long, Double> map = notificationAverageService.preparedAverageSumForVendor(
                VendorProduct.RECOMMENDED_SHOPS,
                ContractType.POSTPAID
        );

        assertTrue(map.isEmpty());
    }

    /**
     * Проверяет, что для не существующего ProductId ничего не возвращается
     */
    @Test
    void testGetAverageChargesPerVendorOnEmptyProductId() {
        Map<Long, Double> map = notificationAverageService.preparedAverageSumForVendor(
                VendorProduct.INCUTS,
                ContractType.PREPAID
        );

        assertTrue(map.isEmpty());
    }

    /**
     * Проверяем, что для двух вендоров средние суммы считаются верно
     * для 100 вендора это сумма арифметической прогрессии от 5 и с шагом d = 1 для 29 дней. + 250 за один день в before
     * b + 1000 для того же вендора, но с другим datasourceId, чтобы проверить что средняя сумма
     * считается именно для вендора, а не для связки вендора и datasourceId
     * <p>
     * для 102 вендора это 66 + 53 в два разных дня
     */
    @Test
    void testGetAverageChargesPerVendorFor31activeDays() {
        int lastIdInCampainChargeTable = generateManyChargesForVendor(5, 55, 501);

        csBillingNamedParameterJdbcTemplate.update("" +
                "INSERT INTO CS_BILLING.CAMPAIGN_CHARGES " +
                "(ID, CS_ID,CAMPAIGN_ID,SRV_TYPE_ID,DT,PRICE,AMOUNT,SUM) " +
                "VALUES (3, 132, 502, 11, '2010-10-18 00:00:00', 100, 1, 66) ", Collections.emptyMap());

        csBillingNamedParameterJdbcTemplate.update("" +
                "INSERT INTO CS_BILLING.CAMPAIGN_CHARGES " +
                "(ID, CS_ID,CAMPAIGN_ID,SRV_TYPE_ID,DT,PRICE,AMOUNT,SUM) " +
                "VALUES (4, 132, 502, 11, '2013-10-18 00:00:00', 100, 1, 53) ", Collections.emptyMap());

        csBillingNamedParameterJdbcTemplate.update("" +
                "INSERT INTO CS_BILLING.CAMPAIGN_CHARGES " +
                "(ID, CS_ID,CAMPAIGN_ID,SRV_TYPE_ID,DT,PRICE,AMOUNT,SUM) " +
                "VALUES (" + lastIdInCampainChargeTable + ", 132, 501, 11, trunc(SYSTIMESTAMP) - 3, 100, 1, 1000) ", Collections.emptyMap());

        lastIdInCampainChargeTable++;

        Map<Long, Double> map = notificationAverageService.preparedAverageSumForVendor(
                VendorProduct.RECOMMENDED_SHOPS,
                ContractType.PREPAID
        );

        assertEquals((sumOfAriphmeticProgress(5, 29) + 250 + 1000) / 30.0, map.get(100L).doubleValue());
        assertEquals((66.0 + 53.0) / 2.0, map.get(102L).doubleValue());
    }

    static int sumOfAriphmeticProgress(int a0, int n) {
        return (2 * a0 + (n - 1)) * n / 2;
    }

    int generateManyChargesForVendor(int id, int countOfCharge, int campaignId) {
        for (; id < countOfCharge; id++) {
            generateCharge(id, campaignId, id, id);
        }
        return id;
    }

    void generateCharge(int id, int campaignId, int day, int charge) {
        csBillingNamedParameterJdbcTemplate.update("" +
                "INSERT INTO CS_BILLING.CAMPAIGN_CHARGES " +
                "(ID,CS_ID,CAMPAIGN_ID,SRV_TYPE_ID,DT,PRICE,AMOUNT,SUM) " +
                "VALUES (" + id + ",132, " + campaignId + ", 11, trunc(SYSTIMESTAMP) - " + day + ", 100, 1, " + charge + ") ", Collections.emptyMap());
    }
}
