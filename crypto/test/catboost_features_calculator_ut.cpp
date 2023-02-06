#include <crypta/lib/native/bigb_catboost_applier/catboost_features_calculator.h>

#include <yabs/proto/user_profile.pb.h>

#include <library/cpp/iterator/enumerate.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/string.h>
#include <util/generic/vector.h>

using namespace NCrypta;

namespace {
    TCatboostFeaturesCalculator GetCalculator() {
        return TCatboostFeaturesCalculator({
            {"bindings_200007098", 2},
            {"bindings_200002552", 0},
            {"bindings_200004930", 1},
            {"operating_systems_3", 4},
            {"mobile_models_iPhone", 3},
        }, {
            {NBSYeti::NCounter::CI_QUERY_CATEGORIES_INTEREST, "bindings"},
        }, {
            {NBSData::NKeywords::KW_DEVICE_MODEL_BT, "mobile_models"},
            {NBSData::NKeywords::KW_DETAILED_DEVICE_TYPE_BT, "operating_systems"},
        });
    };

    NBSYeti::TKeywordDescription MakeTestKeywordDescription(NBSYeti::TKeywordId keywordId, NBSYeti::EKeywordType keywordType) {
        return NBSYeti::TKeywordDescription(
            keywordId, "", false, keywordType, NBSYeti::EUserStatus::Dirty, false, true, false,
            false, false, false, 10, 10, 1, NBSYeti::TLocationV2Set{0}
        );
    }

    const NBSYeti::TKeywordInfo& GetKeywordInfo() {
        static const NBSYeti::TKeywordInfo keywordInfo({
            MakeTestKeywordDescription(NBSData::NKeywords::KW_BT_COUNTER, NBSYeti::EKeywordType::UInt),
            MakeTestKeywordDescription(NBSData::NKeywords::KW_SITE_SEARCH_TEXT, NBSYeti::EKeywordType::UInt),
            MakeTestKeywordDescription(NBSData::NKeywords::KW_DETAILED_DEVICE_TYPE_BT, NBSYeti::EKeywordType::UInt),
            MakeTestKeywordDescription(NBSData::NKeywords::KW_DEVICE_MODEL_BT, NBSYeti::EKeywordType::String),
        });
        return keywordInfo;
    }

    const NBSYeti::TCounterPacker& GetCounterPacker() {
        static const NBSYeti::TCounterPacker packer(NBSYeti::TCounterPackerBuilder::Build(NBSYeti::BuildCounterInfoFromProto()));
        return packer;
    }

    NBSYeti::TProfile PrepareTestProfile() {
        NBSYeti::TProfile profile("y1", GetKeywordInfo(), GetCounterPacker());

        static const TVector<ui64> keys{200007098, 200002552, 200004930};
        static const TVector<float> values{3.46322274208, 8.99720954895, 9.37608909607};

        for (const auto& [key, value]: Zip(keys, values)) {
            profile.Counters.Update(NBSYeti::NCounter::CI_QUERY_CATEGORIES_INTEREST, key, value, 0);
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
        mobileModels.set_string_value("iPhone");
        profile.UserItems.Add(mobileModels);

        NBSYeti::TUserItemProto os;
        os.set_keyword_id(NBSData::NKeywords::KW_DETAILED_DEVICE_TYPE_BT);
        os.add_uint_values(3);
        profile.UserItems.Add(os);

        return profile;
    }

    void AssertFloatVectorsEqual(const TVector<float> &left, const TVector<float> &right) {
        static const float EPS = 0.0001;

        EXPECT_EQ(left.size(), right.size());
        for (const auto&[li, ri] : Zip(left, right)) {
            EXPECT_NEAR(li, ri, EPS);
        }
    }

    const TVector<float> FLOAT_FEATURES_REF{8.99720954895, 9.37608909607, 3.46322274208, 1, 1};
    const TString QUERY_TEXT_REF("сумки karl lagerfeld карл лагерфельд официальных интернет. microsoft прекратит производство xbox one x. новости футбола.");
}

TEST(TCatboostFeaturesCalculator, PrepareTextFeatures) {
    const auto& queriesText = GetCalculator().PrepareTextFeatures(PrepareTestProfile().GetFullPublicProto(false));
    EXPECT_EQ(QUERY_TEXT_REF, queriesText);
}

TEST(TCatboostFeaturesCalculator, PrepareFloatFeatures) {
    const auto& floatFeatures = GetCalculator().PrepareFloatFeatures(PrepareTestProfile().GetFullPublicProto(false));
    AssertFloatVectorsEqual(FLOAT_FEATURES_REF, floatFeatures);
}

TEST(TCatboostFeaturesCalculator, PrepareTextFeaturesBsyetiProfile) {
    const auto& queriesText = GetCalculator().PrepareTextFeatures(PrepareTestProfile());
    EXPECT_EQ(QUERY_TEXT_REF, queriesText);
}

TEST(TCatboostFeaturesCalculator, PrepareFloatFeaturesBsyetiProfile) {
    const auto& floatFeatures = GetCalculator().PrepareFloatFeatures(PrepareTestProfile());
    AssertFloatVectorsEqual(FLOAT_FEATURES_REF, floatFeatures);
}
