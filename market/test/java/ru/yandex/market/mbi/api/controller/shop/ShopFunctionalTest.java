package ru.yandex.market.mbi.api.controller.shop;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.time.DateUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.cutoff.model.CutoffInfo;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.ds.info.PhoneVisibility;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.mbi.api.client.entity.shops.CallCenterAvailable;
import ru.yandex.market.mbi.api.client.entity.shops.ProgramState;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;
import ru.yandex.market.mbi.api.client.entity.shops.ShopOrgInfo;
import ru.yandex.market.mbi.api.client.util.ShopsFilter;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author stille
 */
public class ShopFunctionalTest extends FunctionalTest {
    private static final long SHOP_ID = 774L;
    private static final ShopOrgInfo ORG_INFO = new ShopOrgInfo(
            "other", "1023500000160", "ПАО \"БАНК СГБ\"", "", "Москва", "ya_money", null, "https://mycuteshop" +
            ".com/org-info");

    /**
     * Тест для ручки {@code /shops/${shopId}}.
     * Запрашиваем данные для магазина 774. Проверяем, что данные по нему приходят и соответствуют информации в базе.
     */
    @Test
    @DbUnitDataSet(before = "ShopFunctionalTest.testShops.before.csv")
    void testShops() {
        checkShop(false, true);
    }

    /**
     * Тест для ручки {@code /shops/${shopId}}.
     * Запрашиваем данные для магазина 774. Проверяем, что данные по нему приходят и соответствуют информации в базе.
     */
    @Test
    @DbUnitDataSet(before = "ShopFunctionalTest.testShopsWithoutCampaign.before.csv")
    void testShopsWithoutCampaign() {
        checkShop(false, false);
    }

    /**
     * Проверяем корректность расчета признака smb.
     */
    @Test
    @DbUnitDataSet(before = {"ShopFunctionalTest.testShops.before.csv",
            "ShopFunctionalTest.testShops.smb.before.csv"})
    void testSmbShops() {
        checkShop(true, true);
    }

    private void checkShop(boolean expectedSmb, boolean expectedCampaignDetails) {
        Shop.PaymentStatus paymentStatus = new Shop.PaymentStatus(null, false);
        Shop.CampaignDetails campaignDetails = expectedCampaignDetails ?
                new Shop.CampaignDetails(10774, 325076,
                        1015) : null;
        Shop expectedShop = new Shop(SHOP_ID, "test.yandex.ru", "Тестовый магазин", null, null, null, ProgramState.ON,
                ProgramState.OFF, false, true, paymentStatus, campaignDetails, "WEB", expectedSmb);

        Shop shop = mbiApiClient.getShop(SHOP_ID);

        assertTrue(reflectionEquals(shop, expectedShop, "marketDeliveryServiceSettingsList", "organizationInfos",
                "paymentStatus", "campaignDetails"));
        assertEquals(ORG_INFO, shop.getOrganizationInfos().get(0));
        assertTrue(reflectionEquals(shop.getPaymentInfo(), paymentStatus));
        assertTrue(reflectionEquals(shop.getCampaignDetails(), campaignDetails));
    }

    /**
     * Тестирует получения списка магазинов с пустыми критериями поиска.
     */
    @Test
    @DbUnitDataSet(before = "ShopFunctionalTest.shopsList.before.csv")
    void testGetShopsListWithEmptyParams() {
        ShopsFilter shopsFilter = ShopsFilter.newBuilder().build();
        List<Long> shopsList = mbiApiClient.getShopsList(shopsFilter);
        assertThat(shopsList, Matchers.containsInAnyOrder(2L, 1L, 3L));
    }

    /**
     * Тестирует получения списка магазинов с заполненными критериями поиска.
     */
    @Test
    @DbUnitDataSet(before = "ShopFunctionalTest.shopsList.before.csv")
    void testGetShopsList() {
        ShopsFilter shopsFilter = ShopsFilter.newBuilder()
                .checkStatus(ParamType.PAYMENT_CHECK_STATUS, ParamCheckStatus.SUCCESS)
                .yamContractCompleted(true)
                .cpaEnabled(true)
                .build();
        List<Long> shopsList = mbiApiClient.getShopsList(shopsFilter);

        assertThat(shopsList, Matchers.contains(1L));
    }

    /**
     * тест для ручки {@code /organization-info/${shopId}}
     * проверяем, что достаёт из базы информацию
     */
    @Test
    @DbUnitDataSet(before = "ShopFunctionalTest.testShops.before.csv")
    void testGetOrganizationInfo() {
        ShopOrgInfo organizationInfo = mbiApiClient.getOrganizationInfo(SHOP_ID);
        assertEquals(ORG_INFO, organizationInfo);
    }

