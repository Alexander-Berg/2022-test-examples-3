#include "catch.hpp"

#include "common.h"
#include "fake_blackbox.h"
#include <common/errors.h>
#include <passport/client_impl.h>
#include <yplatform/application/repository.h>

using namespace collectors;
using namespace collectors::passport;

struct passport_client_test
{
    passport_client_test()
    {
        yhttp::cluster_client::settings st;
        st.nodes.push_back("fake_node");
        std::shared_ptr<yhttp::cluster_client> validator_client =
            std::make_shared<yhttp::cluster_client>(io, st);

        auto repo = yplatform::repository::instance_ptr();
        repo->add_service<fake_blackbox>("bbclient", bbclient);
        repo->add_service<yhttp::cluster_client>("validator_client", validator_client);

        client = std::make_shared<client_impl>(boost::make_shared<yplatform::task_context>());
    }

    boost::asio::io_service io;
    std::shared_ptr<fake_blackbox> bbclient = std::make_shared<fake_blackbox>();
    std::shared_ptr<client_impl> client;
};

TEST_CASE_METHOD(passport_client_test, "userinfo/no-addresses")
{
    callback<error, user_info> cb;
    client->get_userinfo_by_login("no-addresses", cb);

    REQUIRE(std::get<0>(cb.args()) == collectors::code::empty_email);
}

TEST_CASE_METHOD(passport_client_test, "userinfo/non-native-address")
{
    callback<error, user_info> cb;
    client->get_userinfo_by_login("non-native-address", cb);

    REQUIRE(std::get<0>(cb.args()) == collectors::code::empty_email);
}

TEST_CASE_METHOD(passport_client_test, "userinfo/non-validated-address")
{
    callback<error, user_info> cb;
    client->get_userinfo_by_login("non-validated-address", cb);

    REQUIRE(std::get<0>(cb.args()) == collectors::code::empty_email);
}

TEST_CASE_METHOD(passport_client_test, "userinfo/non-default-address")
{
    callback<error, user_info> cb;
    client->get_userinfo_by_login("non-default-address", cb);

    REQUIRE(!std::get<0>(cb.args()));
    REQUIRE(std::get<1>(cb.args()).email == "valid@ya.ru");
}

TEST_CASE_METHOD(passport_client_test, "userinfo/many-different-addresses")
{
    callback<error, user_info> cb;
    client->get_userinfo_by_login("many-different-addresses", cb);

    REQUIRE(!std::get<0>(cb.args()));
    REQUIRE(std::get<1>(cb.args()).email == "valid@ya.ru");
}
