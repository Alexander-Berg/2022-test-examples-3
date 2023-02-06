package ru.yandex.market.partner.agency;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.security.BusinessCampaignable;
import ru.yandex.market.core.security.model.DualUidable;
import ru.yandex.market.sec.JavaSecFunctionalTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тестирование прав агентств в ручках необходимых для синих кампаний.
 */
class AgencySuppliersOperationsTest extends JavaSecFunctionalTest {

    private static BusinessCampaignable dataWithCampaignId;
    private static BusinessCampaignable dataWithoutCampaignId;
    private static BusinessCampaignable dataWithBusinessId;

    @BeforeAll
    static void beforeAll() {
        dataWithCampaignId = mock(BusinessCampaignable.class);
        when(dataWithCampaignId.getUid()).thenReturn(1L);
        when(dataWithCampaignId.getCampaignId()).thenReturn(1000L);
        when(dataWithCampaignId.getBusinessId()).thenReturn(null);

        dataWithoutCampaignId = mock(BusinessCampaignable.class);
        when(dataWithoutCampaignId.getUid()).thenReturn(1L);
        when(dataWithoutCampaignId.getCampaignId()).thenReturn(-1L);
        when(dataWithoutCampaignId.getBusinessId()).thenReturn(null);

        dataWithBusinessId = mock(BusinessCampaignable.class);
        when(dataWithBusinessId.getUid()).thenReturn(1L);
        when(dataWithBusinessId.getCampaignId()).thenReturn(-1L);
        when(dataWithBusinessId.getBusinessId()).thenReturn(10000L);
    }

