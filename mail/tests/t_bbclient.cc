#include "common.h"
#include "cluster_client_mock.h"
#include "bbclient.h"
#include "settings.h"
#include <catch.hpp>

using namespace boost::system;

namespace ymod_blackbox {

struct T_bbclient
{
    using raw_response = std::string;
    using user_info_cb = callback<error, raw_response>;

    T_bbclient()
    {
        ctx = boost::make_shared<yplatform::task_context>();
        cluster_client_ptr =
            std::make_shared<ymod_httpclient::cluster_client_mock>(make_resp_body());
        st.pool_size = 1;
        st.max_queue_size = 1;
    }

    auto bbclient_ptr()
    {
        if (!client)
        {
            client = std::make_shared<bbclient>(io, cluster_client_ptr, st);
        }
        return client;
    }

    void run_io()
    {
        io.run();
        io.reset();
    }

    std::string& get_req_url() const
    {
        return cluster_client_ptr->req.url;
    }

    std::string make_resp_body(
        const std::string& uid = "1",
        const std::string& login = "test_login")
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
               "<doc>\n"
               "<uid hosted=\"0\">" +
            uid +
            "</uid>\n"
            "<login>" +
            login +
            "</login>\n"
            "<have_password>1</have_password>\n"
            "<have_hint>1</have_hint>\n"
            "<karma confirmed=\"0\">0</karma>\n"
            "<karma_status>0</karma_status>\n"
            "</doc>";
    }

    void set_resp_body(const std::string& resp_body)
    {
        cluster_client_ptr->resp.body = resp_body;
    }

    error_code& get_ec() const
    {
        return cluster_client_ptr->ec;
    }

    void set_ec(const error_code& ec)
    {
        cluster_client_ptr->ec = ec;
    }

    struct async_info_args
    {
        std::string uid;
        address addr;
        user_info_cb cb;
        options_list opts;
        db_fields_list fields;
        attribute_list attributes;
    };

    void async_info_call(async_info_args& args)
    {
        bbclient_ptr()->async_info(
            ctx,
            args.uid,
            args.addr,
            [this, &args](const error& err, const info_response& resp) {
                args.cb(err, resp.raw_response());
            },
            args.opts,
            args.fields,
            args.attributes);
        run_io();
    }

    boost::shared_ptr<yplatform::task_context> ctx;
    settings st;
    std::shared_ptr<ymod_httpclient::cluster_client_mock> cluster_client_ptr;
    std::shared_ptr<bbclient> client;
    boost::asio::io_service io;
};

TEST_CASE_METHOD(T_bbclient, "should end with task_queue_overflow error")
{
    st.pool_size = 0;
    st.max_queue_size = 0;
    async_info_args args = {};
    async_info_call(args);

    REQUIRE(std::get<0>(args.cb.args()).ext_reason == "queue capacity exceeded");
}

TEST_CASE_METHOD(T_bbclient, "should end with network_down error")
{
    auto ec = errc::make_error_code(errc::errc_t::network_down);
    set_ec(ec);
    async_info_args args = {};
    async_info_call(args);

    REQUIRE(get_ec() == ec);
}

TEST_CASE_METHOD(T_bbclient, "should construct request")
{
    std::string uid = "30";
    address addr = { "127.0.0.1" };
    async_info_args args = { .uid = uid, .addr = addr };
    async_info_call(args);

    REQUIRE(!std::get<0>(args.cb.args()));
    REQUIRE(get_req_url() == "method=userinfo&uid=30&userip=127%2E0%2E0%2E1");
}

TEST_CASE_METHOD(T_bbclient, "should construct request with options")
{
    options_list opts = { { option::regname }, { option::get_default_email } };
    async_info_args args = { .opts = opts };
    async_info_call(args);

    REQUIRE(!std::get<0>(args.cb.args()));
    REQUIRE(get_req_url() == "method=userinfo&uid=&userip=&regname=yes&emails=getdefault");
}

TEST_CASE_METHOD(T_bbclient, "should construct request with db_fields")
{
    db_fields_list fields = { db_field("db_field1"), db_field("db_field2") };
    async_info_args args = { .fields = fields };
    async_info_call(args);

    REQUIRE(!std::get<0>(args.cb.args()));
    REQUIRE(get_req_url() == "method=userinfo&uid=&userip=&dbfields=db_field1,db_field2");
}

TEST_CASE_METHOD(T_bbclient, "should construct request with attributes")
{
    attribute_list attributes = { "attr1", "attr2" };
    async_info_args args = { .attributes = attributes };
    async_info_call(args);

    REQUIRE(!std::get<0>(args.cb.args()));
    REQUIRE(get_req_url() == "method=userinfo&uid=&userip=&attributes=attr1,attr2");
}

TEST_CASE_METHOD(T_bbclient, "should construct request with user_port")
{
    address addr = { "", 8080 };
    async_info_args args = { .addr = addr };
    async_info_call(args);

    REQUIRE(!std::get<0>(args.cb.args()));
    REQUIRE(get_req_url() == "method=userinfo&uid=&userip=&user_port=8080");
}

TEST_CASE_METHOD(T_bbclient, "should parse response")
{
    std::string uid = "30";
    auto resp_body = make_resp_body(uid);
    set_resp_body(resp_body);
    async_info_args args = { .uid = uid };
    async_info_call(args);

    REQUIRE(!std::get<0>(args.cb.args()));
    REQUIRE(cluster_client_ptr->resp.body == resp_body);
}

TEST_CASE_METHOD(T_bbclient, "should end with response body parse error")
{
    auto resp_body = "wrong format response body";
    set_resp_body(resp_body);
    async_info_args args = {};
    async_info_call(args);

    REQUIRE(
        std::get<0>(args.cb.args()).ext_reason ==
        "Failed to parse blackbox response: wrong format response body");
}

}
