package ru.yandex.market.global.checkout.factory;

import java.util.List;
import java.util.function.Function;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.Builder;
import lombok.Data;

import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.mj.generated.server.model.AddressDto;
import ru.yandex.mj.generated.server.model.CartActualizeDto;
import ru.yandex.mj.generated.server.model.CartDto;
import ru.yandex.mj.generated.server.model.CartItemDto;
import ru.yandex.mj.generated.server.model.GeoPointDto;
import ru.yandex.mj.generated.server.model.OrderCartDto;
import ru.yandex.mj.generated.server.model.OrderDeliverySchedulingType;

public class TestCartFactory {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(TestCartFactory.class).build();

    public CartDto createCartDto() {
        return createCartDto(CreateCartDtoBuilder.builder().build());
    }

    public CartDto createCartDto(CreateCartDtoBuilder builder) {
        CartDto cart = builder.setupCart.apply(RANDOM.nextObject(CartDto.class)
                .deliverySchedulingType(OrderDeliverySchedulingType.NOW)
                .requestedDeliveryTime(null)
                .createdAt(null)
                .modifiedAt(null)
        );

        cart.setItems(builder.setupItems.apply(cart.getItems()));

        AddressDto address = builder.setupRecipientAddress.apply(RANDOM.nextObject(AddressDto.class)
                .coordinates(new GeoPointDto()
                        .lat(TestShopFactory.IN_DELIVERY_AREA_LAT)
                        .lon(TestShopFactory.IN_DELIVERY_AREA_LON)
                )
        );
        cart.setRecipientAddress(address);
        return cart;
    }

    public OrderCartDto createOrderCartDto(CreateOrderCartDtoBuilder builder) {
        OrderCartDto cart = builder.setupCart.apply(RANDOM.nextObject(OrderCartDto.class)
                .deliverySchedulingType(OrderDeliverySchedulingType.NOW)
                .requestedDeliveryTime(null)
                .createdAt(null)
                .modifiedAt(null)
        );

        cart.setItems(builder.setupItems.apply(cart.getItems()));

        AddressDto address = builder.setupRecipientAddress.apply(RANDOM.nextObject(AddressDto.class)
                .coordinates(new GeoPointDto()
                        .lat(TestShopFactory.IN_DELIVERY_AREA_LAT)
                        .lon(TestShopFactory.IN_DELIVERY_AREA_LON)
                )
        );
        cart.setRecipientAddress(address);
        return cart;
    }

    public CartActualizeDto createCartActualizeDto(CreateCartActualizeDtoBuilder builder) {
        CartActualizeDto cartActualize = builder.setupCartActualize.apply(RANDOM.nextObject(CartActualizeDto.class)
                .deliverySchedulingType(OrderDeliverySchedulingType.NOW)
                .requestedDeliveryTime(null)
        );
        AddressDto address = builder.setupRecipientAddress.apply(RANDOM.nextObject(AddressDto.class)
                .coordinates(new GeoPointDto()
                        .lat(TestShopFactory.IN_DELIVERY_AREA_LAT)
                        .lon(TestShopFactory.IN_DELIVERY_AREA_LON)
                )
        );
        cartActualize.setRecipientAddress(address);
        return cartActualize;
    }

    public CartActualizeDto createCartActualize() {
        return createCartActualizeDto(CreateCartActualizeDtoBuilder.builder().build());
    }

    @Data
    @Builder
    public static class CreateCartDtoBuilder {
        @Builder.Default
        private Function<CartDto, CartDto> setupCart = Function.identity();

        @Builder.Default
        private Function<AddressDto, AddressDto> setupRecipientAddress = Function.identity();

        @Builder.Default
        private Function<List<CartItemDto>, List<CartItemDto>> setupItems = Function.identity();
    }

    @Data
    @Builder
    public static class CreateOrderCartDtoBuilder {
        @Builder.Default
        private Function<OrderCartDto, OrderCartDto> setupCart = Function.identity();

        @Builder.Default
        private Function<AddressDto, AddressDto> setupRecipientAddress = Function.identity();

        @Builder.Default
        private Function<List<CartItemDto>, List<CartItemDto>> setupItems = Function.identity();
    }

    @Data
    @Builder
    public static class CreateCartActualizeDtoBuilder {
        @Builder.Default
        private Function<CartActualizeDto, CartActualizeDto> setupCartActualize = Function.identity();

        @Builder.Default
        private Function<AddressDto, AddressDto> setupRecipientAddress = Function.identity();
    }

}
