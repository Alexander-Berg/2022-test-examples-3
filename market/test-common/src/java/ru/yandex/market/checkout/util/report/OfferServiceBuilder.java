package ru.yandex.market.checkout.util.report;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.common.report.model.OfferService;

public class OfferServiceBuilder {

    private long serviceId;
    private String title;
    private String description;
    private BigDecimal price;
    private String yaServiceId;
    private Currency currency = Currency.RUR;

    public OfferServiceBuilder serviceId(long serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public OfferServiceBuilder title(String title) {
        this.title = title;
        return this;
    }

    public OfferServiceBuilder description(String description) {
        this.description = description;
        return this;
    }

    public OfferServiceBuilder price(BigDecimal price) {
        this.price = price;
        return this;
    }

    public OfferServiceBuilder currency(Currency currency) {
        this.currency = currency;
        return this;
    }

    public OfferServiceBuilder yaServiceId(String yaServiceId) {
        this.yaServiceId = yaServiceId;
        return this;
    }

    @Nonnull
    public OfferService build() {
        OfferService service = new OfferService();
        service.setServiceId(serviceId);
        service.setTitle(title);
        service.setDescription(description);
        service.setPrice(price);
        service.setCurrency(currency);
        service.setYaServiceId(yaServiceId);
        return service;
    }

    @Nonnull
    public static OfferServiceBuilder create() {
        return new OfferServiceBuilder();
    }

    @Nonnull
    public static OfferServiceBuilder createFrom(@Nonnull OfferService other) {
        return create()
                .serviceId(other.getServiceId())
                .title(other.getTitle())
                .description(other.getDescription())
                .price(other.getPrice())
                .currency(other.getCurrency())
                .yaServiceId(other.getYaServiceId());
    }

    @Nonnull
    public static OfferServiceBuilder createFrom(@Nonnull ItemService other) {
        return create()
                .serviceId(other.getServiceId())
                .title(other.getTitle())
                .description(other.getDescription())
                .price(other.getPrice())
                .yaServiceId(other.getYaServiceId());
    }
}
