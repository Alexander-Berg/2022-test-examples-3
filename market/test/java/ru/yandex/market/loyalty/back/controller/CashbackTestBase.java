package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.CashbackPromoRequest;
import ru.yandex.market.loyalty.api.model.PartnerCashbackRequest;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemRequest;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.CategoryTypeGroupReferenceDao;
import ru.yandex.market.loyalty.core.dao.CategoryTypeGroupTariffReferenceDao;
import ru.yandex.market.loyalty.core.dao.CategoryTypeReferenceDao;
import ru.yandex.market.loyalty.core.dao.PartnerCashbackStandardPromoDao;
import ru.yandex.market.loyalty.core.dao.PartnerCashbackVersionDao;
import ru.yandex.market.loyalty.core.dao.PromoKeyIndexDao;
import ru.yandex.market.loyalty.core.logbroker.TskvLogBrokerClient;
import ru.yandex.market.loyalty.core.model.cashback.entity.PromoKeyIndexEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeGroupEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeGroupTariffEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.StandardPromoEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.VersionEntry;
import ru.yandex.market.loyalty.core.model.promo.CashbackPromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.cashback.CashbackCacheService;
import ru.yandex.market.loyalty.core.service.cashback.PartnerCashbackService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.BuildCustomizer;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;

import static ru.yandex.market.loyalty.core.model.cashback.partner.entity.VersionStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public abstract class CashbackTestBase extends MarketLoyaltyBackMockedDbTestBase {
    public static final int HID_WITH_TARIFF = 1000;
    @Autowired
    protected PromoManager promoManager;
    @Autowired
    protected ConfigurationService configurationService;
    @Autowired
    protected PromoKeyIndexDao promoKeyIndexDao;
    @Autowired
    protected PartnerCashbackService partnerCashbackService;
    @Autowired
    protected PartnerCashbackVersionDao partnerCashbackVersionDao;
    @Autowired
    protected CategoryTypeGroupReferenceDao categoryTypeGroupReferenceDao;
    @Autowired
    protected CategoryTypeReferenceDao categoryTypeReferenceDao;
    @Autowired
    protected CategoryTypeGroupTariffReferenceDao categoryTypeGroupTariffReferenceDao;
    @Autowired
    protected PartnerCashbackStandardPromoDao partnerCashbackStandardPromoDao;
    @Autowired
    protected TskvLogBrokerClient logBrokerClient;
    @Autowired
    protected PromoService promoService;
    @Autowired
    protected DiscountService discountService;

    protected Promo createKnownToReportPromo(String reportPromoKey, CashbackPromoBuilder builder) {
        Promo promo = promoManager.createCashbackPromo(builder);
        savePromoKeyToIndex(promo.getPromoKey(), reportPromoKey);
        return promo;
    }

    protected Promo createNotKnownToReportPromo(CashbackPromoBuilder builder) {
        return promoManager.createCashbackPromo(builder);
    }

    protected void registerTariffs() {
        final VersionEntry version = partnerCashbackVersionDao.save(
                VersionEntry.builder()
                        .setId(0)
                        .setStatus(ACTIVE)
                        .setComment("initial")
                        .setCustomCashbackPromoBucketName("test")
                        .setStandardCashbackPromoBucketName("test")
                        .setCustomCashbackPromoPriorityHigh(10000)
                        .setCustomCashbackPromoPriorityLow(0)
                        .build()
        );
        categoryTypeGroupReferenceDao.save(
                CategoryTypeGroupEntry.builder()
                        .setName("default")
                        .build()
        );
        categoryTypeReferenceDao.save(
                CategoryTypeEntry.builder()
                        .setHid(HID_WITH_TARIFF)
                        .setCategoryTypeGroupName("default")
                        .build()
        );
        categoryTypeGroupTariffReferenceDao.save(
                CategoryTypeGroupTariffEntry.builder()
                        .setCategoryTypeGroupName("default")
                        .setDeleted(false)
                        .setMinCashbackNominal(BigDecimal.valueOf(2))
                        .setMaxCashbackNominal(BigDecimal.valueOf(10))
                        .setExtraCashbackThreshold(BigDecimal.valueOf(4))
                        .setPartnerCashbackVersionId(version.getId())
                        .setMarketTariff(BigDecimal.valueOf(1.3))
                        .build()
        );
        partnerCashbackStandardPromoDao.save(StandardPromoEntry.builder()
                .setCodeName("default")
                .setCategoryTypeReferenceGroupName("default")
                .setDefaultCashbackNominal(BigDecimal.valueOf(5))
                .setDescription("")
                .setPartnerCashbackVersionId(version.getId())
                .setPriority(10)
                .build());
    }

    @SuppressWarnings("SameParameterValue")
    @NotNull
    protected BuildCustomizer<BundledOrderItemRequest, OrderRequestUtils.OrderItemBuilder> cashbackPromo(String reportPromoKey, BigDecimal nominal) {
        return cashbackPromo(reportPromoKey, nominal, null);
    }

    @SuppressWarnings("SameParameterValue")
    @NotNull
    protected BuildCustomizer<BundledOrderItemRequest, OrderRequestUtils.OrderItemBuilder> cashbackPromo(String reportPromoKey, BigDecimal nominal, PartnerCashbackRequest partnerCashback) {
        return cashbackPromo(reportPromoKey, nominal, partnerCashback, 1);
    }

    @SuppressWarnings("SameParameterValue")
    @NotNull
    protected BuildCustomizer<BundledOrderItemRequest, OrderRequestUtils.OrderItemBuilder> cashbackPromo(String reportPromoKey, BigDecimal nominal, PartnerCashbackRequest partnerCashback, int priority) {
        return cashbackPromo(reportPromoKey, nominal, partnerCashback, priority, "default");
    }

    @SuppressWarnings("SameParameterValue")
    @NotNull
    protected BuildCustomizer<BundledOrderItemRequest, OrderRequestUtils.OrderItemBuilder> cashbackPromo(String reportPromoKey, BigDecimal nominal, PartnerCashbackRequest partnerCashback, int priority, String promoBucketName) {
        return OrderRequestUtils.cashbackPromo(new CashbackPromoRequest(reportPromoKey,
                nominal,
                priority,
                promoBucketName,
                partnerCashback,
                null,
                null,
                null,
                null,
                null));
    }

    @SuppressWarnings("SameParameterValue")
    protected void savePromoKeyToIndex(String loyaltyPromoKey, String reportPromoKey) {
        promoKeyIndexDao.save(PromoKeyIndexEntry.builder()
                .setLoyaltyPromoKey(loyaltyPromoKey)
                .setReportPromoKey(reportPromoKey)
                .build()
        );
    }

    protected void configureReportCashback(boolean preferLoayltyPromosIfReportPromoKnown) {
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.CASHBACK_PROMOS_FROM_REPORT_ENABLED, true);
        configurationService.set(ConfigurationService.IF_REPORT_PROMO_KNOWN_LOYALTY_PROMO_PREFERRED,
                preferLoayltyPromosIfReportPromoKnown);
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
    }

    protected void configureReportCashback(Long uid) {
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.CASHBACK_PROMOS_FROM_REPORT_ENABLED_UIDS, uid);
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        BlackboxUtils.mockBlackbox(DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
    }


    /**
     * прямая шкала
     * чем выше цифра тем выше приоритет акции
     */
    protected static class DirectScale {
        /**
         * шкала лоялти обратная: чем ниже тем приоритетнее
         *
         * @param priority
         * @return
         */
        public static int asLoyaltyPriority(int priority) {
            return -priority;
        }

        /**
         * шкала репорта прямая: чем выше тем приоритетнее
         *
         * @param priority
         * @return
         */
        public static int asReportPriority(int priority) {
            return priority;
        }
    }
}
