package ru.yandex.market.pvz.internal.controller.pi.inventory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.dbqueue.finish_inventory.FinishInventoryProducer;
import ru.yandex.market.pvz.core.domain.inventory.InventoryCommandService;
import ru.yandex.market.pvz.core.domain.inventory.InventoryItemQueryService;
import ru.yandex.market.pvz.core.domain.inventory.InventoryQueryService;
import ru.yandex.market.pvz.core.domain.inventory.mapper.OrderWithPlacesParamsMapper;
import ru.yandex.market.pvz.core.domain.inventory.params.InventoryParams;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.place.OrderPlace;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;
import ru.yandex.market.pvz.internal.controller.pi.inventory.dto.InventoryItemDto;
import ru.yandex.market.pvz.internal.controller.pi.inventory.dto.InventoryItemPlaceCodeDto;
import ru.yandex.market.pvz.internal.controller.pi.inventory.dto.InventoryPlaceDto;
import ru.yandex.market.pvz.internal.controller.pi.inventory.mapper.InventoryItemDtoMapper;
import ru.yandex.market.pvz.internal.controller.pi.inventory.mapper.InventoryPageDtoMapper;
import ru.yandex.market.pvz.internal.controller.pi.inventory.mapper.InventoryPlaceDtoMapper;
import ru.yandex.market.pvz.internal.domain.inventory.InventoryReportConverterService;
import ru.yandex.market.pvz.internal.domain.inventory.InventoryService;
import ru.yandex.market.sc.internal.client.ScLogisticsClient;
import ru.yandex.market.tpl.common.util.logging.Tracer;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class InventoryControllerTest extends BaseShallowTest {

    private static final Long UID = 1L;
    private static final String LOGIN = "login";

    private final TestPickupPointFactory pickupPointFactory;
    private final TestableClock clock;
    private final PickupPointCommandService pickupPointCommandService;
    private final TestOrderFactory orderFactory;
    private final InventoryCommandService inventoryCommandService;

    private final InventoryQueryService inventoryQueryService;
    private final PickupPointQueryService pickupPointQueryService;
    private final InventoryItemDtoMapper inventoryItemDtoMapper;
    private final InventoryPlaceDtoMapper inventoryPlaceDtoMapper;
    private final InventoryPageDtoMapper inventoryPageDtoMapper;
    private final OrderWithPlacesParamsMapper orderWithPlacesParamsMapper;
    private final FinishInventoryProducer finishInventoryProducer;
    private final InventoryItemQueryService inventoryItemQueryService;

    @Mock
    private ScLogisticsClient scLogisticsClient;
    private InventoryService inventoryService;
    private InventoryReportConverterService inventoryReportConverterService;

    @BeforeEach
    void setup() {
        Tracer.putUidToStatic(UID);
        Tracer.putLoginToStatic(LOGIN);
        inventoryService = new InventoryService(
                inventoryCommandService, inventoryQueryService, pickupPointQueryService,
                inventoryItemDtoMapper, inventoryPlaceDtoMapper, inventoryPageDtoMapper, orderWithPlacesParamsMapper,
                scLogisticsClient, finishInventoryProducer, inventoryItemQueryService, inventoryReportConverterService
        );
    }

    @Test
    void whenCreateInventoryThenSuccess() throws Exception {
        clock.setFixed(Instant.parse("2022-01-01T23:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        var expected = getFileContent("inventory/response_create_inventory_success.json");

        mockMvc.perform(post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/inventory"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected));
    }

    @Test
    void whenFinishInventoryThenSuccess() throws Exception {
        clock.setFixed(Instant.parse("2022-01-01T23:00:00Z"), ZoneOffset.ofHours(3));
        int timeOffset = 6;
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, timeOffset
        );
        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());

        var expected = String.format(
                getFileContent("inventory/response_finish_inventory_success.json"), inventory.getId(),
                inventory.getDate()
        );

        String url = "/v1/pi/pickup-points/%d/inventory/%d/finish";
        mockMvc.perform(patch(String.format(url, pickupPoint.getPvzMarketId(), inventory.getId())))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected));
    }

    @Test
    void getCurrentInventoryWithoutAny() throws Exception {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        String url = "/v1/pi/pickup-points/%d/inventory/current";
        mockMvc.perform(get(String.format(url, pickupPoint.getPvzMarketId())))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("inventory/response_current_inventory_without_any.json")));
    }

    @Test
    void getCurrentInventoryInProgressWithItems() throws Exception {
        clock.setFixed(Instant.parse("2022-01-01T23:00:00Z"), ZoneOffset.ofHours(3));
        int timeOffset = 6;
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, timeOffset
        );
        long lmsId = 123L;
        pickupPointCommandService.updateLmsId(pickupPoint.getId(), lmsId);

        Order order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), UID,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod()
        );

        List<String> barcodes = Lists.transform(order.getPlaces(), OrderPlace::getBarcode);
        inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(barcodes.get(0)).build(),
                inventory.getId()
        );

        Order orderWithoutPlaces = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .params(
                                TestOrderFactory.OrderParams.builder()
                                        .places(List.of())
                                        .build()
                        )
                        .build()
        );
        InventoryItemDto itemParams2 = inventoryService.addItem(
                pickupPointRequestData,
                InventoryPlaceDto.builder().barcode(orderWithoutPlaces.getExternalId()).build(),
                inventory.getId()
        );

        InventoryItemDto itemParams3 = inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(barcodes.get(2)).build(),
                inventory.getId()
        );
        InventoryItemPlaceCodeDto placeCodeParams1 = itemParams3.getPlaceCodes().get(0);
        InventoryItemPlaceCodeDto placeCodeParams2 = itemParams3.getPlaceCodes().get(1);
        InventoryItemPlaceCodeDto placeCodeParams3 = itemParams3.getPlaceCodes().get(2);

        String pants = "ШК трусов";
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                .when(scLogisticsClient).findInventoryItemByBarcode(lmsId, List.of(pants));
        InventoryItemDto itemParams4 = inventoryService.addItem(
                pickupPointRequestData, InventoryPlaceDto.builder().barcode(pants).build(), inventory.getId()
        );

        clock.setFixed(Instant.parse("2022-01-01T23:30:00Z"), ZoneOffset.ofHours(3));

        var expected = String.format(
                getFileContent("inventory/response_current_inventory_in_progress_with_items.json"),
                inventory.getId(), inventory.getDate(), itemParams4.getId(), pants,
                itemParams2.getId(), orderWithoutPlaces.getExternalId(), itemParams3.getId(), order.getExternalId(),
                placeCodeParams1.getPlaceCode(), placeCodeParams2.getPlaceCode(), placeCodeParams3.getPlaceCode()
        );

        String url = "/v1/pi/pickup-points/%d/inventory/current";
        mockMvc.perform(get(String.format(url, pickupPoint.getPvzMarketId())))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected));
    }

    @Test
    void getCurrentInventoryWithFinished() throws Exception {
        clock.setFixed(Instant.parse("2022-01-17T15:01:00Z"), ZoneOffset.ofHours(0));
        int timeOffset = 6;
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, timeOffset
        );

        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        inventoryCommandService.finish(pickupPoint.getId(), pickupPoint.getTimeOffset(), inventory.getId());
        var expected = String.format(
                getFileContent("inventory/response_current_inventory_with_finished.json"),
                inventory.getId(), inventory.getDate()
        );

        String url = "/v1/pi/pickup-points/%d/inventory/current";
        mockMvc.perform(get(String.format(url, pickupPoint.getPvzMarketId())))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected));
    }

    @Test
    void getCurrentInventoryWithFinishedBefore() throws Exception {
        clock.setFixed(Instant.parse("2022-01-17T15:01:00Z"), ZoneOffset.ofHours(0));
        int timeOffset = 6;
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, timeOffset
        );

        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());
        inventoryCommandService.finish(pickupPoint.getId(), pickupPoint.getTimeOffset(), inventory.getId());

        clock.setFixed(Instant.parse("2022-01-18T15:01:00Z"), ZoneOffset.ofHours(0));
        inventory = inventoryCommandService.create(pickupPoint.getId());

        var expected = String.format(
                getFileContent("inventory/response_current_inventory_with_finished_before.json"),
                inventory.getId(), inventory.getDate()
        );

        String url = "/v1/pi/pickup-points/%d/inventory/current";
        mockMvc.perform(get(String.format(url, pickupPoint.getPvzMarketId())))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected));
    }

}
