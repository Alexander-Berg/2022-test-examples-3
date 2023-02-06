package ru.yandex.market.ff.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple3;
import ru.yandex.bolts.collection.Tuple4;
import ru.yandex.market.deepmind.openapi.client.api.AvailabilitiesApi;
import ru.yandex.market.deepmind.openapi.client.model.AvailabilitiesResponse;
import ru.yandex.market.deepmind.openapi.client.model.AvailabilityInfo;
import ru.yandex.market.deepmind.openapi.client.model.ShopSkuKey;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.enums.WarehouseServiceType;
import ru.yandex.market.ff.model.bo.EnrichmentResultContainer;
import ru.yandex.market.ff.model.bo.RequestItemErrorInfo;
import ru.yandex.market.ff.model.bo.SupplierContentMapping;
import ru.yandex.market.ff.model.bo.SupplierSkuKey;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.util.AssortmentValidateService;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MbocCommon;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ff.client.enums.RequestItemErrorType.ASSORTMENT_SKU_NOT_FOUND;

public class RequestItemAssortmentSkuValidateServiceTest extends IntegrationTest {

    private static final long SUPPLER_ID_1 = 1L;
    private static final long SUPPLER_ID_10 = 10L;
    private static final long SUPPLER_ID_100 = 100L;
    private static final String ARTICLE_1 = "article1";
    private static final String ARTICLE_2 = "article2";

    @Autowired
    private AssortmentValidateService assortmentValidateService;

    @Autowired
    private AvailabilitiesApi availabilitiesApi;

    @Autowired
    private MboMappingsService mboMappingsService;

    @Test
    @DatabaseSetup("classpath:service/assortment-supplier-slu-validation/before.xml")
    @ExpectedDatabase(value = "classpath:service/assortment-supplier-slu-validation/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void validateTest() {
        Map<SupplierSkuKey, SupplierContentMapping> supplierMapping = getSupplierMapping(List.of(
                new Tuple3(SUPPLER_ID_1, ARTICLE_1, true),
                new Tuple3(SUPPLER_ID_10, ARTICLE_2, true),
                new Tuple3(SUPPLER_ID_100, ARTICLE_1, false)
        ));
        List<RequestItem> items = requestItem(List.of(
                new Tuple4(0L, SUPPLER_ID_1, ARTICLE_1, 10D),
                new Tuple4(1L, SUPPLER_ID_10, ARTICLE_2, 100D),
                new Tuple4(2L, SUPPLER_ID_100, ARTICLE_1, 1000D)
        ));
        ShopRequest request = getRequest(items);

        mockMboMappingsService(List.of(
                new Tuple3(SUPPLER_ID_1, ARTICLE_1, List.of("sku1", "sku2", "sku3")),
                new Tuple3(SUPPLER_ID_10, ARTICLE_2, List.of("sku4"))
        ));

        mockAvailabilitiesApi(request, items);
        Set<SupplierSkuKey> sskuKey = getSskuKey(List.of(new Tuple2(SUPPLER_ID_1, ARTICLE_1)));
        assortmentValidateService.validate(supplierMapping, request, items, sskuKey);
    }

    @Test
    @DatabaseSetup("classpath:service/assortment-supplier-slu-validation/before.xml")
    void invalidAssortmentTest() {
        Map<SupplierSkuKey, SupplierContentMapping> mapping = getSupplierMapping(List.of(
                new Tuple3(SUPPLER_ID_1, ARTICLE_1, true),
                new Tuple3(SUPPLER_ID_10, ARTICLE_2, true),
                new Tuple3(SUPPLER_ID_100, ARTICLE_1, null)
        ));
        List<RequestItem> items = requestItem(List.of(
                new Tuple4(0L, SUPPLER_ID_1, ARTICLE_1, 10D),
                new Tuple4(1L, SUPPLER_ID_10, ARTICLE_2, 100D),
                new Tuple4(2L, SUPPLER_ID_100, ARTICLE_1, 1000D)
        ));
        mockMboMappingsService(List.of(
                new Tuple3(SUPPLER_ID_1, ARTICLE_1, List.of()),
                new Tuple3(SUPPLER_ID_10, ARTICLE_2, null)
        ));

        ShopRequest request = getRequest(items);
        Set<SupplierSkuKey> sskuKey = getSskuKey(List.of(
                new Tuple2(SUPPLER_ID_1, ARTICLE_1),
                new Tuple2(SUPPLER_ID_10, ARTICLE_2)
        ));
        Map<Long, EnrichmentResultContainer> result =
                assortmentValidateService.validate(mapping, request, items, sskuKey);
        List<RequestItemErrorType> errorTypes = result.values().stream().map(error -> error.getValidationErrors())
                .flatMap(Set::stream)
                .map(RequestItemErrorInfo::getType)
                .collect(Collectors.toList());
        assertThat(errorTypes, equalTo(List.of(ASSORTMENT_SKU_NOT_FOUND, ASSORTMENT_SKU_NOT_FOUND)));

    }

