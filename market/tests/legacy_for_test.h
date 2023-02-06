#pragma once

#include <market/qpipe/qbid/qbidengine/mbi_bids.h>
#include <util/string/cast.h>

class IMbiProtocolLegacyWrap
{
public:
    IMbiProtocolLegacyWrap() = default;
    virtual ~IMbiProtocolLegacyWrap() = default;
protected:
    virtual void SetFeedOfferId(MBI::Bid& , int64_t , const TProtoStringType& )
    {
    }
    virtual void SetDomainId(MBI::Bid&, const TProtoStringType&)
    {
    }
};

#define MBI_PROTOCOL_DEPRECATED_IMPL                                                            \
    void SetFeedOfferId(MBI::Bid& record, int64_t feedId, const TProtoStringType& offer_id)     \
    {                                                                                           \
        record.set_feed_id(feedId);                                                             \
        record.set_domain_id(offer_id);                                                         \
    }                                                                                           \
    void SetDomainId(MBI::Bid& record, const TProtoStringType& domain_id)                       \
    {                                                                                           \
        record.set_domain_id(domain_id);                                                        \
    }                                                                                           \


#define MBI_PROTOCOL_NEW_IMPL                                                                   \
    void SetFeedOfferId(MBI::Bid& record, int64_t feedId, const TProtoStringType& offer_id)     \
    {                                                                                           \
        record.add_domain_ids(::ToString<int64_t>(feedId));                    \
        record.add_domain_ids(offer_id);                                                        \
    }                                                                                           \
    void SetDomainId(MBI::Bid& record, const TProtoStringType& domain_id)                       \
    {                                                                                           \
        record.add_domain_ids(domain_id);                                                       \
    }                                                                                           \
