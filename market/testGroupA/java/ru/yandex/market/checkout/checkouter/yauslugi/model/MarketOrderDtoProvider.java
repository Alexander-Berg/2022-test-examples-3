package ru.yandex.market.checkout.checkouter.yauslugi.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Function;

/**
 * Фабрика объектов заказа для ЯУ в тестах.
 *
 * @author gelvy
 * Created on: 10.03.2022
 **/
public class MarketOrderDtoProvider {

    public static final String DEFAULT_ITEM_TITLE = "Стиральная машина Мечта";
    public static final LocalDate DEFAULT_FROM_DATE = LocalDate.of(2022, 3, 10);
    public static final LocalDate DEFAULT_TO_DATE = LocalDate.of(2022, 3, 11);
    public static final LocalTime DEFAULT_FROM_TIME = LocalTime.of(12, 0);
    public static final LocalTime DEFAULT_TO_TIME = LocalTime.of(13, 0);

    private MarketOrderDtoProvider() {
    }

    public static MarketOrderDto defaultOrderDto() {
        return builder()
                .configure(MarketOrderDtoProvider::applyDefaults)
                .build();
    }

    public static Builder applyDefaults(Builder builder) {
        return builder.withItemTitle(DEFAULT_ITEM_TITLE)
                .withFromDate(DEFAULT_FROM_DATE)
                .withToDate(DEFAULT_TO_DATE)
                .withFromTime(DEFAULT_FROM_TIME)
                .withToTime(DEFAULT_TO_TIME);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String itemTitle;

        private LocalDate fromDate;

        private LocalDate toDate;

        private LocalTime fromTime;

        private LocalTime toTime;

        public Builder withItemTitle(String itemTitle) {
            this.itemTitle = itemTitle;
            return this;
        }

        public Builder withFromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public Builder withToDate(LocalDate toDate) {
            this.toDate = toDate;
            return this;
        }

        public Builder withFromTime(LocalTime fromTime) {
            this.fromTime = fromTime;
            return this;
        }

        public Builder withToTime(LocalTime toTime) {
            this.toTime = toTime;
            return this;
        }

        public MarketOrderDto build() {
            var item = new MarketItemDto(itemTitle);
            var dates = new MarketDeliveryDatesDto(fromDate, toDate, fromTime, toTime);
            var delivery = new MarketDeliveryDto(dates);
            return new MarketOrderDto(item, delivery);
        }

        public Builder configure(Function<Builder, Builder> function) {
            return function.apply(this);
        }
    }
}
