package ru.yandex.market.fintech.fintechutils.helpers;


import ru.yandex.mj.generated.server.model.OrderItemDto;

public final class OrderItemProvider {

    public static OrderItemDtoBuilder item() {
        return OrderItemDtoBuilder.builder()
                .withItemId(1L)
                .withCategoryId(91013)
                .withSupplierId(112233L)
                .withHyperId(1660228333L)
                .withBuyerPrice("1000")
                .withCount(2)
                .withSku("145616512");
    }

    public static OrderItemDtoBuilder builder() {
        return new OrderItemDtoBuilder();
    }

    public static final class OrderItemDtoBuilder {
        private Long itemId;
        private Integer categoryId;
        private Long supplierId;
        private Long hyperId;
        private String buyerPrice;
        private Integer count;
        private String sku;

        private OrderItemDtoBuilder() {
        }

        public static OrderItemDtoBuilder builder() {
            return new OrderItemDtoBuilder();
        }

        public OrderItemDtoBuilder withItemId(Long itemId) {
            this.itemId = itemId;
            return this;
        }

        public OrderItemDtoBuilder withCategoryId(Integer categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public OrderItemDtoBuilder withSupplierId(Long supplierId) {
            this.supplierId = supplierId;
            return this;
        }

        public OrderItemDtoBuilder withHyperId(Long hyperId) {
            this.hyperId = hyperId;
            return this;
        }

        public OrderItemDtoBuilder withBuyerPrice(String buyerPrice) {
            this.buyerPrice = buyerPrice;
            return this;
        }

        public OrderItemDtoBuilder withCount(Integer count) {
            this.count = count;
            return this;
        }

        public OrderItemDtoBuilder withSku(String sku) {
            this.sku = sku;
            return this;
        }

        public OrderItemDto build() {
            OrderItemDto orderItemDto = new OrderItemDto();
            orderItemDto.setItemId(itemId);
            orderItemDto.setCategoryId(categoryId);
            orderItemDto.setSupplierId(supplierId);
            orderItemDto.setHyperId(hyperId);
            orderItemDto.setBuyerPrice(buyerPrice);
            orderItemDto.setCount(count);
            orderItemDto.setSku(sku);
            return orderItemDto;
        }
    }
}