    private static Stream<Arguments> argsOnlyWithCampaign() {
        return Stream.of(
                "/shop/model/batch/read",
                "/shop/model/batch/write",
                "/fulfillment/catalog",
                "/fulfillment/prices",
                "/fulfillment/withdraw",
                "/fulfillment/supply",
                "/fulfillment/supply/post",
                "/fulfillment/withdraw/post",
                "/fulfillment-stats"
        ).map(Arguments::of);
    }

    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of("isShopOperator"),
                Arguments.of("register-supplier"),
                Arguments.of("logPartnerVisit"),
                Arguments.of("getDatasource"),
                Arguments.of("getAlerts"),
                Arguments.of("client/summary@GET"),
                Arguments.of("getCampaignsBriefInfo"),
                Arguments.of("getPopularDatasourceParams@GET"),
                Arguments.of("suppliers/full-info@GET"),
                Arguments.of("getCampaign@GET"),
                Arguments.of("campaigns/{campaignId}/offer-mapping/market-skus/{marketSku}/shop-skus@POST"),
                Arguments.of("suppliers/{campaignId}/feed@GET"),
                Arguments.of("manageParam"),
                Arguments.of("suppliers/{campaignId}/feed/suggests@POST"),
                Arguments.of("suppliers/{campaignId}/feed/suggests/{suggestId}@GET"),
                Arguments.of("suppliers/{campaignId}/feed/validations/{validationId}@GET"),
                Arguments.of("suppliers/{campaignId}/feed/suggests/{suggestId}/download@GET"),
                Arguments.of("suppliers/{campaignId}/validations/{validationId}/download@GET"),
                Arguments.of("suppliers/{campaignId}/feed/download@GET"),
                Arguments.of("campaigns/{campaignId}/offer-mapping/shop-skus/offer-processing-statuses@GET"),
                Arguments.of("campaigns/{campaignId}/offer-mapping/shop-skus/offer-integral-statuses@GET"),
                Arguments.of("suppliers/{campaignId}/summary/feed@GET"),
                Arguments.of("suppliers/{campaignId}/summary/stock-storage@GET"),
                Arguments.of("hidden-offers/reasons@GET"),
                Arguments.of("hidden-offers@GET"),
                Arguments.of("hidden-offers/categories-v2@GET"),
                Arguments.of("hidden-offers/details/indexer/{offerId}@GET"),
                Arguments.of("hidden-offers/details/abo@GET"),
                Arguments.of("virtual-shop/status@GET"),
                Arguments.of("fulfillment/services@GET"),
                Arguments.of("fulfillment/services/available@GET"),
                Arguments.of("suppliers/{campaignId}/application@GET"),
                Arguments.of("partners/{campaignId}/contracts@GET"),
                Arguments.of("suppliers/{campaignId}/application/edits@POST"),
                Arguments.of("prepay-request/{requestId}/document@POST"),
                Arguments.of("prepay-request/{requestId}/document/{documentId}@DELETE"),
                Arguments.of("suppliers/{campaignId}/application/status@PUT"),
                Arguments.of("prepay-request/{requestId}/document/{documentId}@GET"),
                Arguments.of("/fulfillment/request"),
                Arguments.of("campaigns/{campaignId}/offer-mapping/list-shop-skus@POST"),
                Arguments.of("fulfillment/reports/sales/virtual@GET"),
                Arguments.of("fulfillment/reports/sales/supplier@GET"),
                Arguments.of("suppliers/fulfillment@GET"),
                Arguments.of("statistics-report/months@GET"),
                Arguments.of("statistics-report/url@GET"),
                Arguments.of("campaigns/{campaignId}/offer-mapping/shop-skus/categories@POST"),
                Arguments.of("campaigns/{campaignId}/offer-mapping/shop-skus/xls@POST"),
                Arguments.of("suppliers/{campaignId}/assortment/commits/{commitId}/download@GET"),
                Arguments.of("suppliers/{campaignId}/assortment/commits/latest/download@GET"),
                Arguments.of("campaigns/{campaignId}/offer-mapping/market-skus/queries@POST"),
                Arguments.of("campaigns/{campaignId}/offer-mapping/shop-skus/by-shop-sku/market-skus@PUT"),
                Arguments.of("campaigns/{campaignId}/offer-mapping/shop-skus/by-shop-sku@GET"),
                Arguments.of("suppliers/{campaignId}/assortment/validations@POST"),
                Arguments.of("suppliers/{campaignId}/assortment/commits@POST"),
                Arguments.of("suppliers/{campaignId}/assortment/commits/{commitId}@GET"),
                Arguments.of("campaigns/{campaignId}/offer-mapping/shop-skus/categories/{categoryId}/content-template" +
                        "@POST"),
                Arguments.of("suppliers/{campaignId}/documents/attachment@POST"),
                Arguments.of("suppliers/{campaignId}/documents/update@POST"),
                Arguments.of("suppliers/{campaignId}/documents@GET"),
                Arguments.of("suppliers/{campaignId}/documents/offers@GET"),
                Arguments.of("suppliers/{campaignId}/documents/offers@POST"),
                Arguments.of("suppliers/{campaignId}/documents/offers@DELETE"),
                Arguments.of("campaigns/{campaignId}/offer-mapping/shop-skus@POST"),
                Arguments.of("campaigns/{campaignId}/offer-mapping/shop-skus/by-shop-sku/availability@PUT"),
                Arguments.of("suppliers/{campaignId}/documents/search@GET"),
                Arguments.of("campaigns/{campaignId}/offer-mapping/shop-skus/by-shop-sku/warehouses@GET")
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void testAgencyOperations(String opName) {
        Assertions.assertTrue(secManager.canDo(opName, dataWithCampaignId));
        Assertions.assertTrue(secManager.canDo(opName, dataWithoutCampaignId));
        Assertions.assertTrue(secManager.canDo(opName, dataWithBusinessId));
    }

    @Test
    void testGuestOperations() {
        Object guestData = new Object();
        Assertions.assertTrue(secManager.canDo("getAllowedActions", guestData));
        Assertions.assertTrue(secManager.canDo("getCampaignRoles", guestData));
        Assertions.assertTrue(secManager.canDo("cabinet/{cabinet}/page@GET", guestData));

        DualUidable dualUidable = mock(DualUidable.class);
        when(dualUidable.getUid()).thenReturn(1L);
        when(dualUidable.getEffectiveUid()).thenReturn(1L);
    }

    @ParameterizedTest
    @MethodSource("argsOnlyWithCampaign")
    void testDataOnlyWithCampaign(String op) {
        Assertions.assertTrue(secManager.canDo(op, dataWithCampaignId), op);
    }
}
