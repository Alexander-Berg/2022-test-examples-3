#include <crypta/rt_socdem/lib/cpp/model/socdem_evaluator.h>
#include <crypta/rt_socdem/lib/cpp/model/test_utils/utils.h>

#include <catboost/libs/model/model.h>
#include <yabs/proto/user_profile.pb.h>

#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/hash.h>
#include <util/stream/file.h>

#include <contrib/libs/protobuf-mutator/src/libfuzzer/libfuzzer_macro.h>

using namespace NCrypta::NRtSocdem::NBigb;

DEFINE_PROTO_FUZZER(const yabs::proto::Profile& profile) {
    const NBSYeti::TProfile eagleProfile = NTestUtils::ConvertProfile(profile);

    TSocdemEvaluator evaluator(NTestUtils::GetTestModel(), eagleProfile);
    UNIT_ASSERT_VALUES_EQUAL(2, evaluator.GetGender().Weights.size());
    UNIT_ASSERT_VALUES_EQUAL(6, evaluator.GetAge().Weights.size());
    UNIT_ASSERT_VALUES_EQUAL(5, evaluator.GetIncome().Weights.size());
}
