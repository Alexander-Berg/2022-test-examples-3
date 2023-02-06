#include <crypta/cm/services/common/data/attributes.h>
#include <crypta/cm/services/common/data/match_validator.h>

#include <library/cpp/testing/unittest/registar.h>

#include <limits>

Y_UNIT_TEST_SUITE(NMatchValidator) {
    using namespace NCrypta::NCm;

    void Test(TMatchedIds matchedIds, bool valid) {
        TMatch match(TId("ext_ns", "ABC"));
        for (const auto& matchedId: matchedIds) {
            match.AddId(matchedId);
        }
        UNIT_ASSERT_EQUAL(valid, NMatchValidator::IsValid(match));
    }

    Y_UNIT_TEST(OneId) {
        Test({TMatchedId(TId("yandexuid", "123"))}, true);
    }

    Y_UNIT_TEST(TwoIds) {
        Test({TMatchedId(TId("yandexuid", "123")), TMatchedId(TId("icookie", "234"))}, true);
    }

    Y_UNIT_TEST(FullOneId) {
        Test({TMatchedId{TId("yandexuid", "123"), TInstant::Seconds(std::numeric_limits<ui64>::max()), std::numeric_limits<ui64>::max(),
                         TAttributes{{"aaaaaaaaaaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbbbb"}}}}, true);
    }

    Y_UNIT_TEST(Empty) {
        TMatch match;
        UNIT_ASSERT(!NMatchValidator::IsValid(match));
    }

    Y_UNIT_TEST(EmptyIds) {
        TMatch match;
        match.SetExtId(TId{"ext_ns", "ABC"});
        Test({}, false);
    }

    Y_UNIT_TEST(InvalidIds) {
        TMatch match;
        Test({TMatchedId(TId("", ""))}, false);
    }
}
