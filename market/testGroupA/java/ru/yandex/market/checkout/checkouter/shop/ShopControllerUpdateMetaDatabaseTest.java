package ru.yandex.market.checkout.checkouter.shop;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ShopControllerUpdateMetaDatabaseTest extends AbstractWebTestBase {

    private static final long SHOP_ID = 1337999000L;
    private static final ShopMetaData META = ShopSettingsHelper.getDefaultMeta();
    private static final ShopMetaData META_WITH_AC = ShopSettingsHelper.createCustomNewPrepayMeta(2);

    @Autowired
    private JdbcTemplate masterJdbcTemplate;
    @Autowired
    private TestSerializationService testSerializationService;

    @Test
    public void settingShopMetaDataShouldInsertIntoDatabase() throws Exception {
        putAndCheckShopMetaData(META);
    }

    @Test
    public void settingShopMetaDataShouldAcceptInnWithTrailingSpaces() throws Exception {
        putAndCheckShopMetaData(
                ShopMetaDataBuilder.createCopy(META)
                        .withInn(META.getInn() + "  ")
                        .build(),
                META,
                false
        );
    }

    @Test
    public void settingShopMetaDataShouldAcceptInnWithLeadingAndTrailingSpaces() throws Exception {
        putAndCheckShopMetaData(
                ShopMetaDataBuilder.createCopy(META)
                        .withInn("      " + META.getInn() + "  ")
                        .build(),
                META,
                false
        );
    }

    @Test
    public void settingShopMetaDataShowBuyerContactsTrue() throws Exception {
        putAndCheckShopMetaData(
                ShopMetaDataBuilder.createCopy(META)
                        .withOrderVisibilityMap(Collections.singletonMap(OrderVisibility.BUYER_EMAIL, true))
                        .build()
        );
    }

    @Test
    public void settingShopMetaDataShowBuyerContactsFalse() throws Exception {
        putAndCheckShopMetaData(
                ShopMetaDataBuilder.createCopy(META)
                        .withOrderVisibilityMap(Collections.singletonMap(OrderVisibility.BUYER_EMAIL, false))
                        .build()
        );
    }

    @Test
    public void settingShopMetaDataWithOgrnAndSupplierName() throws Exception {
        putAndCheckShopMetaData(
                ShopMetaDataBuilder.createCopy(META)
                        .withOgrn("1397431111806")
                        .withSupplierName("shop123")
                        .build()
        );
    }

    @Test
    public void settingShopMetaDataWithMedicineLicense() throws Exception {
        putAndCheckShopMetaData(
                ShopMetaDataBuilder.createCopy(META)
                        .withMedicineLicense("medicine_license_12131415")
                        .build()
        );
    }

    @Test
    public void settingShopMetaDataWithFastReturn() throws Exception {
        putAndCheckShopMetaData(
                ShopMetaDataBuilder.createCopy(META)
                        .withSupplierFastReturnEnabled(true)
                        .build()
        );
    }

    @Test
    public void shouldReadMetaFromCache() throws Exception {
        mockMvc.perform(put("/shops/{shopId}", SHOP_ID)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(META)));

        // разогреваем кэш
        mockMvc.perform(get("/shops/{shopId}", SHOP_ID))
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            int updated = masterJdbcTemplate.update("DELETE FROM shop_meta_data WHERE shop_id = ?", SHOP_ID);
            assertThat(updated, is(1));
            return null;
        });

        // достаем данные из кэша
        mockMvc.perform(get("/shops/{shopId}", SHOP_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обнуляем АВ с 2021-05-01 UTC")
    public void shouldReturnZeroAgencyCommissionUtc() throws Exception {
        mockMvc.perform(put("/shops/{shopId}", SHOP_ID)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(META_WITH_AC)));

        freezeTimeAt("2021-04-30T21:00:00Z");

        mockMvc.perform(get("/shops/{shopId}", SHOP_ID))
                .andExpect(jsonPath("$.agencyCommission").value(0));
    }

    @Test
    @DisplayName("Обнуляем АВ с 2021-05-01 Moscow time")
    public void shouldReturnZeroAgencyCommissionMoscow() throws Exception {
        mockMvc.perform(put("/shops/{shopId}", SHOP_ID)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(META_WITH_AC)));

        freezeTimeAt("2021-05-01T00:00:00Z", ZoneId.of("Europe/Moscow"));

        mockMvc.perform(get("/shops/{shopId}", SHOP_ID))
                .andExpect(jsonPath("$.agencyCommission").value(0));
    }

    private void putAndCheckShopMetaData(ShopMetaData shopMetaDataPut) throws Exception {
        putAndCheckShopMetaData(shopMetaDataPut, null, true);
    }

    private void putAndCheckShopMetaData(ShopMetaData shopMetaDataPut,
                                         @Nullable ShopMetaData shopMetaDataExpected,
                                         boolean checkRequestResponseEquals) throws Exception {
        String response = mockMvc.perform(put("/shops/{shopId}", SHOP_ID)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(testSerializationService.serializeCheckouterObject(shopMetaDataPut)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> rowSet = masterJdbcTemplate.queryForList(
                "SELECT shop_id, " +
                        "campaign_id, " +
                        "client_id, " +
                        "sandbox_class, " +
                        "prod_class, " +
                        "prepay_type, " +
                        "ya_money_id, " +
                        "articles, " +
                        "inn, " +
                        "phone_number, " +
                        "commission, " +
                        "order_visibility, " +
                        "ogrn, " +
                        "supplier_name, " +
                        "supplier_fast_return_enabled, " +
                        "medicine_license " +
                        " FROM shop_meta_data WHERE shop_id = ?",
                SHOP_ID
        );

        assertThat(rowSet, hasSize(1));

        shopMetaDataExpected = Optional.ofNullable(shopMetaDataExpected).orElse(shopMetaDataPut);
        Map<String, Object> row = Iterables.getOnlyElement(rowSet);
        assertEquals(SHOP_ID, (long) row.get("shop_id"));
        assertEquals(shopMetaDataExpected.getInn(), row.get("inn"));
        assertEquals(shopMetaDataExpected.getCampaignId(), (long) row.get("campaign_id"));
        assertEquals(shopMetaDataExpected.getClientId(), (long) row.get("client_id"));
        assertEquals(shopMetaDataExpected.getSandboxClass().getId(), (int) row.get("sandbox_class"));
        assertEquals(shopMetaDataExpected.getProdClass().getId(), (int) row.get("prod_class"));
        assertEquals(shopMetaDataExpected.getPrepayType().getId(), (int) row.get("prepay_type"));
        assertEquals(shopMetaDataExpected.getYaMoneyId(), row.get("ya_money_id"));
        Integer commissionActual = row.get("commission") == null ? null : (int) row.get("commission");
        assertEquals(shopMetaDataExpected.getAgencyCommission(), commissionActual);
        assertEquals(shopMetaDataExpected.getOrderVisibilityMap(),
                deserializeOrderVisibilityMap((String) row.get("order_visibility")));
        assertEquals(shopMetaDataExpected.getOgrn(), row.get("ogrn"));
        assertEquals(shopMetaDataExpected.getSupplierName(), row.get("supplier_name"));
        assertEquals(shopMetaDataExpected.isSupplierFastReturnEnabled(), row.get("supplier_fast_return_enabled"));
        assertEquals(shopMetaDataExpected.getMedicineLicense(), row.get("medicine_license"));

        if (checkRequestResponseEquals) {
            Assertions.assertEquals(
                    shopMetaDataPut,
                    testSerializationService.deserializeCheckouterObject(response, ShopMetaData.class)
            );
        }
    }

    private Map<OrderVisibility, Boolean> deserializeOrderVisibilityMap(String json) throws IOException {
        if (json == null) {
            return null;
        }
        return new ObjectMapper().readValue(json, new TypeReference<Map<OrderVisibility, Boolean>>() {
        });
    }
}
