package ru.yandex.market.logistics.nesu.base.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.delivery.calculator.client.model.DeliverySearchRequest;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.OrderItemBoxDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraftDeliveryOption;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.defaultDeliverySearchRequestBuilder;
import static ru.yandex.market.logistics.nesu.base.OrderTestUtils.mockDeliveryOptionValidation;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createDimensions;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createItem;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createItemBoxBuilder;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createItemKorobyte;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createKorobyte;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createLomItemBuilder;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createLomOrderCost;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createMonetary;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createPlace;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createPlaceUnitBuilder;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultDeliveryOption;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultDeliveryOptionServices;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.defaultLomDeliveryServices;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractCreateOrderMultiplaceCasesTest extends AbstractCreateOrderWaybillFromCombinator {
    @Test
    @DisplayName("Создание черновика с заполнением товаров для многоместки")
    void createOrderWithMultiplacesItems() throws Exception {
        mockMultiplace(deliverySearchRequest());

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(o -> {
                o.setPlaces(List.of(
                    createPlace(150, 170, 110, 50, List.of(createItem()))
                ));
                o.setDimensions(createDimensions());
                o.setItems(List.of(createItem().setPlaceExternalIds(List.of("ext_place_id"))));
            }
        ))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(createMultiplaceLomOrderRequest().setUnits(storageUnits(createPlaceUnitBuilder())));
    }

    @Test
    @DisplayName("Создание черновика, проверяем, что общая стоимость товаров считается корректно")
    void createOrderCheckItemsSumCalculatedCorrectly() throws Exception {
        BigDecimal itemPrice = new BigDecimal(72);
        int itemPriceAsInt = itemPrice.intValue();
        int itemsCount = 1234;
        long expectedItemsTotalKopecks = itemPrice.longValue() * itemsCount * 100;
        String cashService = "1510.42";

        mockMultiplace(
            defaultDeliverySearchRequestBuilder()
                .length(150)
                .width(170)
                .height(110)
                .weight(BigDecimal.valueOf(50))
                .offerPrice(expectedItemsTotalKopecks)
                .build()
        );

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(o -> {
                o.setPlaces(List.of(
                    createPlace(
                        150,
                        170,
                        110,
                        50,
                        List.of(createItem(itemPriceAsInt, itemsCount, itemPriceAsInt))
                    )
                ));
                o.setDimensions(createDimensions());
                o.setItems(List.of(
                    createItem(itemPriceAsInt, itemsCount, itemPriceAsInt).setPlaceExternalIds(List.of("ext_place_id"))
                ));
                o.setDeliveryOption(
                    (OrderDraftDeliveryOption) defaultDeliveryOption()
                        .setServices(defaultDeliveryOptionServices(cashService, "0.75"))
                );
            }
        ))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(
            createMultiplaceLomOrderRequest()
                .setUnits(storageUnits(createPlaceUnitBuilder()))
                .setItems(List.of(
                    createLomItemBuilder(null)
                        .count(itemsCount)
                        .price(createMonetary(itemPrice))
                        .assessedValue(createMonetary(itemPrice))
                        .build()
                ))
                .setCost(createLomOrderCost().services(defaultLomDeliveryServices(cashService, "0.75")).build())
        );
    }

    @Test
    @DisplayName("Создание черновика с заполнением товаров для многоместки - у внешних товаров больше приоритет")
    void createOrderWithMultiplacesItemsAndPlaces() throws Exception {
        mockMultiplace(deliverySearchRequest());

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(o -> {
                o.setPlaces(List.of(
                    createPlace(150, 170, 110, 50, List.of(createItem().setCount(1)))
                ));
                o.setDimensions(createDimensions());
                o.setItems(List.of(
                    createItem().setPlaceExternalIds(List.of(o.getPlaces().get(0).getExternalId()))
                ));
            }
        ))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(createMultiplaceLomOrderRequest().setUnits(storageUnits(createPlaceUnitBuilder())));
    }

    @Test
    @DisplayName("Создание черновика с заполнением товаров для многоместки - несколько мест для товара")
    void createOrderWithMultiplacesItemsSeveralPlaces() throws Exception {
        mockMultiplace(defaultDeliverySearchRequestBuilder()
            .weight(BigDecimal.valueOf(100))
            .length(300)
            .width(340)
            .height(220)
            .build()
        );
        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(o -> {
                o.setPlaces(List.of(
                    createPlace(150, 170, 110, 50, List.of(createItem().setCount(1))).setExternalId("ext_place_id"),
                    createPlace(150, 170, 110, 50, List.of(createItem().setCount(1))).setExternalId("ext_place_id-2")
                ));
                o.setItems(List.of(
                    createItem()
                        .setPlaceExternalIds(List.of("ext_place_id", "ext_place_id-2"))
                ));
                o.setDimensions(createDimensions(300, 340, 220, 100));
            }
        ))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        WaybillOrderRequestDto request = createLomOrderRequest();
        List<StorageUnitDto> units = List.of(
            createPlaceUnitBuilder()
                .dimensions(createKorobyte(110, 170, 150, 50))
                .build(),
            createPlaceUnitBuilder()
                .externalId("ext_place_id-2")
                .dimensions(createKorobyte(110, 170, 150, 50))
                .build(),
            StorageUnitDto.builder()
                .type(StorageUnitType.ROOT)
                .externalId("generated-0")
                .dimensions(createKorobyte(220, 340, 300, 100))
                .build()
        );

        verifyLomOrderCreate(request
            .setItems(List.of(createLomItemBuilder()
                .boxes(List.of(
                    createItemBoxBuilder()
                        .storageUnitExternalIds(Set.of("ext_place_id", "ext_place_id-2"))
                        .storageUnitIndexes(null)
                        .build()
                ))
                .build()
            ))
            .setUnits(units)
        );
    }

    @Test
    @DisplayName("Создание черновика с заполнением товаров для многоместки - товары не разложены по местам")
    void createOrderWithMultiplacesItemsNoPlaces() throws Exception {
        mockMultiplace(deliverySearchRequest());

        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(o -> {
                o.setPlaces(List.of(createPlace(150, 170, 110, 50, List.of(createItem()))));
                o.setDimensions(createDimensions());
                o.setItems(List.of(createItem()));
            }
        ))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(
            createLomOrderRequest().setItems(itemWithBoxes()).setUnits(storageUnits(createPlaceUnitBuilder()))
        );
    }

    @Test
    @DisplayName("Создание черновика с заполнением товаров для многоместки - у места нет идентификатора")
    void createOrderWithMultiplacesItemsPlaceWithoutExternalId() throws Exception {
        mockMultiplace(deliverySearchRequest());
        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(o -> {
                o.setPlaces(List.of(createPlace(150, 170, 110, 50, List.of(createItem())).setExternalId(null)));
                o.setDimensions(createDimensions());
                o.setItems(List.of(createItem()));
            }
        ))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(
            createLomOrderRequest()
                .setItems(itemWithBoxes())
                .setUnits(storageUnits(createPlaceUnitBuilder().externalId(null)))
        );
    }

    @Test
    @DisplayName("Создание черновика с заполнением товаров для многоместки - идентификатор из доступных символов")
    void createOrderWithMultiplacesItemsPlaceExternalId() throws Exception {
        mockMultiplace(deliverySearchRequest());
        createOrder(OrderDtoFactory.defaultOrderDraft().andThen(o -> {
                o.setPlaces(List.of(createPlace(150, 170, 110, 50, List.of(createItem())).setExternalId("/\\-_aA90")));
                o.setDimensions(createDimensions());
                o.setItems(List.of(createItem()));
            }
        ))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verifyLomOrderCreate(
            createLomOrderRequest()
                .setItems(itemWithBoxes())
                .setUnits(storageUnits(createPlaceUnitBuilder().externalId("/\\-_aA90")))
        );
    }

    @Nonnull
    private List<ItemDto> itemWithBoxes() {
        return List.of(
            createLomItemBuilder()
                .boxes(List.of(OrderItemBoxDto.builder().dimensions(createItemKorobyte()).build()))
                .build()
        );
    }

    @Nonnull
    private List<StorageUnitDto> storageUnits(StorageUnitDto.StorageUnitDtoBuilder storageUnitDtoBuilder) {
        return List.of(
            storageUnitDtoBuilder
                .dimensions(createKorobyte(110, 170, 150, 50))
                .build(),
            StorageUnitDto.builder()
                .type(StorageUnitType.ROOT)
                .externalId("generated-0")
                .dimensions(createKorobyte(110, 170, 150, 50))
                .build()
        );
    }

    @Nonnull
    private DeliverySearchRequest deliverySearchRequest() {
        return defaultDeliverySearchRequestBuilder()
            .length(150)
            .width(170)
            .height(110)
            .weight(BigDecimal.valueOf(50))
            .build();
    }

    private void mockMultiplace(DeliverySearchRequest deliverySearchRequest) {
        mockGetLogisticsPoints(WAREHOUSE_FROM, SORTING_CENTER_WAREHOUSE_TO, PICKUP_POINT);
        mockDeliveryOptionValidation(
            MAX_DELIVERY_DAYS,
            deliveryCalculatorSearchEngineClient,
            lmsClient,
            deliverySearchRequest
        );
    }
}
