package ru.yandex.market.logistics.management.service.oebs;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.exception.OebsException;
import ru.yandex.market.logistics.management.queue.model.EntityIdPayload;
import ru.yandex.market.logistics.management.queue.model.PartnerOebsRequestPayload;
import ru.yandex.market.logistics.management.queue.processor.billing.AbstractPartnerBillingRegistrationProcessor;
import ru.yandex.market.logistics.management.service.billing.PartnerBillingRegistrationException;
import ru.yandex.market.logistics.oebs.client.OebsClient;
import ru.yandex.market.logistics.oebs.client.model.CreateVirtualAccountRequest;
import ru.yandex.market.logistics.oebs.client.model.CreateVirtualAccountResponse;

import static org.mockito.Mockito.when;

@DatabaseSetup("/data/service/balance/createperson/before/legal_info.xml")
@DatabaseSetup("/data/service/balance/createperson/before/partner_subtypes.xml")
@DatabaseSetup("/data/service/balance/createperson/before/partner.xml")
@DatabaseSetup(value = "/data/service/balance/createoffer/before/billing_person.xml", type = DatabaseOperation.REFRESH)
@DatabaseSetup(value = "/data/service/oebs/virtaulaccount/create/before/offer.xml", type = DatabaseOperation.REFRESH)
class VirtualAccountCreationTest extends AbstractContextualTest {
    private static final EntityIdPayload PAYLOAD = new EntityIdPayload("", 1L);
    @Autowired
    OebsClient oebsClient;
    @Autowired
    private AbstractPartnerBillingRegistrationProcessor<EntityIdPayload, PartnerOebsRequestPayload>
        virtualAccountCreationProcessor;

    @Test
    @DatabaseSetup(
        value = "/data/service/oebs/virtaulaccount/create/before/no_offer.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/create/after/no_offer.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать виртуальный счёт, так как нет balanceContract")
    void failedVirtualAccountCreationWithoutBalanceContract() {
        softly.assertThatThrownBy(() -> virtualAccountCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Невозможно создать виртуальный счёт для партнёра id = 1: поле balanceContract не заполнено.");
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/partner_type_not_ds.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/create/after/partner_type_not_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать договор, так как тип партнёра не СД")
    void failedVirtualAccountCreationSc() {
        virtualAccountCreationProcessor.processPayload(PAYLOAD);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/partner_subtype_not_pickup_point.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/create/after/partner_subtype_not_pickup_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать договор, так как подтип партнёра не партнёрская ПВЗ")
    void failedVirtualAccountCreationForNotPickupPoint() {
        virtualAccountCreationProcessor.processPayload(PAYLOAD);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/oebs/virtaulaccount/create/before/no_billing_person_id.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/create/after/no_billing_person_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать виртуальный счёт, так как нет billingPersonId")
    void failedVirtualAccountCreationWithoutBalancePersonId() {
        softly.assertThatThrownBy(() -> virtualAccountCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Невозможно создать виртуальный счёт для партнёра id = 1: не найден billingPersonId");
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/oebs/virtaulaccount/create/before/no_contract_external_id.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/create/after/no_contract_external_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать виртуальный счёт, так как нет contractExternalId")
    void failedVirtualAccountCreationWithoutContractExternalId() {
        softly.assertThatThrownBy(() -> virtualAccountCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Невозможно создать виртуальный счёт для партнёра id = 1: не найден contractExternalId");
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/oebs/virtaulaccount/create/before/no_contract_balance_id.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/create/after/no_contract_balance_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать виртуальный счёт, так как нет contractBalanceId")
    void failedVirtualAccountCreationWithoutContractBalanceId() {
        softly.assertThatThrownBy(() -> virtualAccountCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Невозможно создать виртуальный счёт для партнёра id = 1: не найден contractBalanceId");
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/oebs/virtaulaccount/create/before/long_balance_id.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/create/after/long_balance_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать виртуальный счёт, так как id договора в Балансе слишком длинный")
    void failedVirtualAccountCreationLongBalanceId() {
        softly.assertThatThrownBy(() -> virtualAccountCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Длина transactionNumber превысила максимальное число символов: 20");
    }

    @Test
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/create/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Успешное создание виртуального договора")
    void successVirtualAccountCreation() {
        CreateVirtualAccountRequest createVirtualAccountRequest = CreateVirtualAccountRequest.builder()
            .transactionNumber("DOSTAVKA_P_133")
            .entityId("MARKET_DOSTAVKA_P_133")
            .entityType("SCHET")
            .customerGuid("P13132301")
            .contractGuid("133")
            .transactionDate(
                Date.from(
                    LocalDateTime.of(2019, 10, 21, 18, 9, 53, 202000000)
                        .atZone(DateTimeUtils.MOSCOW_ZONE)
                        .toInstant()
                )
            )
            .currencyCode("RUB")
            .build();

        when(oebsClient.createVirtualAccount(createVirtualAccountRequest))
            .thenReturn(CreateVirtualAccountResponse.builder().result("SUCCESS").requestId(52855298L).build());

        virtualAccountCreationProcessor.processPayload(PAYLOAD);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/create/after/failed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Корректная ошибка создания договора")
    void failedVirtualAccountCreation() {
        CreateVirtualAccountRequest createVirtualAccountRequest = CreateVirtualAccountRequest.builder()
            .transactionNumber("DOSTAVKA_P_133")
            .entityId("MARKET_DOSTAVKA_P_133")
            .entityType("SCHET")
            .customerGuid("P13132301")
            .contractGuid("133")
            .transactionDate(
                Date.from(
                    LocalDateTime.of(2019, 10, 21, 18, 9, 53, 202000000)
                        .atZone(DateTimeUtils.MOSCOW_ZONE)
                        .toInstant()
                )
            )
            .currencyCode("RUB")
            .build();

        when(oebsClient.createVirtualAccount(createVirtualAccountRequest))
            .thenReturn(
                CreateVirtualAccountResponse.builder()
                    .result("ERROR")
                    .errors(List.of("ORA-20000: Не опознана система-источник запроса"))
                    .build()
            );

        softly.assertThatThrownBy(() -> virtualAccountCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(PartnerBillingRegistrationException.class)
            .hasMessage("Ошибка при создании виртуального счёта.")
            .hasRootCauseInstanceOf(OebsException.class)
            .hasRootCauseMessage("[ORA-20000: Не опознана система-источник запроса]");
    }

    @Test
    @ExpectedDatabase(
        value = "/data/service/oebs/virtaulaccount/create/after/failed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Исключительная ситуация при создании виртуального договора")
    void failedVirtualAccountCreationInternalError() {
        CreateVirtualAccountRequest createVirtualAccountRequest = CreateVirtualAccountRequest.builder()
            .transactionNumber("DOSTAVKA_P_133")
            .entityId("MARKET_DOSTAVKA_P_133")
            .entityType("SCHET")
            .customerGuid("P13132301")
            .contractGuid("133")
            .transactionDate(
                Date.from(
                    LocalDateTime.of(2019, 10, 21, 18, 9, 53, 202000000)
                        .atZone(DateTimeUtils.MOSCOW_ZONE)
                        .toInstant()
                )
            )
            .currencyCode("RUB")
            .build();

        when(oebsClient.createVirtualAccount(createVirtualAccountRequest))
            .thenThrow(new RuntimeException("Internal Error"));

        softly.assertThatThrownBy(() -> virtualAccountCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(PartnerBillingRegistrationException.class)
            .hasMessage("Ошибка при создании виртуального счёта.")
            .hasRootCauseInstanceOf(RuntimeException.class)
            .hasRootCauseMessage("Internal Error");
    }
}
