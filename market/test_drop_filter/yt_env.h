#include <mapreduce/yt/interface/fwd.h>
#include <mapreduce/yt/interface/client.h>
#include <market/idx/feeds/qparser/inc/feed_info.h>
#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

namespace NMarket {

class TYtEnv {
public:
    static Market::DataCamp::Offer CreateServiceOffer(
        const NMarket::TFeedInfo& feedInfo,
        const TString& offerId,
        ui64 price,
        Market::DataCamp::MarketColor color
    );
    static void Shutdown();

    TYtEnv(bool realDataTest);
    ~TYtEnv();
    
    void Initialize();
    TString GetServiceTablePath();
    void InsertServiceOffers(const TVector<Market::DataCamp::Offer>& offers);

private:
    bool IsRealDataTest_;
    NYT::IClientPtr Client_;
    const TString ServiceTablePath_ = "//home/service_offers";
    const TString RealServiceTablePath_ = "//home/market/testing/indexer/datacamp/united/service_offers";
};

} // namespace
