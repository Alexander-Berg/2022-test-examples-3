#include "web/auth/secure_sign.h"
#include <catch.hpp>

namespace {

using string = std::string;
using services = std::vector<string>;
using uids = std::vector<string>;
using topics = std::vector<string>;

}

namespace yxiva::web {

TEST_CASE("compose_sign_data/topic_user_service", "")
{
    services services{ "auto.ru" };
    uids uids{ "123" };
    topics topics{ "courage" };

    auto result = compose_sign_data(services, uids, topics);
    REQUIRE(result == "courage:auto.ru,123,");
}

TEST_CASE("compose_sign_data/topics_user_service", "")
{
    services services{ "mail.ru" };
    uids uids{ "me" };
    topics topics{ "disease", "basis", "hat" };

    auto result = compose_sign_data(services, uids, topics);
    REQUIRE(result == "disease,basis,hat:mail.ru,me,");
}

TEST_CASE("compose_sign_data/user_service", "")
{
    services services{ "distribution" };
    uids uids{ "789879879" };
    topics topics{};

    auto result = compose_sign_data(services, uids, topics);
    REQUIRE(result == "distribution,789879879,");
}

TEST_CASE("compose_sign_data/multi_service", "")
{
    services services{ "agreement", "opinion", "lake" };
    uids uids{ "789879879" };
    topics topics{};

    auto result = compose_sign_data(services, uids, topics);
    REQUIRE(result == "agreement,opinion,lake,789879879,");
}

TEST_CASE("compose_sign_data/multi_service_multi_user", "")
{
    services services{ "championship", "policy" };
    uids uids{ "789879879", "abc", "1001" };
    topics topics{};

    auto result = compose_sign_data(services, uids, topics);
    REQUIRE(result == "championship,policy,789879879,abc,1001,");
}

TEST_CASE("sign_data/multi_topic", "")
{
    uids uids;
    topics topics{ "orange",      "advertising", "disease",     "basis",        "hat",
                   "disk",        "device",      "inflation",   "country",      "affair",
                   "player",      "variety",     "physics",     "championship", "policy",
                   "improvement", "manager",     "development", "cookie",       "education",
                   "pizza",       "trainer",     "apartment",   "aspect",       "arrival",
                   "bird",        "length",      "agreement",   "opinion",      "lake",
                   "indication",  "painting",    "perception",  "courage",      "success",
                   "penalty",     "drama",       "tension",     "consequence",  "distribution",
                   "lab",         "breath",      "poem",        "owner",        "wealth",
                   "importance",  "baseball",    "variation",   "depression",   "satisfaction" };
    services services{ "startrek" };

    auto result = compose_sign_data(services, uids, topics);
    REQUIRE(
        result ==
        "orange,advertising,disease,basis,hat,disk,device,inflation,country,affair,"
        "player,variety,physics,championship,policy,improvement,manager,development,"
        "cookie,education,pizza,trainer,apartment,aspect,arrival,bird,length,"
        "agreement,opinion,lake,indication,painting,perception,courage,success,"
        "penalty,drama,tension,consequence,distribution,lab,breath,poem,owner,"
        "wealth,importance,baseball,variation,depression,satisfaction:startrek,");
}

TEST_CASE("make_secure_sign/simple", "")
{
    string data = "fdgadf";
    time_t ts = 2345345;
    string secret_seed = "213kjiu";

    auto secure_sign = make_secure_sign(data, ts, secret_seed);
    REQUIRE(secure_sign == "db29de00164e9c066fac2675114a59aa");
}

TEST_CASE("make_secure_sign/stable", "")
{
    string data = "some_data";
    time_t ts = 123;
    string secret_seed = "dfgsdf";

    auto secure_sign1 = make_secure_sign(data, ts, secret_seed);
    auto secure_sign2 = make_secure_sign(data, ts, secret_seed);
    REQUIRE(secure_sign1 == secure_sign2);
}

TEST_CASE("make_secure_sign/different_data", "")
{
    string data1 = "some_data1";
    string data2 = "some_data2";
    time_t ts = 98765;
    string secret_seed = "sdafasdgs";

    auto secure_sign1 = make_secure_sign(data1, ts, secret_seed);
    auto secure_sign2 = make_secure_sign(data2, ts, secret_seed);
    REQUIRE(secure_sign1 != secure_sign2);
}

TEST_CASE("make_secure_sign/different_ts", "")
{
    string data = "asgdkmoksadjviuasd";
    time_t ts1 = 23145235;
    time_t ts2 = 98765;
    string secret_seed = "dsacxs";

    auto secure_sign1 = make_secure_sign(data, ts1, secret_seed);
    auto secure_sign2 = make_secure_sign(data, ts2, secret_seed);
    REQUIRE(secure_sign1 != secure_sign2);
}

TEST_CASE("make_secure_sign/different_secret_seed", "")
{
    string data = "jnhbgvfcdxszd";
    time_t ts = 345678878;
    string secret_seed1 = "dsvsadv";
    string secret_seed2 = "87654";

    auto secure_sign1 = make_secure_sign(data, ts, secret_seed1);
    auto secure_sign2 = make_secure_sign(data, ts, secret_seed2);
    REQUIRE(secure_sign1 != secure_sign2);
}

TEST_CASE("make_secure_sign/services_uids_topic_data", "")
{
    services services{ "service1", "service2" };
    uids uids{ "789879879", "2341234" };
    string topic = "topic";
    time_t ts = std::numeric_limits<time_t>::max();
    string secret = "gfdasdf";

    auto sign = make_secure_sign(services_uids_topic_data(services, uids, topic), ts, secret);
    REQUIRE(sign == "017e30a45cda582b194d55e6b4ee5c8e");
}

TEST_CASE("make_secure_sign/services_uids_topic_data/empty_topic", "")
{
    services services{ "service8", "service9" };
    uids uids{ "2", "8765" };
    time_t ts = std::numeric_limits<time_t>::max();
    string secret = "dsfgdsfg4";

    auto sign1 = make_secure_sign(services_uids_topic_data(services, uids, ""), ts, secret);
    auto sign2 = make_secure_sign(services_uids_topics_data(services, uids, {}), ts, secret);
    REQUIRE(sign1 == sign2);
}

TEST_CASE("make_secure_sign/services_uids_topics_data", "")
{
    services services{ "service13", "service666" };
    uids uids{ "123", "124" };
    topics topics{ "topic1", "topic2", "topic3" };
    time_t ts = std::numeric_limits<time_t>::max();
    string secret = "6y5trf4";

    auto sign = make_secure_sign(services_uids_topics_data(services, uids, topics), ts, secret);
    REQUIRE(sign == "50839d771f91c29cf1e492261de4cced");
}

TEST_CASE("make_secure_sign/uid_data", "")
{
    string uid = "87654567";
    time_t ts = std::numeric_limits<time_t>::max();
    string secret = "kjnbvf";

    auto sign = make_secure_sign(uid_data(uid), ts, secret);
    REQUIRE(sign == "d28809f033eb42fceb889c07e84a1dcf");
}

}
