#include <crypta/cm/services/common/data/matched_id_comparator.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/vector.h>


Y_UNIT_TEST_SUITE(NMatchedIdComparator) {
    using namespace NCrypta::NCm;

    const TId ID_1("type", "value");
    const TId ID_2("type-2", "value-2");

    const TInstant MATCH_TS_1 = TInstant::Seconds(1);
    const TInstant MATCH_TS_2 = TInstant::Seconds(2);

    const ui64 CAS_1 = 2;
    const ui64 CAS_2 = 3;

    const TAttributes ATTRS_1({{"foo", "bar"}});
    const TAttributes ATTRS_2({{"foo-2", "bar-2"}});

    Y_UNIT_TEST(Id) {
        TMatchedId matchedId1(ID_1, MATCH_TS_1, CAS_1, ATTRS_1);
        TMatchedId matchedId2(ID_2, MATCH_TS_1, CAS_1, ATTRS_1);
        TMatchedId matchedId3(ID_1, MATCH_TS_2, CAS_2, ATTRS_2);

        UNIT_ASSERT(!NMatchedIdComparator::Equal(matchedId1, matchedId2, NMatchedIdComparator::EMode::Id));
        UNIT_ASSERT(NMatchedIdComparator::Equal(matchedId1, matchedId3, NMatchedIdComparator::EMode::Id));
    }

    Y_UNIT_TEST(Attributes) {
        TMatchedId matchedId1(ID_1, MATCH_TS_1, CAS_1, ATTRS_1);
        TMatchedId matchedId2(ID_1, MATCH_TS_1, CAS_1, ATTRS_2);
        TMatchedId matchedId3(ID_2, MATCH_TS_2, CAS_2, ATTRS_1);

        UNIT_ASSERT(!NMatchedIdComparator::Equal(matchedId1, matchedId2, NMatchedIdComparator::EMode::Attributes));
        UNIT_ASSERT(NMatchedIdComparator::Equal(matchedId1, matchedId3, NMatchedIdComparator::EMode::Attributes));
    }

    Y_UNIT_TEST(MatchTs) {
        TMatchedId matchedId1(ID_1, MATCH_TS_1, CAS_1, ATTRS_1);
        TMatchedId matchedId2(ID_1, MATCH_TS_2, CAS_1, ATTRS_1);
        TMatchedId matchedId3(ID_2, MATCH_TS_1, CAS_2, ATTRS_2);

        UNIT_ASSERT(!NMatchedIdComparator::Equal(matchedId1, matchedId2, NMatchedIdComparator::EMode::MatchTs));
        UNIT_ASSERT(NMatchedIdComparator::Equal(matchedId1, matchedId3, NMatchedIdComparator::EMode::MatchTs));
    }

    Y_UNIT_TEST(Cas) {
        TMatchedId matchedId1(ID_1, MATCH_TS_1, CAS_1, ATTRS_1);
        TMatchedId matchedId2(ID_1, MATCH_TS_1, CAS_2, ATTRS_1);
        TMatchedId matchedId3(ID_2, MATCH_TS_2, CAS_1, ATTRS_2);

        UNIT_ASSERT(!NMatchedIdComparator::Equal(matchedId1, matchedId2, NMatchedIdComparator::EMode::Cas));
        UNIT_ASSERT(NMatchedIdComparator::Equal(matchedId1, matchedId3, NMatchedIdComparator::EMode::Cas));
    }
}
