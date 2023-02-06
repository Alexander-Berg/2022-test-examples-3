#include <crypta/cm/services/common/data/match.h>
#include <crypta/lib/native/test/assert_equality.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TMatch) {
    using namespace NCrypta;
    using namespace NCrypta::NCm;

    const TId EXT_ID_1("type-1", "value-1");
    const TId EXT_ID_2("type-2", "value-2");

    const TMatchedId MATCHED_ID_1_1(TId("int-type-1-1", "value-1-1"), TInstant::Seconds(10), 13, {{"foo-1-1", "bar-1-1"}});
    const TMatchedId MATCHED_ID_1_2(TId("int-type-1-2", "value-1-2"), TInstant::Seconds(20), 23, {{"foo-1-2", "bar-1-2"}});

    const TMatchedId MATCHED_ID_2_1(TId("int_type-2-1", "int_value-2-1"), TInstant::Seconds(10), 13, {{"foo-2-1", "bar-2-1"}});
    const TMatchedId MATCHED_ID_2_2(TId("int_type-2-2", "int_value-2-2"), TInstant::Seconds(20), 23, {{"foo-2-2", "bar-2-2"}});

    const TInstant TOUCH_1 = TInstant::Seconds(101);
    const TInstant TOUCH_2 = TInstant::Seconds(102);

    const TDuration TTL_1 = TDuration::Seconds(51);
    const TDuration TTL_2 = TDuration::Seconds(52);

    Y_UNIT_TEST(Empty) {
        TMatch match;

        UNIT_ASSERT_STRINGS_EQUAL("", match.GetExtId().Type);
        UNIT_ASSERT_STRINGS_EQUAL("", match.GetExtId().Value);

        UNIT_ASSERT_EQUAL(0, match.GetInternalIds().size());
        UNIT_ASSERT_EQUAL(TInstant::Zero(), match.GetTouch());
        UNIT_ASSERT_EQUAL(TDuration::Zero(), match.GetTtl());
    }

    Y_UNIT_TEST(Construct) {
        TId id = EXT_ID_1;
        const TMatch::TMatchedIds ids{
            {MATCHED_ID_1_1.GetId().Type, MATCHED_ID_1_1},
            {MATCHED_ID_1_2.GetId().Type, MATCHED_ID_1_2}
        };

        const TMatch match(id, ids, TOUCH_1, TTL_1);
        UNIT_ASSERT_EQUAL(id, match.GetExtId());
        UNIT_ASSERT_EQUAL(ids, match.GetInternalIds());
        UNIT_ASSERT_EQUAL(TOUCH_1, match.GetTouch());
        UNIT_ASSERT_EQUAL(TTL_1, match.GetTtl());
    }

    Y_UNIT_TEST(GettersSetters) {
        TMatch::TMatchedIds matchedIds{
            {MATCHED_ID_1_1.GetId().Type, MATCHED_ID_1_1},
        };
        TMatch match(EXT_ID_1, matchedIds, TOUCH_1, TTL_1);

        match.SetExtId(EXT_ID_2);
        UNIT_ASSERT_EQUAL(EXT_ID_2, match.GetExtId());

        UNIT_ASSERT_EQUAL(matchedIds, match.GetInternalIds());

        matchedIds[MATCHED_ID_1_2.GetId().Type] = MATCHED_ID_1_2;
        match.AddId(MATCHED_ID_1_2);
        UNIT_ASSERT_EQUAL(matchedIds, match.GetInternalIds());

        match.SetTouch(TOUCH_2);
        UNIT_ASSERT_EQUAL(TOUCH_2, match.GetTouch());

        match.SetTtl(TTL_2);
        UNIT_ASSERT_EQUAL(TTL_2, match.GetTtl());
    }

    Y_UNIT_TEST(Equality) {
        const TMatch::TMatchedIds matched_ids_1 = {
            {MATCHED_ID_1_1.GetId().Type, MATCHED_ID_1_1},
            {MATCHED_ID_1_2.GetId().Type, MATCHED_ID_1_2}
        };
        const TMatch::TMatchedIds matched_ids_2 = {
            {MATCHED_ID_2_1.GetId().Type, MATCHED_ID_2_1},
            {MATCHED_ID_2_2.GetId().Type, MATCHED_ID_2_2}
        };

        const TVector<TMatch> matches = {
            TMatch(TId("", ""), TMatch::TMatchedIds()),
            TMatch(TId("", ""), matched_ids_1),
            TMatch(EXT_ID_1, TMatch::TMatchedIds()),
            TMatch(EXT_ID_1, matched_ids_1, TOUCH_1, TTL_1),
            TMatch(EXT_ID_1, matched_ids_2),
            TMatch(EXT_ID_2, matched_ids_2),
            TMatch(EXT_ID_1, matched_ids_1, TOUCH_2, TTL_1),
            TMatch(EXT_ID_1, matched_ids_1, TOUCH_1, TTL_2)
        };

        for (size_t i = 0; i < matches.size(); ++i) {
            for (size_t j = 0; j < matches.size(); ++j) {
                if (i == j) {
                    AssertEqual(matches[i], matches[i]);
                } else {
                    AssertUnequal(matches[i], matches[j]);
                }
            }
        }
    }
}
