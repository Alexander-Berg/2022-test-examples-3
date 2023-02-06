package ru.yandex.market.logistics.lom.service.validation;

import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.collections.Quadruple;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.embedded.Sender;
import ru.yandex.market.logistics.lom.entity.enums.CampaignType;
import ru.yandex.market.logistics.lom.entity.enums.OrderStatus;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.utils.MarketIdFactory;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Валидация и обогащение заказа")
class OrderExternalValidationAndEnrichingServiceTest extends AbstractOrderExternalValidationAndEnrichingServiceTest {
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Валидация несуществующего заказа")
    @DatabaseSetup("/service/externalvalidation/before/validating_order_with_wrong_order_id.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/before/validating_order_with_wrong_order_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateOrderWithWrongOrderId() {
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus.class,
        names = {"VALIDATING", "VALIDATION_ERROR"},
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Валидация заказа с неподходящим статусом")
    void validateOrderWithWrongStatus(OrderStatus orderStatus) {
        Order order = new Order()
            .setSender(new Sender().setId(-1L))
            .setPlatformClient(PlatformClient.BERU)
            .setStatus(orderStatus, clock);
        orderRepository.save(order);
        ProcessingResult result = orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);
        softly.assertThat(result.getStatus()).isEqualTo(ProcessingResultStatus.UNPROCESSED);
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    @Test
    @DisplayName("Окончательная ошибка")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/fail_final.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void finalFailure() {
        orderExternalValidationAndEnrichingService.processFinalFailure(
            VALIDATION_PAYLOAD,
            new RuntimeException("Failed after all retries")
        );
    }

    @Test
    @DisplayName("Окончательная ошибка после успешного выполнения")
    @DatabaseSetup("/service/externalvalidation/before/validated_order.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/before/validated_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void finalFailureAfterSuccess() {
        orderExternalValidationAndEnrichingService.processFinalFailure(
            VALIDATION_PAYLOAD,
            new RuntimeException("Failed after all retries")
        );

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "Validation and enriching for order 1 in final failure, but order in status: ENQUEUED\\\","
                    + "\\\"exceptionMessage\\\":\\\"RuntimeException: Failed after all retries\\\""
            );
    }

