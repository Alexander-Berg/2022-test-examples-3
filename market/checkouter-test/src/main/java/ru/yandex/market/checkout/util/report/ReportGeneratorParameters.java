package ru.yandex.market.checkout.util.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.ObjectUtils;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.AvailableDeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.DeliveryMethod;
import ru.yandex.market.common.report.model.DeliveryRoute;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.LocalDeliveryOption;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.PickupOption;
import ru.yandex.market.common.report.model.json.credit.CreditInfo;


/**
 * @author Nikolai Iusiumbeli
 * date: 10/07/2017
 */
public class ReportGeneratorParameters {

    private final Set<MarketReportPlace> skipAutoConfiguration = new HashSet<>();
    private long regionId;
    private Order order;
    private List<FoundOffer> offers;
    private boolean shopSupportsSubsidies = false;
    private boolean isPriorityRegion = false;
    private long shopPriorityRegionId = 213;
    private String reportFiltersValue;
    private boolean global = false;
    private Map<FeedOfferId, List<LocalDeliveryOption>> localDeliveryOptions;
    private Map<FeedOfferId, List<PickupOption>> postOptions;
    private Map<FeedOfferId, List<PickupOption>> pickupOptions;
    private Currency shopCurrency = Currency.RUR;
    private Currency buyerCurrency = Currency.RUR;
    private BigDecimal shopToUserConvertRate = new BigDecimal("1");
    private final Map<Pair<Currency, Currency>, BigDecimal> currencyRates = Maps.newHashMap(ImmutableMap.of(
            Pair.of(Currency.RUR, Currency.RUR), BigDecimal.ONE,
            Pair.of(Currency.USD, Currency.USD), BigDecimal.ONE,
            Pair.of(Currency.EUR, Currency.EUR), BigDecimal.ONE
    ));

    // place=shop_info:
    private Currency deliveryCurrency = Currency.RUR;
    private final Map<FeedOfferId, ItemInfo> orderItemsOverride = new HashMap<>();
    private String sellerComment;
    private String shopReturnDeliveryAddress;
    private String returnPolicy;
    // позволяет переопределить shopId в ответе репорта.
    private Long responseShopId = null;

    // инфа для мока actual_delivery
    private ActualDelivery actualDelivery;
    private boolean minifyOutlets;
    private List<DeliveryMethod> deliveryMethods = new ArrayList<>() {{
        add(new DeliveryMethod("100501", true));
        add(new DeliveryMethod("100502", true));
        add(new DeliveryMethod("110510", true));
        add(new DeliveryMethod("123", true));
        add(new DeliveryMethod("99", null));
    }};
    private VatType itemVat = VatType.VAT_18;
    private VatType deliveryVat = VatType.VAT_10;
    private boolean prime;
    private boolean yandexPlus;
    private boolean downloadable;
    private String experiments;
    private boolean configurePreciseActualDelivery = true;
    private boolean ignoreStocks = true;
    private Map<String, String> extraActualDeliveryParams = Collections.emptyMap();
    private List<String> deliveryPartnerTypes = Arrays.asList("YANDEX_MARKET", "SHOP");
    private Boolean isEda;
    private String foodtechType;
    private Boolean isExpress;
    private Boolean largeSize;
    private CreditInfo creditInfo;
    private boolean isCrossborder;
    private boolean loyaltyProgramPartner;
    private final Map<MarketReportPlace, Consumer<MappingBuilder>> mappingBuilderModifiers = new HashMap<>();
    private DeliveryRoute deliveryRoute;

    private boolean isYaSubscriptionOffer;
    private Boolean uniqueOffer;

    private Set<AvailableDeliveryType> availableDeliveryTypes;

    public ReportGeneratorParameters(List<FoundOffer> offers) {
        this.offers = offers;
    }

    public ReportGeneratorParameters(Order order, ActualDelivery actualDelivery) {
        this(order);
        this.actualDelivery = actualDelivery;
    }

    public ReportGeneratorParameters(Order order, ActualDelivery actualDelivery, DeliveryRoute deliveryRoute) {
        this(order, actualDelivery);
        this.deliveryRoute = deliveryRoute;
    }

