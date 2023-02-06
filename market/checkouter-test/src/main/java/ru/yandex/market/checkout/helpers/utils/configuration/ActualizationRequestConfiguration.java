package ru.yandex.market.checkout.helpers.utils.configuration;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.checkouter.order.UserGroup;

public class ActualizationRequestConfiguration {

    private boolean sandbox;
    private Platform platform;
    private HitRateGroup hitRateGroup;
    private Context context;
    private UserGroup userGroup;
    private Color color;
    private boolean debugAllCourierOptions;
    private Boolean mockLoyaltyDegradation;
    private boolean isYandexEmployee;
    private String experiments;
    private String testBuckets;
    private LocalDateTime unfreezeStocksTime;
    private boolean yandexPlus;
    private boolean userHasPrime;
    private String marketRequestId;
    private boolean minifyOutlets;
    private boolean simplifyOutlets;
    private String metaInfo;
    private Boolean skipDiscountCalculation;
    private Long forceShipmentDay;
    private ApiSettings apiSettings;
    private String perkPromoId;
    private Set<Long> priceDropMskuSet;
    private boolean reducePictures;
    private boolean showCredits;
    private boolean showCreditBroker;
    private boolean showInstallments;
    private Boolean showMultiServiceIntervals;
    private Long forceDeliveryId;
    private String perks;
    private Boolean useInternalPromocode;
    private boolean showSbp;
    private boolean showVat;
    private String googleServiceId;
    private String iosDeviceId;
    private Collection<String> bnplFeatures;
    private boolean spreadAlgorithmV2;
    private String icookie;
    private boolean calculateOrdersSeparately;
    private boolean isOptionalRulesEnabled;

