#include "common.h"
#include "prepare_test_data.h"

#include <crypta/lib/proto/user_data/user_data_stats.pb.h>
#include <crypta/lookalike/lib/native/segment_features_calculator.h>

#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <util/string/split.h>

using namespace NCrypta::NLookalike;

namespace {
    static const NLab::TUserDataStats userDataStats = PrepareTestSegmentData();
    static const TSegmentFeaturesCalculator segmentFeaturesCalculator(MakeFeaturesMapping());
}

Y_UNIT_TEST_SUITE(TSegmentFeaturesCalculator) {
    Y_UNIT_TEST(PrepareAffinitiveSitesIds) {
        TVector<TString> segmentAffinitiveSitesIds = StringSplitter(
                segmentFeaturesCalculator.PrepareAffinitiveSitesIds(userDataStats)).Split(' ');
        TVector<TString> reference = StringSplitter("belssb.ru balashikha.regmarkets.ru inbalashikha.ru balashiha.tiu.ru balashikha.cian.ru riamobalashiha.ru pga.gazprombank.ru koleso.ru mosoblast.rt.ru lkk.mosoblgaz.ru parking.mos.ru rpgu.emias.mosreg.ru globus.ru dobrodel.mosreg.ru hawk.ru moscow.petrovich.ru children.school.mosreg.ru xn--90aijkdmaud0d.xn--p1ai uslugi.mosreg.ru zdorov.ru").Split(' ');
        UNIT_ASSERT_EQUAL(segmentAffinitiveSitesIds.size(), reference.size());
        AssertAffinitiesEqual(segmentAffinitiveSitesIds, reference);
    }

    Y_UNIT_TEST(PrepareAffinitiveApps) {
        TVector<TString> segmentAffinitiveApps = StringSplitter(
                segmentFeaturesCalculator.PrepareAffinitiveApps(userDataStats)).Split(' ');
        TVector<TString> reference = StringSplitter("1701137006649742528 4415844435471749707 13258630625277259530 2602359023523816535 17070949863655638876 12832752899727708754 3535685675424256529 1738688780238819464 14665485691697159128 11996346681795636274 13858740684145194394 16188766909187980536 1629428043334141080 5499112896414709141 10233705656694308248 6381836103486858947 4315947600988176081 11141851921676273545 11920439166966769960").Split(' ');
        UNIT_ASSERT_EQUAL(segmentAffinitiveApps.size(), reference.size());
        AssertAffinitiesEqual(segmentAffinitiveApps, reference);
    }

    Y_UNIT_TEST(PrepareFloatFeatures) {
        TVector<TString> segmentFloatFeatures = StringSplitter(
                segmentFeaturesCalculator.PrepareFloatFeatures(userDataStats)).Split(',');
        TVector<TString> reference = StringSplitter("0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.0441942,0.100942,0.100942,0.100942,0.100942,0.100942,0.100942,0.100942,0.00504711,0.644347,0.350606,0.00302826,0.0114401,0.0316285,0.215343,0.403432,0.221736,0.113392,0.000336474,0.00538358,0.0286003,0.248654,0.585801,0.131225").Split(',');
        UNIT_ASSERT_EQUAL(segmentFloatFeatures.size(), reference.size());
        AssertStringFloatVectorsEqual(segmentFloatFeatures, reference);
    }
}