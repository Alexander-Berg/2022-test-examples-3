package ru.yandex.market.logistics.oebs.client;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.oebs.client.model.CreateVirtualAccountRequest;
import ru.yandex.market.logistics.oebs.client.model.CreateVirtualAccountResponse;

class CreateVirtualAccountTest extends AbstractClientTest {
    @Autowired
    OebsClient oebsClient;

    @Test
    void success() {
        prepareMockRequest(
            "/oebsapi/rest/billingImport",
            "data/virtualacconut/create/request/request.json",
            "data/virtualacconut/create/response/success.json"
        );
        CreateVirtualAccountResponse response = oebsClient.createVirtualAccount(
            CreateVirtualAccountRequest.builder()
                .transactionDate(Date.from(Instant.parse("2020-12-03T10:15:30.00Z")))
                .transactionNumber("DOSTAVKA_P_245553")
                .entityId("MARKET_DOSTAVKA_P_245553")
                .entityType("SCHET")
                .customerGuid("P189090")
                .contractGuid("245553")
                .currencyCode("RUB")
                .build()
        );

        softly.assertThat(response).isEqualTo(
            CreateVirtualAccountResponse.builder()
                .requestId(52855297L)
                .result("SUCCESS")
                .build()
        );
    }

    @Test
    void fail() {
        prepareMockRequest(
            "/oebsapi/rest/billingImport",
            "data/virtualacconut/create/request/request.json",
            "data/virtualacconut/create/response/fail.json"
        );
        CreateVirtualAccountResponse response = oebsClient.createVirtualAccount(
            CreateVirtualAccountRequest.builder()
                .transactionDate(Date.from(Instant.parse("2020-12-03T10:15:30.00Z")))
                .transactionNumber("DOSTAVKA_P_245553")
                .entityId("MARKET_DOSTAVKA_P_245553")
                .entityType("SCHET")
                .customerGuid("P189090")
                .contractGuid("245553")
                .currencyCode("RUB")
                .build()
        );

        softly.assertThat(response).isEqualTo(
            CreateVirtualAccountResponse.builder()
                .result("ERROR")
                .errors(List.of("ORA-20000: Не опознана система-источник запроса"))
                .build()
        );
    }
}
