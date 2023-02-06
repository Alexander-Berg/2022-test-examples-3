package ru.yandex.market.logistics.management.service.oebs;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.exception.OebsException;
import ru.yandex.market.logistics.oebs.client.OebsClient;
import ru.yandex.market.logistics.oebs.client.model.CreateVirtualAccountRequest;
import ru.yandex.market.logistics.oebs.client.model.CreateVirtualAccountResponse;
import ru.yandex.market.logistics.oebs.client.model.GetVirtualAccountResponse;
import ru.yandex.market.logistics.oebs.client.model.OebsEntities;
import ru.yandex.market.logistics.oebs.client.model.OebsRequestId;

import static org.mockito.Mockito.when;

class OebsServiceTest extends AbstractContextualTest {
    @Autowired
    private OebsService oebsService;

    @Autowired
    private OebsClient oebsClient;

    @Test
    void successCreateVirtualAccount() {
        CreateVirtualAccountRequest createVirtualAccountRequest = CreateVirtualAccountRequest.builder()
            .transactionNumber("DOSTAVKA_P_245553")
            .entityId("MARKET_DOSTAVKA_P_245553")
            .entityType("SCHET")
            .customerGuid("P_245553")
            .contractGuid("contractGuid")
            .transactionDate(Date.from(Instant.parse("2020-02-25T12:00:00Z")))
            .build();
        long expectedRequestId = 52855298L;

        when(oebsClient.createVirtualAccount(createVirtualAccountRequest))
            .thenReturn(CreateVirtualAccountResponse.builder().result("SUCCESS").requestId(52855298L).build());

        long actualRequestId = oebsService.createVirtualAccount(createVirtualAccountRequest);

        softly.assertThat(actualRequestId).isEqualTo(expectedRequestId);
    }

    @Test
    void failedCreateVirtualAccount() {
        CreateVirtualAccountRequest createVirtualAccountRequest = CreateVirtualAccountRequest.builder()
            .transactionNumber("DOSTAVKA_P_245553")
            .entityId("MARKET_DOSTAVKA_P_245553")
            .entityType("SCHET")
            .customerGuid("P_245553")
            .contractGuid("contractGuid")
            .transactionDate(Date.from(Instant.parse("2020-02-25T12:00:00Z")))
            .build();

        when(oebsClient.createVirtualAccount(createVirtualAccountRequest))
            .thenReturn(
                CreateVirtualAccountResponse.builder()
                    .result("ERROR")
                    .errors(List.of("ORA-20000: Не опознана система-источник запроса"))
                    .build()
            );

        softly.assertThatThrownBy(() -> oebsService.createVirtualAccount(createVirtualAccountRequest))
            .isInstanceOf(OebsException.class)
            .hasMessage("[ORA-20000: Не опознана система-источник запроса]");
    }

    @Test
    void successGettingVirtualAccount() {
        GetVirtualAccountResponse expectedResponse = GetVirtualAccountResponse.builder()
            .status("OK")
            .entityType("SCHET")
            .entityId("MARKET_DOSTAVKA_P_245553")
            .oebsEntities(OebsEntities.builder().accountNumber("MARKET_DOSTAVKA_P_245553").build())
            .requestId(52855298L)
            .build();
        when(oebsClient.getVirtualAccount(List.of(OebsRequestId.builder().requestId(1L).build())))
            .thenReturn(List.of(expectedResponse));

        GetVirtualAccountResponse actualResponse = oebsService.getVirtualAccount(1L);
        softly.assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void errorGettingVirtualAccount() {
        when(oebsClient.getVirtualAccount(List.of(OebsRequestId.builder().requestId(1L).build())))
            .thenReturn(List.of(
                GetVirtualAccountResponse
                    .builder()
                    .result("ERROR")
                    .errors(List.of("Ошибка создания счета : ORA-20001: Value ALMSOF has expired."))
                    .build()
            ));
        softly.assertThatThrownBy(() -> oebsService.getVirtualAccount(1L))
            .isInstanceOf(OebsException.class)
            .hasMessage("[Ошибка создания счета : ORA-20001: Value ALMSOF has expired.]");
    }

    @Test
    void gettingFailedVirtualAccount() {
        when(oebsClient.getVirtualAccount(List.of(OebsRequestId.builder().requestId(1L).build())))
            .thenReturn(List.of(
                GetVirtualAccountResponse
                    .builder()
                    .status("ERROR")
                    .errors(List.of("Ошибка создания счета : ORA-20001: Value ALMSOF has expired."))
                    .build()
            ));
        softly.assertThatThrownBy(() -> oebsService.getVirtualAccount(1L))
            .isInstanceOf(OebsException.class)
            .hasMessage("[Ошибка создания счета : ORA-20001: Value ALMSOF has expired.]");
    }

    @Test
    void errorGettingMultipleVirtualAccount() {
        when(oebsClient.getVirtualAccount(List.of(OebsRequestId.builder().requestId(1L).build())))
            .thenReturn(List.of(
                GetVirtualAccountResponse.builder().build(),
                GetVirtualAccountResponse.builder().build()
            ));
        softly.assertThatThrownBy(() -> oebsService.getVirtualAccount(1L))
            .isInstanceOf(OebsException.class)
            .hasMessage("Multiple requests for requestId 1");
    }

    @Test
    void errorGettingEmptyVirtualAccount() {
        when(oebsClient.getVirtualAccount(List.of(OebsRequestId.builder().requestId(1L).build())))
            .thenReturn(List.of());
        softly.assertThatThrownBy(() -> oebsService.getVirtualAccount(1L))
            .isInstanceOf(OebsException.class)
            .hasMessage("No request for requestId 1");
    }
}
