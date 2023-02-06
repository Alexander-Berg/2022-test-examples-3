#include <crypta/cm/services/common/db_state/change_tracking_map.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/utility.h>

Y_UNIT_TEST_SUITE(TChangeTrackingMap) {
    using namespace NCrypta;
    using namespace NCrypta::NCm;

    using TMap = TChangeTrackingMap<TString>;

    const TId KEY("type", "key");
    const TString VALUE("value");

    Y_UNIT_TEST(WriteGetDelete) {
        TMap map;
        map.Write(KEY, VALUE);

        const auto* valuePtr = map.Get(KEY);
        UNIT_ASSERT(valuePtr);
        UNIT_ASSERT_STRINGS_EQUAL(VALUE, *valuePtr);

        map.Delete(KEY);
        valuePtr = map.Get(KEY);
        UNIT_ASSERT(!valuePtr);

        UNIT_ASSERT_EXCEPTION(map.Delete(KEY), yexception);
    }

    Y_UNIT_TEST(Iterators) {
        TMap map;
        map.Write(KEY, VALUE);

        UNIT_ASSERT(map.DeleteBegin() == map.DeleteEnd());
        UNIT_ASSERT_EQUAL(THashSet<TString>({VALUE}), THashSet<TString>(map.UpdateBegin(), map.UpdateEnd()));

        map.Delete(KEY);

        UNIT_ASSERT(map.UpdateBegin() == map.UpdateEnd());
        UNIT_ASSERT_EQUAL(THashSet<TId>({KEY}), THashSet<TId>(map.DeleteBegin(), map.DeleteEnd()));
    }

    Y_UNIT_TEST(Update) {
        TMap map({{KEY, VALUE}});

        map.Update(KEY, [](TString& value){
            value.append("1");
            return true;
        });

        UNIT_ASSERT(map.DeleteBegin() == map.DeleteEnd());
        UNIT_ASSERT_EQUAL(THashSet<TString>({VALUE + "1"}), THashSet<TString>(map.UpdateBegin(), map.UpdateEnd()));

        map = TMap({{KEY, VALUE}});
        map.Update(KEY, [](TString& value){
            Y_UNUSED(value);
            return false;
        });
        UNIT_ASSERT(map.DeleteBegin() == map.DeleteEnd());
        UNIT_ASSERT(map.UpdateBegin() == map.UpdateEnd());

        UNIT_ASSERT_EXCEPTION(map.Update(TId("missing", "key"), [](TString& value){
            value.append("1");
            return true;
        }), yexception);
    }
}
