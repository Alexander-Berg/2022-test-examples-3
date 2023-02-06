#include <processor/parse_accounts.h>
#include <catch.hpp>

using namespace yxiva::mailpusher::badge;

TEST_CASE("parse_accounts/invalid_extra")
{
    std::vector<account> accounts;
    REQUIRE(!parse_accounts("", accounts));
    REQUIRE(!parse_accounts("{}", accounts));
    REQUIRE(!parse_accounts(R"({"accounts": ""})", accounts));
    REQUIRE(!parse_accounts(R"({"accounts": [{"uid": "1"}]})", accounts));
    REQUIRE(!parse_accounts(R"({"accounts": [{"uid": "1", "badge": "foo"}]})", accounts));
    REQUIRE(!parse_accounts(
        R"({"accounts": [
        {"uid": "1", "badge": {"enabled": "a"}}
    ]})",
        accounts));
    REQUIRE(!parse_accounts(
        R"({"accounts": [
        {"uid": "1", "badge": {"enabled": true, "exfid": "foo"}}
    ]})",
        accounts));
    REQUIRE(!parse_accounts(
        R"({"accounts": [
        {"uid": "1", "badge": {"enabled": true, "exfid": [1, 2, "foo"]}}
    ]})",
        accounts));
    REQUIRE(!parse_accounts(
        R"({"accounts": [
        {"uid": "1", "badge": {"enabled": true}}
    ]})",
        accounts));
}

TEST_CASE("parse_accounts/empty accounts list yields empty vector")
{
    std::vector<account> accounts;
    REQUIRE(parse_accounts(R"({"accounts": []})", accounts));
    REQUIRE(accounts.size() == 0);
}

TEST_CASE("parse_accounts/accounts with disabled badge are ignored")
{
    std::vector<account> accounts;
    REQUIRE(parse_accounts(
        R"({"accounts": [
        {"uid": "1", "environment": "a", "badge": {"enabled": false}}
    ]})",
        accounts));
    REQUIRE(accounts.size() == 0);
}

TEST_CASE("parse_accounts/multiple accounts without exfid and with exfid")
{
    std::vector<account> accounts;
    REQUIRE(
        parse_accounts(
            R"({"accounts": [
        {"uid": "1", "environment": "a", "badge": {"enabled": true}},
        {"uid": "2", "environment": "b", "badge": {"enabled": true, "exfid": [1, 2, 3]}}
    ]})",
            accounts)
            .error_reason == "");
    REQUIRE(accounts.size() == 2);
    REQUIRE(accounts[0].uid == "1");
    REQUIRE(accounts[0].environment == "a");
    REQUIRE(accounts[0].exfid.size() == 0);
    REQUIRE(accounts[1].uid == "2");
    REQUIRE(accounts[1].environment == "b");
    REQUIRE(accounts[1].exfid == std::vector<uint64_t>{ 1, 2, 3 });
}
