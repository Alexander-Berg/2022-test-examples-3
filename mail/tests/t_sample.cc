#include <sintimers/timer.h>
#include <catch.hpp>

//using namespace timers;

TEST_CASE("sintimers/sample", "sample 1==1")
{
    REQUIRE(1==1);
}

struct T_SINTIMERS
{
public:
    int i;
    T_SINTIMERS()
    {
        i = 5;
    }
    ~T_SINTIMERS()
    {
        // tear down
    }
};


TEST_CASE_METHOD(T_SINTIMERS, "sintimers/sample2", "test set up")
{
    REQUIRE(i == 5);
}