    @Test
    @DisplayName("Неудачная валидация почтового заказа - тег удаляется")
    @DatabaseSetup("/service/externalvalidation/before/validating_post_order.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/fail_validating_post_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validatePostOrderValidationFailure() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        ProcessingResult result = orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);
        softly.assertThat(result.getStatus()).isEqualTo(ProcessingResultStatus.UNPROCESSED);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(1L);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Удачная валидация и обогащение заказа")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_validating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateOrderSuccess() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockSenderGetCredentials();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Удачная валидация и обогащение заказа без marketId - данные есть в базе")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order_without_market_id.xml")
    @DatabaseSetup("/service/externalvalidation/before/partner_legal_info.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/validating_success_order_without_market_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateOrderWithoutMarketIdSuccessFromDb() {
        mockLmsClientFully();
        when(marketIdService.findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER)).thenReturn(
            Optional.of(MarketIdFactory.marketAccount(MARKET_ID_FROM, MarketIdFactory.legalInfoBuilder().build()))
        );
        mockPartnersGetCredentials();
        mockSenderGetCredentials();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService).findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER);
        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Удачная валидация и обогащение заказа без marketId - неполные данные есть в базе")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order_without_market_id.xml")
    @DatabaseSetup("/service/externalvalidation/before/partner_legal_info_no_phone_number.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/validating_success_order_without_market_id_and_phone.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateOrderWithoutMarketIdSuccessFromDbNoPhone() {
        mockLmsClientFully();
        when(marketIdService.findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER)).thenReturn(
            Optional.of(MarketIdFactory.marketAccount(MARKET_ID_FROM, MarketIdFactory.legalInfoBuilder().build()))
        );

        when(mbiApiClient.getPartnerInfo(1L)).thenReturn(new PartnerInfoDTO(
            1L,
            null,
            ru.yandex.market.core.campaign.model.CampaignType.SUPPLIER,
            null,
            null,
            null,
            null,
            null,
            true,
            null
        ));

        mockPartnersGetCredentials();
        mockSenderGetCredentials();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService).findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER);
        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);

        verify(mbiApiClient).getPartnerInfo(1L);
    }

    @Test
    @DisplayName("Удачная валидация и обогащение заказа без marketId - данных в базе нет")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order_without_market_id.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/validating_success_order_without_market_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateOrderWithoutMarketIdSuccessFromMbiApi() {
        mockLmsClientFully();
        when(mbiApiClient.getPartnerInfo(1L)).thenReturn(new PartnerInfoDTO(
            1L,
            null,
            ru.yandex.market.core.campaign.model.CampaignType.SUPPLIER,
            null,
            null, "88005553535",
            null,
            null,
            true,
            null
        ));
        when(marketIdService.findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER)).thenReturn(
            Optional.of(MarketIdFactory.marketAccount(MARKET_ID_FROM, MarketIdFactory.legalInfoBuilder().build()))
        );
        mockPartnersGetCredentials();
        mockSenderGetCredentials();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService).findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER);
        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);

        verify(mbiApiClient).getPartnerInfo(1L);
    }

    @Test
    @DisplayName("Удачная валидация и обогащение заказа без marketId - данных в базе нет и Mbi не доступен")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order_without_market_id.xml")
    void validateOrderWithoutMarketIdMbiFailedToConnect() {
        mockLmsClientFully();
        when(mbiApiClient.getPartnerInfo(1L)).thenThrow(new RuntimeException("Mbi failed to connect"));
        mockPartnersGetCredentials();
        mockSenderGetCredentials();
        softly.assertThatThrownBy(() -> orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Mbi failed to connect");
        verify(mbiApiClient).getPartnerInfo(1L);
    }

    @Test
    @DisplayName("Удачная валидация и обогащение заказа без marketId - данных в базе нет и Mbi не нашёл данные")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order_without_market_id.xml")
    void validateOrderWithoutMarketIdMbiFailedToFindInfo() {
        mockLmsClientFully();
        when(mbiApiClient.getPartnerInfo(1L)).thenReturn(null);
        mockPartnersGetCredentials();
        mockSenderGetCredentials();
        softly.assertThatThrownBy(() -> orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [PARTNER_INFO] with id [1]");
        verify(mbiApiClient).getPartnerInfo(1L);
    }

    @Test
    @DisplayName("Удачная валидация и обогащение заказа без marketId - marketId недоступен")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order_without_market_id.xml")
    @DatabaseSetup("/service/externalvalidation/before/partner_legal_info.xml")
    void validateOrderWithoutMarketIdFailMarketIdServiceFailedToConnect() {
        mockLmsClientFully();
        when(marketIdService.findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER)).thenThrow(
            new RuntimeException("MarketId failed to connect")
        );
        mockPartnersGetCredentials();
        mockSenderGetCredentials();
        softly.assertThatThrownBy(() -> orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("MarketId failed to connect");
        verify(marketIdService).findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER);
    }

    @Test
    @DisplayName("Удачная валидация и обогащение заказа без marketId - в marketId нет данных")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order_without_market_id.xml")
    @DatabaseSetup("/service/externalvalidation/before/partner_legal_info.xml")
    void validateOrderWithoutMarketIdFailMarketIdService() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockSenderGetCredentials();
        softly.assertThatThrownBy(() -> orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [LEGAL_INFO] with id [1]");
        verify(marketIdService).findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER);
    }

    @Test
    @DisplayName("Удачная валидация и обогащение заказа - возврат FF")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order_ff_return.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_validating_order_ff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateOrderSuccessFf() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockSenderGetCredentials();

        ProcessingResult processingResult =
            orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);
        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER_FF)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER_FF)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, PICKUP_POINT_PARTNER_ID, RETURN_PARTNER_FF)));

        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService, times(2)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService, times(2)).findAccountById(RETURN_PARTNER_FF_MARKET_ID);

        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
    }

    @Test
    @DisplayName("Удачная валидация и обогащение почтового заказа")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_post_order.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_validating_post_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validatePostOrderSuccess() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockSenderGetCredentials();
        mockTarifficator();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);

        verify(tarifficatorClient).getOptionalTariff(100005L);
    }

    @Test
    @DisplayName("Не указан pickup_point_id для почтового заказа")
    @DatabaseSetup("/service/externalvalidation/before/validating_post_order_pickup_point_null.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/fail_validating_post_order_pickup_point_null.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validatePostOrderNoPickupPoint() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockSenderGetCredentials();
        mockTarifficator();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient, times(2)).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());

        verify(marketIdService, times(4)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(tarifficatorClient).getOptionalTariff(100005L);
    }

    @Test
    @DisplayName("Неудачная валидация из-за отсутствия реквизитов")
    @DatabaseSetup("/service/externalvalidation/before/validating_order_no_credentials.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/fail_validating_order_no_credentials.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCredentialsFailure() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Неудачная валидация из-за отсутствия интервала доставки")
    @DatabaseSetup("/service/externalvalidation/before/validating_order_delivery_interval.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/fail_validating_order_no_delivery_interval.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateNoDeliveryInterval() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getScheduleDay(1L);
        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Неудачная валидация из-за отсутствия возвратного склада")
    @DatabaseSetup("/service/externalvalidation/before/validating_order_no_return_warehouse.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/fail_validating_order_no_return_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateNoReturnWarehouse() {
        mockLmsClientFully();

        mockPartnersGetCredentials();
        mockSenderGetCredentials();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(NON_EXISTENT_RETURN_WAREHOUSE)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, NON_EXISTENT_RETURN_WAREHOUSE)));

        verify(marketIdService).findAccountById(MARKET_ID_FROM);
    }

    @Test
    @DisplayName("Неудачная валидация из-за невалидного интервала доставки")
    @DatabaseSetup("/service/externalvalidation/before/validating_order_delivery_interval.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/fail_validating_order_wrong_delivery_interval.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateWrongDeliveryInterval() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockGetDeliveryInterval(12);
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getScheduleDay(1L);
        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Удачная валидация заказа с интервалом доставки")
    @DatabaseSetup("/service/externalvalidation/before/validating_order_delivery_interval.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_validating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateSuccessDeliveryInterval() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockGetDeliveryInterval(13);
        mockSenderGetCredentials();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getScheduleDay(1L);
        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(RETURN_PARTNER, PARTNER_ID)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Успешная валидация заказа с неверным LegalForm")
    @DatabaseSetup("/service/externalvalidation/before/validating_order_delivery_interval.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_validating_order_wrong_legal_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateWrongLegalForm() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockGetDeliveryInterval(13);

        LegalInfo legalInfo = MarketIdFactory.legalInfoBuilder()
            .setType("OLOLOPISHPISH")
            .build();

        when(marketIdService.findAccountById(MARKET_ID_FROM))
            .thenReturn(Optional.of(MarketIdFactory.marketAccount(MARKET_ID_FROM, legalInfo)));
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Sender legal info by market id 111 is invalid");

        verify(lmsClient).getScheduleDay(1L);
        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(RETURN_PARTNER, PARTNER_ID)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Успешная валидация заказа с кириллическим LegalForm (заказ для Беру)")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_validating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCyrillicLegalForm() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockGetDeliveryInterval(13);

        String cyrillicLLCName = "ООО";
        softly.assertThat(cyrillicLLCName).matches(Pattern.compile("[А-я]+"));

        LegalInfo legalInfo = MarketIdFactory.legalInfoBuilder()
            .setType(cyrillicLLCName)
            .build();

        when(marketIdService.findAccountById(MARKET_ID_FROM))
            .thenReturn(Optional.of(MarketIdFactory.marketAccount(MARKET_ID_FROM, legalInfo)));
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);
        softly.assertThat(backLogCaptor.getResults().toString())
            .doesNotContain("Sender legal info by market id 111 is invalid");

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(RETURN_PARTNER, PARTNER_ID)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Неудачная валидация заказа с пустым LegalInfo для магазина отправителя")
    @DatabaseSetup("/service/externalvalidation/before/validating_order_delivery_interval.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/fail_validating_order_empty_legal_info.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateEmptyLegalInfo() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockGetDeliveryInterval(13);

        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getScheduleDay(1L);
        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Возвратный склад проставлен в контекст")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order.xml")
    void validateReturnWarehouseContext() {
        mockLmsClientFully();
        mockGetDeliveryInterval(13);
        mockSenderGetCredentials();

        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);
        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(RETURN_PARTNER, PARTNER_ID)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService, times(2)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Возвратный партнер не совпадает с вейбилом - появился сегмент и был обогащен partnerType")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_validating_order_pt.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateReturnPartner() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockGetDeliveryInterval(13);
        mockSenderGetCredentials();

        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName(
        "Location возвратного склада был обогащен " +
            "address, externalId, phones, worktime, instruction, incorporation, contact"
    )
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_validating_order_location.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateReturnWhLocation() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockGetDeliveryInterval(13);
        mockSenderGetCredentials();

        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Успешное получение кода тарифа от Тарификатора, если он требуется")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order_with_tariff_code.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_validating_order_tariff_code.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    void tarifficatorEnrichmentWhenNeeded() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockGetDeliveryInterval(13);
        mockSenderGetCredentials();
        mockTarifficator();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);

        verify(tarifficatorClient).getOptionalTariff(100038);
    }

    @Test
    @DisplayName("Если код требуется, его отсутствие в Тарификаторе проваливает валидацию")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order_with_tariff_code.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/fail_validating_no_tariff_code.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    void tarifficatorEnrichmentFailWhenNeededAndNotProvided() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockGetDeliveryInterval(13);
        mockSenderGetCredentials();
        when(tarifficatorClient.getTariff(100038))
            .thenThrow(new HttpTemplateException(404, "No tariff with id 100038"));
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);

        verify(tarifficatorClient).getOptionalTariff(100038);
    }

    @Test
    @DisplayName("Удачная валидация заказа при отсутствии partnerId у склада, но наличии marketId у него")
    @DatabaseSetup("/service/externalvalidation/before/validating_order_without_partner_id_and_with_market_id.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_creating_order_without_partner_id_and_with_market_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateOrderWithoutPartnerIdAndWithMarketIdInLogisticPointSuccess() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockGetDeliveryInterval(13);
        mockSenderGetCredentials();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Неудачная валидация заказа при отсутствии marketId у склада")
    @DatabaseSetup("/service/externalvalidation/before/validating_order_without_both_partner_id_and_market_id.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/fail_validating_order_without_both_partner_id_and_market_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateOrderWithoutBothPartnerIdAndMarketIdInLogisticPointFailed() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockGetDeliveryInterval(13);
        mockSenderGetCredentials();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));
        verify(lmsClient)
            .getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(POINT_ID_WITHOUT_PARTNER_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService, times(2)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(MARKET_ID_FROM);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Удачная валидация и обогащение express заказа")
    @DatabaseSetup("/service/externalvalidation/before/enrich_waybill_segment_dropship_express_setting.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/enrich_waybill_segment_dropship_express_setting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void enrichWaybillDropshipExpress(
        @SuppressWarnings("unused") String name,
        Consumer<Order> fieldsSetter,
        Set<String> logsPresent,
        Set<String> logsMissing
    ) {
        transactionTemplate.execute((ignored) -> {
            Order order = orderRepository.getById(1L);
            fieldsSetter.accept(order);
            orderRepository.save(order);
            return null;
        });
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockSenderGetCredentials();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(
            Set.of(RETURN_PARTNER, DROPSHIP_EXPRESS_PARTNER, GO_COURIER_PARTNER)
        ));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER)));

        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService, times(5)).findAccountById(PARTNER_MARKET_ID);

        assertBackLogCaptor(backLogCaptor, logsPresent, logsMissing);
    }

    @Nonnull
    private static Stream<Arguments> enrichWaybillDropshipExpress() {
        return Stream.<Quadruple<String, Consumer<Order>, Set<String>, Set<String>>>of(
                Quadruple.of(
                    "Корректный заказ",
                    o -> {
                    },
                    Set.of(),
                    Set.of("No recipient coordinates", "Express delivery interval too wide")
                ),
                Quadruple.of(
                    "Нет longitude",
                    o -> o.getRecipient().getAddress().setLongitude(null),
                    Set.of("No recipient coordinates: lat 55.018803, lon null"),
                    Set.of("Express delivery interval too wide")
                ),
                Quadruple.of(
                    "Нет latitude",
                    o -> o.getRecipient().getAddress().setLatitude(null),
                    Set.of("No recipient coordinates: lat null, lon 82.933952"),
                    Set.of("Express delivery interval too wide")
                ),
                Quadruple.of(
                    "Нет recipient.address",
                    o -> o.getRecipient().setAddress(null),
                    Set.of("No recipient coordinates: lat null, lon null"),
                    Set.of("Express delivery interval too wide")
                ),
                Quadruple.of(
                    "Нет recipient",
                    o -> o.setRecipient(null),
                    Set.of("No recipient coordinates: lat null, lon null"),
                    Set.of("Express delivery interval too wide")
                ),
                Quadruple.of(
                    "Широкий интервал доставки, 3.5 часа",
                    o -> o.getDeliveryInterval().setEndTime(LocalTime.of(17, 30)),
                    Set.of("Express delivery interval too wide: 14:00 - 17:30"),
                    Set.of("No recipient coordinates")
                ),
                Quadruple.of(
                    "Широкий интервал для обычного экспресса, но нормальный для экспресса в широкий слот",
                    o -> {
                        o.getWaybill().get(0).addTag(WaybillSegmentTag.EXPRESS_BATCH);
                        o.getDeliveryInterval().setEndTime(LocalTime.of(17, 30));
                    },
                    Set.of(),
                    Set.of("No recipient coordinates", "Express delivery interval too wide")
                ),
                Quadruple.of(
                    "Широкий интервал для экспресс заказа в широкий слот",
                    o -> {
                        o.getWaybill().get(0).addTag(WaybillSegmentTag.EXPRESS_BATCH);
                        o.getDeliveryInterval().setEndTime(LocalTime.of(18, 30));
                    },
                    Set.of("Express delivery interval too wide: 14:00 - 18:30"),
                    Set.of("No recipient coordinates")
                ),
                Quadruple.of(
                    "Широкий интервал доставки, 2,5 часа",
                    o -> o.getDeliveryInterval().setEndTime(LocalTime.of(16, 30)),
                    Set.of(),
                    Set.of("No recipient coordinates", "Express delivery interval too wide")
                ),
                Quadruple.of(
                    "Нет deliveryInterval.endTime",
                    o -> o.getDeliveryInterval().setEndTime(null),
                    Set.of("Express delivery interval too wide: 14:00 - null"),
                    Set.of("No recipient coordinates")
                ),
                Quadruple.of(
                    "Нет deliveryInterval.startTime",
                    o -> o.getDeliveryInterval().setStartTime(null),
                    Set.of("Express delivery interval too wide: null - 15:59"),
                    Set.of("No recipient coordinates")
                ),
                Quadruple.of(
                    "Нет deliveryInterval",
                    o -> o.setDeliveryInterval(null),
                    Set.of("Express delivery interval too wide: null - null"),
                    Set.of("No recipient coordinates")
                ),
                Quadruple.of(
                    "Широкий интервал и отсутствует координата",
                    o -> {
                        o.getDeliveryInterval().setStartTime(LocalTime.of(0, 0)).setEndTime(LocalTime.of(23, 59));
                        o.getRecipient().getAddress().setLongitude(null);
                    },
                    Set.of(
                        "No recipient coordinates: lat 55.018803, lon null",
                        "Express delivery interval too wide: 00:00 - 23:59"
                    ),
                    Set.of()
                )
            )
            .map(quadruple -> Arguments.of(
                quadruple.getFirst(),
                quadruple.getSecond(),
                quadruple.getThird(),
                quadruple.getFourth()
            ));
    }

    @Test
    @DisplayName("Удачная валидация и обогащение заказа (update instances enabled)")
    @DatabaseSetup("/service/externalvalidation/before/enrich_waybill_segment_update_instances_setting.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/enrich_waybill_segment_update_instances_setting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void enrichWaybillUpdateInstancesEnabledSetting() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockSenderGetCredentials();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(RETURN_PARTNER, UPDATE_INSTANCES_ENABLED_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Удачная валидация и обогащение заказа (canUpdateShipmentDate enabled)")
    @DatabaseSetup("/service/externalvalidation/before/enrich_waybill_segment_update_shipment_date_setting.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/enrich_waybill_segment_can_update_shipment_date_setting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void enrichWaybillCanUpdateShipmentDateSetting() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockSenderGetCredentials();

        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(
            RETURN_PARTNER,
            CAN_UPDATE_SHIPMENT_DATE_ENABLED_PARTNER
        )));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    @Test
    @DisplayName("Удачная валидация и обогащение заказа (dropoff партнер сегмента)")
    @DatabaseSetup("/service/externalvalidation/before/enrich_waybill_segment_is_dropoff_setting.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/enrich_waybill_segment_is_dropoff_setting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void enrichWaybillIsDropoffEnabledSetting() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockSenderGetCredentials();
        orderExternalValidationAndEnrichingService.processPayload(VALIDATION_PAYLOAD);

        verify(lmsClient).getLogisticsPoint(PICKUP_POINT_ID);
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER)));
        verify(lmsClient).searchPartners(partnerFilter(Set.of(RETURN_PARTNER, DROPOFF_PARTNER)));
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build());
        verify(lmsClient).searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER, PICKUP_POINT_PARTNER_ID)));

        verify(marketIdService).findAccountById(MARKET_ID_FROM);
        verify(marketIdService, times(3)).findAccountById(PARTNER_MARKET_ID);
        verify(marketIdService).findAccountById(RETURN_PARTNER_MARKET_ID);
    }

    private void assertBackLogCaptor(BackLogCaptor backLogCaptor, Set<String> logsPresent, Set<String> logsMissing) {
        if (CollectionUtils.isNonEmpty(logsPresent)) {
            softly.assertThat(backLogCaptor.getResults().toString()).contains(logsPresent);
        }
        if (CollectionUtils.isNonEmpty(logsMissing)) {
            softly.assertThat(backLogCaptor.getResults().toString()).doesNotContain(logsMissing);
        }
    }
}
