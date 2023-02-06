package ru.yandex.market.partner.mvc.controller.migration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopClient;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;
import ru.yandex.market.partner.util.PartnerFunctionalTestUrlConstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(before = "MigrationMappingsControllerTest.before.csv")
class MigrationMappingsControllerTest extends FunctionalTest {
    private static final int UID = 100500;
    private static final int DONOR_CAMPAIGN_ID = 210;
    private static final int CHILD_CAMPAIGN_ID = 211;
    private static final int EMPTY_PARTNER_CAMPAIGN_ID = 212;
    private static final int NOT_MIGRATED_CAMPAIGN_ID = 214;
    private static final int DONOR_NOT_MIGRATED_CAMPAIGN_ID = 215;
    private static final String PATH = "/migration/mappings";
    private static final String DISABLE_PATH = PATH + "/disable";

    private final CheckouterShopClient checkouterShopClient = mock(CheckouterShopClient.class);

    @Autowired
    private CheckouterClient checkouterClient;

    @Test
    @DisplayName("Правильный файлик маппигов")
    void getMappingsFile() {

        var expectedResponse = List.of(
                "Old partner Id;New partner Id;Old campaign Id;New campaign Id;Old warehouse Id;New warehouse Id;Old shop name;New shop name;Warehouse name",
                "200;201;210;211;400;401;Name2;Name3;fbs1",
                "200;202;210;213;398;402;Name2;Name4;fbs0");
        var response = FunctionalTestHelper.get(PartnerFunctionalTestUrlConstructor.setBaseUrl(baseUrl)
                .withPath("/migration/mappings")
                .withAuthorizationParams(UID, DONOR_CAMPAIGN_ID)
                .withCustomParam("business_id", 100)
                .getUrl());
        var actual = Arrays.asList(response.getBody().split("\n"));

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(CollectionUtils.isEqualCollection(expectedResponse, actual));
    }

    @Test
    @DisplayName("Одинаковые файлики у разных партнеров под одним бизнесом")
    void sameResponsesOnDifferentCampaign() {

        var firstResponse = FunctionalTestHelper.get(PartnerFunctionalTestUrlConstructor.setBaseUrl(baseUrl)
                .withPath(PATH)
                .withCustomParam("business_id", 100)
                .withAuthorizationParams(UID, DONOR_CAMPAIGN_ID).getUrl());
        Assertions.assertEquals(HttpStatus.OK, firstResponse.getStatusCode());
        var firstResponseBody = Arrays.asList(firstResponse.getBody().split("\n"));
        var secondResponse = FunctionalTestHelper.get(PartnerFunctionalTestUrlConstructor.setBaseUrl(baseUrl)
                .withPath(PATH)
                .withAuthorizationParams(UID, CHILD_CAMPAIGN_ID)
                .withCustomParam("business_id", 100).getUrl());
        Assertions.assertEquals(HttpStatus.OK, secondResponse.getStatusCode());
        var secondResponseBody = Arrays.asList(secondResponse.getBody().split("\n"));
        assertTrue(CollectionUtils.isEqualCollection(firstResponseBody, secondResponseBody));
    }

    @Test
    @DisplayName("Пустой файлик, если нет маппингов")
    void emptyMappings() {
        var expected =
                "Old partner Id;New partner Id;Old campaign Id;New campaign Id;Old warehouse Id;New warehouse Id;Old shop name;New shop name;Warehouse name\n";
        var response = FunctionalTestHelper.get(PartnerFunctionalTestUrlConstructor.setBaseUrl(baseUrl)
                .withPath("/migration/mappings")
                .withAuthorizationParams(UID, EMPTY_PARTNER_CAMPAIGN_ID)
                .withCustomParam("business_id", 99).getUrl());
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(expected, response.getBody());
    }

    @Test
    @DbUnitDataSet(after = "MigrationMappingsControllerDeleteMappingsTest.after.csv")
    @DisplayName("Отключить маппинги. id кампании реплики")
    void disableMappingsChild() {
        setupMock();
        FunctionalTestHelper.post(PartnerFunctionalTestUrlConstructor.setBaseUrl(baseUrl)
                .withPath(DISABLE_PATH)
                .withAuthorizationParams(UID, CHILD_CAMPAIGN_ID)
                .withCustomParam("business_id", 100).getUrl());
        verifyMock();
    }

    @Test
    @DbUnitDataSet(after = "MigrationMappingsControllerDeleteMappingsTest.after.csv")
    @DisplayName("Отключить маппинги. id кампании донора")
    void disableMappingsDonor() {
        setupMock();
        FunctionalTestHelper.post(PartnerFunctionalTestUrlConstructor.setBaseUrl(baseUrl)
                .withPath(DISABLE_PATH)
                .withAuthorizationParams(UID, DONOR_CAMPAIGN_ID)
                .withCustomParam("business_id", 100).getUrl());
        verifyMock();
    }

    @Test
    @DbUnitDataSet(after = "MigrationMappingsControllerTest.before.csv")
    @DisplayName("Отключить маппинги. Ничего не делаем, если маппингов нет")
    void disableMappingsNoMappings() {
        setupMock();
        FunctionalTestHelper.post(PartnerFunctionalTestUrlConstructor.setBaseUrl(baseUrl)
                .withPath(DISABLE_PATH)
                .withAuthorizationParams(UID, EMPTY_PARTNER_CAMPAIGN_ID)
                .withCustomParam("business_id", 199).getUrl());
        verify(checkouterShopClient, never()).updateShopData(anyLong(), any(ShopMetaData.class));
    }

    @DisplayName("Тесты ручки проверки миграции")
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("getArgsMigratedPartner")
    void migratedTest(String testName, int campaignId, boolean result) {
        final ResponseEntity<String> response =
                FunctionalTestHelper.get(PartnerFunctionalTestUrlConstructor.setBaseUrl(baseUrl)
                        .withPath("/migration/migrated")
                        .withAuthorizationParams(UID, campaignId)
                        .getUrl());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonTestUtil.assertEquals(response, String.valueOf(result));
    }

    private static Stream<Arguments> getArgsMigratedPartner() {
        return Stream.of(
                Arguments.of("Партнер создан миграцией", CHILD_CAMPAIGN_ID, Boolean.TRUE),
                Arguments.of("Реплика, но не миграция", NOT_MIGRATED_CAMPAIGN_ID, Boolean.FALSE),
                Arguments.of("Донор для мигрированных партнеров", DONOR_CAMPAIGN_ID, Boolean.TRUE),
                Arguments.of("Донор, но не миграция", DONOR_NOT_MIGRATED_CAMPAIGN_ID, Boolean.FALSE),
                Arguments.of("Обычный партнер", EMPTY_PARTNER_CAMPAIGN_ID, Boolean.FALSE));
    }

    private void setupMock() {
        doReturn(checkouterShopClient).when(checkouterClient).shops();
    }

    private void verifyMock() {
        ArgumentCaptor<ShopMetaData> argument = ArgumentCaptor.forClass(ShopMetaData.class);
        verify(checkouterShopClient, times(2)).updateShopData(anyLong(), argument.capture());
        argument.getAllValues().stream()
                .map(ShopMetaData::getMigrationMapping)
                .forEach(Assertions::assertNull);
    }
}
