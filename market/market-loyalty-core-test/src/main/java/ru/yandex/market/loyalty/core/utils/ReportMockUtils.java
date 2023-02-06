package ru.yandex.market.loyalty.core.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.mockito.stubbing.Answer;
import org.springframework.stereotype.Component;

import ru.yandex.market.common.report.DefaultMarketReportService;
import ru.yandex.market.common.report.GenericMarketReportService;
import ru.yandex.market.common.report.model.Color;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.common.report.model.SkuOffers;
import ru.yandex.market.common.report.model.SkuOffersRequest;
import ru.yandex.market.common.report.parser.json.AbstractReportJsonParser;
import ru.yandex.market.common.report.parser.json.SkuOffersReportJsonParser;
import ru.yandex.market.loyalty.api.model.MarketPlatform;
import ru.yandex.market.loyalty.core.model.order.ItemKey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_PRICE;

/**
 * @author ukchuvrus
 */
@Component
public class ReportMockUtils {
    private static final BigDecimal DEFAULT_PRICE_IN_REPORT = BigDecimal.valueOf(100);
    private static final long DEFAULT_SHOP_ID_IN_REPORT = 4598654L;
    private static final String DEFAULT_NAME_IN_REPORT = "name";
    private static final String DEFAULT_MSKU_IN_REPORT = "789846567";
    private static final int DEFAULT_CATEGORY_IN_REPORT = 564561;
    public static final int DEFAULT_CATEGORY_ID = 1;

    private final DefaultMarketReportService marketReportService;
    private final GenericMarketReportService genericMarketReportService;

    public ReportMockUtils(DefaultMarketReportService marketReportService,
                           GenericMarketReportService genericMarketReportService) {
        this.marketReportService = marketReportService;
        this.genericMarketReportService = genericMarketReportService;
    }

    public static SkuOffers.Offers offerWithName(String name) {
        return offerWithName(name, false);
    }

    public static SkuOffers.Offers offerWithName(String name, boolean includeOffersInResponse) {
        List<FoundOffer> offers = Collections.emptyList();
        if (includeOffersInResponse) {
            offers = Collections.singletonList(offerBuilder().withName(name).build());
        }
        return new SkuOffers.Offers(name, offers);
    }

    @SuppressWarnings({"unchecked"})
    public void mockReportService(FoundOffer... foundOffers) throws IOException, InterruptedException {
        when(marketReportService.executeSearchAndParse(
                any(),
                any(AbstractReportJsonParser.class))
        ).then(invocation -> {
            MarketSearchRequest request = invocation.getArgument(0, MarketSearchRequest.class);
            return Arrays.stream(foundOffers)
                    .filter(offer -> request.getOfferIds().contains(offer.getFeedOfferId()))
                    .collect(Collectors.toList());
        });
    }

    public void mockGenericReportService(
            Matcher<SkuOffersRequest> requestMatcher,
            Answer<SkuOffers> answer
    ) throws IOException, InterruptedException {
        when(genericMarketReportService.executeSearchAndParse(
                argThat(requestMatcher),
                any(SkuOffersReportJsonParser.class)
        )).then(answer);
    }

    public static FoundOffer makeDefaultItem(BigDecimal price) {
        return makeDefaultItem(DEFAULT_ITEM_KEY, price);
    }

    public static FoundOffer makeDefaultItem() {
        return makeDefaultItem(DEFAULT_ITEM_KEY);
    }

    @NotNull
    public static FoundOffer makeDefaultItem(String sku, long vendorId) {
        FoundOffer foundOffer = makeDefaultItem(DEFAULT_ITEM_KEY);
        foundOffer.setVendorId(vendorId);
        foundOffer.setSku(sku);
        return foundOffer;
    }

    @NotNull
    public static FoundOffer makeDefaultItem(ItemKey itemKey) {
        return makeDefaultItem(itemKey, BigDecimal.valueOf(5000));
    }

