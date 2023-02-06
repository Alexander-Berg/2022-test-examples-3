#include <catch.hpp>
#include <db/module.h>
#include <ymod_pq/call.h>
#include <yplatform/application/repository.h>

using std::pair;

namespace botserver {

struct fake_deps : yplatform::module
{
    template <typename... Args>
    ymod_pq::future_result request(Args&&...)
    {
        return {};
    }

    template <typename... Args>
    ymod_pq::future_result execute(Args&&...)
    {
        return {};
    }
};

struct const_generator
{
    uint64_t res = 0;

    template <typename Int>
    Int operator()(Int /*min*/, Int /*max*/)
    {
        return res;
    }
};

using db_module = db::module_impl<fake_deps, const_generator>;

struct t_db
{
    shared_ptr<fake_deps> deps_module = make_shared<fake_deps>();
    shared_ptr<db_module> module;
    db::settings settings;

    t_db()
    {
        auto repo = yplatform::repository::instance_ptr();
        repo->add_service<fake_deps>("botdb_pq", deps_module);
        module = make_shared<db_module>(settings);
    }

    void set_code_len(int len)
    {
        module->settings.otp.code_length = len;
    }

    void generate(uint64_t res)
    {
        module->random.res = res;
    }

    yplatform::ptree make_settings_ptree()
    {
        yplatform::ptree res;
        res.put("conninfo", "");
        res.put("log_timins", true);
        res.put("otp.code_length", 1);
        res.put("otp.ttl_sec", 120);
        return res;
    }
};

TEST_CASE_METHOD(t_db, "code_generation_4_digit")
{
    auto [src, res] = GENERATE(pair{ 0, "0000" }, pair{ 1234, "1234" }, pair{ 50, "0050" });

    set_code_len(4);
    generate(src);
    REQUIRE(module->random_code() == res);
}

TEST_CASE_METHOD(t_db, "code_generation_6_digit")
{
    auto [src, res] =
        GENERATE(pair{ 0, "000000" }, pair{ 123456, "123456" }, pair{ 500, "000500" });

    set_code_len(6);
    generate(src);
    REQUIRE(module->random_code() == res);
}

TEST_CASE_METHOD(t_db, "code_generation_19_digit")
{
    auto [src, res] = GENERATE(
        pair{ 0ull, "0000000000000000000" },
        pair{ 1234567890123456789ull, "1234567890123456789" },
        pair{ 500000ull, "0000000000000500000" });

    set_code_len(19);
    generate(src);
    REQUIRE(module->random_code() == res);
}

TEST_CASE_METHOD(t_db, "limits_for_code_len")
{
    auto [len, max_num] =
        GENERATE(pair{ 1, 9ull }, pair{ 4, 9999ull }, pair{ 19, 9999999999999999999ull });
    auto ptree = make_settings_ptree();
    ptree.put("otp.code_length", len);

    auto settings = db::make_settings(ptree);
    REQUIRE(settings.otp.codes_range.second == max_num);
}

}