    public boolean isSandbox() {
        return sandbox;
    }

    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
    }

    public boolean isReducePictures() {
        return reducePictures;
    }

    public void setReducePictures(boolean reducePictures) {
        this.reducePictures = reducePictures;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public HitRateGroup getHitRateGroup() {
        return hitRateGroup;
    }

    public void setHitRateGroup(HitRateGroup hitRateGroup) {
        this.hitRateGroup = hitRateGroup;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public UserGroup getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isDebugAllCourierOptions() {
        return debugAllCourierOptions;
    }

    public void setDebugAllCourierOptions(boolean debugAllCourierOptions) {
        this.debugAllCourierOptions = debugAllCourierOptions;
    }

    public Boolean getMockLoyaltyDegradation() {
        return mockLoyaltyDegradation;
    }

    public void setMockLoyaltyDegradation(Boolean mockLoyaltyDegradation) {
        this.mockLoyaltyDegradation = mockLoyaltyDegradation;
    }


    public boolean isYandexEmployee() {
        return isYandexEmployee;
    }

    public void setYandexEmployee(boolean yandexEmployee) {
        isYandexEmployee = yandexEmployee;
    }

    public void setICookie(String value) {
        icookie = value;
    }

    public String getExperiments() {
        return experiments;
    }

    public void setExperiments(String experiments) {
        this.experiments = experiments;
    }

    public String getTestBuckets() {
        return testBuckets;
    }

    public void setTestBuckets(String testBuckets) {
        this.testBuckets = testBuckets;
    }

    public LocalDateTime getUnfreezeStocksTime() {
        return unfreezeStocksTime;
    }

    public void setUnfreezeStocksTime(LocalDateTime unfreezeStocksTime) {
        this.unfreezeStocksTime = unfreezeStocksTime;
    }

    public boolean isYandexPlus() {
        return yandexPlus;
    }

    public void setYandexPlus(boolean yandexPlus) {
        this.yandexPlus = yandexPlus;
    }

    public boolean isUserHasPrime() {
        return userHasPrime;
    }

    public void setUserHasPrime(boolean userHasPrime) {
        this.userHasPrime = userHasPrime;
    }

    public String getMarketRequestId() {
        return marketRequestId;
    }

    public String getICookie() {
        return icookie;
    }

    public void setMarketRequestId(String marketRequestId) {
        this.marketRequestId = marketRequestId;
    }

    public boolean isMinifyOutlets() {
        return minifyOutlets;
    }

    public void setMinifyOutlets(boolean minifyOutlets) {
        this.minifyOutlets = minifyOutlets;
    }

    public boolean isSimplifyOutlets() {
        return simplifyOutlets;
    }

    public void setSimplifyOutlets(boolean simplifyOutlets) {
        this.simplifyOutlets = simplifyOutlets;
    }

    public String getMetaInfo() {
        return metaInfo;
    }

    public void setMetaInfo(String metaInfo) {
        this.metaInfo = metaInfo;
    }

    public Boolean getSkipDiscountCalculation() {
        return skipDiscountCalculation;
    }

    public void setSkipDiscountCalculation(Boolean skipDiscountCalculation) {
        this.skipDiscountCalculation = skipDiscountCalculation;
    }

    public Long getForceShipmentDay() {
        return forceShipmentDay;
    }

    public void setForceShipmentDay(Long forceShipmentDay) {
        this.forceShipmentDay = forceShipmentDay;
    }

    public ApiSettings getApiSettings() {
        return apiSettings;
    }

    public void setApiSettings(ApiSettings apiSettings) {
        this.apiSettings = apiSettings;
    }

    public String getPerkPromoId() {
        return perkPromoId;
    }

    public void setPerkPromoId(String perkPromoId) {
        this.perkPromoId = perkPromoId;
    }

    public boolean isShowCredits() {
        return showCredits;
    }

    public void setShowCredits(boolean showCredits) {
        this.showCredits = showCredits;
    }

    public boolean isShowCreditBroker() {
        return showCreditBroker;
    }

    public void setShowCreditBroker(boolean showCreditBroker) {
        this.showCreditBroker = showCreditBroker;
    }

    public boolean isShowInstallments() {
        return showInstallments;
    }

    public void setShowInstallments(boolean showInstallments) {
        this.showInstallments = showInstallments;
    }

    public Boolean getShowMultiServiceIntervals() {
        return showMultiServiceIntervals;
    }

    public void setShowMultiServiceIntervals(Boolean showMultiServiceIntervals) {
        this.showMultiServiceIntervals = showMultiServiceIntervals;
    }

    public Long getForceDeliveryId() {
        return forceDeliveryId;
    }

    public void setForceDeliveryId(Long forceDeliveryId) {
        this.forceDeliveryId = forceDeliveryId;
    }

    @Nonnull
    public Set<Long> getPriceDropMskuSet() {
        return Objects.requireNonNullElseGet(priceDropMskuSet, Set::of);
    }

    public void setPriceDropMskuSet(Set<Long> priceDropMskuSet) {
        this.priceDropMskuSet = priceDropMskuSet;
    }

    public void addPriceDropMsku(@Nonnull Long priceDropMsku) {
        if (priceDropMskuSet == null) {
            priceDropMskuSet = new HashSet<>();
        }
        priceDropMskuSet.add(priceDropMsku);
    }

    public String getPerks() {
        return perks;
    }

    public ActualizationRequestConfiguration setPerks(String perks) {
        this.perks = perks;
        return this;
    }

    public Boolean getUseInternalPromocode() {
        return useInternalPromocode;
    }

    public ActualizationRequestConfiguration setUseInternalPromocode(Boolean useInternalPromocode) {
        this.useInternalPromocode = useInternalPromocode;
        return this;
    }

    public boolean isShowSbp() {
        return showSbp;
    }

    public void setShowSbp(boolean showSbp) {
        this.showSbp = showSbp;
    }

    public boolean isShowVat() {
        return showVat;
    }

    public void setShowVat(boolean showVat) {
        this.showVat = showVat;
    }

    public String getGoogleServiceId() {
        return googleServiceId;
    }

    public void setGoogleServiceId(String googleServiceId) {
        this.googleServiceId = googleServiceId;
    }

    public String getIosDeviceId() {
        return iosDeviceId;
    }

    public void setIosDeviceId(String iosDeviceId) {
        this.iosDeviceId = iosDeviceId;
    }

    public Collection<String> getBnplFeatures() {
        return bnplFeatures;
    }

    public void setBnplFeatures(Collection<String> bnplFeatures) {
        this.bnplFeatures = bnplFeatures;
    }

    public boolean isSpreadAlgorithmV2() {
        return spreadAlgorithmV2;
    }

    public void setSpreadAlgorithmV2(boolean spreadAlgorithmV2) {
        this.spreadAlgorithmV2 = spreadAlgorithmV2;
    }

    public boolean isCalculateOrdersSeparately() {
        return calculateOrdersSeparately;
    }

    public void setCalculateOrdersSeparately(boolean calculateOrdersSeparately) {
        this.calculateOrdersSeparately = calculateOrdersSeparately;
    }

    public boolean isOptionalRulesEnabled() {
        return isOptionalRulesEnabled;
    }

    public void setIsOptionalRulesEnabled(boolean isOptionalRulesEnabled) {
        this.isOptionalRulesEnabled = isOptionalRulesEnabled;
    }
}