    private ShopRequest getRequest(List<RequestItem> items) {
        ShopRequest request = new ShopRequest();
        request.setServiceId(1L);
        request.setItems(items);
        return request;
    }

    private Map<SupplierSkuKey, SupplierContentMapping> getSupplierMapping(List<Tuple3<Long, String, Boolean>> data) {
        return data.stream()
                .collect(Collectors.toMap(skuKey -> new SupplierSkuKey(skuKey.get1(), skuKey.get2()),
                        supplierMapping -> {
                            SupplierContentMapping.Builder builder = SupplierContentMapping
                                    .builder(supplierMapping.get2(), supplierMapping.get1(), "title")
                                    .setBoxCount(1);
                            if (Boolean.TRUE.equals(supplierMapping.get3())) {
                                builder.setWarehouseServices(List.of(WarehouseServiceType.IS_NEED_SORT));
                            }
                            return builder.build();
                        }));
    }

    private List<RequestItem> requestItem(List<Tuple4<Long, Long, String, Double>> data) {
        return data.stream()
                .map(value -> {
                    RequestItem requestItem = new RequestItem();
                    requestItem.setId(value.get1());
                    requestItem.setSupplierId(value.get2());
                    requestItem.setCount(10);
                    requestItem.setArticle(value.get3());
                    requestItem.setSupplyPrice(BigDecimal.valueOf(value.get4()));
                    return requestItem;
                })
                .collect(Collectors.toList());
    }

    private void mockMboMappingsService(List<Tuple3<Long, String, List<String>>> data) {
        List<MboMappings.GetAssortmentChildSskusResponse.GetAssortmentChildSskusResult> results =
                data.stream()
                        .map(result -> {
                            MboMappings.GetAssortmentChildSskusResponse.GetAssortmentChildSskusResult.Builder builder =
                                    MboMappings.GetAssortmentChildSskusResponse.GetAssortmentChildSskusResult
                                            .newBuilder()
                                            .setAssortmentShopSkuKey(MbocCommon.ShopSkuKey.newBuilder()
                                                    .setSupplierSkuId(result.get2())
                                                    .setSupplierId(result.get1().intValue())
                                                    .build());
                            if (result.get3() != null) {
                                builder.addAllChildSkus(result.get3());
                            }
                            return builder.build();
                        })
                        .collect(Collectors.toList());

        when(mboMappingsService.getAssortmentChildSskus(any()))
                .thenReturn(MboMappings.GetAssortmentChildSskusResponse.newBuilder().addAllResults(results).build());
    }

    private void mockAvailabilitiesApi(ShopRequest request, List<RequestItem> items) {
        List<AvailabilitiesResponse> availabilitiesResponses = items.stream()
                .map(item -> {
                    ShopSkuKey shopSkuKey = new ShopSkuKey();
                    shopSkuKey.setShopSku(item.getArticle());
                    shopSkuKey.setSupplierId(Math.toIntExact(item.getSupplierId()));

                    AvailabilityInfo info = new AvailabilityInfo();
                    info.setAllowInbound(true);

                    AvailabilitiesResponse availabilitiesResponse = new AvailabilitiesResponse();
                    availabilitiesResponse.setKey(shopSkuKey);
                    availabilitiesResponse.setAvailabilities(List.of(info));
                    return availabilitiesResponse;
                })
                .collect(Collectors.toList());

        when(availabilitiesApi.getBySsku(any())).thenReturn(availabilitiesResponses);
    }

    private Set<SupplierSkuKey> getSskuKey(List<Tuple2<Long, String>> data) {
        return data.stream()
                .map(pair -> new SupplierSkuKey(pair.get1(), pair.get2()))
                .collect(Collectors.toSet());
    }
}
