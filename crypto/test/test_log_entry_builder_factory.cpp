#include <crypta/lib/native/http/log_entry_builder_factory.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta;

namespace {
    void Test(THolder<ILogEntryBuilderFactory> factory, const TString& reference) {
        auto builder = factory->Create();
        builder->Add("uint", 123);
        builder->Add("string", "value");
        auto result = builder->GetAndReset();
        UNIT_ASSERT_STRINGS_EQUAL(reference, result);
    }
}

Y_UNIT_TEST_SUITE(ILogEntryBuilderFactory) {
        Y_UNIT_TEST(Tskv) {
            Test(
                NewTskvLogEntryBuilderFactory("tskv-format"),
                "tskv\ttskv_format=tskv-format\tuint=123\tstring=value"
            );
        }

        Y_UNIT_TEST(Json) {
            Test(
                NewJsonLogEntryBuilderFactory(),
                "{\"uint\":123,\"string\":\"value\"}"
            );
        }
}
