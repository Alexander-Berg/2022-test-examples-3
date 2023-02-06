#include <crypta/prism/lib/model/evaluate.h>
#include <crypta/prism/lib/model/test_utils/utils.h>

#include <catboost/libs/model/model.h>
#include <yabs/proto/user_profile.pb.h>

#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/hash.h>
#include <util/stream/file.h>

#include <contrib/libs/protobuf-mutator/src/libfuzzer/libfuzzer_macro.h>

using namespace NCrypta::NPrism;

DEFINE_PROTO_FUZZER(const yabs::proto::Profile& profile) {
    const NBSYeti::TProfile eagleProfile = NTestUtils::ConvertProfile(profile);

    Evaluate(NTestUtils::GetTestModel(), eagleProfile);
}
