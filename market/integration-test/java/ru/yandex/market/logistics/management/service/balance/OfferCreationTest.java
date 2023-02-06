package ru.yandex.market.logistics.management.service.balance;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.management.queue.model.EntityIdPayload;
import ru.yandex.market.logistics.management.queue.processor.billing.AbstractPartnerBillingRegistrationProcessor;
import ru.yandex.market.logistics.management.service.billing.PartnerBillingRegistrationException;
import ru.yandex.market.logistics.management.util.TestableClock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.CLIENT_ID;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.PERSON_ID;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.createOfferResponse;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.createOfferStructure;

@DatabaseSetup("/data/service/balance/createperson/before/legal_info.xml")
@DatabaseSetup("/data/service/balance/createperson/before/partner_subtypes.xml")
@DatabaseSetup("/data/service/balance/createperson/before/partner.xml")
@DatabaseSetup(value = "/data/service/balance/createoffer/before/billing_person.xml", type = DatabaseOperation.REFRESH)
@DatabaseSetup(
    value = "/data/service/balance/createoffer/before/taxation_system.xml",
    type = DatabaseOperation.REFRESH
)
class OfferCreationTest extends AbstractContextualTest {
    private static final EntityIdPayload PAYLOAD = new EntityIdPayload("", 1L);
    private static final String OPERATOR_UID = "123456";
    private static final Instant CURRENT_TIME = Instant.parse("2021-02-01T00:00:00Z");

