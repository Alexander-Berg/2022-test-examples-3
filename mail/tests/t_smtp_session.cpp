#include <gtest/gtest.h>

#include "dummy_smtp_server.h"

#include <smtp_session_impl.h>
#include <server_response.h>

#include <yplatform/net/io_data.h>

#include <boost/algorithm/string.hpp>
#include <boost/asio/yield.hpp>

using namespace std::chrono_literals;
using namespace ymod_smtpclient;
using testing::server::SmtpServer;
using testing::server::SMTP_PORT;
using testing::server::SMTP_HOST;

namespace {

constexpr unsigned short SMTP_SSL_PORT = 1465;

struct Coro : boost::asio::coroutine {
    using CoroBody = std::function<void()>;

    void operator()() {
        body();
    }

    void run(CoroBody&& coroBody) {
        body = coroBody;
        body();
    }

    CoroBody body;
};

inline void checkCommand(std::string expected, std::string actual) {
    ASSERT_TRUE(boost::starts_with(actual, expected));
}

inline void checkMessage(std::string expected, std::string actual) {
    ASSERT_EQ(actual, expected);
}

class SmtpSessionTest : public testing::Test {
public:
    SmtpSessionTest()
        : ioService()
        , ioData(ioService)
    {
        ioData.setup_ssl({});
        ioData.setup_dns({});
    }

private:
    boost::asio::io_service ioService;
    yplatform::net::io_data ioData;

    auto createSmtpSessionImpl() {
        Settings settings;
        settings.smtpSslDefaultPort = SMTP_SSL_PORT;
        return std::make_shared<SmtpSessionImpl>(ioData, settings);
    }

protected:
    std::shared_ptr<SmtpSessionImpl> session;

    template <class Handler>
    void runTest(Coro& coro, Handler && handler) {
        session = createSmtpSessionImpl();
        coro.run(handler);
        ioService.run();
    }
};

} // namespace


TEST_F(SmtpSessionTest, ConnectSuccess) {
    SmtpServer server([](auto& s){
        s.sock.close();
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none, [](auto errc) {
                EXPECT_EQ(errc, error::Code::Success);
            });
        }
    });
}

TEST_F(SmtpSessionTest, ConnectError) {
    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none, [](auto errc) {
                EXPECT_EQ(errc, error::Code::ConnectError);
            });
        }
    });
}

TEST_F(SmtpSessionTest, SmtpSslNotDefaultPortWithOptionUseSslTrue) {
    SmtpServer server(SMTP_PORT, [](auto& session) {
        session.sock.close();
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, true, [&coro](auto errc) {
                EXPECT_EQ(errc, error::Code::SslError);
                coro();
            });
        }
    });
}

TEST_F(SmtpSessionTest, SmtpSslDefaultPortWithoutOptionUseSsl) {
    SmtpServer server(SMTP_SSL_PORT, [](auto& session) {
        session.sock.close();
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_SSL_PORT, boost::none, [&coro](auto errc) {
                EXPECT_EQ(errc, error::Code::SslError);
                coro();
            });
        }
    });
}

TEST_F(SmtpSessionTest, SmtpSslDefaultPortWithOptionUseSslFalse) {
    SmtpServer server(SMTP_SSL_PORT, [](auto& session) {
        session.writeLine("220 Greeting");
    });
    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_SSL_PORT, false,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
                });
            yield session->asyncGreeting([&coro](auto errc, auto resp) {
                ASSERT_EQ(errc, error::Success);
                ASSERT_EQ(resp.replyCode, 220);
                coro();
            });
        }
    });
}

TEST_F(SmtpSessionTest, SmtpSslNotDefaultPortWithStartTls) {
    SmtpServer server(SMTP_PORT, [](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.readLine());
        session.writeLine("250-localhost");
        session.writeLine("250-SIZE 1024");
        session.writeLine("250 STARTTLS");
        checkCommand("STARTTLS", session.handleCommand("220 STARTTLS"));
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none, [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
                });
            yield session->asyncGreeting([&coro](auto errc, auto resp) {
                ASSERT_EQ(errc, error::Success);
                ASSERT_EQ(resp.replyCode, 220);
                coro();
            });
            yield session->asyncHelo(SmtpPoint::smtp, SMTP_HOST,
                [&coro](auto errc, auto resp) {
                    ASSERT_EQ(errc, error::Success);
                    ASSERT_EQ(resp.replyCode, 250);
                    coro();
            });
            yield session->asyncStartTls([&coro](auto errc, auto) {
                EXPECT_EQ(errc, error::Code::SslError);
                coro();
            });
        }
    });
}

