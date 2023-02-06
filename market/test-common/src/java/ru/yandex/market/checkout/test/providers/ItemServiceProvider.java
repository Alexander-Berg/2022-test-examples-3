package ru.yandex.market.checkout.test.providers;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;

public abstract class ItemServiceProvider {

    public static ItemService defaultItemService() {
        return builder()
                .configure(ItemServiceProvider::applyDefaults)
                .build();
    }

    public static ItemServiceBuilder applyDefaults(@Nonnull ItemServiceBuilder builder) {
        return builder
                .someId()
                .someServiceId()
                .defaultDate()
                .defaultFromTime()
                .defaultToTime()
                .price(BigDecimal.valueOf(150L))
                .title("ItemServiceName")
                .description("ItemServiceDescription")
                .yaServiceId("333222111")
                .count(2)
                .status(ItemServiceStatus.NEW)
                .inn("default_inn")
                .vat(VatType.NO_VAT);
    }

    public static ItemServiceBuilder builder() {
        return new ItemServiceBuilder();
    }

    public static class ItemServiceBuilder {

        private Long id;
        private Long serviceId;
        private Date date;
        private LocalTime fromTime;
        private LocalTime toTime;
        private BigDecimal price;
        private String title;
        private String description;
        private ItemServiceStatus status;
        private String yaServiceId;
        private PaymentType paymentType;
        private String inn;
        private PaymentMethod paymentMethod;
        private int count;
        private VatType vat;

        private ItemServiceBuilder() {
        }

        @Nonnull
        public ItemServiceBuilder configure(@Nonnull Function<ItemServiceBuilder, ItemServiceBuilder> configure) {
            return configure.apply(this);
        }

        public ItemServiceBuilder id(Number id) {
            this.id = id == null ? null : id.longValue();
            return this;
        }

        public ItemServiceBuilder someId() {
            return id(ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE));
        }

        public ItemServiceBuilder serviceId(Long serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public ItemServiceBuilder someServiceId() {
            this.serviceId = ThreadLocalRandom.current().nextLong();
            return this;
        }

        public ItemServiceBuilder date(Date date) {
            this.date = date;
            return this;
        }

        public ItemServiceBuilder defaultDate() {
            this.date = new Date();
            return this;
        }

        public ItemServiceBuilder fromTime(LocalTime fromTime) {
            this.fromTime = fromTime;
            return this;
        }

        public ItemServiceBuilder defaultFromTime() {
            this.fromTime = LocalTime.of(8, 0);
            return this;
        }

        public ItemServiceBuilder toTime(LocalTime toTime) {
            this.toTime = toTime;
            return this;
        }

        public ItemServiceBuilder defaultToTime() {
            this.toTime = LocalTime.of(12, 0);
            return this;
        }

        public ItemServiceBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public ItemServiceBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ItemServiceBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ItemServiceBuilder status(ItemServiceStatus status) {
            this.status = status;
            return this;
        }

        public ItemServiceBuilder yaServiceId(String yaServiceId) {
            this.yaServiceId = yaServiceId;
            return this;
        }

        public ItemServiceBuilder paymentType(PaymentType paymentType) {
            this.paymentType = paymentType;
            return this;
        }

        public ItemServiceBuilder inn(String inn) {
            this.inn = inn;
            return this;
        }

        public ItemServiceBuilder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public ItemServiceBuilder count(int count) {
            this.count = count;
            return this;
        }

        public ItemServiceBuilder vat(VatType vat) {
            this.vat = vat;
            return this;
        }

        public ItemService build() {
            ItemService itemService = new ItemService();
            itemService.setId(id);
            itemService.setServiceId(serviceId);
            itemService.setDate(date);
            itemService.setFromTime(fromTime);
            itemService.setToTime(toTime);
            itemService.setPrice(price);
            itemService.setTitle(title);
            itemService.setDescription(description);
            itemService.setStatus(status);
            itemService.setYaServiceId(yaServiceId);
            itemService.setPaymentType(paymentType);
            itemService.setInn(inn);
            itemService.setPaymentMethod(paymentMethod);
            itemService.setCount(count);
            itemService.setVat(vat);
            return itemService;
        }
    }
}
