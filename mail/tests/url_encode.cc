#include <cassert>
#include <yplatform/encoding/url_encode.h>
#include <yplatform/util.h>

int main()
{
    assert(yplatform::iequals(yplatform::url_decode(std::string("aa+gg%20")), "aa gg "));
    assert(yplatform::iequals(yplatform::url_encode(std::string("aa gg\"")), "aa+gg%22"));

    std::string str1 = yplatform::url_encode<std::string>(std::string("=340-83+-"));
    assert(str1 == "%3d340-83%2b-");

    std::string str2 = yplatform::url_decode<std::string>(std::string("%3d340-83%2b-"));
    assert(str2 == "=340-83+-");
    return 0;
}
