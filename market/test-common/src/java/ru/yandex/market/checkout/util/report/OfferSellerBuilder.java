package ru.yandex.market.checkout.util.report;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.common.report.model.MarkupData;
import ru.yandex.market.common.report.model.OfferSeller;

public class OfferSellerBuilder {

    private BigDecimal price;
    private Currency currency;
    private String comment;
    private BigDecimal sellerToUserExchangeRate;
    private MarkupData markupData;
    private String warrantyPeriod;

    public OfferSellerBuilder price(BigDecimal price) {
        this.price = price;
        return this;
    }

    public OfferSellerBuilder currency(Currency currency) {
        this.currency = currency;
        return this;
    }

    public OfferSellerBuilder comment(String comment) {
        this.comment = comment;
        return this;
    }

    public OfferSellerBuilder sellerToUserExchangeRate(BigDecimal sellerToUserExchangeRate) {
        this.sellerToUserExchangeRate = sellerToUserExchangeRate;
        return this;
    }

    public OfferSellerBuilder markupData(MarkupData markupData) {
        this.markupData = markupData;
        return this;
    }

    public OfferSellerBuilder warrantyPeriod(String warrantyPeriod) {
        this.warrantyPeriod = warrantyPeriod;
        return this;
    }

    @Nonnull
    public OfferSeller build() {
        OfferSeller seller = new OfferSeller();
        seller.setPrice(price);
        seller.setCurrency(currency);
        seller.setComment(comment);
        seller.setSellerToUserExchangeRate(sellerToUserExchangeRate);
        seller.setMarkupData(markupData);
        seller.setWarrantyPeriod(warrantyPeriod);
        return seller;
    }

    @Nonnull
    public static OfferSellerBuilder create() {
        return new OfferSellerBuilder();
    }

    @Nonnull
    public static OfferSellerBuilder createFrom(@Nonnull OfferSeller other) {
        return create()
                .price(other.getPrice())
                .currency(other.getCurrency())
                .comment(other.getComment())
                .sellerToUserExchangeRate(other.getSellerToUserExchangeRate())
                .markupData(other.getMarkupData())
                .warrantyPeriod(other.getWarrantyPeriod());
    }

    @Nonnull
    public static OfferSellerBuilder createFrom(@Nonnull OrderItem other) {
        MarkupData markupData = null;
        if (other.getPrices().getPartnerPrice() != null) {
            markupData = new MarkupData();
            markupData.setPartnerPrice(other.getPrices().getPartnerPrice());
        }
        return create()
                .price(other.getPrice())
                .currency(Currency.RUR)
                .markupData(markupData)
                .warrantyPeriod(other.getSellerWarrantyPeriod());
    }
}
