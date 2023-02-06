#include <crypta/cm/services/common/data/matched_id.h>
#include <crypta/lib/native/test/assert_equality.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/vector.h>


Y_UNIT_TEST_SUITE(TMatchedId) {
    using namespace NCrypta;
    using namespace NCrypta::NCm;

    const TId ID_1("type", "value");
    const TId ID_2("type-2", "value-2");

    const TInstant MATCH_TS = TInstant::Seconds(1);
    const ui64 CAS = 2;

    const TAttributes ATTRS_1({{"foo", "bar"}});
    const TAttributes ATTRS_2({{"foo-2", "bar-2"}});

    Y_UNIT_TEST(ConstructAndClear) {
        TMatchedId matchedId(ID_1, MATCH_TS, CAS, ATTRS_1);

        UNIT_ASSERT_EQUAL(TId("type", "value"), matchedId.GetId());
        UNIT_ASSERT_EQUAL(MATCH_TS, matchedId.GetMatchTs());
        UNIT_ASSERT_EQUAL(CAS, matchedId.GetCas());
        UNIT_ASSERT_EQUAL(ATTRS_1, matchedId.GetAttributes());

        matchedId.Clear();

        UNIT_ASSERT_EQUAL(TId(), matchedId.GetId());
        UNIT_ASSERT_EQUAL(TInstant::Zero(), matchedId.GetMatchTs());
        UNIT_ASSERT_EQUAL(0, matchedId.GetCas());
        UNIT_ASSERT_EQUAL(TAttributes(), matchedId.GetAttributes());
    }

    Y_UNIT_TEST(GettersSetters) {
        TMatchedId matchedId(ID_1, MATCH_TS, CAS, ATTRS_1);

        matchedId.SetId(ID_2);
        UNIT_ASSERT_EQUAL(ID_2, matchedId.GetId());

        matchedId.SetMatchTs(TInstant::Seconds(100500));
        UNIT_ASSERT_EQUAL(TInstant::Seconds(100500), matchedId.GetMatchTs());

        matchedId.SetCas(400500);
        UNIT_ASSERT_EQUAL(400500, matchedId.GetCas());

        matchedId.SetAttributes(TAttributes(ATTRS_2));
        UNIT_ASSERT_EQUAL(ATTRS_2, matchedId.GetAttributes());

        const TMatchedId ref(ID_2, TInstant::Seconds(100500), 400500, ATTRS_2);
        UNIT_ASSERT_EQUAL(ref, matchedId);
    }

    Y_UNIT_TEST(Equality) {
        const TVector<TMatchedId> matchedIds = {
            TMatchedId(ID_1, MATCH_TS, CAS, ATTRS_1),
            TMatchedId(ID_2, MATCH_TS, CAS, ATTRS_1),
            TMatchedId(ID_1, MATCH_TS + TDuration::Seconds(1), CAS, ATTRS_1),
            TMatchedId(ID_1, MATCH_TS, CAS + 1, ATTRS_1),
            TMatchedId(ID_1, MATCH_TS, CAS, ATTRS_2)
        };

        for (size_t i = 0; i < matchedIds.size(); ++i) {
            for (size_t j = 0; j < matchedIds.size(); ++j) {
                if (i == j) {
                    AssertEqual(matchedIds[i], matchedIds[i]);
                } else {
                    AssertUnequal(matchedIds[i], matchedIds[j]);
                }
            }
        }
    }
}
