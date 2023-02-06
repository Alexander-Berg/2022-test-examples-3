#include "arguments_holder.h"

namespace NVH::NPlaylistService {

class TUgcByUuidProcessorTest : public TUgcByUuidProcessor {
public:
    TUgcByUuidProcessorTest(
        const TStreamsByUuidRequest& request,
        const TUserInfo& userInfo,
        const NVideoHosting::TConstantRepository& constantRepository,
        NVideoHosting::TExternalDataProvider& provider,
        NVhApphostStatistics::TVhStatManager& statistics,
        const THashMap<TString, TString>& statisticsTags,
        const TString& statisticsPrefix,
        bool withVsid)
        : TUgcByUuidProcessor(request, userInfo, constantRepository, provider, statistics, statisticsTags, statisticsPrefix, withVsid)
    {
    }

    TStreamsByUuidResponse RenderResponse(TVector<TAtomicSharedPtr<TUgcContentInfo>> ugcContentInfo) {
        DataFromDb = std::move(ugcContentInfo);
        return PrepareRenderInfoInternal();
    }

    void ProcessBaseRequest(const TVector<TStringBuf>& ) override {
    }


    bool IsValidByTag(ui8 requestTag, const TVector<ui8> streamTags) {
        return ValidByTag(requestTag, streamTags);
    }


};

TStreamsByUuidResponse UgcTest(const TStreamsByUuidRequest& request, TVector<TAtomicSharedPtr<TUgcContentInfo>> ugcContentInfo) {
    TTestArgumentsHolder args;
    TUgcByUuidProcessorTest test(request, args.UserInfo, args.ConstantRepository, args.Provider, *args.StatManager, args.StatisticsTags, args.StatisticsPrefix, true);
    return test.RenderResponse(std::move(ugcContentInfo));
}

bool IsValidByTag(ui8 requestTag, const TVector<ui8> streamTags) {
    TStreamsByUuidRequest request;
    TTestArgumentsHolder args;
    TUgcByUuidProcessorTest test(request, args.UserInfo, args.ConstantRepository, args.Provider, *args.StatManager, args.StatisticsTags, args.StatisticsPrefix, true);
    return test.IsValidByTag(requestTag, streamTags);
}

NJson::TJsonValue ReadFromFile(const TString& file) {
    const auto& allFile = NResource::Find(file);
    NJson::TJsonValue result;
    NJson::ReadJsonTree(allFile, &result, true);
    return result;
}

}
