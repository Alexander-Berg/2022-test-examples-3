#include <signal.h>
#include <ymod_smtpclient/call.h>
#include <yplatform/module.h>
#include <yplatform/reactor.h>
#include <yplatform/find.h>
#include <yplatform/time_traits.h>
#include <yplatform/ptree.h>

#include <boost/algorithm/string/predicate.hpp>

namespace ymod_smtpclient_test {

using namespace ymod_smtpclient;

constexpr std::uint32_t KB = 1024;
constexpr std::uint32_t MB = KB * 1024;

namespace p = std::placeholders;

class StressClient: public yplatform::module {
public:
    void init (const yplatform::ptree& pt) {
        msgSize = pt.get("message_size", msgSize);
        maxParallelRequest = pt.get("max_parallel_request", maxParallelRequest);
        totalRequestCount = pt.get("total_request_count", totalRequestCount);
        logDebug = pt.get("log_debug", logDebug);

        auto authNode = pt.get_child_optional("auth");
        if (authNode) {
            auto mech = authNode->get("mechanism", "NONE");
            auto login = authNode->get<std::string>("login");
            auto password = authNode->get<std::string>("password");
            if (boost::iequals(mech, "LOGIN")) {
                auth = AuthData::LOGIN(login, password);
            } else if (boost::iequals(mech, "PLAIN")) {
                auth = AuthData::PLAIN(login, password);
            } else if (boost::iequals(mech, "XOAUTH2")) {
                auth = AuthData::XOAUTH2(login, password);
            } else {
                auth = AuthData::BEST(login, password);
            }
        }

        address = SmtpPoint::fromString(pt.get<std::string>("address"));
        mailfrom = pt.get("mailfrom", mailfrom);
        auto rcptsPt = pt.equal_range("rcpts");
        for (auto it = rcptsPt.first; it != rcptsPt.second; ++it) {
            rcpts.push_back(it->second.data());
        }

        reactor = yplatform::find_reactor(pt.get<std::string>("reactor"));
    }

    void start () {
        impl = yplatform::find<ymod_smtpclient::Call>("smtp_client");
        reactor->io()->post([this] { runAll(); });
    }

    void stop () {
        stopped = true;
    }
private:
    void runAll() {
        YLOG_G(info) << "Test started";
        startedAt = yplatform::time_traits::clock::now();
        currentRequestIndx = 0;
        finishedRequestCount = 0;
        stopped = false;
        for (int i = 0; i < maxParallelRequest; ++i) {
            runOne();
        }
    }

    bool runOne() {
        if (stopped) { return false; }
        std::uint32_t indx = currentRequestIndx++;
        if (indx >= totalRequestCount) { return false; }
        auto ctx = boost::make_shared<yplatform::task_context>(
            "smtp_stress_test_" + std::to_string(indx));
        auto req = makeRequest();
        auto ptr = this->shared_from_this();
        impl->asyncRun(ctx, std::move(req),
            [this, ptr, indx](error::Code errc, Response) {
                if (logDebug || errc) {
                    YLOG_G(info) << "req: " << indx << "', errc: '" << error::message(errc) << "'";
                }
                std::uint32_t count = ++finishedRequestCount;
                handleRequest(count);
        });
        return true;
    }

    void handleRequest(std::uint32_t count) {
        if (count < totalRequestCount) {
            runOne();
        } else if (count == totalRequestCount) {
            auto finishedAt = yplatform::time_traits::clock::now();
            yplatform::time_traits::float_seconds totalTime = finishedAt - startedAt;
            YLOG_G(info) << "Test finished: "
                << " total_requests=" << totalRequestCount
                << ", parallel_requests=" << maxParallelRequest
                << ", duration=" << totalTime.count() << " sec"
                << ", RPS=" << std::uint32_t(1.0 * totalRequestCount / totalTime.count());
            reactor->io()->post([] { kill(getpid(), SIGINT); });
        }
    }

    Request makeRequest() const {
        RequestBuilder builder;
        builder.address(address);
        if (auth) {
            builder.auth(*auth);
        }
        builder.mailfrom(MailFrom(mailfrom)).addRcpts(rcpts.begin(), rcpts.end());
        std::string headers = "From: from@ya.ru\r\nTo: to@ya.ru\r\n";
        std::string body = headers += '\n';
        std::string line(lineSize, 'x');
        while (body.length() < msgSize) {
            body += line;
            body += '\n';
        }
        builder.message(std::move(body));
        return builder.release();
    }

private:
    std::uint32_t msgSize = MB;
    std::uint32_t lineSize = 100;
    unsigned short maxParallelRequest = 4;
    std::uint32_t totalRequestCount = 100;
    bool logDebug = false;

    SmtpPoint address;
    std::string mailfrom;
    std::vector<std::string> rcpts;
    boost::optional<AuthData> auth;

    std::atomic_bool stopped = {false};
    boost::shared_ptr<ymod_smtpclient::Call> impl;

    std::atomic_uint currentRequestIndx = {0};
    std::atomic_uint finishedRequestCount = {0};
    yplatform::time_traits::time_point startedAt;
    yplatform::reactor_ptr reactor;
};

}   // namespace ymod_smtpclient_test

#include <yplatform/module_registration.h>
DEFINE_SERVICE_OBJECT(ymod_smtpclient_test::StressClient)
