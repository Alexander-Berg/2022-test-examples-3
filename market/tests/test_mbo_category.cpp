#include <library/cpp/testing/unittest/registar.h>

#include <market/gumoful/tools/utils/mbo_category.h>
#include <market/gumoful/tools/utils/protobuf_reader.h>
#include <market/proto/content/mbo/MboParameters.pb.h>
#include <util/stream/file.h>
#include <google/protobuf/io/coded_stream.h>
#include <library/cpp/testing/unittest/tests_data.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/folder/path.h>
using namespace NGumoful;

namespace {
    Market::Mbo::Parameters::Word CreateWord(const TString& word) {
        Market::Mbo::Parameters::Word result;
        result.set_name(word);
        return result;
    }
}

Y_UNIT_TEST_SUITE(TestMboCategory)
{
    Y_UNIT_TEST(TestEmptyCategory)
    {
        TExportCategory emptyCategory;
        UNIT_ASSERT_NO_EXCEPTION(TMboCategory(emptyCategory));

        TMboCategory mboCategory(emptyCategory);

        UNIT_ASSERT_EQUAL(0, mboCategory.GetId());
        UNIT_ASSERT_EQUAL("", mboCategory.GetName());

        UNIT_ASSERT_EQUAL(0, mboCategory.GetPropertiesSize());
    }

    Y_UNIT_TEST(TestBasicProperties)
    {
        TExportCategory category;
        category.set_hid(42);
        *category.add_name() = CreateWord("category_name");

        TMboCategory mboCategory(category);

        UNIT_ASSERT_EQUAL(42, mboCategory.GetId());
        UNIT_ASSERT_EQUAL("category_name", mboCategory.GetName());
    }

    Y_UNIT_TEST(TestRealProperties)
    {
        const auto categoryMessage = TFileInput(JoinFsPaths(ArcadiaSourceRoot(), "market/gumoful/tools/tests/input_files/category_91491.pb")).ReadAll();
        TVarIntLenValueProtoReader protoReader(categoryMessage);
        const auto category = *protoReader.Read<TExportCategory>();

        TMboCategory mboCategory(category);
        UNIT_ASSERT_EQUAL(380, mboCategory.GetPropertiesSize());

        {
            auto gps = mboCategory.GetProperty("GPS");
            UNIT_ASSERT_EQUAL(mv_bool, gps.type());
            UNIT_ASSERT_STRINGS_EQUAL("GPS", gps.id());
            UNIT_ASSERT_STRINGS_EQUAL("GPS", gps.long_name());
            UNIT_ASSERT(!std::string(gps.description()).empty());
        }

        {
            auto memorySize = mboCategory.GetProperty("MemoryVolumeGB");
            UNIT_ASSERT_EQUAL(mv_numeric, memorySize.type());
            UNIT_ASSERT_STRINGS_EQUAL("MemoryVolumeGB", memorySize.id());
            UNIT_ASSERT_STRINGS_EQUAL("Объем встроенной памяти", memorySize.long_name());
            UNIT_ASSERT(!std::string(memorySize.description()).empty());
        }

        {
            auto numEnum = mboCategory.GetProperty("num_enum_dts");
            UNIT_ASSERT_EQUAL(mv_string, numEnum.type());
            UNIT_ASSERT_STRINGS_EQUAL("num_enum_dts", numEnum.id());
            UNIT_ASSERT_STRINGS_EQUAL("num_enum", numEnum.long_name());
            UNIT_ASSERT(std::string(numEnum.description()).empty());
        }

        {
            auto os = mboCategory.GetProperty("SmartPhoneOS");
            UNIT_ASSERT_EQUAL(mv_string, os.type());
            UNIT_ASSERT_STRINGS_EQUAL("SmartPhoneOS", os.id());
            UNIT_ASSERT_STRINGS_EQUAL("Операционная система", os.long_name());
            UNIT_ASSERT(!std::string(os.description()).empty());
        }

        {
            auto announceDate = mboCategory.GetProperty("AnnounceDate");
            UNIT_ASSERT_EQUAL(mv_string, announceDate.type());
            UNIT_ASSERT_STRINGS_EQUAL("AnnounceDate", announceDate.id());
            UNIT_ASSERT_STRINGS_EQUAL("Дата анонсирования", announceDate.long_name());
            UNIT_ASSERT(std::string(announceDate.description()).empty());
        }

        UNIT_ASSERT_STRINGS_EQUAL("Windows", mboCategory.GetEnumTextValue(4925670, 13195937).c_str());

        UNIT_ASSERT_EQUAL(91491, mboCategory.GetId());
        UNIT_ASSERT_STRINGS_EQUAL("Мобильные телефоны", mboCategory.GetName());
    }
}
