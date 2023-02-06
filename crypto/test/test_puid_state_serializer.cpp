#include <crypta/styx/services/common/data/proto/puid_state.pb.h>
#include <crypta/styx/services/common/data/proto_comparators.h>
#include <crypta/styx/services/common/serializers/puid_state_serializer.h>
#include <crypta/styx/services/common/data/test_helpers.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/vector.h>

#include <utility>

using namespace NCrypta::NStyx;
using namespace NCrypta::NStyx::NTest;

namespace {
    const ui64 REF_PUID = 100500;
    const TVector<TOblivionEvent> REF_OBLIVION_EVENTS = {
            CreateOblivionEvent(TInstant::Seconds(1600000000), "some_hash"),
            CreateOblivionEvent(TInstant::Seconds(1700000000), "some_new_hash"),
    };
    const TString REF_SERIALIZED = "\x08\x94\x91\6\x12\x11\x08\x80\xA0\xF8\xFA\5\x12\tsome_hash\x12\x15\x08\x80\xE2\xCF\xAA\6\x12\rsome_new_hash";
}

TEST(NPuidStateSerializer, Serialize) {
    const auto& puidState = NTest::GetPuidState(REF_PUID, REF_OBLIVION_EVENTS);
    EXPECT_EQ(REF_SERIALIZED, NPuidStateSerializer::ToString(puidState));
}

TEST(NPuidStateSerializer, Deserialize) {
    TPuidState puidState;
    NPuidStateSerializer::FromString(REF_SERIALIZED, puidState);

    EXPECT_EQ(REF_PUID, puidState.GetPuid());

    const auto& events = OblivionEventsToVector(puidState.GetOblivionEvents());
    EXPECT_EQ(REF_OBLIVION_EVENTS, events);
}
