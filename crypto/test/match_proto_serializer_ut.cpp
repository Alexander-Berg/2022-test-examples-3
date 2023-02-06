#include <crypta/cm/services/common/data/id_utils.h>
#include <crypta/cm/services/common/data/proto_helpers.h>
#include <crypta/cm/services/common/serializers/match/proto/match_proto_serializer.h>
#include <crypta/lib/native/proto_serializer/proto_serializer.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/vector.h>

#include <algorithm>

using namespace NCrypta;
using namespace NCrypta::NCm;

TMatch CreateMatch() {
    return TMatch(
        NCm::TId("tag", "XXXYYY"),
        TMatch::TMatchedIds{
            {"yandexuid", TMatchedId(NCm::TId("yandexuid", "100015001"), TInstant::Seconds(1500000001), 0, TAttributes({{SYNT_ATTRIBUTE, SYNT_TRUE_STR}}))},
            {"icookie", TMatchedId(NCm::TId("icookie", "100015002"), TInstant::Seconds(1500000002), 0, TAttributes({{SYNT_ATTRIBUTE, SYNT_FALSE_STR}, {"something-else", "foo"}}))}
        }
    );
}

TMatch CreateMatchWithMultipleAttributies() {
    return TMatch(
        NCm::TId("tag", "XXXYYY"),
        TMatch::TMatchedIds{
            {"yandexuid", TMatchedId(NCm::TId("yandexuid", "100015001"), TInstant::Seconds(1500000001), 0, TAttributes({{SYNT_ATTRIBUTE, SYNT_TRUE_STR}, {REALTIME_ATTRIBUTE, REALTIME_TRUE_STR}}))},
            {"icookie", TMatchedId(NCm::TId("icookie", "100015002"), TInstant::Seconds(1500000002), 0, TAttributes({{SYNT_ATTRIBUTE, SYNT_FALSE_STR}, {REALTIME_ATTRIBUTE, REALTIME_FALSE_STR}}))}
        }
    );
}

void AssertAttributesEqual(const TMatchedId& id, const crypta::cm::proto::TMatchedId& idProto) {
    const auto& protoAttrs = idProto.GetAttributes();
    const auto& idAttrs = id.GetAttributes();

    for (const auto& protoAttr : protoAttrs) {
        UNIT_ASSERT_STRINGS_EQUAL(id.GetAttributes().at(protoAttr.name()), protoAttr.value());
    }

    for (const auto& idAttr : idAttrs) {
        UNIT_ASSERT(std::find_if(protoAttrs.begin(), protoAttrs.end(),
            [&idAttr](const auto& protoAttr) { return protoAttr.name() == idAttr.first; }) != protoAttrs.end());
    }
}

void AssertMatchedIdEqual(const TMatchedId& id, const crypta::cm::proto::TMatchedId& idProto) {
    UNIT_ASSERT_STRINGS_EQUAL(id.GetId().Type, idProto.GetId().GetType());
    UNIT_ASSERT_STRINGS_EQUAL(id.GetId().Value, idProto.GetId().GetValue());
    UNIT_ASSERT_EQUAL(id.GetMatchTs().Seconds(), idProto.GetMatchTs());
    UNIT_ASSERT_EQUAL(id.GetCas(), idProto.GetCas());

    AssertAttributesEqual(id, idProto);
}

Y_UNIT_TEST_SUITE(TMatchProtoSerializer) {
    using namespace NCrypta::NCm;

    Y_UNIT_TEST(ToProtoString) {
        const auto& match = CreateMatch();
        const auto& protoStr = NMatchSerializer::ToProtoString(match);

        const auto& matchProto = NProtoSerializer::CreateFromString<TMatchProto>(protoStr);

        UNIT_ASSERT_STRINGS_EQUAL(match.GetExtId().Type, matchProto.GetExtId().GetType());
        UNIT_ASSERT_STRINGS_EQUAL(match.GetExtId().Value, matchProto.GetExtId().GetValue());
        UNIT_ASSERT_EQUAL(2, match.GetInternalIds().size());
        UNIT_ASSERT_EQUAL(2, matchProto.GetMatchedIds().IdsSize());
        for (const auto& id: matchProto.GetMatchedIds().GetIds()) {
            AssertMatchedIdEqual(match.GetInternalIds().at(id.GetId().GetType()), id);
        }
    }

    Y_UNIT_TEST(ToProtoStringWithMultipleAttributies) {
        const auto& match = CreateMatchWithMultipleAttributies();
        const auto& protoStr = NMatchSerializer::ToProtoString(match);

        const auto& matchProto = NProtoSerializer::CreateFromString<TMatchProto>(protoStr);

        UNIT_ASSERT_STRINGS_EQUAL(match.GetExtId().Type, matchProto.GetExtId().GetType());
        UNIT_ASSERT_STRINGS_EQUAL(match.GetExtId().Value, matchProto.GetExtId().GetValue());
        UNIT_ASSERT_EQUAL(2, match.GetInternalIds().size());
        UNIT_ASSERT_EQUAL(2, matchProto.GetMatchedIds().IdsSize());
        for (const auto& id: matchProto.GetMatchedIds().GetIds()) {
            AssertMatchedIdEqual(match.GetInternalIds().at(id.GetId().GetType()), id);
        }
    }

    Y_UNIT_TEST(SerializeToProtoDeserializeFromProto) {
        const auto& match = CreateMatch();
        const auto& serializedMatch = NMatchSerializer::ToProto(match);
        const auto& deserializedMatch = NMatchSerializer::FromProto(serializedMatch);

        UNIT_ASSERT_EQUAL(match, deserializedMatch);
    }

    Y_UNIT_TEST(DeserializeEmptyFromProtoString) {
        UNIT_ASSERT_EXCEPTION(NMatchSerializer::FromProtoString(""), yexception);
    }

    Y_UNIT_TEST(TrackBackReference) {
        auto match = CreateMatch();

        for (const auto& trackBackReference : TVector<bool>{false, true}) {
            match.SetTrackBackReference(trackBackReference);

            const auto& serializedMatch = NMatchSerializer::ToProto(match);
            const auto& deserializedMatch = NMatchSerializer::FromProto(serializedMatch);

            UNIT_ASSERT_EQUAL(match, deserializedMatch);
            UNIT_ASSERT_EQUAL(trackBackReference, serializedMatch.GetTrackBackReference());
            UNIT_ASSERT_EQUAL(trackBackReference, deserializedMatch.GetTrackBackReference());

            const auto& serializedToString = NMatchSerializer::ToProtoString(match);
            const auto& deserializedFromString = NMatchSerializer::FromProtoString(serializedToString);
            UNIT_ASSERT_EQUAL(match, deserializedFromString);
            UNIT_ASSERT_EQUAL(trackBackReference, deserializedFromString.GetTrackBackReference());
        }
    }
}
