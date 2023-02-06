#include <crypta/lib/native/database/record.h>
#include <crypta/lib/native/test/assert_equality.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/vector.h>

Y_UNIT_TEST_SUITE(TRecord) {
    using namespace NCrypta;

    TRecord MakeRecord(const TString& key, const TString& value, ui64 cas) {
        TRecord ret;
        ret.Key = key;
        ret.Value = value;
        ret.Cas = cas;
        return ret;
    }

    Y_UNIT_TEST(Equality) {
        TVector<TRecord> records = {
            MakeRecord("", "", 0),

            MakeRecord("key", "", 0),
            MakeRecord("", "value", 0),
            MakeRecord("", "", 1),

            MakeRecord("key", "value", 0),
            MakeRecord("key", "", 1),
            MakeRecord("", "value", 1),

            MakeRecord("key", "value", 1)
        };

        for (size_t i = 0; i < records.size(); ++i) {
            for (size_t j = 0; j < records.size(); ++j) {
                if (i == j) {
                    AssertEqual(records[i], records[i]);
                } else {
                    AssertUnequal(records[i], records[j]);
                }
            }
        }
    }
}
