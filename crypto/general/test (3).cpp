#include <crypta/lib/native/yt/yson/utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

namespace {
    struct TTestSerializable : public NYT::NYTree::TYsonSerializable {
    public:
        TString StringField;
        ui64 Ui64Field;

        TTestSerializable() {
            RegisterParameter("string", StringField);
            RegisterParameter("ui64", Ui64Field);
        }
    };

    using TTestSerializablePtr = NYT::TIntrusivePtr<TTestSerializable>;

    const TString TEST_YSON = "{string=\"string\"; ui64=1234567890;}";
    const TString TEST_STRING = "string";
    const ui64 TEST_UI64 = 1234567890;
} // namespace

Y_UNIT_TEST_SUITE(Utils) {
    using namespace NCrypta::NYsonUtils;

    Y_UNIT_TEST(LoadFromStream) {
        TStringStream stream(TEST_YSON);
        const auto& deserialized = LoadFromStream<TTestSerializable>(stream);

        UNIT_ASSERT_EQUAL(TEST_UI64, deserialized->Ui64Field);
        UNIT_ASSERT_EQUAL(TEST_STRING, deserialized->StringField);
    }

    Y_UNIT_TEST(LoadFromEmptyStream) {
        TStringStream stream;
        UNIT_ASSERT_EXCEPTION(LoadFromStream<TTestSerializable>(stream), NYT::TErrorException);
    }

    Y_UNIT_TEST(LoadFromMalformedStream) {
        TStringStream stream("malformed");
        UNIT_ASSERT_EXCEPTION(LoadFromStream<TTestSerializable>(stream), NYT::TErrorException);
    }

    Y_UNIT_TEST(LoadFromFile) {
        const auto& path = GetOutputPath().Child("test.yson");

        TFileOutput output(path);
        output << TEST_YSON;
        output.Finish();

        const auto& deserialized = LoadFromFile<TTestSerializable>(path);

        UNIT_ASSERT_EQUAL(TEST_UI64, deserialized->Ui64Field);
        UNIT_ASSERT_EQUAL(TEST_STRING, deserialized->StringField);
    }

    Y_UNIT_TEST(LoadFromNonExistentFile) {
        UNIT_ASSERT_EXCEPTION(LoadFromFile<TTestSerializable>(""), yexception);
    }

    Y_UNIT_TEST(LoadFromEmptyFile) {
        const auto& path = GetOutputPath().Child("empty_test.yson");
        path.Touch();
        UNIT_ASSERT_EXCEPTION(LoadFromFile<TTestSerializable>(path), NYT::TErrorException);
    }

    Y_UNIT_TEST(LoadFromMalformedFile) {
        const auto& path = GetOutputPath().Child("malformed_test.yson");

        TFileOutput output(path);
        output << "malformed";
        output.Finish();

        UNIT_ASSERT_EXCEPTION(LoadFromFile<TTestSerializable>(path), NYT::TErrorException);
    }
}