    public ReportGeneratorParameters(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    public List<FoundOffer> getOffers() {
        return offers;
    }

    public void setOffers(List<FoundOffer> offers) {
        this.offers = offers;
    }

    public boolean isShopSupportsSubsidies() {
        return shopSupportsSubsidies;
    }

    public void setShopSupportsSubsidies(boolean shopSupportsSubsidies) {
        this.shopSupportsSubsidies = shopSupportsSubsidies;
    }

    public boolean isPriorityRegion() {
        return isPriorityRegion;
    }

    public VatType getItemVat() {
        return itemVat;
    }

    public void setItemVat(VatType itemVat) {
        this.itemVat = itemVat;
    }

    public VatType getDeliveryVat() {
        return deliveryVat;
    }

    public void setDeliveryVat(VatType deliveryVat) {
        this.deliveryVat = deliveryVat;
    }

    public void setPriorityRegionEnabled(boolean enabled) {
        isPriorityRegion = enabled;
    }

    public long getShopPriorityRegionId() {
        return shopPriorityRegionId;
    }

    public void setShopPriorityRegionId(long shopPriorityRegionId) {
        this.shopPriorityRegionId = shopPriorityRegionId;
    }

    public boolean isGlobal() {
        return global;
    }

    public void setGlobal(boolean global) {
        this.global = global;
    }

    public String getReportFiltersValue() {
        return reportFiltersValue;
    }

    public void setReportFiltersValue(String reportFiltersValue) {
        this.reportFiltersValue = reportFiltersValue;
    }


    public String getSellerComment() {
        return sellerComment;
    }

    public void setSellerComment(String sellerComment) {
        this.sellerComment = sellerComment;
    }

    public String getShopReturnDeliveryAddress() {
        return shopReturnDeliveryAddress;
    }

    public void setShopReturnDeliveryAddress(String shopReturnDeliveryAddress) {
        this.shopReturnDeliveryAddress = shopReturnDeliveryAddress;
    }

    public String getReturnPolicy() {
        return returnPolicy;
    }

    public void setReturnPolicy(String returnPolicy) {
        this.returnPolicy = returnPolicy;
    }

    public Map<FeedOfferId, List<LocalDeliveryOption>> getLocalDeliveryOptions() {
        return localDeliveryOptions;
    }

    public void setLocalDeliveryOptions(Map<FeedOfferId, List<LocalDeliveryOption>> localDeliveryOptions) {
        this.localDeliveryOptions = localDeliveryOptions;
    }

    public Map<FeedOfferId, List<PickupOption>> getPostOptions() {
        return postOptions;
    }

    public void setPostOptions(Map<FeedOfferId, List<PickupOption>> postOptions) {
        this.postOptions = postOptions;
    }

    public Map<FeedOfferId, List<PickupOption>> getPickupOptions() {
        return pickupOptions;
    }

    public void setPickupOptions(Map<FeedOfferId, List<PickupOption>> pickupOptions) {
        this.pickupOptions = pickupOptions;
    }

    public Currency getShopCurrency() {
        return shopCurrency;
    }

    public void setShopCurrency(Currency shopCurrency) {
        this.shopCurrency = shopCurrency;
    }

    public BigDecimal getShopToUserConvertRate() {
        return shopToUserConvertRate;
    }

    public void setShopToUserConvertRate(BigDecimal shopToUserConvertRate) {
        this.shopToUserConvertRate = shopToUserConvertRate;
    }

    public Currency getDeliveryCurrency() {
        return deliveryCurrency;
    }

    public void setDeliveryCurrency(Currency deliveryCurrency) {
        this.deliveryCurrency = deliveryCurrency;
    }

    public Map<Pair<Currency, Currency>, BigDecimal> getCurrencyRates() {
        return currencyRates;
    }

    public ItemInfo overrideItemInfo(FeedOfferId feedOfferId) {
        return orderItemsOverride.computeIfAbsent(feedOfferId, key -> new ItemInfo());
    }

    public Map<FeedOfferId, ItemInfo> getOrderItemsOverride() {
        return orderItemsOverride;
    }

    public Long getResponseShopId() {
        return ObjectUtils.firstNonNull(responseShopId, order.getShopId());
    }

    public void setResponseShopId(Long responseShopId) {
        this.responseShopId = responseShopId;
    }

    public ActualDelivery getActualDelivery() {
        return actualDelivery;
    }

    public void setActualDelivery(ActualDelivery actualDelivery) {
        this.actualDelivery = actualDelivery;
    }

    public boolean isMinifyOutlets() {
        return minifyOutlets;
    }

    public void setMinifyOutlets(boolean minifyOutlets) {
        this.minifyOutlets = minifyOutlets;
    }

    public List<DeliveryMethod> getDeliveryMethods() {
        return deliveryMethods;
    }

    public void setDeliveryMethods(List<DeliveryMethod> deliveryMethods) {
        this.deliveryMethods = deliveryMethods;
    }

    public long getRegionId() {
        return regionId;
    }

    public void setRegionId(long regionId) {
        this.regionId = regionId;
    }

    public boolean isPrime() {
        return prime;
    }

    public void setPrime(boolean prime) {
        this.prime = prime;
    }

    public boolean isYandexPlus() {
        return yandexPlus;
    }

    public void setYandexPlus(boolean yandexPlus) {
        this.yandexPlus = yandexPlus;
    }

    public boolean isDownloadable() {
        return downloadable;
    }

    public void setDownloadable(boolean downloadable) {
        this.downloadable = downloadable;
    }

    public Currency getBuyerCurrency() {
        return buyerCurrency;
    }

    public void setBuyerCurrency(Currency buyerCurrency) {
        this.buyerCurrency = buyerCurrency;
    }

    public String getExperiments() {
        return experiments;
    }

    public void setExperiments(String experiments) {
        this.experiments = experiments;
    }

    public boolean isConfigurePreciseActualDelivery() {
        return configurePreciseActualDelivery;
    }

    public void setConfigurePreciseActualDelivery(boolean configurePreciseActualDelivery) {
        this.configurePreciseActualDelivery = configurePreciseActualDelivery;
    }

    public boolean isIgnoreStocks() {
        return ignoreStocks;
    }

    public void setIgnoreStocks(boolean ignoreStocks) {
        this.ignoreStocks = ignoreStocks;
    }

    public Map<String, String> getExtraActualDeliveryParams() {
        return extraActualDeliveryParams;
    }

    public void setExtraActualDeliveryParams(Map<String, String> extraActualDeliveryParams) {
        this.extraActualDeliveryParams = extraActualDeliveryParams;
    }

    public List<String> getDeliveryPartnerTypes() {
        return deliveryPartnerTypes;
    }

    public void setDeliveryPartnerTypes(List<String> deliveryPartnerTypes) {
        this.deliveryPartnerTypes = deliveryPartnerTypes;
    }

    public Boolean isEda() {
        return isEda;
    }

    public void setIsEda(Boolean isEda) {
        this.isEda = isEda;
    }

    public String getFoodtechType() {
        return foodtechType;
    }

    public void setFoodtechType(String foodtechType) {
        this.foodtechType = foodtechType;
    }

    public Boolean isLargeSize() {
        return largeSize;
    }

    public void setLargeSize(Boolean largeSize) {
        this.largeSize = largeSize;
    }

    public Boolean isExpress() {
        return isExpress;
    }

    public void setIsExpress(Boolean express) {
        isExpress = express;
    }

    public CreditInfo getCreditInfo() {
        return creditInfo;
    }

    public void setCreditInfo(CreditInfo creditInfo) {
        this.creditInfo = creditInfo;
    }

    public boolean isCrossborder() {
        return isCrossborder;
    }

    public void setCrossborder(boolean crossborder) {
        this.isCrossborder = crossborder;
    }

    public boolean isLoyaltyProgramPartner() {
        return loyaltyProgramPartner;
    }

    public void setLoyaltyProgramPartner(boolean loyaltyProgramPartner) {
        this.loyaltyProgramPartner = loyaltyProgramPartner;
    }

    public Optional<Consumer<MappingBuilder>> getMappingBuilderModifier(MarketReportPlace place) {
        return Optional.ofNullable(mappingBuilderModifiers.get(place));
    }

    public void addMappingBuilderModifier(MarketReportPlace place, Consumer<MappingBuilder> mappingBuilderModifier) {
        this.mappingBuilderModifiers.put(place, mappingBuilderModifier);
    }

    public DeliveryRoute getDeliveryRoute() {
        return deliveryRoute;
    }

    public void setDeliveryRoute(DeliveryRoute deliveryRoute) {
        this.deliveryRoute = deliveryRoute;
    }

    public boolean isYaSubscriptionOffer() {
        return isYaSubscriptionOffer;
    }

    public void setYaSubscriptionOffer(boolean yaSubscriptionOffer) {
        isYaSubscriptionOffer = yaSubscriptionOffer;
    }

    public Set<AvailableDeliveryType> getAvailableDeliveryTypes() {
        return availableDeliveryTypes;
    }

    public void setAvailableDeliveryTypes(Set<AvailableDeliveryType> availableDeliveryTypes) {
        this.availableDeliveryTypes = availableDeliveryTypes;
    }

    public void skip(@Nonnull MarketReportPlace place) {
        skipAutoConfiguration.add(place);
    }

    @Nonnull
    public Set<MarketReportPlace> getSkipAutoConfiguration() {
        return skipAutoConfiguration;
    }

    public Boolean getUniqueOffer() {
        return uniqueOffer;
    }

    public void setUniqueOffer(Boolean uniqueOffer) {
        this.uniqueOffer = uniqueOffer;
    }
}
