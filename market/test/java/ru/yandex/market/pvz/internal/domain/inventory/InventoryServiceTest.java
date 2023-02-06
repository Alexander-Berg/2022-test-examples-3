package ru.yandex.market.pvz.internal.domain.inventory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.dbqueue.finish_inventory.FinishInventoryProducer;
import ru.yandex.market.pvz.core.domain.inventory.InventoryCommandService;
import ru.yandex.market.pvz.core.domain.inventory.InventoryFinishedException;
import ru.yandex.market.pvz.core.domain.inventory.InventoryItemQueryService;
import ru.yandex.market.pvz.core.domain.inventory.InventoryQueryService;
import ru.yandex.market.pvz.core.domain.inventory.mapper.OrderWithPlacesParamsMapper;
import ru.yandex.market.pvz.core.domain.inventory.model.Inventory;
import ru.yandex.market.pvz.core.domain.inventory.model.InventoryItemType;
import ru.yandex.market.pvz.core.domain.inventory.model.InventoryStatus;
import ru.yandex.market.pvz.core.domain.inventory.params.InventoryParams;
import ru.yandex.market.pvz.core.domain.inventory.repository.InventoryRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.place.OrderPlace;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryFlow;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryScanType;
import ru.yandex.market.pvz.core.domain.order_delivery_result.params.OrderDeliveryResultParams;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.domain.returns.ReturnRequestCommandService;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequestParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.pi.inventory.dto.InventoryDto;
import ru.yandex.market.pvz.internal.controller.pi.inventory.dto.InventoryItemDto;
import ru.yandex.market.pvz.internal.controller.pi.inventory.dto.InventoryItemPlaceCodeDto;
import ru.yandex.market.pvz.internal.controller.pi.inventory.dto.InventoryPlaceDto;
import ru.yandex.market.pvz.internal.controller.pi.inventory.mapper.InventoryItemDtoMapper;
import ru.yandex.market.pvz.internal.controller.pi.inventory.mapper.InventoryPageDtoMapper;
import ru.yandex.market.pvz.internal.controller.pi.inventory.mapper.InventoryPlaceDtoMapper;
import ru.yandex.market.sc.internal.client.ScLogisticsClient;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.common.util.logging.Tracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class InventoryServiceTest {

    private static final Long UID = 1L;
    private static final String LOGIN = "login";

    private final TestPickupPointFactory pickupPointFactory;
    private final TestableClock clock;
    private final InventoryRepository inventoryRepository;
    private final PickupPointCommandService pickupPointCommandService;
    private final TestReturnRequestFactory returnRequestFactory;
    private final ReturnRequestCommandService returnRequestCommandService;
    private final TestOrderFactory orderFactory;
    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;
    private final OrderDeliveryResultQueryService orderDeliveryResultQueryService;

    private final InventoryQueryService inventoryQueryService;
    private final PickupPointQueryService pickupPointQueryService;
    private final InventoryItemDtoMapper inventoryItemDtoMapper;
    private final InventoryPlaceDtoMapper inventoryPlaceDtoMapper;
    private final InventoryPageDtoMapper inventoryPageDtoMapper;
    private final OrderWithPlacesParamsMapper orderWithPlacesParamsMapper;
    private final FinishInventoryProducer finishInventoryProducer;
    private final InventoryItemQueryService inventoryItemQueryService;
    private final InventoryReportConverterService inventoryReportConverterService;

    @Mock
    private ScLogisticsClient scLogisticsClient;

    @SpyBean
    private InventoryCommandService inventoryCommandService;

    private InventoryService inventoryService;

    @BeforeEach
    void setup() {
        Tracer.putUidToStatic(UID);
        Tracer.putLoginToStatic(LOGIN);
        inventoryService = new InventoryService(
                inventoryCommandService, inventoryQueryService, pickupPointQueryService,
                inventoryItemDtoMapper, inventoryPlaceDtoMapper, inventoryPageDtoMapper, orderWithPlacesParamsMapper,
                scLogisticsClient, finishInventoryProducer, inventoryItemQueryService,  inventoryReportConverterService
        );
    }

    @Test
    void whenCreateInventoryTwiceThenError() {
        Instant now = Instant.parse("2022-01-17T15:01:00Z");
        clock.setFixed(now, ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );

        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), UID,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod()
        );
        InventoryDto inventoryDto = inventoryService.create(pickupPointRequestData).getInventories().get(0);
        clock.setFixed(
                now.plusSeconds(inventoryQueryService.getInventoryDurationInSeconds() + 10), ZoneOffset.ofHours(3)
        );

        doReturn(true).when(inventoryCommandService).isInventoryTimeCompleted(any());
        assertThatThrownBy(() -> inventoryService.create(pickupPointRequestData))
                .isExactlyInstanceOf(InventoryFinishedException.class);

        Inventory inventory = inventoryRepository.findByIdOrThrow(inventoryDto.getId());
        assertThat(inventory.getStatus()).isEqualTo(InventoryStatus.FINISHED);
    }

    @Test
    void whenTryToAddUnknownBarcodeThenSuccess() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        long lmsId = 123L;
        pickupPointCommandService.updateLmsId(pickupPoint.getId(), lmsId);

        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), UID,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod()
        );
        String barcode = "123";
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                .when(scLogisticsClient).findInventoryItemByBarcode(lmsId, List.of(barcode));

        InventoryItemDto output = inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(barcode).build(), inventory.getId()
        );
        InventoryItemDto expected = InventoryItemDto.builder()
                .externalId(barcode)
                .type(InventoryItemType.UNKNOWN)
                .placeCodes(List.of())
                .build();

        assertThat(output).isEqualToIgnoringGivenFields(expected, "id", "updatedAt");
    }

    @Test
    void whenTryToAddReturnsBarcodeThenSuccess() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        long lmsId = 123L;
        pickupPointCommandService.updateLmsId(pickupPoint.getId(), lmsId);

        ReturnRequestParams returnRequest = returnRequestFactory.createReturnRequest(
                TestReturnRequestFactory.CreateReturnRequestBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        returnRequestCommandService.receive(returnRequest.getReturnId(), returnRequest.getPickupPointId());

        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                .when(scLogisticsClient).findInventoryItemByBarcode(lmsId, List.of(returnRequest.getBarcode()));

        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = buildPickupPointRequestData(pickupPoint);

        InventoryItemDto output = inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(returnRequest.getBarcode()).build(),
                inventory.getId()
        );
        InventoryItemDto expected = InventoryItemDto.builder()
                .externalId(returnRequest.getOrderId())
                .type(InventoryItemType.PICKUP_POINT)
                .placeCodes(List.of(buildPlaceCodeDto(returnRequest.getBarcode(), true)))
                .build();

        assertThat(output)
                .usingRecursiveComparison()
                .ignoringFields("id", "updatedAt", "placeCodes")
                .isEqualTo(expected);
        assertThat(output.getPlaceCodes())
                .usingElementComparatorIgnoringFields("updatedAt")
                .containsExactly(expected.getPlaceCodes().get(0));
    }

    private PickupPointRequestData buildPickupPointRequestData(PickupPoint pickupPoint) {
        return new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), UID,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod()
        );
    }

    private InventoryItemPlaceCodeDto buildPlaceCodeDto(String placeCode, boolean scanned) {
        return InventoryItemPlaceCodeDto.builder()
                .placeCode(placeCode)
                .scanned(scanned)
                .build();
    }

    @Test
    void whenTryToAddOrderByExternalIdWithoutPlacesThenSuccess() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        Order order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .params(
                                TestOrderFactory.OrderParams.builder()
                                        .places(List.of())
                                        .build()
                        )
                        .build()
        );

        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = buildPickupPointRequestData(pickupPoint);
        InventoryItemDto output = inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(order.getExternalId()).build(),
                inventory.getId()
        );
        InventoryItemDto expected = InventoryItemDto.builder()
                .externalId(order.getExternalId())
                .type(InventoryItemType.PICKUP_POINT)
                .placeCodes(List.of())
                .build();

        assertThat(output).isEqualToIgnoringGivenFields(expected, "id", "updatedAt");
    }

    @Test
    void whenTryToAddOrderByPlaceWithPlacesThenSuccess() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        Order order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = buildPickupPointRequestData(pickupPoint);

        List<String> barcodes = Lists.transform(order.getPlaces(), OrderPlace::getBarcode);
        InventoryItemDto output = inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(barcodes.get(0)).build(),
                inventory.getId()
        );
        InventoryItemDto expected = InventoryItemDto.builder()
                .externalId(order.getExternalId())
                .type(InventoryItemType.PICKUP_POINT)
                .placeCodes(List.of(
                        buildPlaceCodeDto(barcodes.get(0), true),
                        buildPlaceCodeDto(barcodes.get(1), false),
                        buildPlaceCodeDto(barcodes.get(2), false)
                ))
                .build();

        assertThat(output)
                .usingRecursiveComparison()
                .ignoringFields("id", "updatedAt", "placeCodes")
                .isEqualTo(expected);
        assertThat(output.getPlaceCodes().get(0))
                .isEqualToIgnoringGivenFields(expected.getPlaceCodes().get(0), "updatedAt");
        assertThat(output.getPlaceCodes())
                .usingElementComparatorIgnoringFields("updatedAt")
                .containsExactlyInAnyOrder(
                        expected.getPlaceCodes().get(0), expected.getPlaceCodes().get(1),
                        expected.getPlaceCodes().get(2)
                );
    }

    @Test
    void whenTryToAddOrderFromAnotherPickupPointThenSuccess() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        long lmsId = 123L;
        pickupPointCommandService.updateLmsId(pickupPoint.getId(), lmsId);
        Order order = orderFactory.createOrder();
        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = buildPickupPointRequestData(pickupPoint);

        List<String> barcodes = Lists.transform(order.getPlaces(), OrderPlace::getBarcode);
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                .when(scLogisticsClient).findInventoryItemByBarcode(lmsId, List.of(barcodes.get(0)));
        InventoryItemDto output = inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(barcodes.get(0)).build(),
                inventory.getId()
        );
        InventoryItemDto expected = InventoryItemDto.builder()
                .externalId(barcodes.get(0))
                .type(InventoryItemType.UNKNOWN)
                .placeCodes(List.of())
                .build();

        assertThat(output).isEqualToIgnoringGivenFields(expected, "id", "updatedAt");
    }

    @Test
    void whenTryToAddOrderByExternalIdWithPlacesThenError() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        Order order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = buildPickupPointRequestData(pickupPoint);

        assertThatThrownBy(() -> inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(order.getExternalId()).build(),
                inventory.getId()
        ))
                .isExactlyInstanceOf(TplIllegalArgumentException.class);
    }

    @Test
    void whenTryToAddItemToFinishedInventoryThenError() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        Order order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        inventoryCommandService.finish(pickupPoint.getId(), pickupPoint.getTimeOffset(), inventory.getId());
        PickupPointRequestData pickupPointRequestData = buildPickupPointRequestData(pickupPoint);

        assertThatThrownBy(() -> inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(order.getExternalId()).build(),
                inventory.getId()
        ))
                .isExactlyInstanceOf(InventoryFinishedException.class);
    }

    @Test
    void whenTryToAddOrderBySecondPlaceThenSuccess() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        Order order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = buildPickupPointRequestData(pickupPoint);

        List<String> barcodes = Lists.transform(order.getPlaces(), OrderPlace::getBarcode);
        inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(barcodes.get(0)).build(),
                inventory.getId()
        );
        InventoryItemDto output = inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(barcodes.get(2)).build(),
                inventory.getId()
        );
        InventoryItemDto expected = InventoryItemDto.builder()
                .externalId(order.getExternalId())
                .type(InventoryItemType.PICKUP_POINT)
                .placeCodes(List.of(
                        buildPlaceCodeDto(barcodes.get(2), true),
                        buildPlaceCodeDto(barcodes.get(0), true),
                        buildPlaceCodeDto(barcodes.get(1), false)
                ))
                .build();

        assertThat(output)
                .usingRecursiveComparison()
                .ignoringFields("id", "updatedAt", "placeCodes")
                .isEqualTo(expected);
        assertThat(output.getPlaceCodes())
                .usingElementComparatorIgnoringFields("updatedAt")
                .containsExactly(
                        expected.getPlaceCodes().get(0), expected.getPlaceCodes().get(1),
                        expected.getPlaceCodes().get(2)
                );
    }

    @Test
    void whenTryToAddItemByExternalIdAfterPlaceThenError() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        Order order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = buildPickupPointRequestData(pickupPoint);

        List<String> barcodes = Lists.transform(order.getPlaces(), OrderPlace::getBarcode);
        inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(barcodes.get(0)).build(),
                inventory.getId()
        );

        assertThatThrownBy(() -> inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(order.getExternalId()).build(),
                inventory.getId()
        ))
                .isExactlyInstanceOf(TplIllegalArgumentException.class);
    }

    @Test
    void whenTryToAddOrderByExternalIdWithoutPlacesTwiceThenSuccess() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        Order order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .params(
                                TestOrderFactory.OrderParams.builder()
                                        .places(List.of())
                                        .build()
                        )
                        .build()
        );

        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = buildPickupPointRequestData(pickupPoint);
        inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(order.getExternalId()).build(),
                inventory.getId()
        );
        InventoryItemDto output = inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(order.getExternalId()).build(),
                inventory.getId()
        );
        InventoryItemDto expected = InventoryItemDto.builder()
                .externalId(order.getExternalId())
                .type(InventoryItemType.PICKUP_POINT)
                .placeCodes(List.of())
                .build();

        assertThat(output).isEqualToIgnoringGivenFields(expected, "id", "updatedAt");
    }

    @Test
    void whenTryToAddOrderByExternalIdWithOnePlaceThenSuccess() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        String barcode = "P001248514";
        Order order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .params(
                                TestOrderFactory.OrderParams.builder()
                                        .places(List.of(
                                                TestOrderFactory.OrderPlaceParams.builder()
                                                        .barcode(barcode)
                                                        .build()
                                        ))
                                        .build()
                        )
                        .build()
        );

        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = buildPickupPointRequestData(pickupPoint);
        InventoryItemDto output = inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(order.getExternalId()).build(),
                inventory.getId()
        );
        InventoryItemDto expected = InventoryItemDto.builder()
                .externalId(order.getExternalId())
                .type(InventoryItemType.PICKUP_POINT)
                .placeCodes(List.of(buildPlaceCodeDto(barcode, true)))
                .build();

        assertThat(output)
                .usingRecursiveComparison()
                .ignoringFields("id", "updatedAt", "placeCodes")
                .isEqualTo(expected);
        assertThat(output.getPlaceCodes())
                .usingElementComparatorIgnoringFields("updatedAt")
                .containsExactly(expected.getPlaceCodes().get(0));
    }

    @Test
    void whenTryToAddOrderWithEqualExternalIdAndPlaceThenSuccess() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        String barcode = "P001248514";
        String barcode2 = "P0012485141";
        Order order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .params(
                                TestOrderFactory.OrderParams.builder()
                                        .externalId(barcode)
                                        .places(List.of(
                                                TestOrderFactory.OrderPlaceParams.builder()
                                                        .barcode(barcode)
                                                        .build(),
                                                TestOrderFactory.OrderPlaceParams.builder()
                                                        .barcode(barcode2)
                                                        .build()
                                        ))
                                        .build()
                        )
                        .build()
        );

        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = buildPickupPointRequestData(pickupPoint);
        InventoryItemDto output = inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(barcode).build(),
                inventory.getId()
        );
        InventoryItemDto expected = InventoryItemDto.builder()
                .externalId(order.getExternalId())
                .type(InventoryItemType.PICKUP_POINT)
                .placeCodes(List.of(
                        buildPlaceCodeDto(barcode, true),
                        buildPlaceCodeDto(barcode2, false)
                ))
                .build();

        assertThat(output)
                .usingRecursiveComparison()
                .ignoringFields("id", "updatedAt", "placeCodes")
                .isEqualTo(expected);
        assertThat(output.getPlaceCodes())
                .usingElementComparatorIgnoringFields("updatedAt")
                .containsExactly(expected.getPlaceCodes().get(0), expected.getPlaceCodes().get(1));
    }

    @Test
    void whenScanFashionSafePackage() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        long lmsId = 123L;
        pickupPointCommandService.updateLmsId(pickupPoint.getId(), lmsId);
        Order order = orderFactory.createSimpleFashionOrder(false, pickupPoint);
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());

        OrderDeliveryResultParams deliveryResult = orderDeliveryResultQueryService.get(order.getId());
        long itemInstanceId1 = deliveryResult.getItems().get(0).getItemInstanceId();
        long itemInstanceId2 = deliveryResult.getItems().get(2).getItemInstanceId();
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), null, itemInstanceId1,
                ItemDeliveryFlow.RETURN, ItemDeliveryScanType.SCAN, null);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), null, itemInstanceId2,
                ItemDeliveryFlow.RETURN, ItemDeliveryScanType.MANUAL, null);

        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderDeliveryResultCommandService.pay(order.getId());

        String barcode1 = "123456";
        String barcode2 = "654321";
        orderDeliveryResultCommandService.packageReturn(order.getId(), List.of(barcode1, barcode2));

        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = buildPickupPointRequestData(pickupPoint);
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                .when(scLogisticsClient).findInventoryItemByBarcode(lmsId, List.of(barcode1));
        InventoryItemDto output = inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(barcode1).build(), inventory.getId()
        );
        InventoryItemDto expected = InventoryItemDto.builder()
                .externalId(order.getExternalId())
                .type(InventoryItemType.PICKUP_POINT)
                .placeCodes(List.of(
                        buildPlaceCodeDto(barcode1, true),
                        buildPlaceCodeDto(barcode2, false)
                ))
                .build();

        assertThat(output)
                .usingRecursiveComparison()
                .ignoringFields("id", "updatedAt", "placeCodes")
                .isEqualTo(expected);
        assertThat(output.getPlaceCodes())
                .usingElementComparatorIgnoringFields("updatedAt")
                .containsExactly(expected.getPlaceCodes().get(0), expected.getPlaceCodes().get(1));
    }


    @Test
    void whenTryToAddDropOffOrderByPlaceWithPlacesThenSuccess() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        long lmsId = 123L;
        pickupPointCommandService.updateLmsId(pickupPoint.getId(), lmsId);

        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = buildPickupPointRequestData(pickupPoint);

        String barcode1 = "123456";
        String barcode2 = "654321";
        String externalId = "987654";

        var item = ru.yandex.market.sc.internal.model.InventoryItemDto.builder()
                .externalId(externalId)
                .placeCodes(List.of(barcode1, barcode2))
                .build();
        when(scLogisticsClient.findInventoryItemByBarcode(lmsId, List.of(barcode1))).thenReturn(item);

        InventoryItemDto output = inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(barcode1).build(),
                inventory.getId()
        );
        InventoryItemDto expected = InventoryItemDto.builder()
                .externalId(externalId)
                .type(InventoryItemType.DROP_OFF)
                .placeCodes(List.of(
                        buildPlaceCodeDto(barcode1, true),
                        buildPlaceCodeDto(barcode2, false)
                ))
                .build();

        assertThat(output)
                .usingRecursiveComparison()
                .ignoringFields("id", "updatedAt", "placeCodes")
                .isEqualTo(expected);
        assertThat(output.getPlaceCodes().get(0))
                .isEqualToIgnoringGivenFields(expected.getPlaceCodes().get(0), "updatedAt");
        assertThat(output.getPlaceCodes())
                .usingElementComparatorIgnoringFields("updatedAt")
                .containsExactly(
                        expected.getPlaceCodes().get(0), expected.getPlaceCodes().get(1)
                );
    }

    @Test
    void whenTryToAddNotUniqueDropOffOrderByPlaceThenSuccess() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        long lmsId = 123L;
        pickupPointCommandService.updateLmsId(pickupPoint.getId(), lmsId);

        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = buildPickupPointRequestData(pickupPoint);

        String barcode = "123456";

        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                .when(scLogisticsClient).findInventoryItemByBarcode(lmsId, List.of(barcode));

        InventoryItemDto output = inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(barcode).build(),
                inventory.getId()
        );
        InventoryItemDto expected = InventoryItemDto.builder()
                .externalId(barcode)
                .type(InventoryItemType.UNKNOWN)
                .placeCodes(List.of())
                .build();

        assertThat(output).isEqualToIgnoringGivenFields(expected, "id", "updatedAt");
    }

}
