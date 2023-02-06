package ru.yandex.market.checkout.helpers.utils.configuration;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.util.balance.TrustParameters;
import ru.yandex.market.checkout.util.geocoder.GeocoderParameters;
import ru.yandex.market.checkout.util.loyalty.LoyaltyParameters;

public class MultiCartMockConfiguration {

    private final Map<Long, ShopMetaData> shopMetaData = new HashMap<>();
    private final LoyaltyParameters loyaltyParameters = new LoyaltyParameters();
    private final GeocoderParameters geocoderParameters = new GeocoderParameters();
    private final TrustParameters trustParameters = new TrustParameters();

    private boolean mockPushApi = true;
    private boolean mockLoyalty = false;

    public Map<Long, ShopMetaData> getShopMetaData() {
        return shopMetaData;
    }

    public void addShopMetaData(Long shopId, ShopMetaData shopMetaData) {
        this.shopMetaData.put(shopId, shopMetaData);
    }

    @Nonnull
    public LoyaltyParameters getLoyaltyParameters() {
        return loyaltyParameters;
    }

    @Nonnull
    public TrustParameters getTrustParameters() {
        return trustParameters;
    }

    public boolean mockPushApi() {
        return mockPushApi;
    }

    public void setMockPushApi(boolean mockPushApi) {
        this.mockPushApi = mockPushApi;
    }

    public boolean mockLoyalty() {
        return mockLoyalty;
    }

    public void setMockLoyalty(boolean mockLoyalty) {
        this.mockLoyalty = mockLoyalty;
    }

    @Nonnull
    public GeocoderParameters getGeocoderParameters() {
        return geocoderParameters;
    }
}
