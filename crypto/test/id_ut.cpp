#include <crypta/cm/services/common/data/id.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/vector.h>

Y_UNIT_TEST_SUITE(TId) {
    using namespace NCrypta::NCm;

    Y_UNIT_TEST(Construct) {
        auto id = TId("type", "value");
        UNIT_ASSERT_EQUAL("type", id.Type);
        UNIT_ASSERT_EQUAL("value", id.Value);
    }

    Y_UNIT_TEST(Empty) {
        const TVector ids = {
            TId(),
            TId("", ""),
            TId("type", ""),
            TId("", "value")
        };
        for (const auto& id : ids) {
            UNIT_ASSERT(id.Empty());
        }

        UNIT_ASSERT(!TId("type", "value").Empty());
    }

    Y_UNIT_TEST(Equality) {
        const TVector<TId> ids = {
            TId("", ""),
            TId("", "value"),
            TId("type", ""),
            TId("type", "value"),
            TId("type-2", "value"),
            TId("type", "value-2"),
            TId("type-2", "value-2")
        };

        for (size_t i = 0; i < ids.size(); ++i) {
            for (size_t j = 0; j < ids.size(); ++j) {
                if (i == j) {
                    UNIT_ASSERT_EQUAL(ids[i], ids[i]);
                } else {
                    UNIT_ASSERT_UNEQUAL(ids[i], ids[j]);
                }
            }
        }
    }
}
