package ru.yandex.market.mbi.affiliate.promo.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PartnerPromoDto;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PromoDescriptionDto;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PromoDescriptionStatus;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PromoDescriptionWithPromocodes;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.SortingField;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.SortingOrder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertEquals;

public class AffiliatePromoControllerTest {

    @Test
    public void testSortByBudget() {
         var resultAsc = AffiliatePromoController.withSortingAndPaging(
                prepareData(), SortingField.BUDGET, SortingOrder.ASC, 1000, null);
         assertThat(resultAsc, iterableWithSize(4));
         assertEquals(List.of("3", "1", "2", "4"), toIdList(resultAsc));

         var resultDefault = AffiliatePromoController.withSortingAndPaging(
                 prepareData(), SortingField.BUDGET, null, 1000, null);
        assertThat(resultDefault, iterableWithSize(4));
        assertEquals(List.of("1", "3", "4", "2"), toIdList(resultDefault));
    }

    @Test
    public void testSortByStartDate() {
        var resultAsc = AffiliatePromoController.withSortingAndPaging(
                prepareData(), SortingField.START_DATE, SortingOrder.ASC, 1000, null);
        assertThat(resultAsc, iterableWithSize(4));
        assertEquals(List.of("1", "3", "2", "4"), toIdList(resultAsc));

        var resultDefault = AffiliatePromoController.withSortingAndPaging(
                prepareData(), SortingField.START_DATE, null, 1000, null);
        assertThat(resultDefault, iterableWithSize(4));
        assertEquals(List.of("3", "1", "4", "2"), toIdList(resultDefault));
    }

    @Test
    public void testSortDefault() {
        var resultAsc = AffiliatePromoController.withSortingAndPaging(
                prepareData(), null, null, 1000, null);
        assertThat(resultAsc, iterableWithSize(4));
        assertEquals(List.of("3", "1", "4", "2"), toIdList(resultAsc));
    }

    @Test
    public void testPagingFirstPage() {
        var result = AffiliatePromoController.withSortingAndPaging(
                prepareLongData(50), null, null, 5, null);
        assertThat(result, iterableWithSize(5));
        assertEquals(List.of("1", "2", "3", "4", "5"), toIdList(result));
        assertThat(
                AffiliatePromoController.resolveLastPageMarker(50, 5, null),
                is(false));
    }

    @Test
    public void testPaging() {
       var data =  prepareLongData(50);
       var result = AffiliatePromoController.withSortingAndPaging(
                    data, null, null, 10, 3);
       assertThat(result, iterableWithSize(10));
       assertEquals(List.of("21", "22", "23", "24", "25", "26", "27", "28", "29", "30"), toIdList(result));
       assertThat(
               AffiliatePromoController.resolveLastPageMarker(50, 10, 3),
               is(false));
    }

    @Test
    public void testPagingLastPage() {
        var result = AffiliatePromoController.withSortingAndPaging(
                prepareLongData(32), null, null, 10, 4);
        assertThat(result, iterableWithSize(2));
        assertEquals(List.of("31", "32"), toIdList(result));
        assertThat(
                AffiliatePromoController.resolveLastPageMarker(32, 10, 4),
                is(true));
    }

    @Test
    public void testPagingNoMoreData() {
        var result = AffiliatePromoController.withSortingAndPaging(
                prepareLongData(32), null, null, 10, 5);
        assertThat(result, emptyIterable());
        assertThat(
                AffiliatePromoController.resolveLastPageMarker(32, 10, 5),
                is(true));
    }

    private List<PromoDescriptionWithPromocodes> prepareData() {
        return List.of(
                makeDescriptionWithPromocodes(
                        "1", PromoDescriptionStatus.ACTIVE,
                        LocalDate.of(2021, 8, 1), LocalDate.of(2021, 9, 1),
                        99),
                makeDescriptionWithPromocodes(
                        "2", PromoDescriptionStatus.EXPIRED,
                        LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 31),
                        70),
                makeDescriptionWithPromocodes(
                        "3", PromoDescriptionStatus.ACTIVE,
                        LocalDate.of(2021, 8, 5), LocalDate.of(2021, 10, 1),
                        88),
                makeDescriptionWithPromocodes(
                        "4", PromoDescriptionStatus.INACTIVE,
                        LocalDate.of(2021, 9, 1), LocalDate.of(2021, 9, 10),
                        100)
        );
    }

    private List<PromoDescriptionWithPromocodes> prepareLongData(int number) {
        List<PromoDescriptionWithPromocodes> result = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            result.add(makeDescriptionWithPromocodes(
                    String.valueOf(i + 1),
                    PromoDescriptionStatus.ACTIVE,
                    LocalDate.of(2021, 10, 1),
                    LocalDate.of(2021, 10, 2), 100));
        }
        return result;
    }

    private List<String> toIdList(List<PromoDescriptionWithPromocodes> l)  {
        return l.stream().map(d -> d.getPromoDescription().getPromoDescriptionId())
                .collect(Collectors.toList());
    }

    private PromoDescriptionWithPromocodes makeDescriptionWithPromocodes(
            String id, PromoDescriptionStatus status, LocalDate startDate, LocalDate endDate, int budgetPercent) {
        return new PromoDescriptionWithPromocodes()
                .promoDescription(makePromoDescription(id, status, startDate, endDate, budgetPercent))
                .addPartnerPromoListItem(new PartnerPromoDto());
    }

    private PromoDescriptionDto makePromoDescription(
            String id, PromoDescriptionStatus status, LocalDate startDate, LocalDate endDate, int budgetPercent) {
        return new PromoDescriptionDto()
                .promoDescriptionId(id)
                .promoDescriptionStatus(status)
                .startDate(startDate)
                .endDate(endDate)
                .remainingBudgetPercent(budgetPercent);
    }

}