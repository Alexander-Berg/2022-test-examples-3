#include <crypta/lib/native/resource_service/provider/updatable_provider.h>

#include <library/cpp/testing/unittest/registar.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_writer.h>

using namespace NCrypta;
using namespace NCrypta::NResourceService;

namespace {
    const TString NAME = "name";
    const TString VERSION = "version";

    using TValue = NJson::TJsonValue;

    class TParser {
    public:
        static TValue Parse(const TString& input) {
            NJson::TJsonValue result;
            NJson::ReadJsonTree(TStringBuf(input), &result, true);
            return result;
        }
    };

    TValue MakeDefault() {
        NJson::TJsonValue value{};
        value[VERSION] = 0;
        return value;
    }

    TValue MakeValue(i64 version) {
        NJson::TJsonValue value{};
        value[VERSION] = version;
        return value;
    }

    void Update(TUpdatableProvider<TValue, TParser>& updatableProvider, const TValue& value) {
        TStringStream buffer{};
        NJson::WriteJson(&buffer, &value);
        updatableProvider.Update(buffer.Str(), value[VERSION].GetIntegerSafe());
    }
}

Y_UNIT_TEST_SUITE(TUpdatableProvider) {
    Y_UNIT_TEST(Basic) {
        const auto& defaultValue = MakeDefault();
        TUpdatableProvider<NJson::TJsonValue, TParser> updatableProvider(defaultValue, NAME);

        UNIT_ASSERT_EQUAL(NAME, updatableProvider.GetResourceMeta().Name);

        auto resource = updatableProvider.GetResource();
        UNIT_ASSERT_EQUAL(defaultValue, resource->Value);
        UNIT_ASSERT_EQUAL(0, resource->Meta.Version);
        UNIT_ASSERT_EQUAL(0, updatableProvider.GetResourceMeta().Version);

        auto firstVersion = MakeValue(1);
        Update(updatableProvider, firstVersion);

        resource = updatableProvider.GetResource();
        UNIT_ASSERT_EQUAL(firstVersion, resource->Value);
        UNIT_ASSERT_EQUAL(1, resource->Meta.Version);
        UNIT_ASSERT_EQUAL(1, updatableProvider.GetResourceMeta().Version);
    }
}
