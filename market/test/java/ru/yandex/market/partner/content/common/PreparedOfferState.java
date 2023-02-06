package ru.yandex.market.partner.content.common;

import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.utils.DcpOfferBuilder;

/**
 * @author Nur-Magomed Dzhamiev <a href="mailto:n-mago@yandex-team.ru"></a>
 */
public class PreparedOfferState {

    private DatacampOffer datacampOffer;
    private DcpOfferBuilder dcpOfferBuilder;

    public PreparedOfferState(DatacampOffer datacampOffer) {
        this.datacampOffer = datacampOffer;
        dcpOfferBuilder = new DcpOfferBuilder(datacampOffer.getBusinessId(), datacampOffer.getOfferId());
        prepareRequiredParams();
    }

    public PreparedOfferState reInitWithNewIdentifiers(int bizId, String offerId) {
        dcpOfferBuilder = new DcpOfferBuilder(datacampOffer.getBusinessId(), datacampOffer.getOfferId());
        datacampOffer.setBusinessId(bizId);
        datacampOffer.setOfferId(offerId);
        return this;
    }

    private void prepareRequiredParams() {
        String offerKey = String.format("Offer(bizId = %d, offerId = %s)",
            datacampOffer.getBusinessId(), datacampOffer.getOfferId());
        dcpOfferBuilder
            .withName("Name for " + offerKey)
            .withDescription("Description for " + offerKey)
            .withVendor("Vendor for " + offerKey)
            .withBarCodes("797266714467");
    }

    public DcpOfferBuilder getDcpOfferBuilder() {
        return dcpOfferBuilder;
    }

    public DatacampOffer getDatacampOffer() {
        return datacampOffer;
    }

    public DatacampOffer buildDatacampOffer() {
        datacampOffer.setData(dcpOfferBuilder.build());
        return datacampOffer;
    }

}
