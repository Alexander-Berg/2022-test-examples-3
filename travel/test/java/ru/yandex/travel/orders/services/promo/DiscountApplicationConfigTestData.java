package ru.yandex.travel.orders.services.promo;

import java.util.List;

import org.javamoney.moneta.Money;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.promo.DiscountApplicationConfig;
import ru.yandex.travel.orders.entities.promo.HotelRestriction;
import ru.yandex.travel.orders.entities.promo.UserTypeRestriction;

public class DiscountApplicationConfigTestData {
    private DiscountApplicationConfig config = new DiscountApplicationConfig();

    public DiscountApplicationConfigTestData forStaffOnly() {
        config.setUserTypeRestriction(UserTypeRestriction.STAFF_ONLY);
        return this;
    }

    public DiscountApplicationConfigTestData forPlusOnly() {
        config.setUserTypeRestriction(UserTypeRestriction.PLUS_ONLY);
        return this;
    }

    public DiscountApplicationConfigTestData withMinTotalCost(Money minTotalCost) {
        config.setMinTotalCost(minTotalCost);
        return this;
    }

    public DiscountApplicationConfigTestData withMaxAllowedOrders(int maxAllowedOrders) {
        config.setMaxConfirmedHotelOrders(maxAllowedOrders);
        return this;
    }

    public DiscountApplicationConfigTestData withMaxPromo(int rubles) {
        config.setMaxNominalDiscount(Money.of(rubles, ProtoCurrencyUnit.RUB));
        return this;
    }

    public DiscountApplicationConfigTestData withHotelRestrictions(List<HotelRestriction> hotelRestrictions) {
        config.setHotelRestrictions(hotelRestrictions);
        return this;
    }

    public static DiscountApplicationConfigTestData empty() {
        DiscountApplicationConfigTestData builder = new DiscountApplicationConfigTestData();
        return builder;
    }

    public DiscountApplicationConfig build() {
        return config;
    }
}
