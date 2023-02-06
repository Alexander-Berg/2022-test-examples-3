package ru.yandex.market.logistics.oebs.client;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.oebs.client.model.GetVirtualAccountResponse;
import ru.yandex.market.logistics.oebs.client.model.OebsEntities;
import ru.yandex.market.logistics.oebs.client.model.OebsRequestId;

class GetVirtualAccountTest extends AbstractClientTest {

    @Autowired
    OebsClient oebsClient;

    @Test
    void success() {
        prepareMockRequest(
            "/oebsapi/rest/getStatusBilling",
            "data/virtualacconut/get/request/request.json",
            "data/virtualacconut/get/response/success.json"
        );
        List<GetVirtualAccountResponse> response = oebsClient.getVirtualAccount(
            List.of(OebsRequestId.builder().requestId(52855297L).build())
        );
        softly.assertThat(response).containsExactly(
            GetVirtualAccountResponse.builder()
                .entityId("MARKET_DOSTAVKA_P_245553")
                .entityType("SCHET")
                .requestId(52855298L)
                .status("OK")
                .oebsEntities(OebsEntities.builder().accountNumber("DOSTAVKA_P_245553").build())
                .build()
        );
    }

    @Test
    void requestIdNotFound() {
        prepareMockRequest(
            "/oebsapi/rest/getStatusBilling",
            "data/virtualacconut/get/request/request.json",
            "data/virtualacconut/get/response/not_found.json"
        );
        List<GetVirtualAccountResponse> response = oebsClient.getVirtualAccount(
            List.of(OebsRequestId.builder().requestId(52855297L).build())
        );
        softly.assertThat(response).containsExactly(
            GetVirtualAccountResponse.builder()
                .result("ERROR")
                .errors(List.of("Не найден запрос с ID=52855297"))
                .build()
        );
    }

    @Test
    void statusIsError() {
        prepareMockRequest(
            "/oebsapi/rest/getStatusBilling",
            "data/virtualacconut/get/request/request.json",
            "data/virtualacconut/get/response/error.json"
        );
        List<GetVirtualAccountResponse> response = oebsClient.getVirtualAccount(
            List.of(OebsRequestId.builder().requestId(52855297L).build())
        );
        softly.assertThat(response).containsExactly(
            GetVirtualAccountResponse.builder()
                .entityId("MARKET_DOSTAVKA_P_245553")
                .entityType("SCHET")
                .requestId(52855297L)
                .status("ERROR")
                .errors(List.of("Ошибка создания счета : ORA-20001: Value ALMSOF has expired."))
                .build()
        );
    }
}
