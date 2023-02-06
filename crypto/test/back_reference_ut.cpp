#include <crypta/cm/services/common/data/back_reference.h>
#include <crypta/lib/native/test/assert_equality.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/vector.h>

Y_UNIT_TEST_SUITE(TBackReference) {
    using namespace NCrypta;
    using namespace NCrypta::NCm;

    const TId EXT_ID = TId("type", "value");
    const TId INT_ID_1 = TId("int-type-1", "int-value-1");
    const TId INT_ID_2 = TId("int-type-2", "int-value-2");

    Y_UNIT_TEST(Construct) {
        const TBackReference backRef(EXT_ID, {{INT_ID_1, INT_ID_2}});

        UNIT_ASSERT_EQUAL(EXT_ID, backRef.Id);
        UNIT_ASSERT_EQUAL(THashSet<TId>({INT_ID_1, INT_ID_2}), backRef.Refs);
    }

    Y_UNIT_TEST(Equality) {
        const TVector<TBackReference> backRefs = {
            TBackReference(TId(), {}),
            TBackReference(EXT_ID, {}),
            TBackReference(EXT_ID, {INT_ID_1}),
            TBackReference(EXT_ID, {INT_ID_1, INT_ID_2})
        };

        for (size_t i = 0; i < backRefs.size(); ++i) {
            for (size_t j = 0; j < backRefs.size(); ++j) {
                if (i == j) {
                    AssertEqual(backRefs[i], backRefs[i]);
                } else {
                    AssertUnequal(backRefs[i], backRefs[j]);
                }
            }
        }
    }
}
