package ru.yandex.market.core.supplier.promo.mapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.supplier.promo.dto.PiPromoMechanicDto;
import ru.yandex.market.core.supplier.promo.model.multi.BasePromoDetails;
import ru.yandex.market.core.supplier.promo.model.multi.OfferPromosEnablingInfo;
import ru.yandex.market.core.supplier.promo.model.multi.PromoConstructorInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.supplier.promo.mapper.OfferPromosEnablingMapper.getOfferPromosEnablingInfo;

public class OfferPromosEnablingMapperTest extends FunctionalTest {

    @Test
    public void testGetOfferPromosEnablingInfo_DifferentWarehouses() {
        int year = LocalDateTime.now().getYear();
        List<PromoConstructorInfo> promos = createPromosDW(year + 1);
        OfferPromosEnablingInfo result = getOfferPromosEnablingInfo(promos);

        List<String> fullyEnabledPromos = result.getFullyEnabledPromos().stream()
                .map(BasePromoDetails::getPromoId)
                .collect(Collectors.toList());
        List<String> partiallyEnabledPromos = result.getPartiallyEnabledPromos().stream()
                .map(promo -> promo.getPromo().getPromoId())
                .collect(Collectors.toList());
        List<String> disabledPromos = result.getDisabledPromos().stream()
                .map(BasePromoDetails::getPromoId)
                .collect(Collectors.toList());

        assertThat(fullyEnabledPromos).containsExactlyInAnyOrder("5", "1", "3", "4", "7");
        assertThat(partiallyEnabledPromos).containsExactlyInAnyOrder("6");
        assertThat(disabledPromos).containsExactlyInAnyOrder("2");
    }

    @Test
    public void testGetOfferPromosEnablingInfo_AllEnabled() {
        int year = LocalDateTime.now().getYear();
        List<PromoConstructorInfo> promos = createPromosAE(year + 1);
        OfferPromosEnablingInfo result = getOfferPromosEnablingInfo(promos);

        List<String> fullyEnabledPromos = result.getFullyEnabledPromos().stream()
                .map(BasePromoDetails::getPromoId)
                .collect(Collectors.toList());

        assertThat(fullyEnabledPromos).containsExactlyInAnyOrder("5", "1", "2", "3", "4", "6");
        assertThat(result.getPartiallyEnabledPromos()).isEmpty();
        assertThat(result.getDisabledPromos()).isEmpty();
    }

    private List<PromoConstructorInfo> createPromosDW(int year) {
        PromoConstructorInfo promoDD = new PromoConstructorInfo(
                new BasePromoDetails.Builder()
                        .setPromoId("1")
                        .setMechanic(PiPromoMechanicDto.DIRECT_DISCOUNT)
                        .setDiscountPercentage(10)
                        .setName("One")
                        .setStartDate(LocalDateTime.of(year, 3, 1, 0, 0))
                        .setEndDate(LocalDateTime.of(year, 3, 31, 23, 59, 59))
                        .setPriority(7L)
                        .build(),
                Set.of("2"),
                false
        );

        PromoConstructorInfo promoFlash = new PromoConstructorInfo(
                new BasePromoDetails.Builder()
                        .setPromoId("2")
                        .setMechanic(PiPromoMechanicDto.BLUE_FLASH)
                        .setDiscountPercentage(10)
                        .setName("Two")
                        .setStartDate(LocalDateTime.of(year, 3, 25, 0, 0))
                        .setEndDate(LocalDateTime.of(year, 3, 28, 23, 59, 59))
                        .setPriority(6L)
                        .setWarehouseId(100L)
                        .build(),
                Set.of("1"),
                false
        );

        PromoConstructorInfo promoCAG12 = new PromoConstructorInfo(
                new BasePromoDetails.Builder()
                        .setPromoId("3")
                        .setMechanic(PiPromoMechanicDto.CHEAPEST_AS_GIFT)
                        .setName("Three")
                        .setStartDate(LocalDateTime.of(year, 3, 11, 0, 0))
                        .setEndDate(LocalDateTime.of(year, 3, 30, 23, 59, 59))
                        .setPriority(5L)
                        .setWarehouseId(101L)
                        .build(),
                Collections.emptySet(),
                false
        );

        PromoConstructorInfo promoCAG23 = new PromoConstructorInfo(
                new BasePromoDetails.Builder()
                        .setPromoId("4")
                        .setMechanic(PiPromoMechanicDto.CHEAPEST_AS_GIFT)
                        .setName("Four")
                        .setStartDate(LocalDateTime.of(year, 3, 11, 0, 0))
                        .setEndDate(LocalDateTime.of(year, 3, 31, 23, 59, 59))
                        .setPriority(4L)
                        .setWarehouseId(102L)
                        .build(),
                Collections.emptySet(),
                false
        );

        PromoConstructorInfo customCashback = new PromoConstructorInfo(
                new BasePromoDetails.Builder()
                        .setPromoId("5")
                        .setMechanic(PiPromoMechanicDto.PARTNER_CUSTOM_CASHBACK)
                        .setName("Five")
                        .setStartDate(LocalDateTime.of(year, 3, 25, 0, 0))
                        .setEndDate(LocalDateTime.of(year + 1, 3, 31, 23, 59, 59))
                        .setPriority(10L)
                        .build(),
                Set.of("6"),
                false
        );

        PromoConstructorInfo standardCashback = new PromoConstructorInfo(
                new BasePromoDetails.Builder()
                        .setPromoId("6")
                        .setMechanic(PiPromoMechanicDto.PARTNER_STANDART_CASHBACK)
                        .setName("Six")
                        .setStartDate(LocalDateTime.of(year, 3, 1, 0, 0))
                        .setEndDate(LocalDateTime.of(year, 3, 31, 23, 59, 59))
                        .setPriority(9L)
                        .build(),
                Set.of("5"),
                false
        );

        PromoConstructorInfo promoDD2 = new PromoConstructorInfo(
                new BasePromoDetails.Builder()
                        .setPromoId("7")
                        .setMechanic(PiPromoMechanicDto.DIRECT_DISCOUNT)
                        .setDiscountPercentage(10)
                        .setName("Seven")
                        .setStartDate(LocalDateTime.of(year, 3, 1, 0, 0))
                        .setEndDate(LocalDateTime.of(year, 3, 31, 23, 59, 59))
                        .setPriority(8L)
                        .build(),
                Set.of("1", "2"),
                false
        );

        return List.of(promoDD, promoFlash, promoCAG12, promoCAG23, customCashback, standardCashback, promoDD2);
    }

