
#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <market/media_adv/library/access/access_resource.h>

using namespace NMarket::NMediaAdv::NAccess;

class TResourceTest : public ::testing::Test {
public:
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

    class TImplCritical : public TImpl{
    public:
        static constexpr bool IS_CRITICAL = true;
    };

    class TImplNotWait : public TImpl {
    public:
        static constexpr bool NOT_WAIT = true;
    };

};


TEST_F(TResourceTest, Init){
    TAccessResource<TImpl> resource;
    TAccessResource<TImplCritical> resourceCritical;
    TAccessResource<TImplNotWait> resourceNotWait;

    EXPECT_EQ(resource.GetName(), TImpl::ACCESS_NAME);

    //normal resource (with default parameters)
    EXPECT_TRUE(resource.GetCurrentVersion());
    EXPECT_FALSE(resource.GetCurrentVersion()->IsValid());
    //EXPECT_FALSE(resource.IsCritical());
    //EXPECT_FALSE(resource.IsNotWait());

    //critical resource
    EXPECT_TRUE(resourceCritical.GetCurrentVersion());
    EXPECT_FALSE(resourceCritical.GetCurrentVersion()->IsValid());
    //EXPECT_TRUE(resourceCritical.IsCritical());
    //EXPECT_FALSE(resourceCritical.IsNotWait());

    //not waited resource
    EXPECT_TRUE(resourceNotWait.GetCurrentVersion());
    EXPECT_FALSE(resourceNotWait.GetCurrentVersion()->IsValid());
    //EXPECT_TRUE(resourceNotWait.IsCritical());
    //EXPECT_TRUE(resourceNotWait.IsNotWait());

}



