package ru.yandex.market.ff.service;

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
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.enums.WarehouseServiceType;
import ru.yandex.market.ff.model.bo.SupplierContentMapping;
import ru.yandex.market.ff.model.bo.SupplierSkuKey;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.util.AssortmentEnrichService;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MbocCommon;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AssortmentEnrichServiceTest extends IntegrationTest {

    @Autowired
    private AssortmentEnrichService assortmentEnrichService;

    @Autowired
    private MboMappingsService mboMappingsService;

    @Autowired
    private ShopRequestRepository shopRequestRepository;

    @Autowired
    private RequestItemRepository requestItemRepository;

    @Test
    @DatabaseSetup("classpath:service/assortment-supplier-sku-enricher/1/before.xml")
    @ExpectedDatabase(value = "classpath:service/assortment-supplier-sku-enricher/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void enrichAssortmentItemsTest() {
        Map<SupplierSkuKey, SupplierContentMapping> supplierMapping = getSupplierMapping(List.of(
                new Tuple3(1L, "sku1", true),
                new Tuple3(1L, "sku2", true),
                new Tuple3(1L, "sku3", false)
        ));
        ShopRequest request = shopRequestRepository.findOne(1L);
        List<RequestItem> items = requestItemRepository.findAllByRequestIdOrderById(1L);

        mockMboMappingsService(List.of(
                new Tuple3(1L, "sku1", List.of("sku4", "sku5")),
                new Tuple3(1L, "sku2", List.of("sku6"))
        ));

        mockDeliveryParams(List.of(
                new Tuple2(1L, "sku4"),
                new Tuple2(1L, "sku5"),
                new Tuple2(1L, "sku6")
        ));
        Set<SupplierSkuKey> supplierSkuKeys =
                assortmentEnrichService.enrichAssortmentItems(supplierMapping, request, items);
        assertThat(supplierSkuKeys.size()).isEqualTo(2);
        assertThat(supplierMapping.values().size()).isEqualTo(6);
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

    private void mockDeliveryParams(List<Tuple2<Long, String>> data) {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.Builder builder =
                MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder();
        data.forEach(tuple -> {
            MboMappingsForDelivery.OfferFulfilmentInfo info =
                    MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                            .setSupplierId(tuple.get1().intValue())
                            .setShopSku(tuple.get2())
                            .build();
            builder.addFulfilmentInfo(info);
        });
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(builder.build());
    }
}
