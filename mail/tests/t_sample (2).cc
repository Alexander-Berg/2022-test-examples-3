#include <ymod_lease/ymod_lease.h>
#include <catch.hpp>

using namespace ylease;

TEST_CASE("ymod_lease/sample", "sample 1==1")
{
    REQUIRE(1 == 1);
}

struct T_YMOD_LEASE
{
public:
    int i;
    T_YMOD_LEASE()
    {
        i = 5;
    }
    ~T_YMOD_LEASE()
    {
        // tear down
    }
};

TEST_CASE_METHOD(T_YMOD_LEASE, "ymod_lease/sample2", "test set up")
{
    REQUIRE(i == 5);
}
