package ru.yandex.market.mboc.common.utils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;

import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;

/**
 * Содержит компараторы, которые необходимо использовать для {@link Offer#equals(Object)}
 * для использования в {@link org.assertj.core.api.Assertions}.
 *
 * @author s-ermakov
 */
public class MbocComparators {

    public static final Comparator<Offer> OFFERS_GENERAL = (o1, o2) -> equalsGeneral(o1, o2) ? 0 : 1;
    public static final Comparator<Offer> OFFERS_WITH_CREATED_UPDATED =
        (o1, o2) -> equalsWithCreatedUpdated(o1, o2) ? 0 : 1;

    public static final Comparator<Offer.Mapping> OFFERS_MAPPING_COMPARATOR =
        (m1, m2) -> equalsMappings(m1, m2) ? 0 : 1;

    public static final Comparator<MboUser> MBO_USER_COMPARATOR =
        (o1, o2) -> equalsMboUsers(o1, o2) ? 0 : 1;

    public static final Comparator<CategoryInfo> CATEGORY_INFO_COMPARATOR =
        (o1, o2) -> equalsCategoryInfo(o1, o2) ? 0 : 1;

    public static final Comparator<LocalDateTime> LOCAL_DATE_TIME_COMPARATOR =
        (o1, o2) -> equalsLocalDateTime(o1, o2) ? 0 : 1;

    private MbocComparators() {
    }

    // generalEquals, потому что equals лучше не использовать
    public static boolean equalsGeneral(Offer offer1, Offer offer2) {
        boolean equals = EqualsBuilder.reflectionEquals(offer1, offer2,
            "created", "updated", "lastVersion", "supplierSkuMappingCheckTs",
            "suggestSkuMapping", "approvedSkuMapping", "contentSkuMapping", "supplierSkuMapping",
            "acceptanceStatusModified", "processingStatusModified");
        equals = equals && equalsByMappings(offer1, offer2);
        equals = equals && equalsLocalDateTime(offer1.getCreated(), offer2.getCreated());
        equals = equals && equalsLocalDateTime(offer1.getUpdated(), offer2.getUpdated());
        equals = equals && equalsLocalDateTime(offer1.getSupplierSkuMappingCheckTs(),
            offer2.getSupplierSkuMappingCheckTs());
        equals = equals && equalsLocalDateTime(offer1.getAcceptanceStatusModified(),
            offer2.getAcceptanceStatusModified());
        equals = equals && equalsLocalDateTime(offer1.getProcessingStatusModified(),
            offer2.getProcessingStatusModified());
        return equals;
    }

    public static boolean equalsWithCreatedUpdated(Offer offer1, Offer offer2) {
        boolean equals = EqualsBuilder.reflectionEquals(offer1, offer2,
            "lastVersion", "supplierSkuMappingCheckTs",
            "suggestSkuMapping", "approvedSkuMapping", "contentSkuMapping", "supplierSkuMapping",
            "acceptanceStatusModified", "processingStatusModified");
        equals = equals && equalsByMappings(offer1, offer2);
        equals = equals && equalsLocalDateTime(offer1.getSupplierSkuMappingCheckTs(),
            offer2.getSupplierSkuMappingCheckTs());
        return equals;
    }

    public static boolean equalsMappings(Offer.Mapping m1, Offer.Mapping m2) {
        if (m1 == m2) {
            return true;
        }
        if (m1 == null || m2 == null) {
            return false;
        }

        long id1 = m1.getMappingId();
        long id2 = m2.getMappingId();

        return id1 == id2;
    }

    public static boolean equalsMboUsers(MboUser user1, MboUser user2) {
        if (user1 == user2) {
            return true;
        }
        if (user1 == null) {
            return false;
        }
        return user1.getUid() == user2.getUid() &&
            Objects.equals(user1.getFullName(), user2.getFullName()) &&
            Objects.equals(user1.getYandexLogin(), user2.getYandexLogin()) &&
            Objects.equals(user1.getStaffLogin(), user2.getStaffLogin()) &&
            Objects.equals(user1.getStaffFullname(), user2.getStaffFullname());
    }

    public static boolean equalsCategoryInfo(CategoryInfo categoryInfo1, CategoryInfo categoryInfo2) {
        if (categoryInfo1 == categoryInfo2) {
            return true;
        }
        if (categoryInfo1 == null) {
            return false;
        }
        return categoryInfo1.getCategoryId() == categoryInfo2.getCategoryId() &&
            Objects.equals(categoryInfo1.getContentManagerUid(), categoryInfo2.getContentManagerUid()) &&
            Objects.equals(categoryInfo1.getInputManagerUid(), categoryInfo2.getInputManagerUid()) &&
            Objects.equals(categoryInfo1.isModerationInYang(), categoryInfo2.isModerationInYang());
    }

    private static boolean equalsByMappings(Offer o1, Offer o2) {
        return equalsMappings(o1.getApprovedSkuMapping(), o2.getApprovedSkuMapping())
            && equalsMappings(o1.getContentSkuMapping(), o2.getContentSkuMapping())
            && equalsMappings(o1.getSuggestSkuMapping(), o2.getSuggestSkuMapping())
            && equalsMappings(o1.getSupplierSkuMapping(), o2.getSupplierSkuMapping());
    }

    private static boolean equalsLocalDateTime(LocalDateTime o1, LocalDateTime o2) {
        // так как дата часто ставится у нас как DateTimeUtils.now(),
        // то время просто в юнит тестах не проверить
        // поэтому проверяем, что даты хотя бы выставлены одинаково
        return o1 == o2 || o1 != null && o2 != null;
    }
}
