package ru.yandex.market.logistics.management.service.oebs;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.exception.OebsException;
import ru.yandex.market.logistics.management.queue.model.EntityIdPayload;
import ru.yandex.market.logistics.management.queue.model.PartnerOebsRequestPayload;
import ru.yandex.market.logistics.management.queue.processor.billing.AbstractPartnerBillingRegistrationProcessor;
import ru.yandex.market.logistics.management.service.billing.PartnerBillingRegistrationException;
import ru.yandex.market.logistics.oebs.client.OebsClient;
import ru.yandex.market.logistics.oebs.client.model.GetVirtualAccountResponse;
import ru.yandex.market.logistics.oebs.client.model.OebsEntities;
import ru.yandex.market.logistics.oebs.client.model.OebsRequestId;

import static org.mockito.Mockito.when;

@DatabaseSetup("/data/service/balance/createperson/before/legal_info.xml")
@DatabaseSetup("/data/service/balance/createperson/before/partner_subtypes.xml")
@DatabaseSetup("/data/service/balance/createperson/before/partner.xml")
class VirtualAccountSavingTest extends AbstractContextualTest {
    private static final PartnerOebsRequestPayload PAYLOAD = new PartnerOebsRequestPayload("", 1L, 2L);
    @Autowired
    OebsClient oebsClient;
    @Autowired
    private AbstractPartnerBillingRegistrationProcessor<PartnerOebsRequestPayload, EntityIdPayload>
        virtualAccountSavingProcessor;

    @Test
    @DisplayName("Не можем сохранить виртуальный счёт, так как нет идентификатора запроса в OEBS")
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/save/after/no_oebs_request_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void virtualAccountSavingFailedNoRequestId() {
        PartnerOebsRequestPayload payload = new PartnerOebsRequestPayload("", 1L, null);
        virtualAccountSavingProcessor.processPayload(payload);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/partner_type_not_ds.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/save/after/partner_type_not_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Не можем сохранить договор, так как тип партнёра не СД")
    void failedVirtualAccountSavingSc() {
        virtualAccountSavingProcessor.processPayload(PAYLOAD);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/partner_subtype_not_pickup_point.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/save/after/partner_subtype_not_pickup_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Не можем сохранить договор, так как подтип партнёра не партнёрская ПВЗ")
    void failedVirtualAccountSavingForNotPickupPoint() {
        virtualAccountSavingProcessor.processPayload(PAYLOAD);
    }

    @Test
    @DisplayName("Успешное сохранение виртуального договора")
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/save/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successVirtualAccountSaving() {
        GetVirtualAccountResponse expectedResponse = GetVirtualAccountResponse.builder()
            .status("OK")
            .entityType("SCHET")
            .entityId("MARKET_DOSTAVKA_P_245553")
            .oebsEntities(OebsEntities.builder().accountNumber("DOSTAVKA_P_245553").build())
            .requestId(52855298L)
            .build();
        when(oebsClient.getVirtualAccount(List.of(OebsRequestId.builder().requestId(2L).build())))
            .thenReturn(List.of(expectedResponse));

        virtualAccountSavingProcessor.processPayload(PAYLOAD);
    }

    @Test
    @DisplayName("Корректная ошибка сохранения договора")
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/save/after/failed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failedVirtualAccountSaving() {
        when(oebsClient.getVirtualAccount(List.of(OebsRequestId.builder().requestId(2L).build())))
            .thenReturn(List.of(
                GetVirtualAccountResponse
                    .builder()
                    .result("ERROR")
                    .errors(List.of("Ошибка создания счета : ORA-20001: Value ALMSOF has expired."))
                    .build()
            ));

        softly.assertThatThrownBy(() -> virtualAccountSavingProcessor.processPayload(PAYLOAD))
            .isInstanceOf(PartnerBillingRegistrationException.class)
            .hasMessage("Ошибка при сохранении виртуального счёта.")
            .hasRootCauseInstanceOf(OebsException.class)
            .hasRootCauseMessage("[Ошибка создания счета : ORA-20001: Value ALMSOF has expired.]");
    }

    @Test
    @DisplayName("Исключительная ситуация при сохранении виртуального договора")
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/save/after/failed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failedVirtualAccountSavingInternalError() {
        when(oebsClient.getVirtualAccount(List.of(OebsRequestId.builder().requestId(2L).build())))
            .thenThrow(new RuntimeException("Internal Error"));

        softly.assertThatThrownBy(() -> virtualAccountSavingProcessor.processPayload(PAYLOAD))
            .isInstanceOf(PartnerBillingRegistrationException.class)
            .hasMessage("Ошибка при сохранении виртуального счёта.")
            .hasRootCauseInstanceOf(RuntimeException.class)
            .hasRootCauseMessage("Internal Error");
    }
}
