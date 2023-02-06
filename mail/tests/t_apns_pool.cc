#include "mod_apns/senders_pool.h"
#include <catch.hpp>

namespace yxiva::mobile::apns {

static const auto type_auto = app_environment::automatic;

struct sender_mock : public sender
{
    sender_mock(const yplatform::log::source& parent_logger) : sender(parent_logger)
    {
    }
    void push(const mobile_task_context_ptr&, callback_t&&) override
    {
    }
    const string& secret_type() const override
    {
        static const string secret_type = "mock";
        return secret_type;
    }
};

struct t_apns_pool : public senders_pool
{
    void start(secrets_map secrets)
    {
        reactor->init(1, 1);
        senders_pool::start(secrets, settings, reactor);
    }

    void update(secrets_map secrets)
    {
        senders_pool::update(secrets, settings);
    }

    sender_ptr create_sender(const string&, const secret&, const apns_settings&, const char*)
        override
    {
        return std::make_shared<sender_mock>(yplatform::log::source());
    }

    yplatform::reactor_ptr reactor{ new yplatform::reactor };
    apns_settings settings;
};

// @todo: check when callback return nullptr it is not added to pool

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/start/empty", "")
{
    start({});
    REQUIRE((pool()->empty() && known_secrets()->empty()));
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/start/one_secret", "")
{
    start(secrets_map{ { "nameA", { "secretA", type_auto } } });
    REQUIRE(known_secrets()->size() == 1);
    REQUIRE(pool()->size() == 1);
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/start/two_secrets", "")
{
    start(secrets_map{ { "nameA", { "secretA", type_auto } },
                       { "nameB", { "secretB", type_auto } } });
    REQUIRE(known_secrets()->size() == 2);
    REQUIRE(pool()->size() == 2);
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/start/many_secrets", "")
{
    secrets_map secrets;
    const int count = 10;
    for (int i = 0; i < count; ++i)
    {
        secrets.insert(std::make_pair(std::to_string(i), secret{ std::to_string(i), type_auto }));
    }

    start(secrets);
    REQUIRE(known_secrets()->size() == count);
    REQUIRE(pool()->size() == count);
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/start/empty_one_secret", "")
{
    start(secrets_map{ { "nameA", { "", type_auto } } });
    REQUIRE(known_secrets()->size() == 1);
    REQUIRE(pool()->size() == 0);
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/update/empty/empty_start", "")
{
    start({});
    REQUIRE((pool()->empty() && known_secrets()->empty()));
    update({});
    REQUIRE((pool()->empty() && known_secrets()->empty()));
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/update/empty/full_start", "")
{
    secrets_map vals = { { "nameA", { "secretA", type_auto } },
                         { "nameB", { "secretB", type_auto } } };
    start(vals);
    update({});
    REQUIRE((pool()->size() == 2 && known_secrets()->size() == 2));
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/update/new/empty_start", "")
{
    secrets_map vals = { { "nameA", { "secretA", type_auto } },
                         { "nameB", { "secretB", type_auto } } };
    start({});
    update(vals);
    REQUIRE((pool()->size() == 2 && known_secrets()->size() == 2));
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/update/new/full_start", "")
{
    start({ { "nameA", { "secretA", type_auto } } });
    update({ { "nameB", { "secretB", type_auto } } });
    REQUIRE((pool()->size() == 2 && known_secrets()->size() == 2));
    update({ { "nameC", { "secretC", type_auto } }, { "nameD", { "secretD", type_auto } } });
    REQUIRE((pool()->size() == 4 && known_secrets()->size() == 4));
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/update/duplicates", "")
{
    start({ { "nameA", { "secretA", type_auto } }, { "nameB", { "secretB", type_auto } } });
    update({ { "nameA", { "secretA", type_auto } } });
    REQUIRE((pool()->size() == 2 && known_secrets()->size() == 2));
    update({ { "nameB", { "secretB", type_auto } } });
    REQUIRE((pool()->size() == 2 && known_secrets()->size() == 2));
    update({ { "nameA", { "secretA", type_auto } }, { "nameB", { "secretB", type_auto } } });
    REQUIRE((pool()->size() == 2 && known_secrets()->size() == 2));
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/changed/full_start", "")
{
    start({ { "nameA", { "secretA", type_auto } }, { "nameB", { "secretB", type_auto } } });
    update({ { "nameA", { "newSecretA1", type_auto } } });
    update({ { "nameA", { "newSecretA2", type_auto } }, { "nameB", { "newSecretB", type_auto } } });
    REQUIRE((pool()->size() == 2 && known_secrets()->size() == 2));
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/changed/empty_start", "")
{
    start({});
    update({ { "nameA", { "newSecretA1", type_auto } } });
    update({ { "nameA", { "newSecretA2", type_auto } } });
    update({ { "nameA", { "newSecretA3", type_auto } }, { "nameB", { "newSecretB", type_auto } } });
    REQUIRE((pool()->size() == 2 && known_secrets()->size() == 2));
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/type_changed/full_start", "")
{
    auto type_prod = app_environment::production;
    start({ { "nameA", { "secretA", type_auto } }, { "nameB", { "secretB", type_auto } } });
    update({ { "nameA", { "secretA", type_prod } }, { "nameB", { "secretB", type_prod } } });
    REQUIRE((pool()->size() == 2 && known_secrets()->size() == 2));
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/type_changed/empty_start", "")
{
    auto type_prod = app_environment::production;
    start({});
    update({ { "nameA", { "secretA", type_auto } } });
    update({ { "nameA", { "secretA", type_auto } } });
    update({ { "nameA", { "secretA", type_prod } }, { "nameB", { "secretB", type_prod } } });
    REQUIRE((pool()->size() == 2 && known_secrets()->size() == 2));
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/delete/full_start", "")
{
    start({ { "nameA", { "secretA", type_auto } }, { "nameB", { "secretB", type_auto } } });
    update({ { "nameA", { "", type_auto } } });
    REQUIRE((pool()->size() == 1 && known_secrets()->size() == 2));
    update({ { "nameB", { "", type_auto } } });
    REQUIRE((pool()->size() == 0 && known_secrets()->size() == 2));
    update({ { "nameA", { "secretA", type_auto } }, { "nameB", { "secretB", type_auto } } });
    update({ { "nameA", { "", type_auto } }, { "nameB", { "", type_auto } } });
    REQUIRE((pool()->size() == 0 && known_secrets()->size() == 2));
}

TEST_CASE_METHOD(t_apns_pool, "apns/senders_pool/delete/empty_start", "")
{
    start({});
    update({ { "nameA", { "secretA", type_auto } }, { "nameB", { "secretB", type_auto } } });
    update({ { "nameA", { "", type_auto } }, { "nameB", { "", type_auto } } });
    REQUIRE((pool()->size() == 0 && known_secrets()->size() == 2));
}

}