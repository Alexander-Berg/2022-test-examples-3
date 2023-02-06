#include <yplatform/util/private_access.h>
#include <catch.hpp>

class sample_class
{
public:
    int get_value()
    {
        return value;
    }

private:
    int value = 42;
};

using value_tag = yplatform::private_access_tag<sample_class, int>;
template struct yplatform::private_access<value_tag, &sample_class::value>;

TEST_CASE("private_access/get_value")
{
    sample_class obj;
    REQUIRE(yplatform::get_field<value_tag>(obj) == obj.get_value());
}

TEST_CASE("private_access/set_value")
{
    sample_class obj;
    int& value = yplatform::get_field<value_tag>(obj);
    value = 123;
    REQUIRE(obj.get_value() == 123);
}
