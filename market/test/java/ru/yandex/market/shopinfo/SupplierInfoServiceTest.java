package ru.yandex.market.shopinfo;

import java.io.IOException;
import java.util.Optional;

import org.apache.http.HttpResponse;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.mock.HttpResponseMockFactory.getHttpResponseMock;
import static ru.yandex.market.shopinfo.MockHttpClientUtil.mockHttpClient;

/**
 * @author semin-serg
 */
public class SupplierInfoServiceTest {

    private static final long SUPPLIER_ID = 123423L;
    private static final String SUPPLIER_INFO_SERVICE_CORRECT_RESPONSE = String.format(
            "[\n" +
                    "  {\n" +
                    "    \"prepayRequestId\": \"1\",\n" +
                    "    \"supplierId\": \"%d\",\n" +
                    "    \"type\": \"1\",\n" +
                    "    \"ogrn\": \"12345\",\n" +
                    "    \"name\": \"ООО \\\"Рога и Копыта\\\"\",\n" +
                    "    \"juridicalAddress\": \"Москва, ул. Льва Толстого, д.16\",\n" +
                    "    \"factAddress\": \"Москва, ул. Льва Толстого, д.16\",\n" +
                    "    \"supplierName\": \"Рога и Копыта\",\n" +
                    "    \"supplierDomain\": \"roga-i-kopita.ru\",\n" +
                    "    \"createdAt\": \"2018-02-12 00:00:00.0\",\n" +
                    "    \"regnumName\": \"ОГРН\",\n" +
                    "    \"contactPhone\": \"79261234567\",\n" +
                    "    \"shopPhoneNumber\": \"84950950505\",\n" +
                    "    \"inn\": \"772734154102\"\n" +
                    " }\n" +
                    "]", SUPPLIER_ID);

    @Test
    public void supplierInfosCorrectResponseTest() throws IOException {
        SupplierInfoService supplierInfoService = new SupplierInfoService();
        HttpResponse httpResponse = getHttpResponseMock(SUPPLIER_INFO_SERVICE_CORRECT_RESPONSE, 200);
        supplierInfoService.setHttpClient(mockHttpClient(httpResponse));
        Optional<SupplierInfo> optional = supplierInfoService.getShopInfo(SUPPLIER_ID);
        SupplierInfo supplierInfo = optional.get();
        assertEquals(SUPPLIER_ID, Long.parseLong(supplierInfo.getSupplierId()));
    }

}
