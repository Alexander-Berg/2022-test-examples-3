package ru.yandex.market.loyalty.core.utils;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import ru.yandex.market.loyalty.core.dao.CategoryTypeGroupReferenceDao;
import ru.yandex.market.loyalty.core.dao.CategoryTypeGroupTariffReferenceDao;
import ru.yandex.market.loyalty.core.dao.CategoryTypeReferenceDao;
import ru.yandex.market.loyalty.core.dao.PartnerCashbackStandardPromoDao;
import ru.yandex.market.loyalty.core.dao.PartnerCashbackVersionDao;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeGroupEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.CategoryTypeGroupTariffEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.StandardPromoEntry;
import ru.yandex.market.loyalty.core.model.cashback.partner.entity.VersionEntry;

import static ru.yandex.market.loyalty.core.model.cashback.partner.entity.VersionStatus.ACTIVE;

@Service
public class PartnerCashbackUtil {
    private final PartnerCashbackVersionDao partnerCashbackVersionDao;
    private final CategoryTypeGroupReferenceDao categoryTypeGroupReferenceDao;
    private final CategoryTypeReferenceDao categoryTypeReferenceDao;
    private final CategoryTypeGroupTariffReferenceDao categoryTypeGroupTariffReferenceDao;
    private final PartnerCashbackStandardPromoDao partnerCashbackStandardPromoDao;

    public PartnerCashbackUtil(PartnerCashbackVersionDao partnerCashbackVersionDao,
                               CategoryTypeGroupReferenceDao categoryTypeGroupReferenceDao,
                               CategoryTypeReferenceDao categoryTypeReferenceDao,
                               CategoryTypeGroupTariffReferenceDao categoryTypeGroupTariffReferenceDao,
                               PartnerCashbackStandardPromoDao partnerCashbackStandardPromoDao) {
        this.partnerCashbackVersionDao = partnerCashbackVersionDao;
        this.categoryTypeGroupReferenceDao = categoryTypeGroupReferenceDao;
        this.categoryTypeReferenceDao = categoryTypeReferenceDao;
        this.categoryTypeGroupTariffReferenceDao = categoryTypeGroupTariffReferenceDao;
        this.partnerCashbackStandardPromoDao = partnerCashbackStandardPromoDao;
    }

    public void registerTariffs() {
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
                        .setHid(1000)
                        .setCategoryTypeGroupName("default")
                        .build()
        );
        categoryTypeGroupTariffReferenceDao.save(
                CategoryTypeGroupTariffEntry.builder()
                        .setCategoryTypeGroupName("default")
                        .setDeleted(false)
                        .setMinCashbackNominal(BigDecimal.valueOf(1))
                        .setMaxCashbackNominal(BigDecimal.valueOf(10))
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
}
