#include "arguments_holder.h"

namespace NVH::NPlaylistService {

class TVhByUuidProcessorTest : public TVhByUuidProcessor {
public:
    TVhByUuidProcessorTest(
        const TStreamsByUuidRequest& request,
        const TUserInfo& userInfo,
        const NVideoHosting::TConstantRepository& constantRepository,
        NVideoHosting::TExternalDataProvider& provider,
        NVhApphostStatistics::TVhStatManager& statistics,
        const THashMap<TString, TString>& statisticsTags,
        const TString& statisticsPrefix,
        bool withVsid)
        : TVhByUuidProcessor(request, userInfo, constantRepository, provider, statistics, statisticsTags, statisticsPrefix, withVsid)
    {
    }

    TStreamsByUuidResponse RenderResponse(TContentVersionByUuidFullData fullData) {
        DataFromDb = std::move(fullData);
        return PrepareRenderInfoInternal();
    }

    void ProcessBaseRequest(const TVector<TStringBuf>& ) override {
    }

};

TStreamsByUuidResponse VhTest(const TStreamsByUuidRequest& request, TContentVersionByUuidFullData fullData) {
    TTestArgumentsHolder args;
    TVhByUuidProcessorTest test(request, args.UserInfo, args.ConstantRepository, args.Provider, *args.StatManager, args.StatisticsTags, args.StatisticsPrefix, true);
    return test.RenderResponse(std::move(fullData));
}

}
