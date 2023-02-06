#include <iosfwd>
#include <library/cpp/testing/unittest/registar.h>
#include <crypta/graph/rtdi-rt/lib/rtdi.h>
#include <crypta/lib/native/identifiers/lib/generic.h>

bool FakeCheckAndNormalize(const NCrypta::NIdentifiersProto::NIdType::EIdType& IdType, TString* DeviceId) {
    if (NIdentifiers::TGenericID deviceId{IdType, (*DeviceId)}; deviceId.IsValid()) {
        (*DeviceId) = deviceId.Normalize();
        return true;
    } else {
        return false;
    }
}

Y_UNIT_TEST_SUITE(TestRtDi) {
    Y_UNIT_TEST(TEST_check) {
        using namespace NCrypta::NIdentifiersProto::NIdType;
        // ok, can't run private method test, so let it be 
        {
            TString deviceId{"9B4C361D6A6A6BC4B517B112D6B96D41"};
            UNIT_ASSERT(FakeCheckAndNormalize(GAID, &deviceId));
            UNIT_ASSERT_EQUAL(deviceId, "9b4c361d-6a6a-6bc4-b517-b112d6b96d41");
        }
        {
            TString deviceId{"9B4C361D6A6A6BC4B517B112D6B96D41"};
            UNIT_ASSERT(FakeCheckAndNormalize(OAID, &deviceId));
            UNIT_ASSERT_EQUAL(deviceId, "9b4c361d-6a6a-6bc4-b517-b112d6b96d41");
        }
        {
            TString deviceId{"9B4C361D-6A6A-6BC4-B517-B112D6B96D41"};
            UNIT_ASSERT(FakeCheckAndNormalize(IDFA, &deviceId));
            UNIT_ASSERT_EQUAL(deviceId, "9B4C361D-6A6A-6BC4-B517-B112D6B96D41");
        }
        {
            // should be invalid
            TString deviceId{"9B-4C-36-1D-6A-6A-6B-C4-B5-17-B1-12-D6-B9-6D-41"};
            UNIT_ASSERT(!FakeCheckAndNormalize(YANDEXUID, &deviceId));
            UNIT_ASSERT_EQUAL(deviceId, "9B-4C-36-1D-6A-6A-6B-C4-B5-17-B1-12-D6-B9-6D-41");
        }
    }
}