TEST_F(SmtpSessionTest, SendMail) {
    SmtpServer server([](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.handleCommand("250 Helo"));
        checkCommand("MAIL FROM", session.handleCommand("250 MailFrom"));
        checkCommand("RCPT TO", session.handleCommand("250 RcptTo"));
        checkCommand("DATA", session.handleCommand("354 Data"));
        checkMessage("Header: header\r\n\r\nHello\r\n",
                session.handleMessage("250 Queued ok"));
        checkCommand("QUIT", session.handleCommand("221 Quit"));
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
                });
            yield session->asyncGreeting([&coro](auto errc, auto resp) {
                ASSERT_EQ(errc, error::Success);
                ASSERT_EQ(resp.replyCode, 220);
                coro();
            });
            yield session->asyncHelo(SmtpPoint::smtp, SMTP_HOST,
                [&coro](auto errc, auto resp) {
                    ASSERT_EQ(errc, error::Success);
                    ASSERT_EQ(resp.replyCode, 250);
                    coro();
            });
            yield session->asyncMailFrom({"foo@ya.ru"},
                [&coro](auto errc, auto resp) {
                    ASSERT_EQ(errc, error::Success);
                    ASSERT_EQ(resp.replyCode, 250);
                    coro();
                });
            yield session->asyncRcptTo({"bar@ya.ru"}, /*enableDsn=*/false,
                [&coro](auto errc, auto resp) {
                    ASSERT_EQ(errc, error::Success);
                    ASSERT_EQ(resp.replyCode, 250);
                    coro();
                });
            yield session->asyncDataStart([&coro](auto errc, auto resp) {
                ASSERT_EQ(errc, error::Success);
                ASSERT_EQ(resp.replyCode, 354);
                coro();
            });
            yield session->asyncWriteMessage("Header: header\r\n\r\nHello", /*enableDotStuffing=*/true,
                [&coro](auto errc, auto resp) {
                    ASSERT_EQ(errc, error::Success);
                    ASSERT_EQ(resp.replyCode, 250);
                    coro();
            });
            yield session->asyncQuit([&coro](auto errc, auto resp) {
                EXPECT_EQ(errc, error::Success);
                EXPECT_EQ(resp.replyCode, 221);
                coro();
            });
        }
    });
}

TEST_F(SmtpSessionTest, WaitEofErrorCodes) {
    SmtpServer server([](auto& s) {
        std::this_thread::sleep_for(100ms);
        s.sock.close();
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
                });
            // timeout
            yield session->asyncWaitEof(20ms, [&coro](auto errc) {
                ASSERT_EQ(errc, boost::asio::error::operation_aborted);
                coro();
            });
            // cancel
            yield {
                session->asyncWaitEof(20ms, [&coro](auto errc) {
                    ASSERT_EQ(errc, boost::asio::error::operation_aborted);
                    coro();
                });
                session->cancel();
            }
            // server closed connection
            yield session->asyncWaitEof(100ms, [&coro](auto errc) {
                ASSERT_EQ(errc, boost::asio::error::eof);
                coro();
            });
        }
    });
}

TEST_F(SmtpSessionTest, AuthPlainSuccess) {
    SmtpServer server([](auto& session) {
        session.handleCommand("235 2.7.0 Authentication successful.");
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
                });
            yield session->asyncAuth(AuthData::PLAIN("foo", "bar"),
                [&coro](auto errc, auto resp) {
                    EXPECT_EQ(errc, error::Success);
                    EXPECT_EQ(resp.replyCode, 235);
                    coro();
                });
        }
    });
}

TEST_F(SmtpSessionTest, AuthPlainFailed) {
    SmtpServer server([](auto& session) {
        session.handleCommand("535 Authentication failed");
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
                });
            yield session->asyncAuth(AuthData::PLAIN("foo", "bar"),
                [&coro](auto errc, auto resp) {
                    ASSERT_EQ(errc, error::AuthError);
                    ASSERT_EQ(resp.replyCode, 535);
                    coro();
                });
        }
    });
}

TEST_F(SmtpSessionTest, AuthLoginSuccess) {
    SmtpServer server([](auto& session) {
        ASSERT_EQ(session.readLine(), "AUTH LOGIN");

        session.writeLine("334 LOGIN");
        ASSERT_EQ(session.readLine(), "Zm9v");  // base64("foo")

        session.writeLine("334 PASSWORD");
        ASSERT_EQ(session.readLine(), "YmFy");  // base64("bar")

        session.writeLine("235 ok");
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
            });
            yield session->asyncAuth(AuthData::LOGIN("foo", "bar"),
                [&coro](auto errc, auto resp) {
                    EXPECT_EQ(errc, error::Success);
                    EXPECT_EQ(resp.replyCode, 235);
                    coro();
            });
        }
    });
}

