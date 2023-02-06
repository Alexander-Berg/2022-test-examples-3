#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/service_control/command_source_impl.h>
#include "log_mock.h"
#include <boost/range/adaptors.hpp>

using namespace testing;
using namespace doberman::service_control;
using namespace doberman::testing;
using namespace boost::asio;

static constexpr unsigned short testPort = 34015u;

class TestClient {
public:
    explicit TestClient(io_service& service)
        : socket(service),
          responseBuffer()
    {}

    void connect(yield_context yield, const ip::tcp& protocol) {
        const ip::tcp::endpoint endpoint(protocol, testPort);
        socket.async_connect(endpoint, yield);
    }

    void send(yield_context yield, const std::string& message) {
        socket.async_send(buffer(message), yield);
    }

    std::string readResponse(yield_context yield) {
        async_read_until(socket, responseBuffer, '\n', yield);
        std::string response;
        std::istream stream(&responseBuffer);
        std::getline(stream, response);
        return response;
    }

private:
    ip::tcp::socket socket;
    streambuf responseBuffer;
};

struct CommandSourceImplTest : TestWithParam<ip::tcp> {
    io_service service;
    RunStatus runStatus;
    LogMock logMock;
    const ip::tcp::endpoint endpoint{ GetParam(), testPort };

    auto createLog() {
        return doberman::make_log(logdog::none, &logMock);
    }

    template <typename Log>
    auto createCommandSource(Log& log) {
        return makeCommandSourceImpl(service, runStatus, log, endpoint);
    }

    void run() {
        service.run();
    }
};

TEST_P(CommandSourceImplTest, readingCommandsTest) {
    auto log = createLog();
    auto cmdSource = createCommandSource(log);

    using boost::make_optional;
    std::map<std::string, OptCommandType> commands = {
        { "reopen_log\n",  make_optional(CommandType::reopenLog) },
        { "stop\n",        make_optional(CommandType::stop)      },
        { "bad command\n", make_optional(CommandType::invalid)   },
        { "\n",            make_optional(CommandType::invalid)   }
    };

    std::vector<OptCommandType> receivedCommands;
    boost::asio::spawn(service, [&](auto yield) {
        for (size_t i = 0; i < commands.size(); i++) {
            receivedCommands.push_back(cmdSource.getCommand(yield));
        }
    });

    TestClient client(service);
    boost::asio::spawn(service, [&](auto yield) {
        client.connect(yield, this->GetParam());
        for (const auto& command : commands) {
            client.send(yield, command.first);
        }
    });

    run();

    std::vector<OptCommandType> expectedCommands;
    boost::copy(commands | boost::adaptors::map_values, std::back_inserter(expectedCommands));
    EXPECT_EQ(receivedCommands, expectedCommands);
}

TEST_P(CommandSourceImplTest, sendingOkResponseTest) {
    auto log = createLog();
    auto cmdSource = createCommandSource(log);

    boost::asio::spawn(service, [&](auto yield) {
        cmdSource.getCommand(yield);
        cmdSource.sendOkResponse(yield);
    });

    TestClient client(service);
    std::string response;

    boost::asio::spawn(service, [&](auto yield) {
        client.connect(yield, this->GetParam());
        client.send(yield, "\n");
        response = client.readResponse(yield);
    });

    run();

    EXPECT_EQ(response, "Ok");
}

TEST_P(CommandSourceImplTest, sendingErrorResponsesTest) {
    auto log = createLog();
    auto cmdSource = createCommandSource(log);

    std::map<std::string, std::string> responses = {
        { "Response without newline", "Response without newline" },
        { "Response with newline\n",  "Response with newline"    },
        { "",                         ""                         }
    };

    boost::asio::spawn(service, [&](auto yield) {
        cmdSource.getCommand(yield);
        for (const auto& response : responses) {
            cmdSource.sendErrorResponse(yield, response.first);
        }
    });

    TestClient client(service);
    std::vector<std::string> receivedResponses;

    boost::asio::spawn(service, [&](auto yield) {
        client.connect(yield, this->GetParam());
        client.send(yield, "\n");

        for (size_t i = 0; i < responses.size(); i++) {
            receivedResponses.push_back(client.readResponse(yield));
        }
    });

    run();

    std::vector<std::string> expectedResponses;
    boost::copy(responses | boost::adaptors::map_values, std::back_inserter(expectedResponses));
    EXPECT_EQ(receivedResponses, expectedResponses);
}

INSTANTIATE_TEST_SUITE_P(,
                        CommandSourceImplTest,
                        Values(ip::tcp::v4(), ip::tcp::v6()));
