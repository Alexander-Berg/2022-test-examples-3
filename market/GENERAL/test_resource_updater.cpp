
#include <library/cpp/testing/gtest/gtest.h>
#include <market/media_adv/library/access/resource_updater.h>

using namespace NMarket::NMediaAdv::NAccess;

class TResourceUpdaterTest : public ::testing::Test {
public:
    //impl for version
    class TImpl {
    public:
        static constexpr const char* PATTERN_REGEX = R"(*.txt$)";
        static constexpr const char* ACCESS_NAME = "test_resource";

        bool Load(const TString& filePath){
            Y_UNUSED(filePath);
            return true;
        }
    };

    class TImplCritical : public TImpl{
    public:
        static constexpr bool IS_CRITICAL = true;
    };

};


TEST_F(TResourceUpdaterTest, WaitResources) {
    //TODO add test for resource waiting (critical and not)
}


