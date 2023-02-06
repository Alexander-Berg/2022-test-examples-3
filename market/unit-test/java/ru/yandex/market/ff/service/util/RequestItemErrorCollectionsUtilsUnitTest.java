package ru.yandex.market.ff.service.util;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import ru.yandex.market.ff.model.bo.EnrichmentResultContainer;
import ru.yandex.market.ff.model.entity.ExternalRequestItemError;
import ru.yandex.market.ff.model.entity.RequestItem;

public class RequestItemErrorCollectionsUtilsUnitTest {


    @Test
    public void requestItemErrorCollectionsUtilsExternalErrorsTest() {

        RequestItem requestItem = new RequestItem();
        requestItem.setRequestId(1L);
        requestItem.setId(2L);
        requestItem.setArticle("sku1");

        ExternalRequestItemError externalRequestItemError = new ExternalRequestItemError();
        externalRequestItemError.setRequestItemId(2L);
        externalRequestItemError.setTemplate(
                "Запрещены поставки товара поставщика '{{supplierTitle}}' #{{supplierId}} на склад " +
                        "'{{warehouseName}}' #{{warehouseId}}, возможные склады к поставке: " +
                        "{{#allowedWarehouses}}'{{warehouseName}}'{{^last}}, {{/last}}{{/allowedWarehouses}}");
        externalRequestItemError.setErrorParams(
                "{\"supplierId\":610482,\"warehouseId\":145,\"allowedWarehouses\":" +
                        "[{\"last\":false,\"warehouseId\":147,\"warehouseName\":\"Яндекс.Маркет (Ростов-на-Дону)\"}," +
                        "{\"last\":false,\"warehouseId\":171,\"warehouseName\":\"Яндекс.Маркет (Томилино)\"}," +
                        "{\"last\":false,\"warehouseId\":172,\"warehouseName\":\"Яндекс.Маркет (Софьино)\"}," +
                        "{\"last\":true,\"warehouseId\":300,\"warehouseName\":\"Яндекс.Маркет (Екатеринбург)\"}]," +
                        "\"supplierTitle\":\"Галерея Красоты\",\"warehouseName\":\"Маршрут (Котельники)\"}");
        externalRequestItemError.setServiceIds(Set.of(145L));

        HashMap<String, EnrichmentResultContainer> result = RequestItemErrorCollectionsUtils
                .combineErrorsIntoArticleToErrorsMap(1L, List.of(requestItem), List.of(),
                        List.of(externalRequestItemError));

        Assertions.assertNotNull(result);

        Assertions.assertEquals(
                "Запрещены поставки товара поставщика 'Галерея Красоты' #610482 на склад " +
                        "'Маршрут (Котельники)' #145, возможные склады к поставке:" +
                        " 'Яндекс.Маркет (Ростов-на-Дону)', 'Яндекс.Маркет (Томилино)', " +
                        "'Яндекс.Маркет (Софьино)', 'Яндекс.Маркет (Екатеринбург)'",
                result.get("sku1").getExternalErrors().asList().get(0).getFullErrorMessage());

    }

}
