#include <crypta/cm/services/common/serializers/match/record/match_record_serializer.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/string/hex.h>

Y_UNIT_TEST_SUITE(NMatchSerializer) {

    using namespace NCrypta::NCm;

    namespace {
        const TString KEY = "adfox:1:1200365125";
        const TString SERIALIZED_VALUE = HexDecode("789C13D2E2D2E052E0E2AC4CCC4B49AD28CD4C1112B634303737B43435363734353334353131B23011F8FCF7DC1B560930A9D0F05545831100A42211EC");
        const NCrypta::TRecord RECORD{.Key=KEY, .Value=SERIALIZED_VALUE};

        const TMatch MATCH(TId("adfox", "1:1200365125"),
                           {{"yandexuid", {TMatchedId(TId("yandexuid", "9077195371561544284"), TInstant::Seconds(1569963763))}}},
                           TInstant::Seconds(1569963763),
                           TDuration::Seconds(604800),
                           true);
    }

    Y_UNIT_TEST(FromRecord) {
        UNIT_ASSERT_EQUAL(MATCH, NMatchSerializer::FromRecord(RECORD));
    }

    Y_UNIT_TEST(ToRecord) {
        UNIT_ASSERT_EQUAL(RECORD, NMatchSerializer::ToRecord(MATCH));
    }

    Y_UNIT_TEST(ToFromRecord) {
        UNIT_ASSERT_EQUAL(MATCH, NMatchSerializer::FromRecord(NMatchSerializer::ToRecord(MATCH)));
    }

    Y_UNIT_TEST(FromToRecord) {
        UNIT_ASSERT_EQUAL(RECORD, NMatchSerializer::ToRecord(NMatchSerializer::FromRecord(RECORD)));
    }
}
