package ru.yandex.market.logistics.logistics4shops.queue.processor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.MovementDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationPartnerExtendedInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationUnitDto;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory;
import ru.yandex.market.logistics.logistics4shops.queue.payload.TransferActPutOutboundPayload;
import ru.yandex.market.tpl.common.transferact.client.api.TransferApi;
import ru.yandex.market.tpl.common.transferact.client.model.ActorDto;
import ru.yandex.market.tpl.common.transferact.client.model.ActorTypeDto;
import ru.yandex.market.tpl.common.transferact.client.model.ItemQualifierDto;
import ru.yandex.market.tpl.common.transferact.client.model.RegistryDirectionDto;
import ru.yandex.market.tpl.common.transferact.client.model.RegistryDto;
import ru.yandex.market.tpl.common.transferact.client.model.RegistryItemDto;
import ru.yandex.market.tpl.common.transferact.client.model.RegistryItemTypeDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferCreateRequestDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferQualifierDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferQualifierTypeDto;
import ru.yandex.market.tpl.common.transferact.client.model.TwoActorQualifierDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DisplayName("Отправка фактического реестра отгрузки")
@ParametersAreNonnullByDefault
class TransferActPutOutboundProcessorTest extends AbstractIntegrationTest {

    private static final Long SHOP_PARTNER_ID = 1234L;
    private static final Long MOVEMENT_PARTNER_ID = 1235L;
    private static final String OUTBOUND_YANDEX_ID = "ya-id";
    private static final String TRANSPORTATION_ID = "345";
    private static final String EXPECTED_TRANSPORTATION_ID = "TM345";
    private static final LocalDateTime TRANSFER_LOCAL_DATE_TIME = LocalDateTime.of(2022, 4, 25, 15, 0);
    public static final String IDEMPOTENCY_KEY = "1";
    private static final Pager DEFAULT_PAGER = Pager.atPage(1, 50).setTotal(Integer.MAX_VALUE);
    private static final Long[] ORDER_IDS = {1000001L, 1000002L, 1000003L};

    @Autowired
    private TransferActPutOutboundProcessor processor;

    @Autowired
    private TransferApi transferApi;

    @Autowired
    private TransportManagerClient transportManagerClient;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Captor
    private ArgumentCaptor<TransferCreateRequestDto> captor;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(transferApi, transportManagerClient, checkouterAPI);
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Успех")
    @DatabaseSetup("/queue/processor/transfer_act_put_outbound/before/success.xml")
    void success(
        String prefix,
        Long movementPartnerId
    ) {
        mockCheckouterOrderBoxes();
        when(transportManagerClient.getTransportation(Long.valueOf(TRANSPORTATION_ID))).thenReturn(
            Optional.of(createTransportation(movementPartnerId))
        );

        softly.assertThat(processor.execute(createPayload(prefix))).isEqualTo(TaskExecutionResult.finish());

        verify(transportManagerClient).getTransportation(Long.valueOf(TRANSPORTATION_ID));
        verify(transferApi).transferPut(captor.capture());
        verify(checkouterAPI).getOrders(
            safeRefEq(checkouterFactory.systemUserInfo()),
            safeRefEq(OrderSearchRequest.builder()
                .withOrderIds(ORDER_IDS)
                .withPageInfo(DEFAULT_PAGER)
                .withRgbs(Color.BLUE, Color.WHITE)
                .build()
            )
        );

        var result = captor.getValue();
        softly.assertThat(result).usingRecursiveComparison().isEqualTo(expectedResult());
    }

    @Nonnull
    private static Stream<Arguments> success() {
        return Stream.of(
            Arguments.of("", SHOP_PARTNER_ID),
            Arguments.of("TM", SHOP_PARTNER_ID),
            Arguments.of("", MOVEMENT_PARTNER_ID),
            Arguments.of("TM", MOVEMENT_PARTNER_ID)
        );
    }

    @Test
    @DisplayName("Неудача: некорректный идентификатор отгрузки")
    @DatabaseSetup("/queue/processor/transfer_act_put_outbound/before/success.xml")
    void failIncorrectTransportationId() {
        when(transportManagerClient.getTransportation(Long.valueOf(TRANSPORTATION_ID))).thenReturn(
            Optional.of(createTransportation(4321L))
        );

        softly.assertThatThrownBy(() -> processor.execute(createPayload("FAKE_TM_PREFIX")))
            .hasMessage("Incorrect transportation id");
    }

    @Test
    @DisplayName("Неудача: отгрузка не найдена в ТМ")
    @DatabaseSetup("/queue/processor/transfer_act_put_outbound/before/success.xml")
    void failNotFoundTransportation() {
        softly.assertThatThrownBy(() -> processor.execute(createPayload()))
            .hasMessage("Failed to find [SHIPMENT] with id [345]");

        verify(transportManagerClient).getTransportation(Long.valueOf(TRANSPORTATION_ID));
    }

