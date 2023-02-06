package ru.yandex.market.mboc.common.services.offers;

import java.util.Collections;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.Offer.AcceptanceStatus;
import ru.yandex.market.mboc.common.services.offers.OfferStatisticsService.OfferStatistics;

import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.CONTENT;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.mapping;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.simpleOffer;

/**
 * @author yuramalinov
 * @created 24.09.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class OfferStatisticsServiceTest {
    @Test
    public void testSimpleApproved() {
        assertStat(simpleOffer().updateApprovedSkuMapping(mapping(42), CONTENT))
            .isEqualToComparingFieldByField(OfferStatistics.approved(1));
    }

    @Test
    public void testApprovedSupplierMapping() {
        assertStat(
            simpleOffer()
                .updateApprovedSkuMapping(mapping(42), CONTENT)
                .setSupplierSkuMapping(mapping(44))
                .setSupplierSkuMappingStatus(Offer.MappingStatus.REJECTED))
            .isEqualToComparingFieldByField(OfferStatistics.approved(1));
    }

    @Test
    public void testInWorkEvenWhenApproved() {
        assertStat(
            simpleOffer()
                .updateApprovedSkuMapping(mapping(42), CONTENT)
                .setSupplierSkuMapping(mapping(44))
                .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW))
            .isEqualToComparingFieldByField(OfferStatistics.inWork(1));

        assertStat(
            simpleOffer()
                .updateApprovedSkuMapping(mapping(42), CONTENT)
                .setSupplierSkuMapping(mapping(44))
                .setSupplierSkuMappingStatus(Offer.MappingStatus.RE_SORT))
            .isEqualToComparingFieldByField(OfferStatistics.inWork(1));
    }

    @Test
    public void testApprovedEvenIfTrash() {
        assertStat(
            simpleOffer()
                .updateApprovedSkuMapping(mapping(42), CONTENT)
                .updateAcceptanceStatusForTests(AcceptanceStatus.TRASH))
            .isEqualToComparingFieldByField(OfferStatistics.approved(1));
    }

    @Test
    public void testSimpleInWork() {
        assertStat(simpleOffer())
            .isEqualToComparingFieldByField(OfferStatistics.inWork(1));
    }

    private AbstractObjectAssert<?, OfferStatistics> assertStat(Offer offer) {
        return Assertions.assertThat(stat(offer));
    }

    @Test
    public void testRejected() {
        Assertions.assertThat(stat(simpleOffer().updateAcceptanceStatusForTests(AcceptanceStatus.TRASH)))
            .isEqualToComparingFieldByField(OfferStatistics.rejected(1));
    }

    private OfferStatistics stat(Offer offer) {
        return OfferStatisticsService.calcOfferStatistics(Collections.singletonList(offer));
    }
}
