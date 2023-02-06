#include <crypta/cm/services/common/data/match_comparator.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/vector.h>


Y_UNIT_TEST_SUITE(NMatchComparator) {
    using namespace NCrypta::NCm;

    const TId EXT_ID_1("type-1", "value-1");
    const TId EXT_ID_2("type-2", "value-2");

    const TMatchedId MATCHED_ID_1_1(TId("int-type-1-1", "value-1-1"), TInstant::Seconds(10), 13, {{"foo-1-1", "bar-1-1"}});
    const TMatchedId MATCHED_ID_1_1_2(TId("int-type-1-1", "value-1-2"), TInstant::Seconds(10), 13, {{"foo-1-1", "bar-1-1"}});
    const TMatchedId MATCHED_ID_1_2(TId("int-type-1-2", "value-1-2"), TInstant::Seconds(20), 23, {{"foo-1-2", "bar-1-2"}});

    const TMatchedId MATCHED_ID_2_1(TId("int_type-2-1", "int_value-2-1"), TInstant::Seconds(10), 13, {{"foo-2-1", "bar-2-1"}});
    const TMatchedId MATCHED_ID_2_2(TId("int_type-2-2", "int_value-2-2"), TInstant::Seconds(20), 23, {{"foo-2-2", "bar-2-2"}});

    const TMatch::TMatchedIds MATCHED_IDS_1 = {{MATCHED_ID_1_1.GetId().Type, MATCHED_ID_1_1}, {MATCHED_ID_1_2.GetId().Type, MATCHED_ID_1_2}};
    const TMatch::TMatchedIds MATCHED_IDS_1_2 = {{MATCHED_ID_1_1_2.GetId().Type, MATCHED_ID_1_1_2}, {MATCHED_ID_1_2.GetId().Type, MATCHED_ID_1_2}};
    const TMatch::TMatchedIds MATCHED_IDS_2 = {{MATCHED_ID_2_1.GetId().Type, MATCHED_ID_2_1}, {MATCHED_ID_2_2.GetId().Type, MATCHED_ID_2_2}};

    const TInstant TOUCH_1 = TInstant::Seconds(101);
    const TInstant TOUCH_2 = TInstant::Seconds(102);

    const TDuration TTL_1 = TDuration::Seconds(51);
    const TDuration TTL_2 = TDuration::Seconds(52);

    // Track back reference
    const bool TRACK_BACK_REFERENCE_TRUE = true;
    const bool TRACK_BACK_REFERENCE_FALSE = false;

    Y_UNIT_TEST(ExtId) {
        const TMatch match1(EXT_ID_1, MATCHED_IDS_1, TOUCH_1, TTL_1);
        const TMatch match2(EXT_ID_2, MATCHED_IDS_1, TOUCH_1, TTL_1);
        const TMatch match3(EXT_ID_1, MATCHED_IDS_2, TOUCH_2, TTL_2);

        UNIT_ASSERT(!NMatchComparator::Equal(match1, match2, NMatchComparator::EMode::ExtId));
        UNIT_ASSERT(NMatchComparator::Equal(match1, match3, NMatchComparator::EMode::ExtId));
    }

    Y_UNIT_TEST(InternalIds) {
        const TMatch match1(EXT_ID_1, MATCHED_IDS_1, TOUCH_1, TTL_1);
        const TMatch match2(EXT_ID_1, MATCHED_IDS_2, TOUCH_1, TTL_1);
        const TMatch match3(EXT_ID_2, MATCHED_IDS_1, TOUCH_2, TTL_2);

        UNIT_ASSERT(!NMatchComparator::Equal(match1, match2, NMatchComparator::EMode::InternalIds));
        UNIT_ASSERT(NMatchComparator::Equal(match1, match3, NMatchComparator::EMode::InternalIds));
    }

    Y_UNIT_TEST(Touch) {
        const TMatch match1(EXT_ID_1, MATCHED_IDS_1, TOUCH_1, TTL_1);
        const TMatch match2(EXT_ID_1, MATCHED_IDS_1, TOUCH_2, TTL_1);
        const TMatch match3(EXT_ID_2, MATCHED_IDS_2, TOUCH_1, TTL_2);

        UNIT_ASSERT(!NMatchComparator::Equal(match1, match2, NMatchComparator::EMode::Touch));
        UNIT_ASSERT(NMatchComparator::Equal(match1, match3, NMatchComparator::EMode::Touch));
    }

    Y_UNIT_TEST(Ttl) {
        const TMatch match1(EXT_ID_1, MATCHED_IDS_1, TOUCH_1, TTL_1);
        const TMatch match2(EXT_ID_1, MATCHED_IDS_1, TOUCH_1, TTL_2);
        const TMatch match3(EXT_ID_2, MATCHED_IDS_2, TOUCH_2, TTL_1);

        UNIT_ASSERT(!NMatchComparator::Equal(match1, match2, NMatchComparator::EMode::Ttl));
        UNIT_ASSERT(NMatchComparator::Equal(match1, match3, NMatchComparator::EMode::Ttl));
    }

    Y_UNIT_TEST(TrackBackReference) {
        const TMatch match1(EXT_ID_1, MATCHED_IDS_1, TOUCH_1, TTL_1, TRACK_BACK_REFERENCE_FALSE);
        const TMatch match2(EXT_ID_1, MATCHED_IDS_1, TOUCH_1, TTL_1, TRACK_BACK_REFERENCE_TRUE);
        const TMatch match3(EXT_ID_1, MATCHED_IDS_1, TOUCH_1, TTL_1, TRACK_BACK_REFERENCE_FALSE);

        UNIT_ASSERT(!NMatchComparator::Equal(match1, match2, NMatchComparator::EMode::TrackBackReference));
        UNIT_ASSERT(NMatchComparator::Equal(match1, match3, NMatchComparator::EMode::TrackBackReference));
    }

    Y_UNIT_TEST(MatchedIdComparatorMode) {
        const TMatch match1(EXT_ID_1, MATCHED_IDS_1, TOUCH_1, TTL_1);
        const TMatch match2(EXT_ID_1, MATCHED_IDS_1_2, TOUCH_1, TTL_1);

        UNIT_ASSERT(!NMatchComparator::Equal(match1, match2, NMatchComparator::EMode::All, NMatchedIdComparator::EMode::All));
        UNIT_ASSERT(NMatchComparator::Equal(match1, match2, NMatchComparator::EMode::All, NMatchedIdComparator::EMode::MatchTs));
    }
}
