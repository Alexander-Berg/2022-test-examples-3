package ru.yandex.market.mboc.common.offers.mbi;


import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.http.SupplierOffer;

public class ProcessingStatusConverterTest {

    @Test
    public void testCorrectStatusConversion() {
        Assertions.assertThat(
            ProcessingStatusConverter
                .convertSupplierOfferOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_INFO))
            .isEqualTo(Offer.ProcessingStatus.NEED_INFO);
        Assertions.assertThat(
            ProcessingStatusConverter
                .convertSupplierOfferOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_CONTENT))
            .isEqualTo(Offer.ProcessingStatus.NEED_CONTENT);
        Assertions.assertThat(
            ProcessingStatusConverter
                .convertSupplierOfferOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.NEED_CONTENT))
            .isEqualTo(Offer.ProcessingStatus.NEED_CONTENT);
        Assertions.assertThat(
            ProcessingStatusConverter
                .convertSupplierOfferOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.CONTENT_PROCESSING))
            .isEqualTo(Offer.ProcessingStatus.CONTENT_PROCESSING);
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testUnknownStatusConvertion() {
        Assertions.assertThatThrownBy(() ->
            ProcessingStatusConverter
                .convertSupplierOfferOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.IN_WORK))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() ->
            ProcessingStatusConverter
                .convertSupplierOfferOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.READY))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() ->
            ProcessingStatusConverter
                .convertSupplierOfferOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.REJECTED))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() ->
            ProcessingStatusConverter
                .convertSupplierOfferOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.REVIEW))
            .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() ->
            ProcessingStatusConverter
                .convertSupplierOfferOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.SUSPENDED))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
