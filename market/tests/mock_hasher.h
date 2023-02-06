#pragma once

#include <market/idx/feeds/qparser/inc/feed_info.h>
#include <market/idx/feeds/qparser/inc/writer.h>

struct TMockProcessedOfferHasher: public NMarket::IProcessedOfferHasher {
    virtual TString CalculateHash(
        const NMarket::TFeedInfo&,
        NMarket::IWriter::TConstMsgPtr msg
    ) const override {
        return msg->DataCampOffer.identifiers().offer_id();
    }
};
