package ru.yandex.market.checkout.checkouter.yauslugi.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Function;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;

/**
 * @author zagidullinri
 * @date 21.09.2021
 */
public abstract class ServiceDtoProvider {

    public static final Long DEFAULT_ID = 1L;
    public static final String DEFAULT_YA_SERVICE_ID = "yaServiceId";
    public static final Long DEFAULT_ORDER_ID = 2L;
    public static final ItemServiceStatus DEFAULT_STATUS = ItemServiceStatus.NEW;
    public static final String DEFAULT_TITLE = "someTitle";
    public static final String DEFAULT_DESCRIPTION = "someDescription";
    public static final LocalDate DEFAULT_DATE = LocalDate.of(2021, 9, 21);
    public static final LocalTime DEFAULT_FROM_TIME = LocalTime.of(10, 0);
    public static final LocalTime DEFAULT_TO_TIME = LocalTime.of(15, 0);
    public static final BigDecimal DEFAULT_PRICE = BigDecimal.valueOf(100);
    public static final PaymentType DEFAULT_PAYMENT_TYPE = PaymentType.PREPAID;
    public static final DeliveryType DEFAULT_DELIVERY_TYPE = DeliveryType.DELIVERY;

    public static YaServiceDto defaultServiceDto() {
        return builder()
                .configure(ServiceDtoProvider::applyDefaults)
                .build();
    }

    public static ServiceDtoBuilder applyDefaults(ServiceDtoBuilder serviceDtoBuilder) {
        return serviceDtoBuilder
                .withId(DEFAULT_ID)
                .withYaServiceId(DEFAULT_YA_SERVICE_ID)
                .withOrderId(DEFAULT_ORDER_ID)
                .withStatus(DEFAULT_STATUS)
                .withTitle(DEFAULT_TITLE)
                .withDescription(DEFAULT_DESCRIPTION)
                .withDate(DEFAULT_DATE)
                .withFromTime(DEFAULT_FROM_TIME)
                .withToTime(DEFAULT_TO_TIME)
                .withPrice(DEFAULT_PRICE)
                .withPaymentType(DEFAULT_PAYMENT_TYPE)
                .withClient(ClientDtoProvider.defaultClientDto())
                .withAddress(AddressDtoProvider.defaultAddressDto())
                .withDeliveryType(DEFAULT_DELIVERY_TYPE)
                .withOrder(MarketOrderDtoProvider.defaultOrderDto());
    }

    public static ServiceDtoBuilder builder() {
        return new ServiceDtoBuilder();
    }


    public static class ServiceDtoBuilder {

        private Long id;
        private String yaServiceId;
        private Long orderId;
        private ItemServiceStatus status;
        private String title;
        private String description;
        private LocalDate date;
        private LocalTime fromTime;
        private LocalTime toTime;
        private BigDecimal price;
        private PaymentType paymentType;
        private ClientDto client;
        private AddressDto address;
        private DeliveryType deliveryType;
        private MarketOrderDto order;

        private ServiceDtoBuilder() {

        }

        public ServiceDtoBuilder configure(Function<ServiceDtoBuilder, ServiceDtoBuilder> function) {
            return function.apply(this);
        }

        public ServiceDtoBuilder withId(Long id) {
            this.id = id;
            return this;
        }


        public ServiceDtoBuilder withYaServiceId(String yaServiceId) {
            this.yaServiceId = yaServiceId;
            return this;
        }

        public ServiceDtoBuilder withOrderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public ServiceDtoBuilder withStatus(ItemServiceStatus status) {
            this.status = status;
            return this;
        }

        public ServiceDtoBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        public ServiceDtoBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public ServiceDtoBuilder withDate(LocalDate date) {
            this.date = date;
            return this;
        }

        public ServiceDtoBuilder withFromTime(LocalTime fromTime) {
            this.fromTime = fromTime;
            return this;
        }

        public ServiceDtoBuilder withToTime(LocalTime toTime) {
            this.toTime = toTime;
            return this;
        }

        public ServiceDtoBuilder withPrice(BigDecimal price) {
            this.price = price;
            return this;
        }

        public ServiceDtoBuilder withPaymentType(PaymentType paymentType) {
            this.paymentType = paymentType;
            return this;
        }

        public ServiceDtoBuilder withClient(ClientDto client) {
            this.client = client;
            return this;
        }

        public ServiceDtoBuilder withAddress(AddressDto address) {
            this.address = address;
            return this;
        }

        public ServiceDtoBuilder withDeliveryType(DeliveryType deliveryType) {
            this.deliveryType = deliveryType;
            return this;
        }

        public ServiceDtoBuilder withOrder(MarketOrderDto order) {
            this.order = order;
            return this;
        }

        public YaServiceDto build() {
            YaServiceDto yaServiceDto = new YaServiceDto();
            yaServiceDto.setId(id);
            yaServiceDto.setYaServiceId(yaServiceId);
            yaServiceDto.setOrderId(orderId);
            yaServiceDto.setStatus(status);
            yaServiceDto.setTitle(title);
            yaServiceDto.setDescription(description);
            yaServiceDto.setDate(date);
            yaServiceDto.setFromTime(fromTime);
            yaServiceDto.setToTime(toTime);
            yaServiceDto.setPrice(price);
            yaServiceDto.setPaymentType(paymentType);
            yaServiceDto.setClient(client);
            yaServiceDto.setAddress(address);
            yaServiceDto.setDeliveryType(deliveryType);
            yaServiceDto.setOrder(order);
            return yaServiceDto;
        }
    }
}