    @Nonnull
    private TransferActPutOutboundPayload createPayload() {
        return createPayload("TM");
    }

    @Nonnull
    private TransferActPutOutboundPayload createPayload(String transportationIdPrefix) {
        return TransferActPutOutboundPayload.builder()
            .transportationId(transportationIdPrefix + TRANSPORTATION_ID)
            .idempotencyKey(IDEMPOTENCY_KEY)
            .build();
    }

    @Nonnull
    private TransportationDto createTransportation(Long movementPartnerId) {
        var transportation = new TransportationDto();
        transportation.setId(Long.valueOf(TRANSPORTATION_ID));
        transportation.setOutbound(
            TransportationUnitDto.builder()
                .yandexId(OUTBOUND_YANDEX_ID)
                .plannedIntervalEnd(TRANSFER_LOCAL_DATE_TIME)
                .partner(
                    TransportationPartnerExtendedInfoDto.builder()
                        .id(SHOP_PARTNER_ID)
                        .name("outboundPartnerName")
                        .legalName("outboundPartnerCompanyName")
                        .build()
                )
                .build()
        );

        transportation.setMovement(
            MovementDto.builder()
                .partner(
                    TransportationPartnerExtendedInfoDto.builder()
                        .id(movementPartnerId)
                        .name("movementPartnerName")
                        .legalName("movementPartnerCompanyName")
                        .build()
                )
                .build()
        );

        transportation.setInbound(
            TransportationUnitDto.builder()
                .partner(
                    TransportationPartnerExtendedInfoDto.builder()
                        .id(1236L)
                        .name("inboundPartnerName")
                        .legalName("inboundPartnerCompanyName")
                        .build()
                )
                .build()
        );
        return transportation;
    }

    @Nonnull
    private TransferCreateRequestDto expectedResult() {
        var result = new TransferCreateRequestDto();

        result.setIdempotencyKey(IDEMPOTENCY_KEY);

        var registryDto = new RegistryDto();
        registryDto.setDirection(RegistryDirectionDto.PROVIDER);
        registryDto.setItems(createRegistryItems());

        registryDto.setSkippedItems(Collections.emptyList());

        var transferQualifierDto = new TransferQualifierDto();
        transferQualifierDto.setType(TransferQualifierTypeDto.TWO_ACTOR);

        var twoActorQualifierDto = new TwoActorQualifierDto();

        twoActorQualifierDto.setLocalDate(TRANSFER_LOCAL_DATE_TIME.toLocalDate());
        transferQualifierDto.setTwoActorQualifier(twoActorQualifierDto);

        var actor = new ActorDto();
        actor.setType(ActorTypeDto.MARKET_SHOP);
        actor.setName("outboundPartnerName");
        actor.setExternalId(SHOP_PARTNER_ID.toString());
        actor.setCompanyName("outboundPartnerCompanyName");
        twoActorQualifierDto.setActorFrom(actor);
        twoActorQualifierDto.setActorTo(actor);

        result.setRegistry(registryDto);
        result.setTransferQualifier(transferQualifierDto);
        result.setTransportationId(EXPECTED_TRANSPORTATION_ID);
        result.setAutoSign(true);

        return result;
    }

    @Nonnull
    private List<RegistryItemDto> createRegistryItems() {
        return List.of(
            createPlaceItemDto("1000001", "box11-1"),
            createPlaceItemDto("1000002", "box1-1"),
            createPlaceItemDto("1000002", "box2-1")
        );
    }

    private void mockCheckouterOrderBoxes() {
        Pager pager = Pager.atPage(1, 50).setTotal(2);
        var firstOrder = CheckouterFactory.createOrder(1000001L, List.of("box11-1"));
        var secondOrder = CheckouterFactory.createOrder(1000002L, List.of("box1-1", "box2-1"));
        when(checkouterAPI.getOrders(
            any(RequestClientInfo.class),
            any(OrderSearchRequest.class)
        )).thenReturn(new PagedOrders(List.of(firstOrder, secondOrder), pager));
    }

    @Nonnull
    private RegistryItemDto createPlaceItemDto(String orderExternalId, String placeId) {
        var itemQualifier = new ItemQualifierDto();
        itemQualifier.setType(RegistryItemTypeDto.PLACE);
        itemQualifier.setExternalId(orderExternalId);
        itemQualifier.setPlaceId(placeId);

        var item = new RegistryItemDto();
        item.setItemQualifier(itemQualifier);
        item.setPlaceCount(1);
        return item;
    }

}
