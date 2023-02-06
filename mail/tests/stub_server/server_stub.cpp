#include <ymod_smtpserver/server.h>
#include <ymod_smtpserver/session.h>
#include <ymod_smtpserver/commands.h>

#include <yplatform/module.h>
#include <yplatform/find.h>
#include <yplatform/time_traits.h>
#include <yplatform/ptree.h>
#include <yplatform/reactor.h>

#include <boost/variant/static_visitor.hpp>
#include <boost/algorithm/string/trim.hpp>

namespace ymod_smtpserver_stub {

std::string get_remote_ip_string(boost::asio::ip::address ip) {
    if (ip.is_v6()) {
        boost::asio::ip::address_v6 v6 = ip.to_v6();
        if (v6.is_v4_mapped() || v6.is_v4_compatible()) {
            boost::asio::ip::address_v4 v4 = v6.to_v4();
            ip = v4;
        }
    }
    return ip.to_string();
}

std::string format_params(const ymod_smtpserver::commands::Params& params) {
    std::ostringstream os;
    for (const auto& keyValue: params) {
        os << "{" << keyValue.first;
        if (!keyValue.second.empty()) {
            os << ", " << keyValue.second;
        }
        os << "} ";
    }
    return os.str();
}

class SessionStub
    : public ymod_smtpserver::Session
    , private boost::static_visitor<>
    , public std::enable_shared_from_this<SessionStub>
{
public:
    using ConnectionPtr = ymod_smtpserver::ConnectionPtr;

    explicit SessionStub(ConnectionPtr connection, boost::asio::io_service& ios)
        : Session(connection)
        , ios(ios)
    {}

    void start() {
        YLOG_G(info) << "start session, connect to ["
            << get_remote_ip_string(connection->remoteAddr()) << "]";
        writeRead(ymod_smtpserver::Response(220, boost::asio::ip::host_name() + " SMTP"));
    }

    template <typename C>
    void operator()(C) {
        writeRead(ymod_smtpserver::Response(502, ymod_smtpserver::EnhancedStatusCode(551)));
    }

    void operator()(const ymod_smtpserver::commands::Ehlo& ehlo) {
        YLOG_G(info) << "EHLO from '" << ehlo.name << "'";
        std::ostringstream respText;
        respText << boost::asio::ip::host_name() << "\n"
            << "8BITMIME\n" << "PIPELINING\n" << "ENHANCEDSTATUSCODES\n";
        writeRead(ymod_smtpserver::Response(250, respText.str()));
    }

    void operator()(const ymod_smtpserver::commands::Lhlo& lhlo) {
        YLOG_G(info) << "LHLO from '" << lhlo.name << "'";
        writeRead(ymod_smtpserver::Response(250, boost::asio::ip::host_name()));
    }

    void operator()(const ymod_smtpserver::commands::StartTls&) {
        YLOG_G(info) << "STARTTLS";
        auto self(shared_from_this());
        writeResponse(ymod_smtpserver::Response(250), [this, self](){
            connection->tlsHandshake(ios.wrap([this, self](boost::system::error_code ec) {
                if (ec) {
                    YLOG_G(error) << "tls handshake error: " << ec.message();
                    return connection->close();
                }
                readCommand();
            }));
        });
    }

    void operator()(const ymod_smtpserver::commands::Data&) {
        YLOG_G(info) << "DATA";
        auto self(shared_from_this());
        writeResponse(
            ymod_smtpserver::Response(354, "Start mail input, end with <CRLF>.<CRLF>"),
            [this, self]() { readMessage(); }
        );
    }

    void operator()(const ymod_smtpserver::commands::Quit&) {
        YLOG_G(info) << "QUIT";
        auto self(shared_from_this());
        writeResponse(ymod_smtpserver::Response(221, ymod_smtpserver::EnhancedStatusCode(200)),
            [this, self]() { connection->close(); }
        );
    }

    void operator()(const ymod_smtpserver::commands::Rset&) {
        YLOG_G(info) << "RSET";
        writeRead(ymod_smtpserver::Response(250, "Flushed", ymod_smtpserver::EnhancedStatusCode(215)));
    }

    void operator()(const ymod_smtpserver::commands::RcptTo& rcpt) {
        YLOG_G(info) << "recipient = '" << rcpt.addr << "', params = " << format_params(rcpt.params);
        writeRead(ymod_smtpserver::Response(250));
    }

    void operator()(const ymod_smtpserver::commands::MailFrom& mailfrom) {
        YLOG_G(info) << "mailfrom = '" << mailfrom.addr << "', params = " << format_params(mailfrom.params);
        writeRead(ymod_smtpserver::Response(250));
    }

    void operator()(const ymod_smtpserver::commands::Unknown& unknown) {
        YLOG_G(info) << "got unknown command = '" << boost::trim_copy(unknown.ctx) << "'";
        writeRead(ymod_smtpserver::Response(502, ymod_smtpserver::EnhancedStatusCode(551)));
    }

    void operator()(const ymod_smtpserver::commands::SyntaxError& syntaxError) {
        YLOG_G(info) << "syntax error = '" << boost::trim_copy(syntaxError.ctx) << "'";
        writeRead(ymod_smtpserver::Response(555, ymod_smtpserver::EnhancedStatusCode(552)));
    }

private:
    using Command = ymod_smtpserver::Command;

    void readCommand() {
        auto self(shared_from_this());
        connection->readCommand(
            ios.wrap([this, self](boost::system::error_code ec, Command cmd) mutable {
                if (ec) {
                    YLOG_G(error) << "read command error: " << ec.message();
                    return connection->close();
                }
                return boost::apply_visitor(*this, cmd);
        }));
    }

    void readMessage() {
        auto self(shared_from_this());
        connection->readMessage(
            ios.wrap([this, self](boost::system::error_code ec, std::shared_ptr<std::string> msg) {
                if (ec) {
                    YLOG_G(error) << "read message error: " << ec.message();
                    return connection->close();
                }
                YLOG_G(info) << "got message, size = " << msg->size();
                writeRead(ymod_smtpserver::Response(250, ymod_smtpserver::EnhancedStatusCode(200)));
        }));
    }

    void writeRead(ymod_smtpserver::Response response) {
        auto self(shared_from_this());
        writeResponse(std::move(response), [this, self]() { readCommand(); });
    }

    template <typename Handler>
    void writeResponse(ymod_smtpserver::Response response, Handler handler) {
        auto self(shared_from_this());
        connection->writeResponse(std::move(response),
            ios.wrap([this, self, handler] (boost::system::error_code ec, std::size_t) {
                if (ec) {
                    YLOG_G(error) << "write response error: " << ec.message();
                    return connection->close();
                }
                handler();
        }));
    }

private:
    boost::asio::io_service& ios;
};

class SessionFactory: public ymod_smtpserver::SessionFactory {
public:
    using SessionPtr = ymod_smtpserver::SessionPtr;
    using ConnectionPtr = ymod_smtpserver::ConnectionPtr;

    explicit SessionFactory(yplatform::reactor_ptr reactor): reactor(reactor) {}

    SessionPtr create(ConnectionPtr connection) override {
        return std::make_shared<SessionStub>(connection, *reactor->io());
    }

private:
    yplatform::reactor_ptr reactor;
};

class ServerStub: public yplatform::module {
public:
    void init(const yplatform::ptree& conf) {
        auto smtpServer = yplatform::find<ymod_smtpserver::Server>("smtp_server");
        auto reactor = yplatform::global_reactor_set->get(conf.get("reactor", "global"));
        smtpServer->setFactory(std::make_shared<SessionFactory>(reactor));
    }
};

}   // namespace ymod_smtpserver_stub

#include <yplatform/module_registration.h>
DEFINE_SERVICE_OBJECT(ymod_smtpserver_stub::ServerStub)
