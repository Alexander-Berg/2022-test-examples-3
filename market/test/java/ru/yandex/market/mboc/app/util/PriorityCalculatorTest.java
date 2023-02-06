package ru.yandex.market.mboc.app.util;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.PriorityCalculator;

public class PriorityCalculatorTest {
    @Test
    public void firstPartyGreaterThirdPartyTest() {
        Offer firstPartyOffer = createOffer(LocalDate.now(), 1L, MbocSupplierType.REAL_SUPPLIER, false);
        Offer thirdPartyOffer = createOffer(LocalDate.now(), 1L, MbocSupplierType.THIRD_PARTY, false);

        int fpp  = PriorityCalculator.calculatePriority(firstPartyOffer);
        int tpp = PriorityCalculator.calculatePriority(thirdPartyOffer);

        Assert.assertTrue(fpp > tpp);
    }

    @Test
    public void critGreaterNotCritTest() {
        Offer critOffer = createOffer(LocalDate.now(), 1L, MbocSupplierType.THIRD_PARTY, true);
        Offer notCritOffer = createOffer(LocalDate.now(), 1L, MbocSupplierType.THIRD_PARTY, false);

        int cp  = PriorityCalculator.calculatePriority(critOffer);
        int ncp = PriorityCalculator.calculatePriority(notCritOffer);

        Assert.assertTrue(cp > ncp);
    }

    @Test
    public void priorityGreaterIfDeadlineNearTest() {
        Offer oldOffer = createOffer(LocalDate.now(), 1L, MbocSupplierType.THIRD_PARTY, true);
        Offer newOffer = createOffer(LocalDate.now().plusDays(1), 1L, MbocSupplierType.THIRD_PARTY, true);

        int op  = PriorityCalculator.calculatePriority(oldOffer);
        int np = PriorityCalculator.calculatePriority(newOffer);

        Assert.assertTrue(op > np);
    }

    @Test
    public void firstPartyVSCritTest() {
        Offer firstPartyOffer = createOffer(LocalDate.now(), 1L, MbocSupplierType.REAL_SUPPLIER, false);
        Offer critOffer = createOffer(LocalDate.now(), 1L, MbocSupplierType.THIRD_PARTY, true);

        int fp  = PriorityCalculator.calculatePriority(firstPartyOffer);
        int cp = PriorityCalculator.calculatePriority(critOffer);

        Assert.assertTrue(fp > cp);
    }


    @Test
    public void critVSDeadlineTest() {
        Offer critOffer = createOffer(LocalDate.now(), 1L, MbocSupplierType.THIRD_PARTY, true);
        Offer deadlineOffer = createOffer(LocalDate.now().minusDays(1000), 1L, MbocSupplierType.THIRD_PARTY, false);

        int cp = PriorityCalculator.calculatePriority(critOffer);
        int dp  = PriorityCalculator.calculatePriority(deadlineOffer);

        Assert.assertTrue(cp > dp);
    }

    @Test
    public void categoryTest() {
        Offer offer1 = createOffer(LocalDate.now(), 2L, MbocSupplierType.THIRD_PARTY, true);
        Offer offer2 = createOffer(LocalDate.now(), 1L, MbocSupplierType.THIRD_PARTY, true);

        int p1  = PriorityCalculator.calculatePriority(offer1);
        int p2 = PriorityCalculator.calculatePriority(offer2);

        Assert.assertTrue(p1 != p2);
    }


    @Test
    public void priorityNotChangeTest() {
        Offer offer = createOffer(LocalDate.now(), 1L, MbocSupplierType.THIRD_PARTY, true);
        int p1  = PriorityCalculator.calculatePriority(offer);

        offer.setHideFromToloka(true);
        Assert.assertEquals(p1, PriorityCalculator.calculatePriority(offer));
        offer.setSupplierId(1);
        Assert.assertEquals(p1, PriorityCalculator.calculatePriority(offer));
        offer.setModelId(1L);
        Assert.assertEquals(p1, PriorityCalculator.calculatePriority(offer));
        offer.setVendorId(1);
        Assert.assertEquals(p1, PriorityCalculator.calculatePriority(offer));
    }

    @Test
    public void checkDiapasonTest() {
        Offer offer = createOffer(LocalDate.of(1970, 1,1), 1324567L, MbocSupplierType.REAL_SUPPLIER, true);
        int p1  = PriorityCalculator.calculatePriority(offer);

        Assert.assertTrue(p1 > 0);
        Assert.assertTrue(p1 < 2_000_000_000);
    }

    @Test
    public void checkOfferWithoutDeadlineTest() {
        Offer offer = createOffer(null, 1324567L, MbocSupplierType.REAL_SUPPLIER, true);
        int p1  = PriorityCalculator.calculatePriority(offer);

        Assert.assertTrue(p1 > 0);
        Assert.assertTrue(p1 < 2_000_000_000);
    }

    private Offer createOffer(LocalDate deadline, long categoryId, MbocSupplierType supplierType, boolean crit) {
        Offer offer = new Offer();
        offer.setTicketDeadline(deadline);
        offer.setCategoryIdInternal(categoryId);
        offer.setServiceOffers(Collections.singletonList(new Offer.ServiceOffer().setSupplierType(supplierType)));
        offer.setTicketCritical(crit);
        return offer;
    }

}