    /**
     * тест для ручки {@code /organization-info/${shopId}}
     * если информации нет в базе, должна вернуть null
     */
    @Test
    @DbUnitDataSet(before = "ShopFunctionalTest.testShops.before.csv")
    void testGetOrganizationInfo_absent() {
        assertThat(
                mbiApiClient.getOrganizationInfo(-1),
                Matchers.nullValue()
        );
    }

    // Работает только при сборке через Gradle. Иначе нужно поменять часовой пояс на +03:00
    @Test
    @DbUnitDataSet(before = "ShopFunctionalTest.testCutoffs.before.csv")
    void testGetOpenCutoffsXML() {
        String url = baseUrl() + "/shops/cutoffs/open/" + SHOP_ID;

        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(url);
        String actual = responseEntity.getBody();

        String expected =
                // language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<cutoff-info>\n" +
                        "    <cutoffs>\n" +
                        "        <cutoff id=\"-1\" shopId=\"774\" type=\"8\" " +
                        "startDate=\"2017-01-07T00:00:00+10:00\">\n" +
                        "            <comment>comment</comment>\n" +
                        "        </cutoff>\n" +
                        "        <cutoff id=\"-1\" shopId=\"774\" type=\"30\" " +
                        "startDate=\"2017-01-01T00:00:00+10:00\">\n" +
                        "            <comment>cpa_comment</comment>\n" +
                        "        </cutoff>\n" +
                        "    </cutoffs>\n" +
                        "</cutoff-info>";

        MbiAsserts.assertXmlEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(before = "ShopFunctionalTest.testCutoffs.before.csv")
    void testGetOpenCutoffs_absent() {
        List<CutoffInfo> shopCutoffs = mbiApiClient.getOpenCutoffs(0);
        assertTrue(shopCutoffs.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "ShopFunctionalTest.testIsCallCenterAvailable.before.csv")
    void testIsCallCenterAvailable() {
        CallCenterAvailable actual = mbiApiClient.isCallCenterAvailable(200L);
        assertTrue(actual.isAvailable());
        assertEquals(PhoneVisibility.NOWHERE, actual.getPhoneVisibility());
    }

    @Test
    @DbUnitDataSet(before = "ShopFunctionalTest.testClosedCutoffs.before.csv")
    void testGetClosedCutoffs() {
        List<CutoffInfo> closedCutoffs = mbiApiClient.getClosedCutoffs(
                SHOP_ID, DateUtils.addDays(new Date(), -2),
                CollectionFactory.set(CutoffType.FINANCE)
        );
        assertEquals(1, closedCutoffs.size());
        assertThat(closedCutoffs.stream().map(CutoffInfo::getType).collect(Collectors.toList()),
                containsInAnyOrder(CutoffType.FINANCE));
    }

    // Работает только при сборке через Gradle. Иначе нужно поменять часовой пояс на +03:00
    @Test
    @DbUnitDataSet(before = "CloseCutoffXMLBefore.csv")
    void testGetClosedCutoffsXML() {

        String url = baseUrl() + "/shops/cutoffs/closed/" + SHOP_ID +
                "?from=1234567&types=3&types=33&types=34";

        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(url);
        String actual = responseEntity.getBody();

        String expected =
                // language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<cutoff-info>\n" +
                        "    <cutoffs>\n" +
                        "        <cutoff id=\"11\" shopId=\"774\" type=\"3\" " +
                        "startDate=\"2018-05-02T16:28:31+10:00\">\n" +
                        "            <comment>comment</comment>\n" +
                        "        </cutoff>\n" +
                        "        <cutoff id=\"21\" shopId=\"774\" type=\"33\" " +
                        "startDate=\"2018-02-02T10:28:31+10:00\">\n" +
                        "            <comment>comment</comment>\n" +
                        "        </cutoff>\n" +
                        "        <cutoff id=\"22\" shopId=\"774\" type=\"34\" " +
                        "startDate=\"2018-01-02T14:22:31+10:00\">\n" +
                        "            <comment>comment</comment>\n" +
                        "        </cutoff>\n" +
                        "    </cutoffs>\n" +
                        "</cutoff-info>";

        MbiAsserts.assertXmlEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(before = "ShopFunctionalTest.testClosedCutoffs.before.csv")
    void testGetClosedCutoffs_absent() {
        List<CutoffInfo> closedCutoffs = mbiApiClient.getClosedCutoffs(0, new Date(), new HashSet<>());
        assertTrue(closedCutoffs.isEmpty());
    }

}
