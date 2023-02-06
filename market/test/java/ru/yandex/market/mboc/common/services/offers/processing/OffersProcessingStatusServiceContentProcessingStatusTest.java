package ru.yandex.market.mboc.common.services.offers.processing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.market.mboc.common.assertions.custom.OfferAssertions;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.expression.DescribeExprHandler;

import static org.assertj.core.api.Assertions.assertThat;

public class OffersProcessingStatusServiceContentProcessingStatusTest extends OffersProcessingStatusServiceTestBase {

    @Test
    public void whenNotAcceptedThenNone() {
        var newOffer = OfferTestUtils.nextOffer()
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.NEW);
        var trashOffer = OfferTestUtils.nextOffer()
            .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH);

        offersProcessingStatusService.processOffers(List.of(newOffer, trashOffer));

        OfferAssertions.assertThat(newOffer).hasContentProcessingStatus(Offer.ContentProcessingStatus.NONE);
        OfferAssertions.assertThat(trashOffer).hasContentProcessingStatus(Offer.ContentProcessingStatus.NONE);
    }

    @Test
    public void whenInNoContentStatusThenNone() {
        Set<Offer.ProcessingStatus> noContentStatuses = Set.of(
            Offer.ProcessingStatus.NO_KNOWLEDGE,
            Offer.ProcessingStatus.NO_CATEGORY,
            Offer.ProcessingStatus.NEED_INFO,
            Offer.ProcessingStatus.LEGAL_PROBLEM
        );

        var offers = noContentStatuses.stream()
            .map(status -> OfferTestUtils.nextOffer()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT)
                .updateProcessingStatusIfValid(status)
            )
            .collect(Collectors.toList());

        offersProcessingStatusService.processOffers(offers);

        offers.forEach(offer -> {
            OfferAssertions.assertThat(offer)
                .as("check offer with status" + offer.getProcessingStatus())
                .hasContentProcessingStatus(Offer.ContentProcessingStatus.NONE);
        });
    }

    @Test
    public void whenInInvalidStatusThenNone() {
        Set<Offer.ProcessingStatus> noContentStatuses = Set.of(
            Offer.ProcessingStatus.INVALID,
            Offer.ProcessingStatus.HOLD
        );

        var offers = noContentStatuses.stream()
            .map(status -> OfferTestUtils.nextOffer()
                .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
                .updateContentProcessingStatus(Offer.ContentProcessingStatus.NEED_CONTENT)
                .updateProcessingStatusIfValid(status)
            )
            .collect(Collectors.toList());

        offersProcessingStatusService.processOffers(offers);

        offers.forEach(offer -> {
            OfferAssertions.assertThat(offer)
                .as("check offer with status" + offer.getProcessingStatus())
                .hasContentProcessingStatus(Offer.ContentProcessingStatus.NONE);
        });
    }


    @Test
    public void checkCurrentContentProcessingCalculationMatchesCommitted() throws IOException {
        DescribeExprHandler.Node node = offersProcessingStatusService.describeContentProcessingStatusCalculation();

        // История изменения пайплайнов. Когда падает - надо чекнуть и просто вкоммитеть дифф тоже.
        // зачем: можно будет в текстовом виде видеть диффы того, как обрабатываются оферы.
        assertThat(normalize(node.toString()))
            .isEqualTo(normalize(
                new String(getClass().getClassLoader()
                    .getResourceAsStream("OfferProcessingStatusService/current-offer-content-processing-pipeline.txt")
                    .readAllBytes(),
                    StandardCharsets.UTF_8)));
    }
}
