package ru.yandex.market.tpl.core.query.order.mapper;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.order.OrderItemDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.OrderTagsDto;
import ru.yandex.market.tpl.api.model.order.PlaceDto;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.service.order.OrderPhotoValidator;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class OrderDtoMapperTest extends TplAbstractTest {

    private static final String EXPECTED_PLACE_BARCODE_PREFIX = "P123";

    private final OrderDtoMapper orderDtoMapper;
    private final OrderGenerateService orderGenerateService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper tplObjectMapper;


    @Test
    void mapSinglePlaceOrder() {
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .places(
                                List.of(
                                        OrderPlaceDto.builder()
                                                .barcode(new OrderPlaceBarcode("145", EXPECTED_PLACE_BARCODE_PREFIX))
                                                .build()
                                )
                        )
                        .build()
        );

        OrderDto orderDto = transactionTemplate.execute(status ->
                orderDtoMapper.mapOrderDto(order, clock.instant(), null));

        assertThat(orderDto.getPlaces()).isNotNull().hasSize(1);

        PlaceDto place = orderDto.getPlaces().iterator().next();

        assertThat(place.getBarcode()).isEqualTo(EXPECTED_PLACE_BARCODE_PREFIX);

        long countItemsWithoutService = order.getItems().stream()
                .filter(i -> !i.isService())
                .count();

        assertThat(place.getItems()).isNotNull().hasSize((int) countItemsWithoutService);
    }

    @SneakyThrows
    @Test
    void checkUitsAreNullInJson() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(true)
                                        .build()
                        )
                        .build()
        );

        var orderItemInstance = order.getItems().get(0).getInstances().get(0);
        orderItemInstance.setUit(null);
        orderItemInstance.setCis(null);

        OrderDto orderDto = transactionTemplate.execute(status ->
                orderDtoMapper.mapOrderDto(order, clock.instant(), null));

        var result = tplObjectMapper.writeValueAsString(orderDto);

        assertThat(result).contains("\"uit\":null");
    }

    @Test
    void mapMultiPlaceOrder() {
        List<OrderPlaceDto> places = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            places.add(
                    OrderPlaceDto.builder()
                            .barcode(new OrderPlaceBarcode("145", EXPECTED_PLACE_BARCODE_PREFIX + (i + 1)))
                            .build()
            );
        }

        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .places(
                                places
                        )
                        .build()
        );
        OrderDto orderDto = transactionTemplate.execute(status ->
                orderDtoMapper.mapOrderDto(order, clock.instant(), null));

        assertThat(orderDto.getPlaces()).isNotNull().hasSize(2);
        assertThat(orderDto.getPlaces())
                .extracting(PlaceDto::getBarcode)
                .containsExactlyInAnyOrder(EXPECTED_PLACE_BARCODE_PREFIX + 1, EXPECTED_PLACE_BARCODE_PREFIX + 2);

        orderDto.getPlaces()
                .forEach((p) -> assertThat(p.getItems()).isNotNull().isNotEmpty());
    }

    @Test
    void mapOrderThatRequiresTakingAPhoto() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .recipientNotes(TrackingService.CONTACTLESS_DELIVERY_PREFIX)
                .paymentType(OrderPaymentType.PREPAID)
                .build());

        OrderDto orderDto = transactionTemplate.execute(status -> orderDtoMapper.mapOrderDto(order, clock.instant(),
                null));

        assertThat(orderDto.getTakePhoto().isRequired()).isTrue();
    }

    @Test
    void mapR18OrderThatRequiresShowingADocument() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder().isR18(true).build())
                .build());

        OrderDto orderDto = transactionTemplate.execute(status ->
                orderDtoMapper.mapOrderDto(order, clock.instant(), null));

        assertThat(orderDto.getTags()).contains(OrderTagsDto.IS_R18, OrderTagsDto.SHOW_DOCUMENT);
    }

    @Test
    void mapExpensiveOrderThatRequiresShowingADocument() {
        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.PREPAID)
                .paymentStatus(OrderPaymentStatus.PAID)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsPrice(OrderPhotoValidator.EXPENSIVE_ORDER_TOTAL_PRICE)
                        .itemsCount(1)
                        .itemsItemCount(1)
                        .build())
                .build());

        OrderDto orderDto = transactionTemplate.execute(status ->
                orderDtoMapper.mapOrderDto(order1, clock.instant(), null));

        assertThat(orderDto.getTags()).contains(OrderTagsDto.SHOW_DOCUMENT);
    }

    @Test
    void mapFittingDisabledOrder() {
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                                .isNoTrying(true)
                                .itemsCount(1)
                                .build()
                        )
                        .build()
        );

        OrderDto orderDto = transactionTemplate.execute(status ->
                orderDtoMapper.mapOrderDto(order, clock.instant(), null));

        assertThat(orderDto.getItems()).isNotNull().hasSize(1);

        OrderItemDto itemDto = orderDto.getItems().iterator().next();

        assertThat(itemDto.getIsFittingEnabled()).isEqualTo(false);

    }
}
