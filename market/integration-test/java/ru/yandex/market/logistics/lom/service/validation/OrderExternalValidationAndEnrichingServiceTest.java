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

@DisplayName("?????????????????? ?? ???????????????????? ????????????")
class OrderExternalValidationAndEnrichingServiceTest extends AbstractOrderExternalValidationAndEnrichingServiceTest {
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("?????????????????? ?????????????????????????????? ????????????")
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
    @DisplayName("?????????????????? ???????????? ?? ???????????????????????? ????????????????")
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
    @DisplayName("?????????????????????????? ????????????")
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
    @DisplayName("?????????????????????????? ???????????? ?????????? ?????????????????? ????????????????????")
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
    @DisplayName("?????????????????? ?????????????????? ?????????????????? ???????????? - ?????? ??????????????????")
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
    @DisplayName("?????????????? ?????????????????? ?? ???????????????????? ????????????")
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
    @DisplayName("?????????????? ?????????????????? ?? ???????????????????? ???????????? ?????? marketId - ???????????? ???????? ?? ????????")
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
    @DisplayName("?????????????? ?????????????????? ?? ???????????????????? ???????????? ?????? marketId - ???????????????? ???????????? ???????? ?? ????????")
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
    @DisplayName("?????????????? ?????????????????? ?? ???????????????????? ???????????? ?????? marketId - ???????????? ?? ???????? ??????")
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
    @DisplayName("?????????????? ?????????????????? ?? ???????????????????? ???????????? ?????? marketId - ???????????? ?? ???????? ?????? ?? Mbi ???? ????????????????")
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
    @DisplayName("?????????????? ?????????????????? ?? ???????????????????? ???????????? ?????? marketId - ???????????? ?? ???????? ?????? ?? Mbi ???? ?????????? ????????????")
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
    @DisplayName("?????????????? ?????????????????? ?? ???????????????????? ???????????? ?????? marketId - marketId ????????????????????")
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
    @DisplayName("?????????????? ?????????????????? ?? ???????????????????? ???????????? ?????? marketId - ?? marketId ?????? ????????????")
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
    @DisplayName("?????????????? ?????????????????? ?? ???????????????????? ???????????? - ?????????????? FF")
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
    @DisplayName("?????????????? ?????????????????? ?? ???????????????????? ?????????????????? ????????????")
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
    @DisplayName("???? ???????????? pickup_point_id ?????? ?????????????????? ????????????")
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
    @DisplayName("?????????????????? ?????????????????? ????-???? ???????????????????? ????????????????????")
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
    @DisplayName("?????????????????? ?????????????????? ????-???? ???????????????????? ?????????????????? ????????????????")
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
    @DisplayName("?????????????????? ?????????????????? ????-???? ???????????????????? ?????????????????????? ????????????")
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
    @DisplayName("?????????????????? ?????????????????? ????-???? ?????????????????????? ?????????????????? ????????????????")
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
    @DisplayName("?????????????? ?????????????????? ???????????? ?? ???????????????????? ????????????????")
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
    @DisplayName("???????????????? ?????????????????? ???????????? ?? ???????????????? LegalForm")
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
    @DisplayName("???????????????? ?????????????????? ???????????? ?? ?????????????????????????? LegalForm (?????????? ?????? ????????)")
    @DatabaseSetup("/service/externalvalidation/before/validating_success_order.xml")
    @ExpectedDatabase(
        value = "/service/externalvalidation/after/success_validating_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void validateCyrillicLegalForm() {
        mockLmsClientFully();
        mockPartnersGetCredentials();
        mockGetDeliveryInterval(13);

        String cyrillicLLCName = "??????";
        softly.assertThat(cyrillicLLCName).matches(Pattern.compile("[??-??]+"));

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
    @DisplayName("?????????????????? ?????????????????? ???????????? ?? ???????????? LegalInfo ?????? ???????????????? ??????????????????????")
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
    @DisplayName("???????????????????? ?????????? ???????????????????? ?? ????????????????")
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
    @DisplayName("???????????????????? ?????????????? ???? ?????????????????? ?? ???????????????? - ???????????????? ?????????????? ?? ?????? ???????????????? partnerType")
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
        "Location ?????????????????????? ???????????? ?????? ???????????????? " +
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
    @DisplayName("???????????????? ?????????????????? ???????? ???????????? ???? ????????????????????????, ???????? ???? ??????????????????")
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
    @DisplayName("???????? ?????? ??????????????????, ?????? ???????????????????? ?? ???????????????????????? ?????????????????????? ??????????????????")
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
    @DisplayName("?????????????? ?????????????????? ???????????? ?????? ???????????????????? partnerId ?? ????????????, ???? ?????????????? marketId ?? ????????")
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
    @DisplayName("?????????????????? ?????????????????? ???????????? ?????? ???????????????????? marketId ?? ????????????")
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
    @DisplayName("?????????????? ?????????????????? ?? ???????????????????? express ????????????")
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
                    "???????????????????? ??????????",
                    o -> {
                    },
                    Set.of(),
                    Set.of("No recipient coordinates", "Express delivery interval too wide")
                ),
                Quadruple.of(
                    "?????? longitude",
                    o -> o.getRecipient().getAddress().setLongitude(null),
                    Set.of("No recipient coordinates: lat 55.018803, lon null"),
                    Set.of("Express delivery interval too wide")
                ),
                Quadruple.of(
                    "?????? latitude",
                    o -> o.getRecipient().getAddress().setLatitude(null),
                    Set.of("No recipient coordinates: lat null, lon 82.933952"),
                    Set.of("Express delivery interval too wide")
                ),
                Quadruple.of(
                    "?????? recipient.address",
                    o -> o.getRecipient().setAddress(null),
                    Set.of("No recipient coordinates: lat null, lon null"),
                    Set.of("Express delivery interval too wide")
                ),
                Quadruple.of(
                    "?????? recipient",
                    o -> o.setRecipient(null),
                    Set.of("No recipient coordinates: lat null, lon null"),
                    Set.of("Express delivery interval too wide")
                ),
                Quadruple.of(
                    "?????????????? ???????????????? ????????????????, 3.5 ????????",
                    o -> o.getDeliveryInterval().setEndTime(LocalTime.of(17, 30)),
                    Set.of("Express delivery interval too wide: 14:00 - 17:30"),
                    Set.of("No recipient coordinates")
                ),
                Quadruple.of(
                    "?????????????? ???????????????? ?????? ???????????????? ??????????????????, ???? ???????????????????? ?????? ?????????????????? ?? ?????????????? ????????",
                    o -> {
                        o.getWaybill().get(0).addTag(WaybillSegmentTag.EXPRESS_BATCH);
                        o.getDeliveryInterval().setEndTime(LocalTime.of(17, 30));
                    },
                    Set.of(),
                    Set.of("No recipient coordinates", "Express delivery interval too wide")
                ),
                Quadruple.of(
                    "?????????????? ???????????????? ?????? ???????????????? ???????????? ?? ?????????????? ????????",
                    o -> {
                        o.getWaybill().get(0).addTag(WaybillSegmentTag.EXPRESS_BATCH);
                        o.getDeliveryInterval().setEndTime(LocalTime.of(18, 30));
                    },
                    Set.of("Express delivery interval too wide: 14:00 - 18:30"),
                    Set.of("No recipient coordinates")
                ),
                Quadruple.of(
                    "?????????????? ???????????????? ????????????????, 2,5 ????????",
                    o -> o.getDeliveryInterval().setEndTime(LocalTime.of(16, 30)),
                    Set.of(),
                    Set.of("No recipient coordinates", "Express delivery interval too wide")
                ),
                Quadruple.of(
                    "?????? deliveryInterval.endTime",
                    o -> o.getDeliveryInterval().setEndTime(null),
                    Set.of("Express delivery interval too wide: 14:00 - null"),
                    Set.of("No recipient coordinates")
                ),
                Quadruple.of(
                    "?????? deliveryInterval.startTime",
                    o -> o.getDeliveryInterval().setStartTime(null),
                    Set.of("Express delivery interval too wide: null - 15:59"),
                    Set.of("No recipient coordinates")
                ),
                Quadruple.of(
                    "?????? deliveryInterval",
                    o -> o.setDeliveryInterval(null),
                    Set.of("Express delivery interval too wide: null - null"),
                    Set.of("No recipient coordinates")
                ),
                Quadruple.of(
                    "?????????????? ???????????????? ?? ?????????????????????? ????????????????????",
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
    @DisplayName("?????????????? ?????????????????? ?? ???????????????????? ???????????? (update instances enabled)")
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
    @DisplayName("?????????????? ?????????????????? ?? ???????????????????? ???????????? (canUpdateShipmentDate enabled)")
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
    @DisplayName("?????????????? ?????????????????? ?? ???????????????????? ???????????? (dropoff ?????????????? ????????????????)")
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