    private List<PromoConstructorInfo> createPromosAE(int year) {
        PromoConstructorInfo promoDD = new PromoConstructorInfo(
                new BasePromoDetails.Builder()
                        .setPromoId("1")
                        .setMechanic(PiPromoMechanicDto.DIRECT_DISCOUNT)
                        .setDiscountPercentage(10)
                        .setName("One")
                        .setStartDate(LocalDateTime.of(year, 3, 1, 0, 0))
                        .setEndDate(LocalDateTime.of(year, 3, 31, 23, 59, 59))
                        .setPriority(7L)
                        .build(),
                Collections.emptySet(),
                false
        );

        PromoConstructorInfo promoCAG1 = new PromoConstructorInfo(
                new BasePromoDetails.Builder()
                        .setPromoId("2")
                        .setMechanic(PiPromoMechanicDto.CHEAPEST_AS_GIFT)
                        .setName("Two")
                        .setStartDate(LocalDateTime.of(year, 3, 11, 0, 0))
                        .setEndDate(LocalDateTime.of(year, 3, 30, 23, 59, 59))
                        .setPriority(6L)
                        .setWarehouseId(100L)
                        .build(),
                Collections.emptySet(),
                false
        );

        PromoConstructorInfo promoCAG2 = new PromoConstructorInfo(
                new BasePromoDetails.Builder()
                        .setPromoId("3")
                        .setMechanic(PiPromoMechanicDto.CHEAPEST_AS_GIFT)
                        .setName("Three")
                        .setStartDate(LocalDateTime.of(year, 3, 11, 0, 0))
                        .setEndDate(LocalDateTime.of(year, 3, 30, 23, 59, 59))
                        .setPriority(5L)
                        .setWarehouseId(101L)
                        .build(),
                Collections.emptySet(),
                false
        );

        PromoConstructorInfo promoCAG3 = new PromoConstructorInfo(
                new BasePromoDetails.Builder()
                        .setPromoId("4")
                        .setMechanic(PiPromoMechanicDto.CHEAPEST_AS_GIFT)
                        .setName("Four")
                        .setStartDate(LocalDateTime.of(year, 3, 11, 0, 0))
                        .setEndDate(LocalDateTime.of(year, 3, 31, 23, 59, 59))
                        .setPriority(4L)
                        .setWarehouseId(102L)
                        .build(),
                Collections.emptySet(),
                false
        );

        PromoConstructorInfo promoCAG4 = new PromoConstructorInfo(
                new BasePromoDetails.Builder()
                        .setPromoId("6")
                        .setMechanic(PiPromoMechanicDto.CHEAPEST_AS_GIFT)
                        .setName("Six")
                        .setStartDate(LocalDateTime.of(year, 3, 11, 0, 0))
                        .setEndDate(LocalDateTime.of(year, 3, 31, 23, 59, 59))
                        .setPriority(3L)
                        .setWarehouseId(103L)
                        .build(),
                Collections.emptySet(),
                false
        );

        PromoConstructorInfo customCashback = new PromoConstructorInfo(
                new BasePromoDetails.Builder()
                        .setPromoId("5")
                        .setMechanic(PiPromoMechanicDto.PARTNER_CUSTOM_CASHBACK)
                        .setName("Five")
                        .setStartDate(LocalDateTime.of(year, 3, 25, 0, 0))
                        .setEndDate(LocalDateTime.of(year + 1, 3, 31, 23, 59, 59))
                        .setPriority(10L)
                        .build(),
                Set.of("6"),
                false
        );

        return List.of(promoDD, promoCAG1, promoCAG2, promoCAG3, promoCAG4, customCashback);
    }
}