    @Autowired
    private TestableClock clock;
    @Autowired
    private AbstractPartnerBillingRegistrationProcessor offerCreationProcessor;
    @Autowired
    private Balance2 balance2;
    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void setup() {
        clock.setFixed(CURRENT_TIME, ZoneOffset.UTC);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/without_uid.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/createoffer/after/partner_without_uid.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать договор, так как нет passportUid")
    void failedOfferCreationWithoutUid() {
        softly.assertThatThrownBy(() -> offerCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Невозможно создать договор для партнёра id = 1: поле passportUid не заполнено.");
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/partner_type_not_ds.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/createoffer/after/partner_type_not_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать договор, так как тип партнёра не СД")
    void failedOfferCreationSc() {
        offerCreationProcessor.processPayload(PAYLOAD);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/partner_subtype_not_pickup_point.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/createoffer/after/partner_subtype_not_pickup_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать договор, так как подтип партнёра не партнёрская ПВЗ")
    void failedOfferCreationForNotPickupPoint() {
        offerCreationProcessor.processPayload(PAYLOAD);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createoffer/before/no_billing_person_id.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/createoffer/after/no_billing_person_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать договор, так как нет billingPersonId")
    void failedOfferCreationNoBillingClientId() {
        softly.assertThatThrownBy(() -> offerCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Невозможно создать договор для партнёра id = 1: не найден billingPersonId");
    }

    @Test
    @ExpectedDatabase(
        value = "/data/service/balance/createoffer/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/data/service/balance/createoffer/after/contract_signed_since_is_filled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Успешное создание договора")
    void successOfferCreation() throws XmlRpcException {
        when(balance2.CreateOffer(OPERATOR_UID, createOfferStructure()))
            .thenReturn(createOfferResponse());
        offerCreationProcessor.processPayload(PAYLOAD);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createoffer/before/contract_signed_since_is_not_null.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/createoffer/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Успешное создание договора с датой, указанной при создании партнёра")
    void successOfferCreationWithOfferSignedSinceDate() throws XmlRpcException {
        when(balance2.CreateOffer(OPERATOR_UID, createOfferStructure(
            new Calendar.Builder()
                .setDate(2021, 3, 19)
                .setTimeZone(TimeZone.getTimeZone(DateTimeUtils.MOSCOW_ZONE))
                .build()
                .getTime()
        )))
            .thenReturn(createOfferResponse());
        offerCreationProcessor.processPayload(PAYLOAD);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/service/balance/createoffer/after/balance_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать договор, ошибка на стороне баланса")
    void failCreateOffer() throws XmlRpcException {
        when(balance2.CreateOffer(OPERATOR_UID, createOfferStructure()))
            .thenThrow(new XmlRpcException("Balance CreateOffer error"));
        softly.assertThatThrownBy(() -> offerCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(PartnerBillingRegistrationException.class)
            .hasMessage(
                "Создание договора завершилось с ошибкой: org.apache.xmlrpc.XmlRpcException: Balance CreateOffer error"
            );
    }

    @Test
    @DatabaseSetup(
            value = "/data/service/balance/createoffer/before/contract_signed_since_is_not_null.xml",
            type = DatabaseOperation.REFRESH
    )
    @DisplayName("Не можем создать договор, потому что уже есть несколько дубликатов активных")
    void failCreateOfferWithAlreadyExistsOffer() throws XmlRpcException {
        featureProperties.setEnableCheckExistingOffer(true);

        when(balance2.CreateOffer(OPERATOR_UID, createOfferStructure(
                new Calendar.Builder()
                        .setDate(2021, 3, 19)
                        .setTimeZone(TimeZone.getTimeZone(DateTimeUtils.MOSCOW_ZONE))
                        .build()
                        .getTime()
        )))
                .thenReturn(createOfferResponse());
        when(balance2.GetClientContracts(
                eq(Long.valueOf(CLIENT_ID)),
                eq(Long.valueOf(PERSON_ID)),
                any(Date.class),
                eq("SPENDABLE")
        )).thenReturn(List.of(
                Map.of(
                        "IS_ACTIVE", true,
                        "SERVICES", new Integer[]{725},
                        "ID", 123,
                        "EXTERNAL_ID", "externalId"
                ),
                Map.of(
                        "IS_ACTIVE", true,
                        "SERVICES", new Integer[]{725},
                        "ID", 124,
                        "EXTERNAL_ID", "anotherExternalId"
                )
        ));
        softly.assertThatThrownBy(() -> offerCreationProcessor.processPayload(PAYLOAD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Невозможно создать договор для партнёра id = 1: Найдено 2 активных договоров на 725 сервис"
                );
        featureProperties.setEnableCheckExistingOffer(false);
    }

    @Test
    @DisplayName("Тест на успешное получение уже существующего активного договора, вместо создания нового")
    @DatabaseSetup(
            value = "/data/service/balance/createoffer/before/contract_signed_since_is_not_null.xml",
            type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
            value = "/data/service/balance/createoffer/after/success_with_existing_offer.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testUpdatePersonWithExistingBalanceOfferExists() throws XmlRpcException {
        featureProperties.setEnableCheckExistingOffer(true);
        when(balance2.CreateOffer(OPERATOR_UID, createOfferStructure(
                new Calendar.Builder()
                        .setDate(2021, 3, 19)
                        .setTimeZone(TimeZone.getTimeZone(DateTimeUtils.MOSCOW_ZONE))
                        .build()
                        .getTime()
        )))
                .thenReturn(createOfferResponse());
        when(balance2.GetClientContracts(
                eq(Long.valueOf(CLIENT_ID)),
                eq(Long.valueOf(PERSON_ID)),
                any(Date.class),
                eq("SPENDABLE")
        )).thenReturn(List.of(Map.of(
                "IS_ACTIVE", true,
                "SERVICES", new Integer[]{725},
                "ID", 123,
                "EXTERNAL_ID", "externalId"
        )));

        offerCreationProcessor.processPayload(PAYLOAD);

        //убеждаемся, что реально в баланс не сходили
        verify(balance2, never()).CreateOffer(any(), any());
        featureProperties.setEnableCheckExistingOffer(false);
    }

    @Test
    @DisplayName("Тест на успешное создание договора, потому что существующий - неактивный")
    @DatabaseSetup(
            value = "/data/service/balance/createoffer/before/contract_signed_since_is_not_null.xml",
            type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
            value = "/data/service/balance/createoffer/after/success_with_existing_offer_is_wrong.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testCreateOfferWithExistingButIsNotActive() throws XmlRpcException {
        featureProperties.setEnableCheckExistingOffer(true);
        when(balance2.CreateOffer(OPERATOR_UID, createOfferStructure(
                new Calendar.Builder()
                        .setDate(2021, 3, 19)
                        .setTimeZone(TimeZone.getTimeZone(DateTimeUtils.MOSCOW_ZONE))
                        .build()
                        .getTime()
        )))
                .thenReturn(createOfferResponse());
        when(balance2.GetClientContracts(
                eq(Long.valueOf(CLIENT_ID)),
                eq(Long.valueOf(PERSON_ID)),
                any(Date.class),
                eq("SPENDABLE")
        )).thenReturn(List.of(Map.of(
                "IS_ACTIVE", false,
                "SERVICES", new Integer[]{725},
                "ID", 123,
                "EXTERNAL_ID", "externalId"
        )));

        offerCreationProcessor.processPayload(PAYLOAD);

        //в баланс ходим, т.к. нам не подходит существующий договор
        verify(balance2).CreateOffer(any(), any());
        featureProperties.setEnableCheckExistingOffer(false);
    }

    @Test
    @DisplayName("Тест на успешное создание договора, потому что существующий - не на 725 сервис")
    @DatabaseSetup(
            value = "/data/service/balance/createoffer/before/contract_signed_since_is_not_null.xml",
            type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
            value = "/data/service/balance/createoffer/after/success_with_existing_offer_is_wrong.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testCreateOfferWithExistingButIsNotPickupPointService() throws XmlRpcException {
        featureProperties.setEnableCheckExistingOffer(true);
        when(balance2.CreateOffer(OPERATOR_UID, createOfferStructure(
                new Calendar.Builder()
                        .setDate(2021, 3, 19)
                        .setTimeZone(TimeZone.getTimeZone(DateTimeUtils.MOSCOW_ZONE))
                        .build()
                        .getTime()
        )))
                .thenReturn(createOfferResponse());
        when(balance2.GetClientContracts(
                eq(Long.valueOf(CLIENT_ID)),
                eq(Long.valueOf(PERSON_ID)),
                any(Date.class),
                eq("SPENDABLE")
        )).thenReturn(List.of(Map.of(
                "IS_ACTIVE", true,
                "SERVICES", new Integer[]{612},
                "ID", 123,
                "EXTERNAL_ID", "externalId"
        )));

        offerCreationProcessor.processPayload(PAYLOAD);

        //в баланс ходим, т.к. нам не подходит существующий договор
        verify(balance2).CreateOffer(any(), any());
        featureProperties.setEnableCheckExistingOffer(false);
    }
}
