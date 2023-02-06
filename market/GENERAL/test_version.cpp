
#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <market/media_adv/library/access/access_version.h>
#include <filesystem>
#include <fstream>

using namespace NMarket::NMediaAdv::NAccess;
namespace fs = std::filesystem;

class TVersionTest : public ::testing::Test {
public:
    using TProtoVersion = NMarket::NAccessAgent::TInstalledVersion;

    //impl for verison
    class TImpl {
    public:
        static constexpr const char* PATTERN_REGEX = R"(*.txt$)";
        static constexpr const char* ACCESS_NAME = "test_resource";

        bool Load(const TString& filePath){
            Y_UNUSED(filePath);
            return true;
        }
    };

protected:
    const TString ResourceName = "TestResource";
};


TEST_F(TVersionTest, InitEmpty){
    {
        TAccessVersion<TImpl> version;      //base class
        //EXPECT_TRUE(version.GetImpl());
        EXPECT_FALSE(version.IsValid());
        EXPECT_TRUE(version.GetFilePath().empty());
    }
    //no exception
}


TEST_F(TVersionTest, RemoveAfterDestroyed) {
    const TString filename = "temp/test_file.txt";
    fs::path filepath = {filename.c_str()};
    fs::create_directories(filepath.parent_path());
    std::ofstream ofs(filepath);
    ofs << "this is some text in the new file\n";
    ofs.close();

    ASSERT_TRUE(fs::exists(filename.c_str()));

    {
        TAccessVersion<TImpl> version(TProtoVersion(), filename, true);
        EXPECT_EQ(filepath.string(), version.GetFilePath());
    }
    //expect to file should be removed
    EXPECT_FALSE(fs::exists(filename.c_str()));
}