    @NotNull
    public static FoundOffer makeDefaultItem(ItemKey itemKey, BigDecimal price) {
        return makeDefaultItem(itemKey, null, price);
    }

    @NotNull
    public static FoundOffer makeDefaultItem(Integer categoryId) {
        return makeDefaultItem(DEFAULT_ITEM_KEY, categoryId, DEFAULT_PRICE);
    }

    @NotNull
    public static FoundOffer makeDefaultItem(ItemKey itemKey, Integer categoryId, BigDecimal price) {
        return makeDefaultItem(itemKey, categoryId, price, MarketPlatform.BLUE);
    }

    @NotNull
    public static FoundOffer makeDefaultItem(
            ItemKey itemKey,
            Integer categoryId,
            BigDecimal price,
            MarketPlatform platform
    ) {
        FoundOffer fo = new FoundOffer();
        fo.setFeedId(itemKey.getFeedId());
        fo.setShopOfferId(itemKey.getOfferId());
        fo.setHyperCategoryId(categoryId);
        fo.setPrice(price);
        fo.setHyperCategoryId(DEFAULT_CATEGORY_ID);
        fo.setCargoTypes(Collections.emptySet());
        fo.setWeight(BigDecimal.ONE);
        fo.setRgb(Color.findByValue(platform.getCode()));

        return fo;
    }

    @NotNull
    public static FoundOffer makeLargeDimensionItem(ItemKey itemKey) {
        FoundOffer fo2 = new FoundOffer();
        fo2.setFeedId(itemKey.getFeedId());
        fo2.setShopOfferId(itemKey.getOfferId());
        fo2.setPrice(BigDecimal.valueOf(5000));
        fo2.setHyperCategoryId(DEFAULT_CATEGORY_ID);
        fo2.setCargoTypes(Collections.singleton(300));

        return fo2;
    }

    @NotNull
    public static FoundOffer makeLargeWeightItem(ItemKey itemKey) {
        FoundOffer fo2 = new FoundOffer();
        fo2.setFeedId(itemKey.getFeedId());
        fo2.setShopOfferId(itemKey.getOfferId());
        fo2.setPrice(BigDecimal.valueOf(5000));
        fo2.setHyperCategoryId(DEFAULT_CATEGORY_ID);
        fo2.setWeight(BigDecimal.valueOf(21.0));

        return fo2;
    }

    public static OfferBuilder offerBuilder() {
        return new OfferBuilder();
    }

    public static class OfferBuilder {
        private final FoundOffer offer;

        private OfferBuilder() {
            offer = new FoundOffer();
            offer.setFeedId(DEFAULT_ITEM_KEY.getFeedId());
            offer.setShopOfferId(DEFAULT_ITEM_KEY.getOfferId());
            offer.setCpa20(true);
            offer.setPrice(DEFAULT_PRICE_IN_REPORT);
            offer.setShopId(DEFAULT_SHOP_ID_IN_REPORT);
            offer.setName(DEFAULT_NAME_IN_REPORT);
            offer.setSku(DEFAULT_MSKU_IN_REPORT);
            offer.setHyperCategoryId(DEFAULT_CATEGORY_IN_REPORT);
        }

        public OfferBuilder withItemKey(ItemKey itemKey) {
            offer.setFeedId(itemKey.getFeedId());
            offer.setShopOfferId(itemKey.getOfferId());
            return this;
        }

        public OfferBuilder withName(String name) {
            offer.setName(name);
            return this;
        }

        public OfferBuilder withOldMinPrice(BigDecimal discount) {
            offer.setOldMin(discount);
            return this;
        }

        public OfferBuilder withCategory(int category) {
            offer.setHyperCategoryId(category);
            return this;
        }

        public OfferBuilder withMsku(String msku) {
            offer.setSku(msku);
            return this;
        }

        public OfferBuilder withShopId(long shopId) {
            offer.setShopId(shopId);
            return this;
        }

        public FoundOffer build() {
            return offer;
        }
    }
}
