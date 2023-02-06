#include "utils.h"

#include <crypta/lib/native/bigb_catboost_applier/proto/catboost_features_mapping.pb.h>

#include <ads/bsyeti/libs/primitives/query.h>
#include <catboost/libs/model/model.h>
#include <library/cpp/iterator/enumerate.h>
#include <library/cpp/iterator/zip.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/common/env.h>
#include <yabs/proto/user_profile.pb.h>

#include <util/generic/vector.h>
#include <util/stream/file.h>
#include <util/string/join.h>

using namespace NCrypta;
using namespace NCrypta::NRtSocdem::NBigb;

namespace {
    NProtobufJson::TJson2ProtoConfig GetJson2ProtoConfig() {
        NProtobufJson::TJson2ProtoConfig config;
        config.MapAsObject = true;
        config.CastFromString = true;
        return config;
    }

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
        static const NBSYeti::TCounterPacker packer(NBSYeti::TCounterPackerBuilder::Build(NBSYeti::BuildCounterInfoFromProto()));
        return packer;
    }

    TSocdemModel InitTestModel() {
        const auto& config = GetJson2ProtoConfig();

        TStringStream inFeatures(NResource::Find("features_mapping.json"));
        const auto& featuresMapping = NProtobufJson::Json2Proto<NCrypta::TCatboostFeaturesMapping>(inFeatures, config);

        TStringStream inThresholds(NResource::Find("thresholds.json"));
        auto thresholds = NProtobufJson::Json2Proto<TThresholds>(inThresholds, config);

        const auto& rawGenderModel = NResource::Find("gender_rt_model.bin");
        auto genderModel = ReadModel(rawGenderModel.data(), rawGenderModel.size());

        const auto& rawAgeModel = NResource::Find("age_rt_model.bin");
        auto ageModel = ReadModel(rawAgeModel.data(), rawAgeModel.size());

        const auto& rawIncomeModel = NResource::Find("income_rt_model.bin");
        auto incomeModel = ReadModel(rawIncomeModel.data(), rawIncomeModel.size());

        return TSocdemModel{
            .GenderModel = std::move(genderModel),
            .AgeModel = std::move(ageModel),
            .IncomeModel = std::move(incomeModel),
            .Thresholds = std::move(thresholds),
            .CatboostFeaturesCalculator = TCatboostFeaturesCalculator(
                {featuresMapping.GetFeatureToIndex().begin(), featuresMapping.GetFeatureToIndex().end()},
                {featuresMapping.GetCounterToPrefix().begin(), featuresMapping.GetCounterToPrefix().end()},
                {featuresMapping.GetKeywordToPrefix().begin(), featuresMapping.GetKeywordToPrefix().end()})
        };
    }
}

const TSocdemModel& NTestUtils::GetTestModel() {
    static const TSocdemModel model = InitTestModel();
    return model;
}

NBSYeti::TProfile NTestUtils::PrepareTestProfile() {
    NBSYeti::TProfile profile("y1", GetKeywordInfo(), GetCounterPacker());

    static const TVector<ui64> keys{200000073, 200000079, 200000244, 200000246, 200000435};
    static const TVector<float> values{3.46322274208, 8.99720954895, 9.37608909607, 13.7542047501, 29.181854248};

    for (const auto& [key, value]: Zip(keys, values)) {
        profile.Counters.Update(200, key, value, 0);
    }

    const TVector<TString> queryTexts{
        "сумки karl lagerfeld карл лагерфельд официальных интернет",
        "microsoft прекратит производство xbox one x",
        "новости футбола"
    };

    for (const auto& [i, queryText]: Enumerate(queryTexts)) {
        NBSYeti::TQuery query;
        query.SetQueryId(i);
        query.SetQueryText(queryText);

        profile.Queries.Add(query);
    }

    NBSYeti::TUserItemProto mobileModels;
    mobileModels.set_keyword_id(NBSData::NKeywords::KW_DEVICE_MODEL_BT);
    mobileModels.set_string_value("iPhone12,1");
    profile.UserItems.Add(mobileModels);

    mobileModels.set_string_value("iPad");
    profile.UserItems.Add(mobileModels);

    NBSYeti::TUserItemProto os;
    os.set_keyword_id(NBSData::NKeywords::KW_DETAILED_DEVICE_TYPE_BT);
    os.add_uint_values(3);
    profile.UserItems.Add(os);

    NBSYeti::TUserItemProto region;
    region.set_keyword_id(NBSData::NKeywords::KW_USER_REGION);
    region.add_uint_values(213);
    profile.UserItems.Add(region);

    return profile;
}

NBSYeti::TProfile NTestUtils::ConvertProfile(const yabs::proto::Profile& profile) {
    return NBSYeti::TProfile("y1", GetKeywordInfo(), GetCounterPacker(), profile);
}
