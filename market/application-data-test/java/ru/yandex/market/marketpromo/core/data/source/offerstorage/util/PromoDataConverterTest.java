package ru.yandex.market.marketpromo.core.data.source.offerstorage.util;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;

import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampPromo;
import Market.DataCamp.DataCampPromo.PromoAdditionalInfo.PromoStatus;
import Market.DataCamp.DataCampPromo.PromoConstraints.OffersMatchingRule;
import NMarket.Common.Promo.Promo.ESourceType;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.model.CheapestAsGiftProperties;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.PromoKey;
import ru.yandex.market.mbi.promo.PromoInfo;
import ru.yandex.market.mbi.promo.PromoInfo.OffersMatchingRule.CategoryRestriction;
import ru.yandex.market.mbi.promo.PromoInfo.OffersMatchingRule.IntList;
import ru.yandex.market.mbi.promo.PromoInfo.OffersMatchingRule.WarehouseRestriction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class PromoDataConverterTest extends ServiceTestBase {

    @Autowired
    private PromoDataConverter promoDataConverter;
    @Autowired
    private PromoDao promoDao;

    @Test
    void shouldMapDirectDiscountFromYt() {
        ZonedDateTime now = clock.dateTime().atZone(clock.getZone());

        PromoKey promoKey = promoDao.replace(promoDataConverter.mapPromo(PromoInfo.PromoDescription.newBuilder()
                .setId("#1568")
                .setName("some promo")
                .setType(PromoInfo.PromoType.DISCOUNT)
                .addPromoChannels(PromoInfo.Channel.BANNER_IN_CATALOG)
                .setAdditionalInfo(PromoInfo.AdditionalInfo.newBuilder()
                        .build())
                .setConstraints(PromoInfo.PromoConstraints.newBuilder()
                        .setStartDate(now.toEpochSecond())
                        .setEndDate(now.plusDays(1).toEpochSecond())
                        .setOffersMatchingRules(PromoInfo.OffersMatchingRule.newBuilder()
                                .setCategoryRestriction(CategoryRestriction.newBuilder()
                                        .addPromoCategory(PromoInfo.PromoCategory.newBuilder()
                                                .setId(12)
                                                .setMinDiscount(10)
                                                .setName("категория1")
                                                .build())
                                        .addPromoCategory(PromoInfo.PromoCategory.newBuilder()
                                                .setId(13)
                                                .setMinDiscount(15)
                                                .setName("категория2")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .setPromoResponsible(PromoInfo.PromoResponsible.newBuilder()
                        .setAuthor("stan-marsh")
                        .setTm("Stan Marsh")
                        .build())
                .setStatus(PromoInfo.PromoStatus.CONFIRMED)
                .build())).toPromoKey();

        Promo promo = promoDao.get(promoKey);

        assertThat(promo, notNullValue());
        assertThat(promo.getPromoId(), is("#1568"));
        assertThat(promo.getName(), is("some promo"));
        assertThat(promo.getStatus(), is(ru.yandex.market.marketpromo.model.PromoStatus.CONFIRMED));
        assertThat(promo.getSource(), is("ANAPLAN"));
        assertThat(promo.getMechanicsType(), is(MechanicsType.DIRECT_DISCOUNT));
        assertThat(promo.getStartDate(), comparesEqualTo(now.toLocalDateTime()));
        assertThat(promo.getEndDate(), comparesEqualTo(now.plusDays(1).toLocalDateTime()));
        assertThat(promo.getCategories(), hasItems(12L, 13L));
        assertThat(promo.getCategoriesWithDiscounts(), hasItems(allOf(
                hasProperty("categoryId", comparesEqualTo(12L)),
                hasProperty("discount", comparesEqualTo(BigDecimal.TEN))
        ), allOf(
                hasProperty("categoryId", comparesEqualTo(13L)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(15)))
        )));
        assertThat(promo.getTrade(), notNullValue());
        assertThat(promo.getTrade().getId(), is("stan-marsh"));
        assertThat(promo.getTrade().getFirstName(), is("Stan"));
        assertThat(promo.getTrade().getLastName(), is("Marsh"));
    }

    @Test
    void shouldMapCheapestAsGiftFromYt() {
        ZonedDateTime now = clock.dateTime().atZone(clock.getZone());

        PromoKey promoKey = promoDao.replace(promoDataConverter.mapPromo(PromoInfo.PromoDescription.newBuilder()
                .setId("#1568")
                .setName("some promo")
                .setType(PromoInfo.PromoType.CHEAPEST_AS_GIFT)
                .addPromoChannels(PromoInfo.Channel.BANNER_IN_CATALOG)
                .setAdditionalInfo(PromoInfo.AdditionalInfo.newBuilder()
                        .build())
                .setPromoType(PromoInfo.Promo.newBuilder()
                        .setCheapestAsGift(PromoInfo.CheapestAsGift.newBuilder()
                                .setCount(5)
                                .setWarehouseId(123)
                                .build())
                        .build())
                .setConstraints(PromoInfo.PromoConstraints.newBuilder()
                        .setStartDate(now.toEpochSecond())
                        .setEndDate(now.plusDays(1).toEpochSecond())
                        .setOffersMatchingRules(PromoInfo.OffersMatchingRule.newBuilder()
                                .setWarehouseRestriction(WarehouseRestriction.newBuilder()
                                        .setWarehouse(IntList.newBuilder()
                                                .addId(123)
                                                .build())
                                        .build())
                                .setCategoryRestriction(CategoryRestriction.newBuilder()
                                        .addPromoCategory(PromoInfo.PromoCategory.newBuilder()
                                                .setId(12L)
                                                .setMinDiscount(10)
                                                .setName("категория1")
                                                .build())
                                        .addPromoCategory(PromoInfo.PromoCategory.newBuilder()
                                                .setId(13L)
                                                .setMinDiscount(15)
                                                .setName("категория2")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .setPromoResponsible(PromoInfo.PromoResponsible.newBuilder()
                        .setAuthor("stan-marsh")
                        .setTm("Stan Marsh")
                        .build())
                .setStatus(PromoInfo.PromoStatus.CONFIRMED)
                .build())).toPromoKey();

        Promo promo = promoDao.get(promoKey);

        assertThat(promo, notNullValue());
        assertThat(promo.getPromoId(), is("#1568"));
        assertThat(promo.getName(), is("some promo"));
        assertThat(promo.getStatus(), is(ru.yandex.market.marketpromo.model.PromoStatus.CONFIRMED));
        assertThat(promo.getSource(), is("ANAPLAN"));
        assertThat(promo.getMechanicsType(), is(MechanicsType.CHEAPEST_AS_GIFT));
        CheapestAsGiftProperties cheapestAsGiftProperties =
                promo.getMechanicsPropertiesAs(CheapestAsGiftProperties.class).orElseThrow();
        assertThat(cheapestAsGiftProperties.getQuantityInBundle(), comparesEqualTo(5));
        assertThat(promo.getWarehouseId(), comparesEqualTo(123L));
        assertThat(promo.getStartDate(), comparesEqualTo(now.toLocalDateTime()));
        assertThat(promo.getEndDate(), comparesEqualTo(now.plusDays(1).toLocalDateTime()));
        assertThat(promo.getCategories(), hasItems(12L, 13L));
        assertThat(promo.getCategoriesWithDiscounts(), hasItems(allOf(
                hasProperty("categoryId", comparesEqualTo(12L)),
                hasProperty("discount", comparesEqualTo(BigDecimal.TEN))
        ), allOf(
                hasProperty("categoryId", comparesEqualTo(13L)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(15)))
        )));
        assertThat(promo.getTrade(), notNullValue());
        assertThat(promo.getTrade().getId(), is("stan-marsh"));
        assertThat(promo.getTrade().getFirstName(), is("Stan"));
        assertThat(promo.getTrade().getLastName(), is("Marsh"));
    }

    @Test
    void shouldMapDirectDiscountFromLogbroker() {
        ZonedDateTime now = clock.dateTime().atZone(clock.getZone());

        DataCampOfferMeta.UpdateMeta meta = DataCampOfferMeta.UpdateMeta.newBuilder()
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.toEpochSecond())
                        .build())
                .build();

        PromoKey promoKey = promoDao.replace(Objects.requireNonNull(promoDataConverter.mapPromo(
                DataCampPromo.PromoDescription.newBuilder()
                        .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setBusinessId(10)
                                .setPromoId("#1568")
                                .setSource(ESourceType.CATEGORYIFACE)
                                .build())
                        .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT)
                                .setMeta(meta)
                                .build())
                        .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                                .setLendingUrl("http://some-url")
                                .setLendingUrlText("lending_url_text")
                                .setStatus(PromoStatus.CONFIRMED)
                                .setName("some promo")
                                .setCreatedAt(0)
                                .setAssortmentDeadline(0)
                                .setPublishDatePi(now.toEpochSecond())
                                .setUpdatedAt(now.toEpochSecond())
                                .setMeta(meta)
                                .build())
                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                .setMeta(meta)
                                .setStartDate(now.toEpochSecond())
                                .setEndDate(now.plusDays(1).toEpochSecond())
                                .setEnabled(true)
                                .setHidden(true)
                                .addOffersMatchingRules(OffersMatchingRule.newBuilder()
                                        .setCategoryRestriction(OffersMatchingRule.CategoryRestriction.newBuilder()
                                                .addPromoCategory(OffersMatchingRule.PromoCategory.newBuilder()
                                                        .setId(123)
                                                        .setMinDiscount(15)
                                                        .setName("категория1")
                                                        .build())
                                                .addPromoCategory(OffersMatchingRule.PromoCategory.newBuilder()
                                                        .setId(124)
                                                        .setMinDiscount(17)
                                                        .setName("категория2")
                                                        .build())
                                                .build())
                                        .setOrigionalCategoryRestriction(
                                                OffersMatchingRule.OriginalCategoryRestriction.newBuilder()
                                                        .addIncludeCategegoryRestriction(
                                                                OffersMatchingRule.PromoCategory.newBuilder()
                                                                        .setId(123)
                                                                        .setMinDiscount(15)
                                                                        .setName("категория1")
                                                                        .build())
                                                        .addIncludeCategegoryRestriction(
                                                                OffersMatchingRule.PromoCategory.newBuilder()
                                                                        .setId(124)
                                                                        .setMinDiscount(17)
                                                                        .setName("категория2")
                                                                        .build())
                                                        .build())
                                        .build())
                                .build())
                        .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                                .setMeta(meta)
                                .build())
                        .setResponsible(DataCampPromo.PromoResponsible.newBuilder()
                                .setMeta(meta)
                                .setAuthor("stan-marsh")
                                .setTm("Stan Marsh")
                                .build())
                        .build()))).toPromoKey();

        Promo promo = promoDao.get(promoKey);

        assertThat(promo, notNullValue());
        assertThat(promo.getPromoId(), is("#1568"));
        assertThat(promo.getBusinessId(), comparesEqualTo(10L));
        assertThat(promo.getName(), is("some promo"));
        assertThat(promo.getUrl(), is("http://some-url"));
        assertThat(promo.getDescription(), is("lending_url_text"));
        assertThat(promo.getSource(), is("CATEGORYIFACE"));
        assertThat(promo.getStatus(), is(ru.yandex.market.marketpromo.model.PromoStatus.CONFIRMED));
        assertThat(promo.getMechanicsType(), is(MechanicsType.DIRECT_DISCOUNT));
        assertThat(promo.getStartDate(), comparesEqualTo(now.toLocalDateTime()));
        assertThat(promo.getEndDate(), comparesEqualTo(now.plusDays(1).toLocalDateTime()));
        assertThat(promo.getCategories(), hasItems(123L, 124L));
        assertThat(promo.getCategoriesWithDiscounts(), hasItems(allOf(
                hasProperty("categoryId", comparesEqualTo(123L)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(15)))
        ), allOf(
                hasProperty("categoryId", comparesEqualTo(124L)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(17)))
        )));
        assertThat(promo.getTrade(), notNullValue());
        assertThat(promo.getTrade().getId(), is("stan-marsh"));
        assertThat(promo.getTrade().getFirstName(), is("Stan"));
        assertThat(promo.getTrade().getLastName(), is("Marsh"));
    }

    @Test
    void shouldMapCheapestAsGiftFromLogbroker() {
        ZonedDateTime now = clock.dateTime().atZone(clock.getZone());

        DataCampOfferMeta.UpdateMeta meta = DataCampOfferMeta.UpdateMeta.newBuilder()
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.toEpochSecond())
                        .build())
                .build();

        PromoKey promoKey = promoDao.replace(Objects.requireNonNull(promoDataConverter.mapPromo(
                DataCampPromo.PromoDescription.newBuilder()
                        .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setBusinessId(10)
                                .setPromoId("#1568")
                                .setSource(ESourceType.CATEGORYIFACE)
                                .build())
                        .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.CHEAPEST_AS_GIFT)
                                .setMeta(meta)
                                .build())
                        .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                                .setLendingUrl("http://some-url")
                                .setStatus(PromoStatus.CONFIRMED)
                                .setName("some promo")
                                .setCreatedAt(0)
                                .setAssortmentDeadline(0)
                                .setPublishDatePi(now.toEpochSecond())
                                .setUpdatedAt(now.toEpochSecond())
                                .setMeta(meta)
                                .build())
                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                .setMeta(meta)
                                .setStartDate(now.toEpochSecond())
                                .setEndDate(now.plusDays(1).toEpochSecond())
                                .setEnabled(true)
                                .setHidden(true)
                                .addOffersMatchingRules(OffersMatchingRule.newBuilder()
                                        .setCategoryRestriction(OffersMatchingRule.CategoryRestriction.newBuilder()
                                                .addPromoCategory(OffersMatchingRule.PromoCategory.newBuilder()
                                                        .setId(123)
                                                        .setMinDiscount(15)
                                                        .setName("категория1")
                                                        .build())
                                                .addPromoCategory(OffersMatchingRule.PromoCategory.newBuilder()
                                                        .setId(124)
                                                        .setMinDiscount(17)
                                                        .setName("категория2")
                                                        .build())
                                                .build())
                                        .setOrigionalCategoryRestriction(
                                                OffersMatchingRule.OriginalCategoryRestriction.newBuilder()
                                                        .addIncludeCategegoryRestriction(
                                                                OffersMatchingRule.PromoCategory.newBuilder()
                                                                        .setId(123)
                                                                        .setMinDiscount(15)
                                                                        .setName("категория1")
                                                                        .build())
                                                        .addIncludeCategegoryRestriction(
                                                                OffersMatchingRule.PromoCategory.newBuilder()
                                                                        .setId(124)
                                                                        .setMinDiscount(17)
                                                                        .setName("категория2")
                                                                        .build())
                                                        .build())
                                        .build())
                                .addOffersMatchingRules(OffersMatchingRule.newBuilder()
                                        .setWarehouseRestriction(OffersMatchingRule.WarehouseRestriction.newBuilder()
                                                .setWarehouse(OffersMatchingRule.IntList.newBuilder()
                                                        .addId(123L)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                                .setMeta(meta)
                                .setCheapestAsGift(DataCampPromo.PromoMechanics.CheapestAsGift.newBuilder()
                                        .setCount(5L)
                                        .build())
                                .build())
                        .setResponsible(DataCampPromo.PromoResponsible.newBuilder()
                                .setMeta(meta)
                                .setAuthor("stan-marsh")
                                .setTm("Stan Marsh")
                                .build())
                        .build()))).toPromoKey();

        Promo promo = promoDao.get(promoKey);

        assertThat(promo, notNullValue());
        assertThat(promo.getPromoId(), is("#1568"));
        assertThat(promo.getBusinessId(), comparesEqualTo(10L));
        assertThat(promo.getName(), is("some promo"));
        assertThat(promo.getMechanicsType(), is(MechanicsType.CHEAPEST_AS_GIFT));
        assertThat(promo.getStartDate(), comparesEqualTo(now.toLocalDateTime()));
        assertThat(promo.getEndDate(), comparesEqualTo(now.plusDays(1).toLocalDateTime()));
        assertThat(promo.getCategories(), hasItems(123L, 124L));
        assertThat(promo.getCategoriesWithDiscounts(), hasItems(allOf(
                hasProperty("categoryId", comparesEqualTo(123L)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(15)))
        ), allOf(
                hasProperty("categoryId", comparesEqualTo(124L)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(17)))
        )));
        assertThat(promo.getTrade(), notNullValue());
        assertThat(promo.getTrade().getId(), is("stan-marsh"));
        assertThat(promo.getTrade().getFirstName(), is("Stan"));
        assertThat(promo.getTrade().getLastName(), is("Marsh"));
        CheapestAsGiftProperties cheapestAsGiftProperties =
                promo.getMechanicsPropertiesAs(CheapestAsGiftProperties.class).orElseThrow();
        assertThat(cheapestAsGiftProperties.getQuantityInBundle(), comparesEqualTo(5));
        assertThat(promo.getWarehouseId(), comparesEqualTo(123L));
    }
}
