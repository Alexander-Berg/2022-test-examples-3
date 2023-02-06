#include "web/utils/service_list_parser.h"
#include <catch.hpp>

namespace yxiva { namespace web {

inline bool operator==(const service_with_tags& s1, const service_with_tags& s2)
{
    return s1.service == s2.service && s1.tags == s2.tags;
}

inline std::vector<service_with_tags> parse_service_list_with_tags(const string& source)
{
    return parse_service_list_with_tags(source, 20U);
}

void check_result(const string& source, std::vector<service_with_tags> to_compare)
{
    auto result = parse_service_list_with_tags(source);
    REQUIRE(result == to_compare);
}

TEST_CASE("parse_service_list_tags/compatibility", "")
{
    check_result("mail,disk,passport", { { "mail", {} }, { "disk", {} }, { "passport", {} } });
}

TEST_CASE("parse_service_list_tags/one_tag", "")
{
    check_result("mail,disk:tagA", { { "mail", {} }, { "disk", { { "tagA" } } } });
}

TEST_CASE("parse_service_list_tags/different_operations", "")
{
    check_result(
        "mail:tagF+tagD,disk:tagA*tagB",
        { { "mail", { { "tagF" }, { "tagD" } } }, { "disk", { { "tagA", "tagB" } } } });
}

TEST_CASE("parse_service_list_tags/complex", "")
{
    check_result(
        "mail:tagE+tagR*tagF*tagL+tagQ*tagS",
        { { "mail", { { "tagE" }, { "tagR", "tagF", "tagL" }, { "tagQ", "tagS" } } } });
}

TEST_CASE("parse_service_list_tags/empty", "")
{
    REQUIRE_NOTHROW(parse_service_list_with_tags(""));
}

TEST_CASE("parse_service_list_tags/bad/no_service", "")
{
    REQUIRE_THROWS(parse_service_list_with_tags(",disk"));
    REQUIRE_NOTHROW(parse_service_list_with_tags("mail,"));
    REQUIRE_THROWS(parse_service_list_with_tags(":tagA+tagB,1"));
}

TEST_CASE("parse_service_list_tags/bad/no_tag", "")
{
    REQUIRE_THROWS(parse_service_list_with_tags("mail:,disk"));
    REQUIRE_THROWS(parse_service_list_with_tags("mail:+,disk"));
    REQUIRE_THROWS(parse_service_list_with_tags("mail:*,disk"));
    REQUIRE_NOTHROW(parse_service_list_with_tags("mail:"));
    REQUIRE_NOTHROW(parse_service_list_with_tags("mail,disk:"));
}

TEST_CASE("parse_service_list_tags/bad/space", "")
{
    REQUIRE_THROWS(parse_service_list_with_tags("mail, disk"));
}

TEST_CASE("parse_service_list_tags/bad/service_name", "")
{
    REQUIRE_THROWS(parse_service_list_with_tags("m@il,disk"));
    REQUIRE_THROWS(parse_service_list_with_tags("m@il*!"));
}

TEST_CASE("parse_service_list_tags/bad/tag_name", "")
{
    REQUIRE_THROWS(parse_service_list_with_tags("mail:tagA@"));
    REQUIRE_THROWS(parse_service_list_with_tags("mail:tag-A"));
    REQUIRE_NOTHROW(parse_service_list_with_tags("mail:tag.A"));
    REQUIRE_NOTHROW(parse_service_list_with_tags("mail:tag_A"));
}

}}