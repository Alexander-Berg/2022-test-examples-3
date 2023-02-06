package ru.yandex.market.mbo.export;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mboc.http.MboMappingsForDelivery;
import ru.yandex.market.mboc.http.MboMappingsForDelivery.SearchFulfilmentSskuParamsRequest;
import ru.yandex.market.mboc.http.MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.protobuf.ProtobufToJson;

public class SearchFulfilmentSskuParamsTest {

    @Test
    public void testJsonNamingAreNotChangedForSearchFulfilmentSskuParamsRequest() throws IOException {
        // тест проверяет, что json представление не изменилось
        SearchFulfilmentSskuParamsRequest request = SearchFulfilmentSskuParamsRequest.newBuilder()
            .addKeys(MboMappingsForDelivery.FulfillmentShopSkuKey.newBuilder()
                .setSupplierId(1)
                .setShopSku("a")
                .build())
            .addWarehouseId(1L)
            .setInboundDate("2020-01-01")
            .setReturnMasterData(true)
            .build();

        String actualJson = ProtobufToJson.protoToJson(request, Integer.MAX_VALUE);
        Assert.assertEquals("{\"keys\": [{\"supplier_id\": 1,\"shop_sku\": \"a\"}],\"return_master_data\": true," +
            "\"warehouse_id\": [1],\"inbound_date\": \"2020-01-01\"}", actualJson);
    }

    @Test
    public void testJsonNamingAreNotChangedForSearchFulfilmentSskuParamsResponse() throws IOException {
        SearchFulfilmentSskuParamsResponse response = SearchFulfilmentSskuParamsResponse.newBuilder()
            .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                .setAvailability(SupplierOffer.Availability.ACTIVE)
                .addCargoTypes(MboMappingsForDelivery.OfferFulfilmentInfo.MskuCargoType.newBuilder()
                    .setId(1)
                    .setName("a")
                    .setParameterId(1)
                    .build())
                .setShopSku("a")
                .setSupplierId(1)
                .setMarketCategoryId(1)
                .setMarketSkuId(2)
                .build())
            .build();

        String actualJson = ProtobufToJson.protoToJson(response, Integer.MAX_VALUE);
        Assert.assertEquals("{\"fulfilment_info\": [{\"market_sku_id\": 2,\"supplier_id\": 1,\"shop_sku\": \"a\"," +
            "\"market_category_id\": 1,\"availability\": \"ACTIVE\",\"cargo_types\": [{\"id\": 1,\"parameter_id\": 1," +
            "\"name\": \"a\"}]}]}", actualJson);
    }
}
