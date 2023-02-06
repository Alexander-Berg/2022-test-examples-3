#include <catch.hpp>

#include "src/h2/utils.h"
#include <ymod_httpclient/h2/client.h>
#include <yplatform/reactor.h>
#include <yplatform/net/server.h>
#include <yplatform/application/global_init.h>
#include <nghttp2/nghttp2.h>
#include <map>
#include <sstream>
#include <string>
#include <thread>

#define NGHTTP2_FRAME_HDLEN 9

#define MAKE_NV(K, V)                                                                              \
    {                                                                                              \
        (uint8_t*)K, (uint8_t*)V, sizeof(K) - 1, sizeof(V) - 1, NGHTTP2_NV_FLAG_NONE               \
    }

using namespace ymod_httpclient;

inline bool select_proto(
    const unsigned char** out,
    unsigned char* outlen,
    const unsigned char* in,
    unsigned int inlen,
    const string& key)
{
    for (auto p = in, end = in + inlen; p + key.size() <= end; p += *p + 1)
    {
        if (std::equal(std::begin(key), std::end(key), p))
        {
            *out = p + 1;
            *outlen = *p;
            return true;
        }
    }
    return false;
}

inline void nghttp2_put_uint32be(uint8_t* buf, uint32_t n)
{
    uint32_t x = htonl(n);
    memcpy(buf, &x, sizeof(uint32_t));
}

inline void nghttp2_frame_pack_frame_hd(uint8_t* buf, const nghttp2_frame_hd* hd)
{
    nghttp2_put_uint32be(&buf[0], (uint32_t)(hd->length << 8));
    buf[3] = hd->type;
    buf[4] = hd->flags;
    nghttp2_put_uint32be(&buf[5], (uint32_t)hd->stream_id);
    /* ignore hd->reserved for now */
}

inline std::vector<char> make_settings_ack_frame(bool ack)
{
    auto shared_buf = make_shared<std::vector<char>>();
    auto& buf = *shared_buf;
    nghttp2_frame_hd hd;
    hd.length = 0;
    hd.type = NGHTTP2_SETTINGS;
    hd.flags = ack ? 1 : 0;
    hd.stream_id = 0;
    buf.resize(NGHTTP2_FRAME_HDLEN, 0);
    nghttp2_frame_pack_frame_hd(reinterpret_cast<unsigned char*>(&buf[0]), &hd);
    return buf;
}

inline std::vector<char> make_simple_answer_frame(int32_t stream_id)
{
    std::vector<char> ret;

    nghttp2_nv nva[] = { MAKE_NV(":status", "200") };
    auto nvalen = sizeof(nva) / sizeof(nva[0]);

    nghttp2_hd_deflater* deflater;
    int res = nghttp2_hd_deflate_new(&deflater, 1024);
    if (res) throw std::runtime_error("initialize deflater error");
    auto len = nghttp2_hd_deflate_bound(deflater, nva, nvalen);
    ret.resize(NGHTTP2_FRAME_HDLEN + len, 0);
    unsigned char* buf = reinterpret_cast<unsigned char*>(&ret[0]);
    len = nghttp2_hd_deflate_hd(deflater, &buf[NGHTTP2_FRAME_HDLEN], len, nva, nvalen);
    nghttp2_hd_deflate_del(deflater);
    ret.resize(NGHTTP2_FRAME_HDLEN + len);

    nghttp2_frame_hd hd;
    hd.length = len;
    hd.type = NGHTTP2_HEADERS;
    hd.flags = NGHTTP2_FLAG_END_STREAM | NGHTTP2_FLAG_END_HEADERS;
    hd.stream_id = stream_id;

    nghttp2_frame_pack_frame_hd(buf, &hd);
    return ret;
}

struct fake_session : enable_shared_from_this<fake_session>
{
    fake_session(yplatform::net::tcp_socket&& socket) : socket(std::move(socket))
    {
        read_buffer.resize(5 * 1024 * 1024);
    }

    template <typename Handler>
    void tls_handshake(time_traits::duration t, Handler h)
    {
        socket.async_tls_handshake(yplatform::net::tcp_socket::handshake_type::server, t, h);
    }

    template <typename Handler>
    void read_next(time_traits::duration t, Handler h)
    {
        auto self = shared_from_this();
        socket.async_read(
            boost::asio::buffer(read_buffer.data(), read_buffer.size()),
            t,
            [self, this, h](boost::system::error_code ec, size_t bytes) {
                if (!ec)
                {
                    h(read_buffer.data(), bytes);
                }
            });
    }

    template <typename Handler>
    void write_settings(time_traits::duration t, Handler h)
    {
        auto buf = make_settings_ack_frame(false);
        auto buf_settings_ack = make_settings_ack_frame(true);
        buf.insert(buf.end(), buf_settings_ack.begin(), buf_settings_ack.end());
        socket.async_write(
            boost::asio::buffer(buf.data(), buf.size()),
            t,
            [buf, h](boost::system::error_code ec, size_t) {
                if (!ec)
                {
                    h();
                }
            });
    }