TEST_F(SmtpSessionTest, AuthLoginFailed) {
    SmtpServer server([](auto& session) {
        ASSERT_EQ(session.readLine(), "AUTH LOGIN");

        session.writeLine("334 LOGIN");
        ASSERT_EQ(session.readLine(), "Zm9v");  // base64("foo")

        session.writeLine("334 PASSWORD");
        ASSERT_EQ(session.readLine(), "YmFy");  // base64("bar")

        session.writeLine("550 failed");
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
            });
            yield session->asyncAuth(AuthData::LOGIN("foo", "bar"),
                [&coro](auto errc, auto resp) {
                    ASSERT_EQ(errc, error::AuthError);
                    ASSERT_EQ(resp.replyCode, 550);
                    coro();
            });
        }
    });
}

TEST_F(SmtpSessionTest, BadReplyCodeOnGreeting) {
    SmtpServer server([](auto& session) {
        session.writeLine("554 Greeting");
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
                });
            yield session->asyncGreeting([&coro](auto errc, auto resp) {
                ASSERT_EQ(errc, error::BadReplyCode);
                ASSERT_EQ(resp.replyCode, 554);
                coro();
            });
        }
    });
}

TEST_F(SmtpSessionTest, BadReplyCodeOnHelo) {
    SmtpServer server([](auto& session) {
        checkCommand("EHLO", session.handleCommand("502 Helo"));
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
                });
            yield session->asyncHelo(SmtpPoint::smtp, SMTP_HOST,
                [&coro](auto errc, auto resp) {
                    ASSERT_EQ(errc, error::BadReplyCode);
                    ASSERT_EQ(resp.replyCode, 502);
                    coro();
            });
        }
    });
}

TEST_F(SmtpSessionTest, BadReplyCodeOnMailFrom) {
    SmtpServer server([](auto& session) {
        checkCommand("MAIL FROM", session.handleCommand("550 MailFrom"));
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
                });
            yield session->asyncMailFrom({"foo@ya.ru"},
                [&coro](auto errc, auto resp) {
                    ASSERT_EQ(errc, error::BadReplyCode);
                    ASSERT_EQ(resp.replyCode, 550);
                    coro();
                });
        }
    });
}

TEST_F(SmtpSessionTest, BadReplyCodeOnRcptTo) {
    SmtpServer server([](auto& session) {
        checkCommand("RCPT TO", session.handleCommand("503 RcptTo"));
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
                });
            yield session->asyncRcptTo({"bar@ya.ru"}, /*enableDsn=*/false,
                [&coro](auto errc, auto resp) {
                    ASSERT_EQ(errc, error::BadReplyCode);
                    ASSERT_EQ(resp.replyCode, 503);
                    coro();
                });
        }
    });
}

TEST_F(SmtpSessionTest, BadReplyCodeOnData) {
    SmtpServer server([](auto& session) {
        checkCommand("DATA", session.handleCommand("503 Data"));
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
                });
            yield session->asyncDataStart([&coro](auto errc, auto resp) {
                ASSERT_EQ(errc, error::BadReplyCode);
                ASSERT_EQ(resp.replyCode, 503);
                coro();
            });
        }
    });
}

TEST_F(SmtpSessionTest, BadReplyCodeOnStartTls) {
    SmtpServer server([](auto& session) {
        checkCommand("STARTTLS", session.handleCommand("502 StartTls"));
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
                });
            yield session->asyncStartTls([&coro](auto errc, auto resp) {
                ASSERT_EQ(errc, error::BadReplyCode);
                ASSERT_EQ(resp.replyCode, 502);
                coro();
            });
        }
    });
}

TEST_F(SmtpSessionTest, BadReplyCodeOnRset) {
    SmtpServer server([](auto& session) {
        checkCommand("RSET", session.handleCommand("502 Rset"));
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
                });
            yield session->asyncRset([&coro](auto errc, auto resp) {
                ASSERT_EQ(errc, error::BadReplyCode);
                ASSERT_EQ(resp.replyCode, 502);
                coro();
            });
        }
    });
}

TEST_F(SmtpSessionTest, BadReplyCodeOnQuit) {
    SmtpServer server([](auto& session) {
        checkCommand("QUIT", session.handleCommand("502 Quit"));
    });

    Coro coro;
    runTest(coro, [this, &coro]() {
        reenter(coro) {
            yield session->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none,
                [&coro](auto errc) {
                    ASSERT_EQ(errc, error::Success);
                    coro();
                });
            yield session->asyncQuit([&coro](auto errc, auto resp) {
                ASSERT_EQ(errc, error::BadReplyCode);
                ASSERT_EQ(resp.replyCode, 502);
                coro();
            });
        }
    });
}
