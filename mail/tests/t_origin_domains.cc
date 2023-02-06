#include "web/origin_domains.h"
#include <catch.hpp>
#include <sstream>

namespace yxiva {

TEST_CASE("origin_domains/check/suffix", "")
{
    web::origin_domains_list list;
    list.add(".yandex.net");
    REQUIRE(list.check_allowed("mail.yandex.ru").first == false);
    REQUIRE(list.check_allowed("mail.yandex.net").first == true);
    REQUIRE(list.check_allowed("mail.yandex.net").second == ".yandex.net");
    REQUIRE(list.check_allowed("yandex.net").first == false);
    REQUIRE(list.check_allowed(".net").first == false);
}

TEST_CASE("origin_domains/check/full_domain", "")
{
    web::origin_domains_list list;
    list.add("yandex.ru");
    REQUIRE(list.check_allowed("mail.yandex.ru").first == false);
    REQUIRE(list.check_allowed("yandex.ru").first == true);
    REQUIRE(list.check_allowed("yandex.ru").second == "yandex.ru");
    REQUIRE(list.check_allowed(".net").first == false);
    REQUIRE(list.check_allowed(".ru").first == false);
}

}
