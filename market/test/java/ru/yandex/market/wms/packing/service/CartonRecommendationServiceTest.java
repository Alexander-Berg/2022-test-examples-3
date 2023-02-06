package ru.yandex.market.wms.packing.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.pojo.BoxImpl;
import ru.yandex.market.wms.common.spring.pojo.Carton;
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.ItemDto;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.RecommendedBoxDto;
import ru.yandex.market.wms.common.spring.servicebus.model.response.RecommendCartonResponse;
import ru.yandex.market.wms.packing.pojo.TaskItemDimensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CartonRecommendationServiceTest {
    private static final String CARTON_GROUP = "PK";
    private static final Carton CARTON_A = Carton.builder().group(CARTON_GROUP)
            .type("cartonA").length(80).width(50).height(35).build();
    private static final Carton CARTON_B = Carton.builder().group(CARTON_GROUP)
            .type("cartonB").length(100).width(60).height(40).build();
    private static final Carton CARTON_C = Carton.builder().group(CARTON_GROUP)
            .type("cartonC").length(120).width(70).height(45).build();
    private static final List<Carton> CARTONS = Arrays.asList(CARTON_A, CARTON_B, CARTON_C);
    private static final int VOLUME_RESERVE_PERCENT = 15;
    private static final double VOLUME_RESERVE_FACTOR = VOLUME_RESERVE_PERCENT / 100.0;

    private final CartonRecommendationService service;
    private ServicebusClient servicebusClient;

    {
        CartonService cartonService = mock(CartonService.class);
        when(cartonService.getCartons(CARTON_GROUP)).thenReturn(CARTONS);

        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getParcelVolumeReservePercent()).thenReturn(VOLUME_RESERVE_PERCENT);
        when(settingsService.useRemoteCartonRecommendation()).thenReturn(true);
        when(settingsService.shouldGetRemoteCartonRecommendation()).thenReturn(true);
        servicebusClient = mock(ServicebusClient.class);
        service = new CartonRecommendationService(cartonService, settingsService, servicebusClient);
    }


    /**
     * Для маленького товара должна подойти самая маленькая коробка
     */
    @Test
    void recommendSmallestCarton() {
        List<TaskItemDimensions> items = Arrays.asList(newItem(10, 10, 10, 1));

        CARTONS.forEach(c -> {
            assertTrue(cartonFitsByLinearDimensions(c, items), c::getType);
            assertTrue(cartonFitsByVolume(c, items), c::getType);
        });

        assertSame(CARTON_A, service.recommendCartonLocal(CARTON_GROUP, items.get(0), items, true));
        assertSame(CARTON_A, service.recommendCartonLocal(CARTON_GROUP, items.get(0), items, false));
    }

    /**
     * Здесь весь заказ по объему влезет в любую коробку, проверяем соответствие линейных измерений
     */
    @Test
    void recommendCartonByLinearDimension() {
        List<TaskItemDimensions> items = Arrays.asList(
                newItem(95, 45, 1, 1),
                newItem(90, 55, 1, 1),
                newItem(80, 59, 1, 1)
        );

        assertFalse(cartonFitsByLinearDimensions(CARTON_A, items));
        assertTrue(cartonFitsByLinearDimensions(CARTON_B, items));
        assertTrue(cartonFitsByLinearDimensions(CARTON_C, items));

        CARTONS.forEach(c -> assertTrue(cartonFitsByVolume(c, items), c::getType));

        assertSame(CARTON_B, service.recommendCartonLocal(CARTON_GROUP, items.get(0), items, true));
        assertSame(CARTON_B, service.recommendCartonLocal(CARTON_GROUP, items.get(0), items, false));
    }

    /**
     * Здесь все товары подходят по линейным измерениям, проверяем суммарный объем заказа
     */
    @Test
    void recommendCartonByVolume() {
        List<TaskItemDimensions> items = Arrays.asList(
                newItem(10, 10, 10, 20),
                newItem(15, 15, 15, 20),
                newItem(20, 20, 20, 20)
        );

        CARTONS.forEach(c -> assertTrue(cartonFitsByLinearDimensions(c, items), c::getType));

        assertFalse(cartonFitsByVolume(CARTON_A, items));
        assertFalse(cartonFitsByVolume(CARTON_B, items));
        assertTrue(cartonFitsByVolume(CARTON_C, items));

        assertSame(CARTON_C, service.recommendCartonLocal(CARTON_GROUP, items.get(0), items, true));
        assertSame(CARTON_C, service.recommendCartonLocal(CARTON_GROUP, items.get(0), items, false));
    }

    /**
     * Все товары маленькие, но по объему заказ не входит ни в одну коробку,
     * нужно рекомендовать самую большую коробку для многопосылочного перевозчика
     * и NONPACK для не многопосылочного.
     */
    @Test
    void recommendBiggestBox() {
        List<TaskItemDimensions> items = Arrays.asList(
                newItem(10, 10, 10, 50),
                newItem(15, 15, 15, 50),
                newItem(20, 20, 20, 50)
        );

        CARTONS.forEach(c -> {
            assertTrue(cartonFitsByLinearDimensions(c, items));
            assertFalse(cartonFitsByVolume(c, items));
        });

        assertSame(CARTON_C, service.recommendCartonLocal(CARTON_GROUP, items.get(0), items, true));
        assertTrue(service.recommendCartonLocal(CARTON_GROUP, items.get(0), items, false).isSizeless());
    }

    /**
     * Товар не подходит по линейным измерениям, нужно рекомендовать NONPACK
     */
    @Test
    void recommendNonPack() {
        List<TaskItemDimensions> items = Arrays.asList(
                newItem(10, 10, 10, 1),
                newItem(20, 20, 20, 1),
                newItem(46, 46, 46, 1)
        );

        CARTONS.forEach(c -> assertFalse(cartonFitsByLinearDimensions(c, items)));

        assertTrue(service.recommendCartonLocal(CARTON_GROUP, items.get(2), items, true).isSizeless());
        assertTrue(service.recommendCartonLocal(CARTON_GROUP, items.get(2), items, false).isSizeless());
    }

    /**
     * В группе есть товар, не влезающий ни в одну коробку,
     * он не должен учитываться в расчетах для многопосылочного перевозчика,
     * а для немногопосылочного должен, рекомендуем NONPACK
     */
    @Test
    void recommendCartonExcludingOutfitItem() {
        List<TaskItemDimensions> items = Arrays.asList(
                newItem(10, 10, 10, 1),
                newItem(20, 20, 20, 1),
                newItem(46, 46, 46, 1)
        );

        CARTONS.forEach(c -> assertFalse(cartonFitsByLinearDimensions(c, items)));

        assertSame(CARTON_A, service.recommendCartonLocal(CARTON_GROUP, items.get(0), items, true));
        assertTrue(service.recommendCartonLocal(CARTON_GROUP, items.get(0), items, false).isSizeless());
    }


    static boolean cartonFitsByLinearDimensions(Carton carton, List<TaskItemDimensions> items) {
        return items.stream().allMatch(item ->
                carton.getLongestDim() > item.getLongestDim()
                        && carton.getMiddleDim() > item.getMiddleDim()
                        && carton.getShortestDim() > item.getShortestDim());
    }

    static boolean cartonFitsByVolume(Carton carton, List<TaskItemDimensions> items) {
        double orderVolume = items.stream()
                .mapToDouble(item -> item.getVolume() * item.getQty().doubleValue())
                .sum();
        return carton.getVolume() > orderVolume / (1.0 - VOLUME_RESERVE_FACTOR);
    }

    static TaskItemDimensions newItem(double x, double y, double z, int quantity) {
        return newItem(x, y, z, quantity, 1);
    }

    static TaskItemDimensions newItem(double x, double y, double z, int quantity, int num) {
        return new TaskItemDimensions(new SkuId("storer" + num, "sku" + num), BigDecimal.valueOf(quantity),
                new BoxImpl(x, y, z, x * y * z), 0, "manufacturerSku" + num);
    }

    @Test
    public void remoteRecommendation_singleRecommendation() {
        TaskItemDimensions currentItem = newItem(10, 11, 12, 2);
        when(servicebusClient.recommendCarton(any())).thenReturn(RecommendCartonResponse.builder()
                .recommendations(List.of(getRecommendedBoxDto(CARTON_A, currentItem)))
                .build());
        List<TaskItemDimensions> items = List.of(currentItem);
        Optional<Carton> carton = service.recommendCartonRemote(CARTON_GROUP, currentItem, items, false);
        assertTrue(carton.isPresent());
        assertEquals(CARTON_A, carton.get());
    }

    @Test
    public void remoteRecommendation_singleRecommendationForTwoItems() {
        TaskItemDimensions item1 = newItem(10, 11, 12, 2, 1);
        TaskItemDimensions item2 = newItem(13, 14, 15, 3, 2);
        when(servicebusClient.recommendCarton(any())).thenReturn(RecommendCartonResponse.builder()
                .recommendations(List.of(getRecommendedBoxDto(CARTON_A, item1, item2)))
                .build());
        List<TaskItemDimensions> items = List.of(item1, item2);
        Optional<Carton> carton = service.recommendCartonRemote(CARTON_GROUP, item1, items, false);
        assertTrue(carton.isPresent());
        assertEquals(CARTON_A, carton.get());
    }

    @Test
    public void remoteRecommendation_multiPackingNotSupported() {
        TaskItemDimensions item1 = newItem(10, 11, 12, 2, 1);
        TaskItemDimensions item2 = newItem(13, 14, 15, 3, 2);
        when(servicebusClient.recommendCarton(any())).thenReturn(RecommendCartonResponse.builder()
                .recommendations(List.of(
                        getRecommendedBoxDto(CARTON_A, item1),
                        getRecommendedBoxDto(CARTON_B, item2)
                )).build());
        List<TaskItemDimensions> items = List.of(item1, item2);
        Optional<Carton> carton = service.recommendCartonRemote(CARTON_GROUP, item1, items, false);
        assertFalse(carton.isPresent());
    }

    @Test
    public void remoteRecommendation_multiPackingSupported() {
        TaskItemDimensions item1 = newItem(10, 11, 12, 2, 1);
        TaskItemDimensions item2 = newItem(13, 14, 15, 3, 2);
        when(servicebusClient.recommendCarton(any())).thenReturn(RecommendCartonResponse.builder()
                .recommendations(List.of(
                        getRecommendedBoxDto(CARTON_A, item1),
                        getRecommendedBoxDto(CARTON_B, item2)
                )).build());
        List<TaskItemDimensions> items = List.of(item1, item2);
        Optional<Carton> cartonForItem1 = service.recommendCartonRemote(CARTON_GROUP, item1, items, true);
        assertTrue(cartonForItem1.isPresent());
        assertEquals(CARTON_A, cartonForItem1.get());
        Optional<Carton> cartonForItem2 = service.recommendCartonRemote(CARTON_GROUP, item2, items, true);
        assertTrue(cartonForItem2.isPresent());
        assertEquals(CARTON_B, cartonForItem2.get());
    }

    @Test
    public void remoteRecommendation_recommendationNotFound() {
        TaskItemDimensions item1 = newItem(10, 11, 12, 2, 1);
        TaskItemDimensions item2 = newItem(13, 14, 15, 3, 2);
        TaskItemDimensions wrongItem = newItem(16, 17, 18, 1, 3);
        when(servicebusClient.recommendCarton(any())).thenReturn(RecommendCartonResponse.builder()
                .recommendations(List.of(
                        getRecommendedBoxDto(CARTON_A, wrongItem),
                        getRecommendedBoxDto(CARTON_B, item2)
                )).build());
        List<TaskItemDimensions> items = List.of(item1, item2);
        Optional<Carton> carton = service.recommendCartonRemote(CARTON_GROUP, item1, items, true);
        assertFalse(carton.isPresent());
    }

    @Test
    public void remoteRecommendation_emptyRecommendations() {
        TaskItemDimensions item1 = newItem(10, 11, 12, 2, 1);
        TaskItemDimensions item2 = newItem(13, 14, 15, 3, 2);
        when(servicebusClient.recommendCarton(any())).thenReturn(RecommendCartonResponse.builder()
                .recommendations(Collections.emptyList()).build());
        List<TaskItemDimensions> items = List.of(item1, item2);
        Optional<Carton> cartonWhenMultiPackingSupported =
                service.recommendCartonRemote(CARTON_GROUP, item1, items, true);
        assertFalse(cartonWhenMultiPackingSupported.isPresent());
        Optional<Carton> cartonWhenMultiPackingNotSupported =
                service.recommendCartonRemote(CARTON_GROUP, item1, items, false);
        assertFalse(cartonWhenMultiPackingNotSupported.isPresent());
    }

    private RecommendedBoxDto getRecommendedBoxDto(Carton cartonA, TaskItemDimensions... items) {
        return RecommendedBoxDto.builder()
                .boxId(cartonA.getType())
                .items(Arrays.stream(items).map(currentItem -> ItemDto.builder()
                        .storerKey(currentItem.getSkuId().getStorerKey())
                        .manufacturerSku(currentItem.getManufacturerSku())
                        .qty(currentItem.getQty().intValue())
                        .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
