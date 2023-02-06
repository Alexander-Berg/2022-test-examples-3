#include <crypta/lib/native/yaml/yaml2proto.h>
#include <crypta/siberia/bin/custom_audience/common/rule/encoded_ca_rule.h>
#include <crypta/siberia/bin/custom_audience/common/rule/encoded_extended_ca_rule.h>
#include <crypta/siberia/bin/custom_audience/common/rule/filter.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta::NSiberia::NCustomAudience;
using namespace NCrypta::NSiberia::NCustomAudience::NFilter;

namespace {
    static const NLab::NEncodedUserData::TTokenToIdDict WORD_DICT = {
        {"word_1", 1},
        {"word_2", 2},
        {"word_3", 3},
        {"word_4", 4}
    };

    static const NLab::NEncodedUserData::TTokenToIdDict HOST_DICT = {
        {"host_5", 5},
        {"host_6", 6},
        {"host_7", 7},
        {"host_8", 8},
        {"host_9", 9},
        {"host_10", 10}
    };

    static const NLab::NEncodedUserData::TTokenToIdDict APP_DICT = {
        {"app_1", 1},
        {"app_2", 2},
        {"app_3", 3},
    };

    static const TString USER_DATA = "attributes:\n"
                                     "  age: from_0_to_17\n"
                                     "  device: desktop\n"
                                     "  gender: male\n"
                                     "  region: 100\n"
                                     "  income: income_b1\n"
                                     "  country: russia\n"
                                     "  city: moscow\n"
                                     "  has_crypta_i_d: true\n"
                                     "segments:\n"
                                     "  segment:\n"
                                     "  - keyword: 1\n"
                                     "    i_d: 2\n"
                                     "  - keyword: 2\n"
                                     "    i_d: 3\n"
                                     "affinities_encoded:\n"
                                     "  hosts:\n"
                                     "  - 5\n"
                                     "  - 6\n"
                                     "  words:\n"
                                     "  - 1\n"
                                     "  - 2\n"
                                     "  apps:\n"
                                     "  - 1\n"
                                     "  - 2\n"
                                     "  affinitive_sites:\n"
                                     "  - id: 5\n"
                                     "    weight: 1.3\n"
                                     "  - id: 6\n"
                                     "    weight: 2.6\n"
                                     "  top_common_sites:\n"
                                     "  - id: 7\n"
                                     "    weight: 1.4\n"
                                     "  - id: 8\n"
                                     "    weight: 2.8";

    static const TString RULE = "ages:\n"
                                "- from_0_to_17\n"
                                "devices:\n"
                                "- desktop\n"
                                "segments:\n"
                                "- keyword: 1\n"
                                "  i_d: 2\n"
                                "- keyword: 2\n"
                                "  i_d: 3\n"
                                "words:\n"
                                "- word_1\n"
                                "- word_2\n"
                                "hosts:\n"
                                "- host_5\n"
                                "- host_6\n"
                                "apps:\n"
                                "- app_1\n"
                                "- app_2\n"
                                "affinitive_sites:\n"
                                "- host_5\n"
                                "- host_6\n"
                                "top_common_sites:\n"
                                "- host_7\n"
                                "- host_8";

    NLab::TUserData CreateUserData() {
        return NCrypta::Yaml2Proto<NLab::TUserData>(USER_DATA);
    }

    TCaRule CreateFullRule() {
        return NCrypta::Yaml2Proto<TCaRule>(RULE);
    }

    TEncodedCaRule GetEncodedRule(const TCaRule& rule) {
        return TEncodedCaRule(rule, WORD_DICT, HOST_DICT, APP_DICT);
    }
}

