package ru.yandex.market.mbi.api.controller.operation.partner;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.client.model.BooleanParamValueDTO;
import ru.yandex.market.mbi.open.api.client.model.GetBusinessIdsForPartnersResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerIdToBusinessIdDTO;
import ru.yandex.market.mbi.open.api.client.model.ShopFeatureSearchResponse;
import ru.yandex.market.mbi.open.api.client.model.ShopsFeatureSearchResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Тесты для {@link ru.yandex.market.mbi.open.api.PartnerApi}.
 */
public class PartnerApiControllerTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "PartnerApiControllerTest.useStocks.before.csv",
            after = "PartnerApiControllerTest.useStocks.after.csv")
    void testUseStocks() {
        getMbiOpenApiClient().useStocks(1, List.of(5L, 6L, 7L));
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerApiControllerTest.setParam.before.csv",
            after = "PartnerApiControllerTest.setParam.after.csv"
    )
    void testSetBoolParam() {
        getMbiOpenApiClient().setBoolParam(
                1L,
                (BooleanParamValueDTO) new BooleanParamValueDTO()
                        .value(false)
                        .paramType(ParamType.CPA_IS_PARTNER_INTERFACE.name())
                        .partnerId(10L)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerApiControllerTest.setStocksByPi.before.csv",
            after = "PartnerApiControllerTest.setStocksByPi.after.csv"
    )
    void testSetBoolParamStocksByPi() {
        getMbiOpenApiClient().setBoolParam(
                1L,
                (BooleanParamValueDTO) new BooleanParamValueDTO()
                        .value(true)
                        .paramType(ParamType.STOCKS_BY_PARTNER_INTERFACE.name())
                        .partnerId(10L)
        );
    }

    @Test
    @DbUnitDataSet(before = "getFeatureInfos.before.csv")
    void testGetShopsWithFeaturePaged() {
        List<ShopFeatureSearchResponse> subsidies = getAllShopsWithFeatureType(FeatureType.SUBSIDIES);
        assertThat(subsidies).containsExactlyInAnyOrder(
                new ShopFeatureSearchResponse().shopId(1L).cpaPartnerInterface(true).isSuccess(true)
        );

        List<ShopFeatureSearchResponse> promoCPC = getAllShopsWithFeatureType(FeatureType.DROPSHIP);
        assertThat(promoCPC).containsExactlyInAnyOrder(
                new ShopFeatureSearchResponse().shopId(100L).cpaPartnerInterface(false).isSuccess(false),
                new ShopFeatureSearchResponse().shopId(102L).cpaPartnerInterface(false).isSuccess(true),
                new ShopFeatureSearchResponse().shopId(103L).cpaPartnerInterface(true).isSuccess(true),
                new ShopFeatureSearchResponse().shopId(115L).cpaPartnerInterface(true).isSuccess(false),
                new ShopFeatureSearchResponse().shopId(305L).cpaPartnerInterface(false).isSuccess(false)
        );
    }

    @Test
    @DbUnitDataSet(before = "getFeatureInfos.before.csv")
    void testGetShopsWithFeaturePaging() {
        FeatureType featureType = FeatureType.DROPSHIP;
        ShopsFeatureSearchResponse page1 =
                getMbiOpenApiClient().getShopsWithFeaturePaged(featureType.getId(), 2, null);

        assertThat(page1.getShops()).containsExactlyInAnyOrder(
                new ShopFeatureSearchResponse().shopId(100L).cpaPartnerInterface(false).isSuccess(false),
                new ShopFeatureSearchResponse().shopId(102L).cpaPartnerInterface(false).isSuccess(true)
        );

        ShopsFeatureSearchResponse page2 =
                getMbiOpenApiClient().getShopsWithFeaturePaged(featureType.getId(), 2, page1.getNextToken());

        assertThat(page2.getShops()).containsExactlyInAnyOrder(
                new ShopFeatureSearchResponse().shopId(103L).cpaPartnerInterface(true).isSuccess(true),
                new ShopFeatureSearchResponse().shopId(115L).cpaPartnerInterface(true).isSuccess(false)
        );

        ShopsFeatureSearchResponse page3 =
                getMbiOpenApiClient().getShopsWithFeaturePaged(featureType.getId(), 2, page2.getNextToken());

        assertThat(page3.getShops()).containsExactlyInAnyOrder(
                new ShopFeatureSearchResponse().shopId(305L).cpaPartnerInterface(false).isSuccess(false)
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerApiControllerTest.getBusinessIdsForPartners.before.csv")
    void getBusinessIdsForPartners() {
        var businessesForPartners = getMbiOpenApiClient().getBusinessIdsForPartners(List.of(11L, 12L, 13L, 14L));

        var expectedResponse = new GetBusinessIdsForPartnersResponse()
                .partnerBusinessList(List.of(
                        new PartnerIdToBusinessIdDTO()
                                .partnerId(11L)
                                .businessId(101L),
                        new PartnerIdToBusinessIdDTO()
                                .partnerId(12L)
                                .businessId(102L),
                        new PartnerIdToBusinessIdDTO()
                                .partnerId(13L)
                                .businessId(103L)
                ));
        MatcherAssert.assertThat(businessesForPartners, equalTo(expectedResponse));
    }

    private List<ShopFeatureSearchResponse> getAllShopsWithFeatureType(FeatureType featureType) {
        return getMbiOpenApiClient().getShopsWithFeaturePaged(featureType.getId(), 100, null)
                .getShops();
    }
}
