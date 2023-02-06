#include "utils.h"
#include <ads/bsyeti/libs/counter_lib/counter_packer/counter_packer.h>

#include <crypta/lib/native/bigb_catboost_applier/proto/catboost_features_mapping.pb.h>

#include <ads/bsyeti/libs/primitives/query.h>
#include <catboost/libs/model/model.h>
#include <library/cpp/iterator/enumerate.h>
#include <library/cpp/iterator/zip.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <yabs/proto/user_profile.pb.h>

#include <util/generic/vector.h>
#include <util/stream/file.h>
#include <util/string/join.h>

using namespace NCrypta;
using namespace NCrypta::NPrism;

namespace {
    NProtobufJson::TJson2ProtoConfig GetJson2ProtoConfig() {
        NProtobufJson::TJson2ProtoConfig config;
        config.MapAsObject = true;
        config.CastFromString = true;
        return config;
    }

    const float EPS = 0.0001;

    NBSYeti::TKeywordInfo InitKeywordInfo() {
        const auto& rawKeywordInfo = NResource::Find("keyword_info.json");
        TMemoryInput keywordInfoInput(rawKeywordInfo);
        return NBSYeti::BuildKeywordInfoFromJson(keywordInfoInput);
    }

    const NBSYeti::TKeywordInfo& GetKeywordInfo() {
        static const NBSYeti::TKeywordInfo keywordInfo(InitKeywordInfo());
        return keywordInfo;
    }

    const NBSYeti::TCounterPacker& GetCounterPacker() {
        static const NBSYeti::TCounterPacker& packer = NBSYeti::GetCounterPacker();
        return packer;
    }

    TModel InitTestModel() {
        const auto& config = GetJson2ProtoConfig();

        TStringStream inFeatures(NResource::Find("features_mapping.json"));
        const auto& featuresMapping = NProtobufJson::Json2Proto<NCrypta::TCatboostFeaturesMapping>(inFeatures, config);

        TStringStream inThresholds(NResource::Find("thresholds.json"));
        auto thresholds = NProtobufJson::Json2Proto<TThresholds>(inThresholds, config);

        TStringStream inClusters(NResource::Find("clusters.json"));
        auto clusters = NProtobufJson::Json2Proto<TClusters>(inClusters, config);

        const auto& rawModel = NResource::Find("model.bin");
        auto model = ReadModel(rawModel.data(), rawModel.size());

        return TModel{
            .Model = std::move(model),
            .Thresholds = std::move(thresholds),
            .Clusters = std::move(clusters),
            .CatboostFeaturesCalculator = TCatboostFeaturesCalculator(
                {featuresMapping.GetFeatureToIndex().begin(), featuresMapping.GetFeatureToIndex().end()},
                {featuresMapping.GetCounterToPrefix().begin(), featuresMapping.GetCounterToPrefix().end()},
                {featuresMapping.GetKeywordToPrefix().begin(), featuresMapping.GetKeywordToPrefix().end()})
        };
    }
}

void NTestUtils::AssertFloatVectorsEqual(const TVector<float> &left, const TVector<float> &right) {
    EXPECT_EQ(left.size(), right.size());
    for (const auto&[li, ri] : Zip(left, right)) {
        EXPECT_NEAR(li, ri, EPS);
    }
}

const TModel& NTestUtils::GetTestModel() {
    static const TModel model = InitTestModel();
    return model;
}

NBSYeti::TProfile NTestUtils::PrepareTestProfile() {
    NBSYeti::TProfile profile("y1", GetKeywordInfo(), GetCounterPacker());

    static const TVector<ui64> keys{200006283,200004279,200000645,200027720,200002554,200001097,200000379,200051912,200002899};
    static const TVector<float> values{1,2,2.55751419067,1,7.48025560379,1.99999666214,2.55751419067,2.99991559982,1};

    for (const auto& [key, value]: Zip(keys, values)) {
        profile.Counters.Update(NBSYeti::NCounter::CI_QUERY_CATEGORIES_INTEREST, key, value, 0);
    }

    const TVector<TString> queryTexts{
        "попутные грузоперевозки по россии",
        "терка чеснок и целлофан",
        "виды бетоносмесителей",
        "бетоносмеситель бетоносмеситель",
        "саженцы клубники оптом по снг",
        "игра аркада кухонная лихорадка",
        "доставляем груз от 50 кг",
        "цветочный магазин в ереване",
        "маникюр стразами фото дизайн 2020 2021 видео",
        "пищевая соль оптом от производителя",
    };

    for (const auto& [i, queryText]: Enumerate(queryTexts)) {
        NBSYeti::TQuery query;
        query.SetQueryId(i);
        query.SetQueryText(queryText);

        profile.Queries.Add(query);
    }

    NBSYeti::TUserItemProto mobileModels;
    mobileModels.set_keyword_id(NBSData::NKeywords::KW_DEVICE_MODEL_BT);
    mobileModels.set_string_value("Galaxy A51");
    profile.UserItems.Add(mobileModels);

    mobileModels.set_string_value("SM-A307FN");
    profile.UserItems.Add(mobileModels);

    NBSYeti::TUserItemProto os;
    os.set_keyword_id(NBSData::NKeywords::KW_DETAILED_DEVICE_TYPE_BT);
    os.add_uint_values(2);
    profile.UserItems.Add(os);

    NBSYeti::TUserItemProto region;
    region.set_keyword_id(NBSData::NKeywords::KW_USER_REGION);
    region.add_uint_values(2);
    profile.UserItems.Add(region);

    return profile;
}

NBSYeti::TProfile NTestUtils::ConvertProfile(const yabs::proto::Profile& profile) {
    return NBSYeti::TProfile("y1", GetKeywordInfo(), GetCounterPacker(), profile);
}
