#include <common/callback.h>
#include <backend/mbody/storage/raw_storage.h>
#include <gtest/gtest.h>

using namespace yimap;
using namespace yimap::mbody;

const string STID = "123";
const string MESSAGE = "Subject: Hello\r\n\r\nHello world!";

struct HTTPClientFake
{
    using Handler = std::function<void(boost::system::error_code, yhttp::response)>;

    void async_run(yplatform::task_context_ptr, yhttp::request, const Handler& handler)
    {
        ++requests_count;
        yhttp::response resp;
        resp.status = 200;
        resp.body = MESSAGE;
        handler({}, resp);
    }

    size_t requests_count = 0;
};

struct RateControllerFake
{
    using CompletionHandler = std::function<void()>;
    using Task = std::function<void(boost::system::error_code, CompletionHandler)>;

    void post(const Task& task)
    {
        task({}, [] {});
    }
};

struct CacheFake
{
    StringPtr get(const string& stid, size_t, size_t)
    {
        auto it = container.find(stid);
        if (it == container.end()) return {};
        return std::make_shared<string>(it->second);
    }

    void put(const string& stid, size_t, size_t, const string& message)
    {
        container[stid] = message;
    }

    std::map<string, string> container;
};

using RawStorageType = RawStorage<HTTPClientFake*, RateControllerFake*, CacheFake*>;

struct RawStorageTest : ::testing::Test
{
    boost::asio::io_service io;
    HTTPClientFake httpClient;
    RateControllerFake rateController;
    CacheFake cache;
    std::shared_ptr<RawStorageType> storage;

    RawStorageTest()
    {
        auto ctx = boost::make_shared<ImapContext>(io);
        auto settings = std::make_shared<const StorageSettings>();
        storage = std::make_shared<RawStorageType>(
            io, settings, ctx, STID, &httpClient, &rateController, &cache);
    }

    std::tuple<string, string> get()
    {
        Callback<string, string> cb;
        storage->get(cb);
        run_io();
        return cb.args();
    }

    void run_io()
    {
        io.reset();
        io.run();
    }

    void putToCache(const string& stid, const string& message)
    {
        cache.container[stid] = message;
    }
};

TEST_F(RawStorageTest, loadMessage)
{
    auto [err, msg] = get();
    EXPECT_EQ(err, "");
    EXPECT_EQ(msg, MESSAGE);
}

TEST_F(RawStorageTest, putMessageToCache)
{
    get();
    ASSERT_EQ(cache.container.size(), 1);
    EXPECT_EQ(cache.container[STID], MESSAGE);
}

TEST_F(RawStorageTest, getMessageFromCache)
{
    putToCache(STID, MESSAGE);
    auto [err, msg] = get();
    EXPECT_EQ(err, "");
    EXPECT_EQ(msg, MESSAGE);
    EXPECT_EQ(httpClient.requests_count, 0);
}
