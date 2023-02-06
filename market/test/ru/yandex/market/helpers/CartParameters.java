package ru.yandex.market.helpers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.report.indexer.model.OfferDetails;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.outlet.Outlet;
import ru.yandex.market.providers.CartRequestProvider;
import ru.yandex.market.providers.OutletProvider;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.shopadminstub.model.Item;
import ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils;
import ru.yandex.market.util.report.ReportGeoParameters;
import ru.yandex.market.util.report.ReportParameters;


public class CartParameters {

    public static final long DEFAULT_SHOP_ID = 242102;
    private static final String DEFAULT_WARE_MD5 = "4wTSrqUBspf3hkJrw6Peww";

    private final long shopId;

    private CartRequest cartRequest;
    private ReportParameters reportParameters;
    private ReportGeoParameters reportGeoParameters;
    private LocalDateTime fakeNow;
    private List<Pair<FeedOfferId, OfferDetails>> feedDispatcherOffers;

    public CartParameters() {
        this(DEFAULT_SHOP_ID, CartRequestProvider.buildCartRequest());
    }

    public CartParameters(CartRequest cartRequest) {
        this(DEFAULT_SHOP_ID, cartRequest);
    }

    public CartParameters(long shopId, @Nonnull CartRequest cartRequest) {
        this.cartRequest = Objects.requireNonNull(cartRequest);
        this.shopId = shopId;
        init();
    }

    private void init() {
        if (reportParameters == null) {
            reportParameters = new ReportParameters(shopId);
            reportParameters.setCartRequest(cartRequest);
        }

        if (reportGeoParameters == null) {
            List<Outlet> outlets = Arrays.asList(OutletProvider.buildFirst(), OutletProvider.buildSecond());

            reportGeoParameters = new ReportGeoParameters();
            reportGeoParameters.setShopId(shopId);
            reportGeoParameters.setResourceUrl(DEFAULT_WARE_MD5, outlets);
        }

        if (feedDispatcherOffers == null) {
            feedDispatcherOffers = mapRequestToFeedDispatcherOffers(cartRequest);
        }
    }

    public long getShopId() {
        return shopId;
    }

    public ReportParameters getReportParameters() {
        return reportParameters;
    }

    public ReportGeoParameters getReportGeoParameters() {
        return reportGeoParameters;
    }

    public CartRequest getCartRequest() {
        return cartRequest;
    }

    public LocalDateTime getFakeNow() {
        return fakeNow;
    }

    public void setFakeNow(LocalDateTime fakeNow) {
        this.fakeNow = fakeNow;
    }

    public List<Pair<FeedOfferId, OfferDetails>> getFeedDispatcherOffers() {
        return feedDispatcherOffers;
    }

    public void setFeedDispatcherOffers(List<Pair<FeedOfferId, OfferDetails>> feedDispatcherOffers) {
        this.feedDispatcherOffers = feedDispatcherOffers;
    }

    private static List<Pair<FeedOfferId, OfferDetails>> mapRequestToFeedDispatcherOffers(CartRequest cartRequest) {
        return cartRequest.getItems().entrySet()
                .stream()
                .map(e -> {
                    FeedOfferId feedOfferId = e.getKey().getFeedOfferPart();
                    Item item = e.getValue();

                    OfferDetails offerDetails = StubPushApiTestUtils.mapItemToOfferDetails(item);

                    return Pair.of(feedOfferId, offerDetails);
                })
                .distinct()
                .collect(Collectors.toList());
    }

}