    template <typename Handler>
    void write_simple_response(int32_t stream_id, time_traits::duration t, Handler h)
    {
        auto buf = make_simple_answer_frame(stream_id);

        socket.async_write(
            boost::asio::buffer(buf.data(), buf.size()),
            t,
            [buf, h](boost::system::error_code ec, size_t) {
                if (!ec)
                {
                    h();
                }
            });
    }

    yplatform::net::tcp_socket socket;
    std::vector<char> read_buffer;
    bool finished = false;
};

struct t_h2_basic
{
    t_h2_basic() : io(1), iodata(io)
    {
        setup_actors();

        ctx = boost::make_shared<yplatform::task_context>();
        url = "https://localhost:16200";
        http_url = "http://localhost:16200";
    }

    ~t_h2_basic()
    {
        L_(info) << "end of test";
        server->stop();
        sessions.clear();
        server.reset();
        client.reset();
    }

    void setup_actors()
    {
        h2::settings st;
        st.socket.tcp_no_delay = 1;
        // openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 900 -nodes
        st.ssl.cert = R"(-----BEGIN CERTIFICATE-----
MIIFXTCCA0WgAwIBAgIJAO+7yOAqaj0AMA0GCSqGSIb3DQEBCwUAMEUxCzAJBgNV
BAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBX
aWRnaXRzIFB0eSBMdGQwHhcNMTkwNzE2MDM1ODMwWhcNMjIwMTAxMDM1ODMwWjBF
MQswCQYDVQQGEwJBVTETMBEGA1UECAwKU29tZS1TdGF0ZTEhMB8GA1UECgwYSW50
ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIIC
CgKCAgEAsiKDxt70wn33/qGmez6a8nTJoB7Q1PQ0BAa+GPwSSuFIuLXeb18kMm/s
LE1dH3LzUoodphTH9U9mvE6LOvWAJdf7vDMdH77MJKboJpsfnYwdUxjYgs4Mv41j
9dEz6gzI8oPQMIqupCRbMkPue7KvXA16RsbztLJjrfO6lDH3IZxtfUCcg/5wbE91
LFXTX9eeRmIxPQmobNq6yYgQRgazqPT6Xe6ym60YWdZaF/xlQEMURKgIKCQOD95B
uw+uIoP4K2HuTVNxAIChuuUAf/vIiFXxFHO1U44biAXao+d8FvPjS4YsOp4qhV9v
ir2C7CXvx2Xi3mH0iPKm57Djt1jtXFioXs186f/ia0oPZisofMywXV3X2DCtgoTg
MuaLR9XhUvXNDRVZh5Iw/VD3/Q3oatzlLlL3t9ebZLnq8gRId+FQbaF9uOjpvJFT
mdwO+fQXWse0Wa1f2Z5ug/wSgLKaeua+pA2bs8Kv3gbJnPzk9Mu/sJGDxmW3JyEa
nUicQgIZksnoC7N3NLCmMLbXmQwuS8Z5Itr74NoQYNzI3JXHOHEzfUz0AYBvb4NL
L8AFdfH5S3t5TOpQo9OKHwFFt50SaPj5sVj+jM4k0qfH0RZf4rHpqwLU5m2tIjes
Q0EHM1VVMjLFKAIz+BM9VaSJcXW2TlsRijV3jSdAPkKnPV1Icx0CAwEAAaNQME4w
HQYDVR0OBBYEFGYNeEmxdudVi8LxUAEhM2AKFt12MB8GA1UdIwQYMBaAFGYNeEmx
dudVi8LxUAEhM2AKFt12MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQELBQADggIB
AHY6+7iCExtxs14VHbr4MLEzjQGyExgNAkmf/6nAtC5hpedKIOoZobrB9IFviB98
XBBQa1OEPi4WvPxPMBD0towlpgIBRi0SFdfADrlLst2MlJjnKapvgkyXyWZXKtvu
shwQcZTfmTGntI0E3dVESS+VmcdQgwL85vc1iEvPuhgCC1crG+7apdkriIKJTwRZ
j9CeAj6HXsSUM2baLEqHgnn00jdUG1FFnsRbgacPzDQufiXMwVLN/DRkGBNPm0D1
1Cl2pfRzCBs4S7Kj6RAW0eFYYRcEdiLm2K1BbNLKKDH16QkxvKazcX2IDFihbK7v
AfWsBOWxXI/KYT6BPB5b7yw35PdCtqoeA19NuqETea52f+rUd/h8M+9qHX6TmMOJ
9OaepYw1j8u9lYSppLMbMJxqWBzhiaSaIylwW7NHrG2YWgs+VxgWDA9tmxzteQr3
1soawkw3rICR3xEUtuTWHPt9XEPouO8p6Xkft7t6MoHa1NZdkZ1tgWuNeRcoetuU
n++PjcpGv09UFvMCPTEXEaLlKtTLTgvghKKOM2bzwydOm6CmCEZuDoC927Jpy6+e
NAzSGRINBOo/XF2EjZ3yX3ptN0aTsZN2cI0SYlICCD95BSS91snaxVHB1yon8CcC
givb96GV1Rb2W08XRVBcVlWOfEOKR6J0dqYq5r2ZEo2a
-----END CERTIFICATE-----
-----BEGIN PRIVATE KEY-----
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
z5QWbydOKT8XV7FL2wRRHCXiCFERVQ==
-----END PRIVATE KEY-----)";
        st.ssl.key = st.ssl.cert;

