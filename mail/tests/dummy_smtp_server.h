#pragma once

#include <boost/asio.hpp>

#include <iostream>
#include <thread>
#include <future>

namespace testing {
namespace server {

using boost::asio::ip::tcp;
using boost::asio::buffer;
using boost::asio::write;
using boost::asio::read_until;
using boost::asio::streambuf;

const std::string SMTP_HOST =  "127.0.0.1";
const uint16_t SMTP_PORT =  2525;

struct Session {
    explicit Session(tcp::socket sock)
        : sock(std::move(sock)) {}

    auto readLine() {
        read_until(sock, readBuf, "\r\n");
        std::istream stream(&readBuf);
        std::string line;
        std::getline(stream, line);
        if (!line.empty() && line.back() == '\r') {
            line.pop_back();
        }
        return line;
    }

    void writeLine(std::string buf) {
        write(sock, buffer(buf + "\r\n"));
    }

    auto readMessage() {
        std::string msg;
        while (true) {
            std::string line = readLine();
            if (line == ".") {
                break;
            }
            msg += line + "\r\n";
        }
        return msg;
    }

    auto handleCommand(std::string resp) {
        auto line = readLine();
        writeLine(resp);
        return line;
    }

    auto handleMessage(std::string resp) {
        auto msg = readMessage();
        writeLine(resp);
        return msg;
    }

    tcp::socket sock;
    streambuf readBuf;
};


class SmtpServer {
public:
    template <class ConnectHandler>
    explicit SmtpServer(ConnectHandler connectHandler)
        : ioService()
        , acceptor(ioService)
        , thread()
    {
        listen();
        startSessionThread(connectHandler);
    }

    template <class ConnectHandler>
    SmtpServer(uint16_t smtpPort, ConnectHandler connectHandler)
        : ioService()
        , acceptor(ioService)
        , thread()
        , smtpPort(smtpPort)
    {
        listen();
        startSessionThread(connectHandler);
    }

    ~SmtpServer() {
        if (thread.joinable()) {
            thread.join();
        }
    }

private:
    boost::asio::io_service ioService;
    tcp::acceptor acceptor;
    std::thread thread;
    uint16_t smtpPort = SMTP_PORT;

    template <typename Handler>
    void startSessionThread(Handler&& handler) {
        std::promise<void> promise;
        auto isReady = promise.get_future();
        thread = std::thread([this, &promise, handler]() mutable {
            auto p = std::move(promise);
            p.set_value();
            tcp::socket sock(ioService);
            acceptor.accept(sock);
            Session session(std::move(sock));
            handler(session);
        });
        isReady.wait();
    }

    void listen() {
        tcp::endpoint endpoint(tcp::v4(), smtpPort);
        acceptor.open(endpoint.protocol());
        acceptor.set_option(tcp::acceptor::reuse_address(true));
        acceptor.bind(endpoint);
        acceptor.listen();
    }
};

}  // namespace server
}  // namespace testing