Y_UNIT_TEST_SUITE(NFilter) {
    Y_UNIT_TEST(FilterAttributes) {
        const auto& userData = CreateUserData();

        TCaRule positiveAgeRule;
        positiveAgeRule.AddAges(NLab::TAge::FROM_0_TO_17);
        positiveAgeRule.AddAges(NLab::TAge::FROM_18_TO_24);
        UNIT_ASSERT(FilterAttributes(userData, GetEncodedRule(positiveAgeRule)));

        TCaRule negativeAgeRule;
        negativeAgeRule.AddAges(NLab::TAge::FROM_18_TO_24);
        UNIT_ASSERT(!FilterAttributes(userData, GetEncodedRule(negativeAgeRule)));

        TCaRule positiveGenderRule;
        positiveGenderRule.SetGender(NLab::TGender::MALE);
        UNIT_ASSERT(FilterAttributes(userData, GetEncodedRule(positiveGenderRule)));

        TCaRule negativeGenderRule;
        negativeGenderRule.SetGender(NLab::TGender::FEMALE);
        UNIT_ASSERT(!FilterAttributes(userData, GetEncodedRule(negativeGenderRule)));

        TCaRule positiveDeviceRule;
        positiveDeviceRule.AddDevices(NLab::TDevice::DESKTOP);
        positiveDeviceRule.AddDevices(NLab::TDevice::PHONE);
        UNIT_ASSERT(FilterAttributes(userData, GetEncodedRule(positiveDeviceRule)));

        TCaRule negativeDeviceRule;
        negativeDeviceRule.AddDevices(NLab::TDevice::PHONE);
        UNIT_ASSERT(!FilterAttributes(userData, GetEncodedRule(negativeDeviceRule)));

        TCaRule positiveMixedRule;
        positiveMixedRule.AddAges(NLab::TAge::FROM_0_TO_17);
        positiveMixedRule.AddAges(NLab::TAge::FROM_18_TO_24);
        positiveMixedRule.AddDevices(NLab::TDevice::DESKTOP);
        positiveMixedRule.AddDevices(NLab::TDevice::PHONE);
        UNIT_ASSERT(FilterAttributes(userData, GetEncodedRule(positiveMixedRule)));

        TCaRule negativeMixedRule;
        negativeMixedRule.AddAges(NLab::TAge::FROM_0_TO_17);
        negativeMixedRule.AddDevices(NLab::TDevice::PHONE);
        UNIT_ASSERT(!FilterAttributes(userData, GetEncodedRule(negativeMixedRule)));
    }

    Y_UNIT_TEST(FilterSegments) {
        const auto& userData = CreateUserData();

        TCaRule singleSegmentPositiveRule;
        auto* segment1 = singleSegmentPositiveRule.AddSegments();
        segment1->SetKeyword(1);
        segment1->SetID(2);
        UNIT_ASSERT(FilterSegments(userData, singleSegmentPositiveRule.GetSegments()));

        // --------------------------------------------

        TCaRule singleSegmentNegativeRule;
        auto* segment2 = singleSegmentNegativeRule.AddSegments();
        segment2->SetKeyword(10);
        segment2->SetID(20);
        UNIT_ASSERT(!FilterSegments(userData, singleSegmentNegativeRule.GetSegments()));

        // --------------------------------------------

        TCaRule multipleSegmentsPositiveRule;
        auto* segment3 = multipleSegmentsPositiveRule.AddSegments();
        segment3->SetKeyword(1);
        segment3->SetID(2);

        auto* segment4 = multipleSegmentsPositiveRule.AddSegments();
        segment4->SetKeyword(10);
        segment4->SetID(20);

        UNIT_ASSERT(FilterSegments(userData, multipleSegmentsPositiveRule.GetSegments()));

        // --------------------------------------------

        TCaRule multipleSegmentsNegativeRule;
        auto* segment5 = multipleSegmentsNegativeRule.AddSegments();
        segment5->SetKeyword(10);
        segment5->SetID(20);

        auto* segment6 = multipleSegmentsNegativeRule.AddSegments();
        segment6->SetKeyword(20);
        segment6->SetID(30);

        UNIT_ASSERT(!FilterSegments(userData, multipleSegmentsNegativeRule.GetSegments()));
    }

    Y_UNIT_TEST(FilterKernel) {
        const auto& userData = CreateUserData();

        TCaRule baseRuleWithUnknownItem;
        baseRuleWithUnknownItem.AddHosts("unknown_host");
        TEncodedCaRule ruleWithUnknownItem(baseRuleWithUnknownItem, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(!FilterKernel(userData, ruleWithUnknownItem));

        // --------------------------------------------

        TCaRule emptyBaseRule;
        TEncodedCaRule emptyRule(emptyBaseRule, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(FilterKernel(userData, emptyRule));

        // --------------------------------------------

        TCaRule wordsPositiveBaseRule;
        wordsPositiveBaseRule.AddWords("word_1");
        wordsPositiveBaseRule.AddWords("word_3");
        TEncodedCaRule wordsPositiveRule(wordsPositiveBaseRule, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(FilterKernel(userData, wordsPositiveRule));

        // --------------------------------------------

        TCaRule wordsNegativeBaseRule;
        wordsNegativeBaseRule.AddWords("word_3");
        wordsNegativeBaseRule.AddWords("word_4");
        TEncodedCaRule wordsNegativeRule(wordsNegativeBaseRule, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(!FilterKernel(userData, wordsNegativeRule));

        // --------------------------------------------

        TCaRule hostsPositiveBaseRule;
        hostsPositiveBaseRule.AddHosts("host_5");
        hostsPositiveBaseRule.AddHosts("host_7");
        TEncodedCaRule hostsPositiveRule(hostsPositiveBaseRule, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(FilterKernel(userData, hostsPositiveRule));

        // --------------------------------------------

        TCaRule hostsNegativeBaseRule;
        hostsNegativeBaseRule.AddHosts("host_7");
        hostsNegativeBaseRule.AddHosts("host_8");
        TEncodedCaRule hostsNegativeRule(hostsNegativeBaseRule, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(!FilterKernel(userData, hostsNegativeRule));

        // --------------------------------------------

        TCaRule appsPositiveBaseRule;
        appsPositiveBaseRule.AddApps("app_1");
        appsPositiveBaseRule.AddApps("app_3");
        TEncodedCaRule appsPositiveRule(appsPositiveBaseRule, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(FilterKernel(userData, appsPositiveRule));

        // --------------------------------------------

        TCaRule appsNegativeBaseRule;
        appsNegativeBaseRule.AddApps("app_3");
        TEncodedCaRule appsNegativeRule(appsNegativeBaseRule, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(!FilterKernel(userData, appsNegativeRule));

        // --------------------------------------------


        TCaRule affinitiveSitesPositiveBaseRule;
        affinitiveSitesPositiveBaseRule.AddAffinitiveSites("host_5");
        affinitiveSitesPositiveBaseRule.AddAffinitiveSites("host_7");
        TEncodedCaRule affinitiveSitesPositiveRule(affinitiveSitesPositiveBaseRule, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(FilterKernel(userData, affinitiveSitesPositiveRule));

        // --------------------------------------------

        TCaRule affinitiveSitesNegativeBaseRule;
        affinitiveSitesNegativeBaseRule.AddAffinitiveSites("host_7");
        affinitiveSitesNegativeBaseRule.AddAffinitiveSites("host_8");
        TEncodedCaRule affinitiveSitesNegativeRule(affinitiveSitesNegativeBaseRule, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(!FilterKernel(userData, affinitiveSitesNegativeRule));

        // --------------------------------------------

        TCaRule topCommonSitesPositiveBaseRule;
        topCommonSitesPositiveBaseRule.AddTopCommonSites("host_7");
        topCommonSitesPositiveBaseRule.AddTopCommonSites("host_9");
        TEncodedCaRule topCommonSitesPositiveRule(topCommonSitesPositiveBaseRule, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(FilterKernel(userData, topCommonSitesPositiveRule));

        // --------------------------------------------

        TCaRule topCommonSitesNegativeBaseRule;
        topCommonSitesNegativeBaseRule.AddTopCommonSites("host_9");
        topCommonSitesNegativeBaseRule.AddTopCommonSites("host_10");
        TEncodedCaRule topCommonSitesNegativeRule(topCommonSitesNegativeBaseRule, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(!FilterKernel(userData, topCommonSitesNegativeRule));

        // --------------------------------------------
        TCaRule mixedPositiveBaseRule;
        mixedPositiveBaseRule.AddWords("word_3");
        mixedPositiveBaseRule.AddWords("word_4");

        mixedPositiveBaseRule.AddHosts("host_7");
        mixedPositiveBaseRule.AddHosts("host_8");

        mixedPositiveBaseRule.AddApps("app_1");

        mixedPositiveBaseRule.AddAffinitiveSites("host_7");
        mixedPositiveBaseRule.AddAffinitiveSites("host_8");

        mixedPositiveBaseRule.AddTopCommonSites("host_9");
        mixedPositiveBaseRule.AddTopCommonSites("host_10");

        auto* segment1 = mixedPositiveBaseRule.AddSegments();
        segment1->SetKeyword(1);
        segment1->SetID(2);

        TEncodedCaRule mixedPositiveRule(mixedPositiveBaseRule, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(FilterKernel(userData, mixedPositiveRule));

        // --------------------------------------------

        TCaRule mixedNegativeBaseRule;
        mixedNegativeBaseRule.AddWords("word_3");
        mixedNegativeBaseRule.AddWords("word_4");

        mixedNegativeBaseRule.AddHosts("host_7");
        mixedNegativeBaseRule.AddHosts("host_8");

        mixedNegativeBaseRule.AddAffinitiveSites("host_7");
        mixedNegativeBaseRule.AddAffinitiveSites("host_8");

        mixedNegativeBaseRule.AddTopCommonSites("host_9");
        mixedNegativeBaseRule.AddTopCommonSites("host_10");

        auto* segment2 = mixedNegativeBaseRule.AddSegments();
        segment2->SetKeyword(10);
        segment2->SetID(20);

        TEncodedCaRule mixedNegativeRule(mixedNegativeBaseRule, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(!FilterKernel(userData, mixedNegativeRule));
    }

    Y_UNIT_TEST(Filter) {
        const auto& userData = CreateUserData();

        auto positiveBaseRule = CreateFullRule();

        Cout << positiveBaseRule.DebugString() << Endl;
        TEncodedCaRule positiveRule(positiveBaseRule, WORD_DICT, HOST_DICT, APP_DICT);
        UNIT_ASSERT(Filter(userData, positiveRule));

        // --------------------------------------------

        auto negativeBaseRule = CreateFullRule();
        negativeBaseRule.AddRegions(1);
        TEncodedCaRule negativeRule(negativeBaseRule, WORD_DICT, HOST_DICT, APP_DICT);

        UNIT_ASSERT(!Filter(userData, negativeRule));
    }

    Y_UNIT_TEST(HasPhrase) {
        const auto& phrase = TEncodedExtendedCaRule::TPhrase({1, 2}, false);
        UNIT_ASSERT(HasPhrase({1, 2, 3}, phrase));
        UNIT_ASSERT(!HasPhrase({1, 4}, phrase));

        const auto& phraseWithUnkownWords = TEncodedExtendedCaRule::TPhrase({1, 2}, true);
        UNIT_ASSERT(!HasPhrase({1, 2, 3}, phraseWithUnkownWords));
        UNIT_ASSERT(!HasPhrase({1, 4}, phraseWithUnkownWords));
    }

    Y_UNIT_TEST(SatisfyPhraseGroup) {
        const auto& phraseGroup = TEncodedExtendedCaRule::TPhraseGroup(
            {
                TEncodedExtendedCaRule::TPhrase({1, 2}, false),
                TEncodedExtendedCaRule::TPhrase({3, 4}, false)
            },
            false
        );
        UNIT_ASSERT(SatisfyPhraseGroup({1, 2, 3}, phraseGroup));
        UNIT_ASSERT(!SatisfyPhraseGroup({2, 3}, phraseGroup));

        const auto& negativePhraseGroup = TEncodedExtendedCaRule::TPhraseGroup(
            {
                TEncodedExtendedCaRule::TPhrase({1, 2}, false),
                TEncodedExtendedCaRule::TPhrase({3, 4}, false)
            },
            true
        );
        UNIT_ASSERT(!SatisfyPhraseGroup({1, 2, 3}, negativePhraseGroup));
        UNIT_ASSERT(SatisfyPhraseGroup({2, 3}, negativePhraseGroup));
    }

    Y_UNIT_TEST(SatisfyPhraseGroups) {
        const TVector<TEncodedExtendedCaRule::TPhraseGroup>& phraseGroups = {
            TEncodedExtendedCaRule::TPhraseGroup(
                {TEncodedExtendedCaRule::TPhrase({3, 4}, false)},
                false
            ),
            TEncodedExtendedCaRule::TPhraseGroup(
                {TEncodedExtendedCaRule::TPhrase({1, 2}, false)},
                true
            ),
        };
        const TVector<ui32>& words1 = {1, 2, 3, 4};
        const TVector<ui32>& words2 = {3};
        const TVector<ui32>& words3 = {2, 3, 4};
        const TVector<ui32>& words4 = {3, 4};

        const auto& proto1 = ::google::protobuf::RepeatedField<ui32>(words1.begin(), words1.end());
        const auto& proto2 = ::google::protobuf::RepeatedField<ui32>(words2.begin(), words2.end());
        const auto& proto3 = ::google::protobuf::RepeatedField<ui32>(words3.begin(), words3.end());
        const auto& proto4 = ::google::protobuf::RepeatedField<ui32>(words4.begin(), words4.end());

        UNIT_ASSERT(!SatisfyPhraseGroups(proto1, phraseGroups));
        UNIT_ASSERT(!SatisfyPhraseGroups(proto2, phraseGroups));
        UNIT_ASSERT(SatisfyPhraseGroups(proto3, phraseGroups));
        UNIT_ASSERT(SatisfyPhraseGroups(proto4, phraseGroups));
    }

    Y_UNIT_TEST(SatisfyTokenGroup) {
        const auto& tokenGroup = TEncodedExtendedCaRule::TTokenGroup({1, 2, 3, 4}, false, false);
        UNIT_ASSERT(SatisfyTokenGroup({1, 2, 3}, tokenGroup));
        UNIT_ASSERT(!SatisfyTokenGroup({5, 6}, tokenGroup));

        const auto& negativeTokenGroup = TEncodedExtendedCaRule::TTokenGroup({1, 2, 3, 4}, false, true);
        UNIT_ASSERT(!SatisfyTokenGroup({1, 2, 3}, negativeTokenGroup));
        UNIT_ASSERT(SatisfyTokenGroup({5, 6}, negativeTokenGroup));
    }

    Y_UNIT_TEST(SatisfyTokenGroups) {
        const TVector<TEncodedExtendedCaRule::TTokenGroup>& tokenGroups = {
            TEncodedExtendedCaRule::TTokenGroup(
                {3, 4},
                false,
                false
            ),
            TEncodedExtendedCaRule::TTokenGroup(
                {1, 2},
                false,
                true
            ),
        };
        const TVector<ui32>& tokens1 = {1, 2, 3, 4};
        const TVector<ui32>& tokens2 = {3};
        const TVector<ui32>& tokens3 = {2, 3, 4};
        const TVector<ui32>& tokens4 = {3, 4};

        const auto& proto1 = ::google::protobuf::RepeatedField<ui32>(tokens1.begin(), tokens1.end());
        const auto& proto2 = ::google::protobuf::RepeatedField<ui32>(tokens2.begin(), tokens2.end());
        const auto& proto3 = ::google::protobuf::RepeatedField<ui32>(tokens3.begin(), tokens3.end());
        const auto& proto4 = ::google::protobuf::RepeatedField<ui32>(tokens4.begin(), tokens4.end());

        UNIT_ASSERT(!SatisfyTokenGroups(proto1, tokenGroups));
        UNIT_ASSERT(SatisfyTokenGroups(proto2, tokenGroups));
        UNIT_ASSERT(!SatisfyTokenGroups(proto3, tokenGroups));
        UNIT_ASSERT(SatisfyTokenGroups(proto4, tokenGroups));
    }
}
