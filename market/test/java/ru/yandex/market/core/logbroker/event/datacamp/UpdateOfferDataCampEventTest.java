package ru.yandex.market.core.logbroker.event.datacamp;

import java.time.Instant;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.offer.IndexerOfferKey;
import ru.yandex.market.core.offer.KnownShopIndexerOfferKey;
import ru.yandex.market.core.offer.OfferUpdate;
import ru.yandex.market.core.offer.PapiHidingEvent;
import ru.yandex.market.core.offer.PapiHidingSource;
import ru.yandex.market.core.util.DateTimes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateOfferDataCampEventTest {

    private final KnownShopIndexerOfferKey OFFER_KEY = KnownShopIndexerOfferKey.of(
            PartnerId.businessId(1L),
            IndexerOfferKey.offerId(1L, "offerId"));

    @Test
    void statusDisabled_ifHidingRequestIsTrueThenStatusFlagIsTrue() {
        boolean isHidingRequest = true;
        OfferUpdate offerUpdate = offerBuilder()
                .setRequestedIsHiddenUpdate(isHidingRequest)
                .build();

        UpdateOfferDataCampEvent event = new UpdateOfferDataCampEvent(1L, offerUpdate);
        DataCampOffer.Offer result = event.convertToDataCampOffer();

        assertTrue(result.getStatus().getDisabled(0).getFlag());
    }

    private OfferUpdate.Builder offerBuilder() {
        return OfferUpdate.builder()
                .setOfferKey(OFFER_KEY)
                .setUpdateTime(Instant.MIN);
    }

    @Test
    void statusDisabled_ifHidingRequestIsFalseThenStatusFlagIsFalse() {
        boolean isHidingRequest = false;
        OfferUpdate offerUpdate = offerBuilder()
                .setRequestedIsHiddenUpdate(isHidingRequest)
                .build();

        UpdateOfferDataCampEvent event = new UpdateOfferDataCampEvent(1L, offerUpdate);
        DataCampOffer.Offer result = event.convertToDataCampOffer();

        assertFalse(result.getStatus().getDisabled(0).getFlag());
    }

    @Test
    void statusDisabled_ifHidingRequestIsSetThenFlagMetaIsFromHidingUpdate() {
        OfferUpdate offerUpdate = offerBuilder()
                .setUpdateTime(Instant.MIN)
                .setRequestedIsHiddenUpdate(true)
                .setRequestedHiddenUpdate(PapiHidingEvent.builder().setSource(PapiHidingSource.PUSH_PARTNER_API).build())
                .build();

        UpdateOfferDataCampEvent event = new UpdateOfferDataCampEvent(1L, offerUpdate);
        DataCampOffer.Offer result = event.convertToDataCampOffer();

        DataCampOfferMeta.UpdateMeta flagMeta = result.getStatus().getDisabled(0).getMeta();
        assertEquals(DataCampOfferMeta.DataSource.PUSH_PARTNER_API, flagMeta.getSource());
        assertEquals(DateTimes.toTimestamp(Instant.MIN), flagMeta.getTimestamp());
    }

    @Test
    void statusDisabled_ifHidingRequestIsNotSetThenZeroDisabledFlags() {
        OfferUpdate offerUpdate = offerBuilder().build();

        UpdateOfferDataCampEvent event = new UpdateOfferDataCampEvent(1L, offerUpdate);
        DataCampOffer.Offer result = event.convertToDataCampOffer();

        assertEquals(0, result.getStatus().getDisabledCount());
    }

    @Test
    void statusAvailableForBusinesses_ifAvailableIsTrueThenFlagIsTrue() {
        boolean isAvailableForBusiness = true;
        OfferUpdate offerUpdate = offerBuilder()
                .setRequestedAvailableForBusinesses(isAvailableForBusiness)
                .build();

        UpdateOfferDataCampEvent event = new UpdateOfferDataCampEvent(1L, offerUpdate);
        DataCampOffer.Offer result = event.convertToDataCampOffer();

        assertTrue(result.getStatus().getAvailableForBusinesses().getFlag());
    }

    @Test
    void statusAvailableForBusinesses_ifAvailableIsFalseThenFlagIsFalse() {
        boolean isAvailableForBusiness = false;
        OfferUpdate offerUpdate = offerBuilder()
                .setRequestedAvailableForBusinesses(isAvailableForBusiness)
                .build();

        UpdateOfferDataCampEvent event = new UpdateOfferDataCampEvent(1L, offerUpdate);
        DataCampOffer.Offer result = event.convertToDataCampOffer();

        assertFalse(result.getStatus().getAvailableForBusinesses().getFlag());
    }

    @Test
    void statusAvailableForBusinesses_ifAvailableIsNotSetThenFlagIsFalse() {
        OfferUpdate offerUpdate = offerBuilder().build();

        UpdateOfferDataCampEvent event = new UpdateOfferDataCampEvent(1L, offerUpdate);
        DataCampOffer.Offer result = event.convertToDataCampOffer();

        assertFalse(result.getStatus().getAvailableForBusinesses().getFlag());
    }

    @Test
    void statusAvailableForBusinesses_ifAvailableIsNotSetThenFlagMetaIsUnknown() {
        OfferUpdate offerUpdate = offerBuilder().build();

        UpdateOfferDataCampEvent event = new UpdateOfferDataCampEvent(1L, offerUpdate);
        DataCampOffer.Offer result = event.convertToDataCampOffer();

        assertEquals(DataCampOfferMeta.DataSource.UNKNOWN_SOURCE,
                result.getStatus().getAvailableForBusinesses().getMeta().getSource());
    }

    @Test
    void statusAvailableForBusinesses_ifAvailableIsSetThenFlagMetaIsPartnerApi() {
        OfferUpdate offerUpdate = offerBuilder()
                .setRequestedAvailableForBusinesses(true)
                .setUpdateTime(Instant.MIN)
                .build();

        UpdateOfferDataCampEvent event = new UpdateOfferDataCampEvent(1L, offerUpdate);
        DataCampOffer.Offer result = event.convertToDataCampOffer();

        DataCampOfferMeta.UpdateMeta flagMeta = result.getStatus().getAvailableForBusinesses().getMeta();
        assertEquals(DataCampOfferMeta.DataSource.PUSH_PARTNER_API, flagMeta.getSource());
        assertEquals(DateTimes.toTimestamp(Instant.MIN), flagMeta.getTimestamp());
    }

    @Test
    void statusProhibitedForPersons_ifProhibitedTrueThenFlagIsTrue() {
        boolean isProhibitedForCustomers = true;
        OfferUpdate offerUpdate = offerBuilder()
                .setRequestedProhibitedForPersons(isProhibitedForCustomers)
                .build();

        UpdateOfferDataCampEvent event = new UpdateOfferDataCampEvent(1L, offerUpdate);
        DataCampOffer.Offer result = event.convertToDataCampOffer();

        assertTrue(result.getStatus().getProhibitedForPersons().getFlag());
    }

    @Test
    void statusProhibitedForPersons_ifProhibitedIsFalseThenFlagIsFalse() {
        boolean isProhibitedForCustomers = true;
        OfferUpdate offerUpdate = offerBuilder()
                .setRequestedProhibitedForPersons(isProhibitedForCustomers)
                .build();

        UpdateOfferDataCampEvent event = new UpdateOfferDataCampEvent(1L, offerUpdate);
        DataCampOffer.Offer result = event.convertToDataCampOffer();

        assertTrue(result.getStatus().getProhibitedForPersons().getFlag());
    }

    @Test
    void statusProhibitedForPersons_ifProhibitedIsNotSetThenFlagIsFalse() {
        OfferUpdate offerUpdate = offerBuilder().build();

        UpdateOfferDataCampEvent event = new UpdateOfferDataCampEvent(1L, offerUpdate);
        DataCampOffer.Offer result = event.convertToDataCampOffer();

        assertFalse(result.getStatus().getProhibitedForPersons().getFlag());
    }

    @Test
    void statusProhibitedForPersons_ifProhibitedIsNotSetThenFlagMetaIsUnknown() {
        OfferUpdate offerUpdate = offerBuilder()
                .build();

        UpdateOfferDataCampEvent event = new UpdateOfferDataCampEvent(1L, offerUpdate);
        DataCampOffer.Offer result = event.convertToDataCampOffer();

        assertEquals(DataCampOfferMeta.DataSource.UNKNOWN_SOURCE,
                result.getStatus().getProhibitedForPersons().getMeta().getSource());
    }

    @Test
    void statusProhibitedForPersons_ifProhibitedIsSetThenFlagMetaIsPartnerApi() {
        OfferUpdate offerUpdate = offerBuilder()
                .setRequestedProhibitedForPersons(true)
                .setUpdateTime(Instant.MIN)
                .build();

        UpdateOfferDataCampEvent event = new UpdateOfferDataCampEvent(1L, offerUpdate);
        DataCampOffer.Offer result = event.convertToDataCampOffer();

        DataCampOfferMeta.UpdateMeta flagMeta = result.getStatus().getProhibitedForPersons().getMeta();
        assertEquals(DataCampOfferMeta.DataSource.PUSH_PARTNER_API, flagMeta.getSource());
        assertEquals(DateTimes.toTimestamp(Instant.MIN), flagMeta.getTimestamp());
    }
}
