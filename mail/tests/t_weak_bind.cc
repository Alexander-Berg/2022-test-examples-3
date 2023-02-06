#include <yplatform/util/weak_bind.h>
#include <catch.hpp>

struct foo
{
    void bar(bool& f)
    {
        f = true;
    }
};

TEST_CASE("weak_bind/basic")
{
    auto ptr = std::make_shared<foo>();
    auto f = yplatform::weak_bind(&foo::bar, ptr, std::placeholders::_1);
    {
        bool called = false;
        f(called);
        REQUIRE(called);
    }
    ptr.reset();
    {
        bool called = false;
        f(called);
        REQUIRE(!called);
    }
}