        yplatform::log::init_global_log_console();
        client.reset(new h2::client(*iodata.get_io(), st));
        client->logger(YGLOBAL_LOGGER);

        iodata.setup_ssl(st.ssl);
        auto ssl_ctx = iodata.get_ssl_context()->native_handle();

        SSL_CTX_set_alpn_select_cb(
            ssl_ctx,
            [](SSL*,
               const unsigned char** out,
               unsigned char* outlen,
               const unsigned char* in,
               unsigned int inlen,
               void*) {
                int rv = nghttp2_select_next_protocol((unsigned char**)out, outlen, in, inlen);
                return (rv != 1) ? SSL_TLSEXT_ERR_NOACK : SSL_TLSEXT_ERR_OK;
            },
            nullptr);
        iodata.setup_dns();
        yplatform::net::server_settings server_st;
        server_st.tcp_no_delay = 1;
        server.reset(new yplatform::net::tcp_server(iodata, "::", 16200, server_st));
        server->logger(YGLOBAL_LOGGER);
        server->listen([this](yplatform::net::tcp_socket&& socket) {
            auto sess = make_shared<fake_session>(std::move(socket));
            sessions.push_back(sess);
        });
        io.run_for(timeout);
    }

    void nanosleep(int multiplier = 1)
    {
        io.run_for(timeout * multiplier);
    }

    boost::asio::io_service io;
    yplatform::net::io_data iodata;
    std::unique_ptr<h2::client> client;
    std::unique_ptr<yplatform::net::tcp_server> server;
    task_context_ptr ctx;
    string url;
    string http_url;
    time_traits::duration timeout = std::chrono::milliseconds(10);
    std::vector<shared_ptr<fake_session>> sessions;
};

TEST_CASE_METHOD(t_h2_basic, "h2/ssl_err_if_server_drops_connection")
{
    bool executed = false;
    client->async_run(
        ctx, h2::request::GET(url + "/test"), [&](boost::system::error_code e, response) {
            REQUIRE(e == http_error::ssl_error);
            executed = true;
        });
    nanosleep();

    REQUIRE(sessions.size() == 1);
    sessions.clear();
    nanosleep();

    REQUIRE(executed == true);
}

TEST_CASE_METHOD(t_h2_basic, "h2/unsupported_scheme")
{
    bool executed = false;
    client->async_run(
        ctx,
        h2::request::GET(http_url + "/test"),
        [&](const boost::system::error_code& e, response) {
            REQUIRE(e == http_error::unsupported_scheme);
            executed = true;
        });
    nanosleep();
    REQUIRE(executed == true);
}

TEST_CASE_METHOD(t_h2_basic, "h2/sends_request_after_handshake_and_settings")
{
    bool executed = false;
    client->async_run(
        ctx, h2::request::GET(url + "/test"), [&](const boost::system::error_code& e, response r) {
            REQUIRE(e == http_error::success);
            REQUIRE(r.status == 200);
            executed = true;
        });
    nanosleep();

    REQUIRE(sessions.size() == 1);
    auto& session = *sessions[0];
    session.tls_handshake(timeout, [](boost::system::error_code ec) { REQUIRE(!ec); });
    nanosleep();

    session.read_next(timeout, [](const char*, size_t len) { REQUIRE(len > 0); });
    nanosleep();

    session.write_settings(timeout, []() {});
    nanosleep();

    session.write_simple_response(1, timeout, []() {});
    nanosleep();

    REQUIRE(executed == true);
}

// WARN Looks like a bug in nghttp2.
// Protocol obliges to send settings first but we have no guarantees
// that server's implementations is correct.
TEST_CASE_METHOD(t_h2_basic, "h2/nothing_happens_if_send_request_after_handshake_before_settings")
{
    bool executed = false;
    client->async_run(
        ctx, h2::request::GET(url + "/test"), [&](boost::system::error_code, response) {});
    nanosleep();

    REQUIRE(sessions.size() == 1);
    auto& session = *sessions[0];

    session.tls_handshake(timeout, [](boost::system::error_code ec) { REQUIRE(!ec); });
    nanosleep();

    session.read_next(timeout, [](const char*, size_t len) { REQUIRE(len > 0); });
    nanosleep();

    session.write_simple_response(1, timeout, []() {});
    nanosleep();

    session.write_settings(timeout, []() {});
    nanosleep(4);

    REQUIRE(executed == false);
}
