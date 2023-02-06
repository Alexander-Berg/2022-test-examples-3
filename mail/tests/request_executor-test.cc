#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "printers.h"
#include <pgg/factory.h>
#include <pgg/request_executor.h>
#include <pgg/query/ids.h>
#include <pgg/hooks/wrap.h>

#include <boost/asio/spawn.hpp>

using namespace testing;
using namespace pgg;
using namespace std::string_literals;
namespace io = io_result;

using OptStr = boost::optional<std::string>;

namespace reflection {

struct Revision {
    std::string revision = "";
    OptStr optValue = boost::none;

    Revision() = default;
    Revision(std::string revision)
        : revision(std::move(revision))
    {}
    Revision(std::string revision, OptStr optValue)
        : revision(std::move(revision)),
          optValue(std::move(optValue))
    {}

    bool operator== (const Revision& that) const noexcept {
        return (revision == that.revision) && (optValue == that.optValue);
    }

    bool operator< (const Revision& that) const noexcept {
        return revision < that.revision;
    }
};

} // namespace reflection

BOOST_FUSION_ADAPT_STRUCT(reflection::Revision,
    revision,
    optValue
)

PGG_QUERY_ID((test), SomeId, std::uint64_t)

namespace test {

struct TestQuery : pgg::query::QueryImpl<TestQuery,
    SomeId> {
    using Inherited::Inherited;
};

} // namespace test

namespace {

struct Row {
    std::unordered_map<std::string, std::string> data;

    bool has_column(const std::string& name) const {
        return data.count(name) > 0;
    }

    template <typename T>
    void at(const std::string& name, T& value) const {
        const auto it = data.find(name);
        value = (it == data.end()) ? T() : it->second;
    }
};

using FakeTable = std::vector<Row>;

struct FakeCursor {
    using Handler = std::function<void(error_code, FakeCursor)>;
    struct Context;
    using ContextPtr = std::shared_ptr<Context>;

    struct Context : std::enable_shared_from_this<Context> {
        Context(FakeTable table, Handler h)
            : table(std::move(table)),
              iter(this->table.begin()),
              end(this->table.end()),
              h(std::move(h))
        {}

        auto continuation() {
            struct Continuation {
                ContextPtr ctx;
                void resume() {
                    std::advance(ctx->iter, 1);
                    if (ctx->iter == ctx->end) {
                        ctx->h(error_code(), FakeCursor());
                    } else {
                        ctx->h(error_code(), FakeCursor(ctx));
                    }
                }
            };
            return Continuation{ shared_from_this() };
        }

        FakeTable table;
        FakeTable::iterator iter;
        FakeTable::iterator end;
        Handler h;
    };

    FakeCursor() = default;
    FakeCursor(FakeTable table, std::function<void(error_code, FakeCursor)> hook)
        : ctx() {
        if (!table.empty()) {
            ctx = std::make_shared<Context>(std::move(table), std::move(hook));
        }
    }
    FakeCursor(ContextPtr ctx)
        : ctx(std::move(ctx))
    {}

    ContextPtr get() const {
        return ctx;
    }

    ContextPtr ctx;
};

inline auto row(FakeCursor::Context& ctx) {
    return ctx.iter;
}

inline auto continuation(FakeCursor::Context& ctx) {
    return ctx.continuation();
}

struct DatabaseMock {
    using FakeDataRange = decltype(boost::make_iterator_range(std::declval<FakeTable>().begin(),
                                                              std::declval<FakeTable>().end()));

    using RequestHandler = std::function<void(error_code, FakeCursor)>;
    using FetchHandler = std::function<void(error_code, FakeDataRange)>;
    using UpdateHandler = std::function<void(error_code, int)>;
    using ExecuteHandler = std::function<void(error_code)>;

    MOCK_METHOD(void, request, (const query::Query&, RequestHandler), (const));
    MOCK_METHOD(void, fetch, (const query::Query&, FetchHandler), (const));
    MOCK_METHOD(void, update, (const query::Query&, UpdateHandler), (const));
    MOCK_METHOD(void, execute, (const query::Query&, ExecuteHandler), (const));
};

struct DatabaseGeneratorMock {
    DatabaseMock* dbMock;

    DatabaseMock* operator()() const {
        return dbMock;
    }
};

static const std::string queryConfText =
"sql TestQuery;"
"query \""
"   SELECT"
"       id"
"   FROM"
"       table t"
"   WHERE"
"       t.someId = $SomeId"
"\";";

using TestRequestExecutor = BasicRequestExecutor<DatabaseGeneratorMock>;
using TestRequestExecutorPtr = std::unique_ptr<TestRequestExecutor>;

struct RequestExecutorTest : Test {
    RequestExecutorTest() {
        query::Configuration cfg;
        cfg.loadFromSting(queryConfText);
        auto queryRepository = boost::make_shared<query::Repository>(
            cfg.queries(), cfg.parameters(), query::RegisterQueries<test::TestQuery>(),
            query::RegisterParameters<>());

        DatabaseGeneratorMock dbGeneratorMock{ &dbMock };
        executor = std::make_unique<TestRequestExecutor>(std::move(queryRepository),
                                                         std::move(dbGeneratorMock));
    }

    using R = reflection::Revision;

    static auto q() {
        return withQuery<test::TestQuery>(test::SomeId(0));
    }

    static auto forwardRequest(FakeTable& table) {
        return [&](const auto&, auto requestHandler) {
            FakeCursor cur{ table, requestHandler };
            requestHandler(error_code(), std::move(cur));
        };
    }

    static auto forwardFetch(FakeTable& table) {
        auto range = boost::make_iterator_range(table.begin(), table.end());
        return [range](const auto&, auto fetchHandler) {
            fetchHandler(error_code(), range);
        };
    }

    static auto forwardUpdate(const int value) {
        return [value](const auto&, auto updateHandler) {
            updateHandler(error_code(), value);
        };
    }

    static auto forwardExecute() {
        return [](const auto&, auto executeHandler) {
            executeHandler(error_code());
        };
    }

    static std::string getRevision(R r) {
        return r.revision;
    }

    static OptStr getOptValue(R r) {
        return r.optValue;
    }

    template <typename Handler>
    void runIO(Handler handle) {
        boost::asio::io_service ios;
        boost::asio::spawn(ios, [&](auto yield) {
            handle(io::make_yield_context(yield));
        });
        ios.run();
    }

    DatabaseMock dbMock;
    TestRequestExecutorPtr executor;
};

static FakeTable emptyTable = {};

static FakeTable oneElementTable = {
    Row {{
        { "revision"s, "str"s }
    }}
};

static FakeTable oneElementWithOptValueTable = {
    Row {{
        { "revision"s, "str"s },
        { "optValue"s, "val"s }
    }}
};

static FakeTable twoElementTable = {
    Row {{
        { "revision"s, "one"s }
    }},
    Row {{
        { "revision"s, "two"s }
    }}
};

static FakeTable twoElementWithOneOptValueTable = {
    Row {{
        { "revision"s, "one"s }
    }},
    Row {{
        { "revision"s, "two"s },
        { "optValue"s, "val"s }
    }}
};

// request

TEST_F(RequestExecutorTest, request_single_value_using_callback) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));

    R res;
    executor->request(q(), [&](error_code, R val) { res = val; });
    EXPECT_EQ(res, R{ "str" });
}

TEST_F(RequestExecutorTest, request_single_value_using_callback_with_converter) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));

    std::string res;
    executor->request(q(), [&](error_code, std::string val) { res = val; }, getRevision);
    EXPECT_EQ(res, "str");
}

TEST_F(RequestExecutorTest, request_not_null_nullable_value_using_callback) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementWithOptValueTable)));

    OptStr res;
    executor->request(q(), [&](error_code, OptStr val) { res = val; }, getOptValue);
    EXPECT_EQ(res, std::string("val"));
}

TEST_F(RequestExecutorTest, request_null_nullable_value_using_callback) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));

    OptStr res;
    executor->request(q(), [&](error_code, OptStr val) { res = val; }, getOptValue);
    EXPECT_FALSE(res);
}

TEST_F(RequestExecutorTest, request_existing_optional_value_using_callback) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));

    io::Optional<R> res;
    executor->request(q(), [&](error_code, io::Optional<R> val) { res = val; });
    EXPECT_EQ(res, R{ "str" });
}

TEST_F(RequestExecutorTest, request_existing_optional_value_using_callback_with_converter) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));

    io_result::Optional<std::string> res;
    executor->request(q(), [&](error_code, io::Optional<std::string> val) { res = val; }, getRevision);
    EXPECT_EQ(res, std::string("str"));
}

TEST_F(RequestExecutorTest, request_non_existing_optional_value_using_callback) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(emptyTable)));

    io::Optional<R> res;
    executor->request(q(), [&](error_code, io::Optional<R> val) { res = val; });
    EXPECT_FALSE(res);
}

TEST_F(RequestExecutorTest, request_non_existing_optional_value_using_callback_with_converter) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(emptyTable)));

    io_result::Optional<std::string> res;
    executor->request(q(), [&](error_code, io::Optional<std::string> val) { res = val; }, getRevision);
    EXPECT_FALSE(res);
}

TEST_F(RequestExecutorTest, request_existing_not_null_optional_and_nullable_value_using_callback) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementWithOptValueTable)));

    io::Optional<OptStr> res;
    executor->request(q(), [&](error_code, io::Optional<OptStr> val) { res = val; }, getOptValue);
    EXPECT_EQ(res.flatten(), std::string("val"));
}

TEST_F(RequestExecutorTest, request_existing_null_optional_and_nullable_value_using_callback) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));

    io::Optional<OptStr> res;
    executor->request(q(), [&](error_code, io::Optional<OptStr> val) { res = val; }, getOptValue);
    EXPECT_TRUE(res);
    EXPECT_FALSE(res.flatten());
}

TEST_F(RequestExecutorTest, request_not_existing_optional_and_nullable_value_using_callback) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(emptyTable)));

    io::Optional<OptStr> res;
    executor->request(q(), [&](error_code, io::Optional<OptStr> val) { res = val; }, getOptValue);
    EXPECT_FALSE(res);
}

TEST_F(RequestExecutorTest, request_sequence_using_callback) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(twoElementTable)));

    std::vector<R> res;
    executor->request(q(), [&](error_code, io::Cursor<R> cur) {
        if (cur) {
            res.push_back(cur.get());
        }
    });
    EXPECT_THAT(res, ElementsAre(R{ "one" }, R{ "two" }));
}

TEST_F(RequestExecutorTest, request_empty_sequence_using_callback) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(emptyTable)));

    std::vector<R> res;
    executor->request(q(), [&](error_code, io::Cursor<R> cur) {
        if (cur) {
            res.push_back(cur.get());
        }
    });
    EXPECT_THAT(res, IsEmpty());
}

TEST_F(RequestExecutorTest, request_sequence_using_callback_with_coverter) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(twoElementTable)));

    std::vector<std::string> res;
    executor->request(q(), [&](error_code, io::Cursor<std::string> cur) {
        if (cur) {
            res.push_back(cur.get());
        }
    }, getRevision);
    EXPECT_THAT(res, ElementsAre("one"s, "two"s));
}

TEST_F(RequestExecutorTest, request_sequence_of_nullable_values_using_callback) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(twoElementWithOneOptValueTable)));

    std::vector<OptStr> res;
    executor->request(q(), [&](error_code, io::Cursor<OptStr> cur) {
        if (cur) {
            res.push_back(cur.get());
        }
    }, getOptValue);
    EXPECT_THAT(res, ElementsAre(OptStr{}, OptStr{ "val" }));
}

TEST_F(RequestExecutorTest, request_container_using_callback) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(twoElementTable)));
    std::vector<R> res;
    executor->request(q(), [&](error_code, std::vector<R> vec) { res = std::move(vec); });
    EXPECT_THAT(res, ElementsAre(R{ "one" }, R{ "two" }));
}

TEST_F(RequestExecutorTest, request_container_using_callback_with_converter) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(twoElementTable)));
    std::vector<std::string> res;
    executor->request(q(), [&](error_code, std::vector<std::string> vec) { res = std::move(vec); }, getRevision);
    EXPECT_THAT(res, ElementsAre("one"s, "two"s));
}

TEST_F(RequestExecutorTest, request_with_raw_callback) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(emptyTable)));
    executor->request(q(), [](error_code, FakeCursor) {});
}

TEST_F(RequestExecutorTest, fetch_with_raw_callback) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(emptyTable)));
    executor->request(q(), [](error_code, DatabaseMock::FakeDataRange) {});
}

TEST_F(RequestExecutorTest, request_single_value_using_sync) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));
    EXPECT_EQ(executor->request<R>(q()), R{ "str" });
}

TEST_F(RequestExecutorTest, request_single_value_using_sync_with_converter) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));
    const auto res = executor->request<std::string>(q(), io::use_sync, getRevision);
    EXPECT_EQ(res, "str");
}

TEST_F(RequestExecutorTest, request_existing_optional_value_using_sync) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));
    const auto res = executor->request<io::Optional<R>>(q());
    EXPECT_EQ(res, R{ "str" });
}

TEST_F(RequestExecutorTest, request_existing_optional_value_using_sync_with_converter) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));
    const auto res = executor->request<io::Optional<std::string>>(q(), io::use_sync, getRevision);
    EXPECT_EQ(res, std::string("str"));
}

TEST_F(RequestExecutorTest, request_non_existing_optional_value_using_sync) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(emptyTable)));
    EXPECT_FALSE(executor->request<io::Optional<R>>(q()));
}

TEST_F(RequestExecutorTest, request_non_existing_optional_value_using_sync_with_converter) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(emptyTable)));
    const auto res = executor->request<io::Optional<std::string>>(q(), io::use_sync, getRevision);
    EXPECT_FALSE(res);
}

TEST_F(RequestExecutorTest, request_existing_not_null_optional_nullable_value_using_sync) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementWithOptValueTable)));
    const io::Optional<OptStr> res = executor->request<io::Optional<OptStr>>(q(), io::use_sync, getOptValue);
    EXPECT_EQ(res.flatten(), std::string("val"));
}

TEST_F(RequestExecutorTest, request_existing_null_optional_nullable_value_using_sync) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));
    const io::Optional<OptStr> res = executor->request<io::Optional<OptStr>>(q(), io::use_sync, getOptValue);
    EXPECT_TRUE(res);
    EXPECT_FALSE(res.flatten());
}

TEST_F(RequestExecutorTest, request_sequence_using_sync) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(twoElementTable)));
    const auto res = executor->request<io::Sequence<R>>(q());
    EXPECT_THAT(res, ElementsAre(R{ "one" }, R{ "two" }));
}

TEST_F(RequestExecutorTest, request_empty_sequence_using_sync) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(emptyTable)));
    EXPECT_THAT(executor->request<io::Sequence<R>>(q()), IsEmpty());
}

TEST_F(RequestExecutorTest, request_container_using_sync) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(twoElementTable)));
    const auto res = executor->request<std::vector<R>>(q());
    EXPECT_THAT(res, ElementsAre(R{ "one" }, R{ "two" }));
}

TEST_F(RequestExecutorTest, request_empty_container_using_sync) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(emptyTable)));
    const auto res = executor->request<std::vector<R>>(q());
    EXPECT_THAT(res, IsEmpty());
}

TEST_F(RequestExecutorTest, request_sequence_using_back_inserter) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(twoElementTable)));

    std::vector<R> res;
    executor->request(q(), std::back_inserter(res));
    EXPECT_THAT(res, ElementsAre(R{ "one" }, R{ "two" }));
}

TEST_F(RequestExecutorTest, request_sequence_using_back_inserter_with_converter) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(twoElementTable)));

    std::vector<std::string> res;
    executor->request(q(), std::back_inserter(res), getRevision);
    EXPECT_THAT(res, ElementsAre("one"s, "two"s));
}

TEST_F(RequestExecutorTest, request_sequence_of_nullable_values_using_back_inserter) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(twoElementWithOneOptValueTable)));

    std::vector<OptStr> res;
    executor->request(q(), std::back_inserter(res), getOptValue);
    EXPECT_THAT(res, ElementsAre(OptStr{}, OptStr{ "val" }));
}

TEST_F(RequestExecutorTest, request_empty_sequence_using_back_inserter) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(emptyTable)));

    std::vector<R> res;
    executor->request(q(), std::back_inserter(res));
    EXPECT_THAT(res, IsEmpty());
}

TEST_F(RequestExecutorTest, request_sequence_using_inserter) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(twoElementTable)));

    std::set<R> res;
    executor->request(q(), std::inserter(res, res.end()));
    EXPECT_THAT(res, UnorderedElementsAre(R{ "one" }, R{ "two" }));
}

TEST_F(RequestExecutorTest, request_sequence_using_inserter_with_converter) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(twoElementTable)));

    std::set<std::string> res;
    executor->request(q(), std::inserter(res, res.end()), getRevision);
    EXPECT_THAT(res, UnorderedElementsAre("one"s, "two"s));
}

TEST_F(RequestExecutorTest, request_sequence_of_nullable_values_using_inserter) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(twoElementWithOneOptValueTable)));

    std::set<OptStr> res;
    executor->request(q(), std::inserter(res, res.end()), getOptValue);
    EXPECT_THAT(res, UnorderedElementsAre(OptStr{}, OptStr{ "val" }));
}

TEST_F(RequestExecutorTest, request_empty_sequence_using_inserter) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(emptyTable)));

    std::set<R> res;
    executor->request(q(), std::inserter(res, res.end()));
    EXPECT_THAT(res, IsEmpty());
}

TEST_F(RequestExecutorTest, request_single_value_using_future) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));
    const auto res = executor->request<R>(q(), io::use_future).get();
    EXPECT_EQ(res, R{ "str" });
}

TEST_F(RequestExecutorTest, request_single_value_using_future_with_converter) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));
    const auto res = executor->request<std::string>(q(), io::use_future, getRevision).get();
    EXPECT_EQ(res, "str");
}

TEST_F(RequestExecutorTest, request_existing_optional_value_using_future) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));
    const auto res = executor->request<io::Optional<R>>(q(), io::use_future).get();
    EXPECT_EQ(res, R{ "str" });
}

TEST_F(RequestExecutorTest, request_non_existing_optional_value_using_future) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(emptyTable)));
    const auto res = executor->request<io::Optional<R>>(q(), io::use_future).get();
    EXPECT_FALSE(res);
}

TEST_F(RequestExecutorTest, request_not_null_nullable_value_using_future) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementWithOptValueTable)));
    const auto res = executor->request<OptStr>(q(), io::use_future, getOptValue).get();
    EXPECT_EQ(res, std::string("val"));
}

TEST_F(RequestExecutorTest, request_null_nullable_value_using_future) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));
    const auto res = executor->request<OptStr>(q(), io::use_future, getOptValue).get();
    EXPECT_FALSE(res);
}

TEST_F(RequestExecutorTest, request_single_value_using_coro) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));

    runIO([&](auto yield) {
        EXPECT_EQ(executor->request<R>(q(), yield), R{ "str" });
    });
}

TEST_F(RequestExecutorTest, request_single_value_using_coro_with_converter) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));

    runIO([&](auto yield) {
        const auto res = executor->request<std::string>(q(), yield, getRevision);
        EXPECT_EQ(res, "str");
    });
}

TEST_F(RequestExecutorTest, request_not_null_nullable_value_using_coro) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementWithOptValueTable)));

    runIO([&](auto yield) {
        const auto res = executor->request<OptStr>(q(), yield, getOptValue);
        EXPECT_EQ(res, std::string("val"));
    });
}

TEST_F(RequestExecutorTest, request_null_nullable_value_using_coro) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));

    runIO([&](auto yield) {
        const auto res = executor->request<OptStr>(q(), yield, getOptValue);
        EXPECT_FALSE(res);
    });
}

TEST_F(RequestExecutorTest, request_existing_optional_value_using_coro) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));

    runIO([&](auto yield) {
        const auto res = executor->request<io::Optional<R>>(q(), yield);
        EXPECT_EQ(res, R{ "str" });
    });
}

TEST_F(RequestExecutorTest, request_non_existing_optional_value_using_coro) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(emptyTable)));

    runIO([&](auto yield) {
        const auto res = executor->request<io::Optional<R>>(q(), yield);
        EXPECT_FALSE(res);
    });
}

TEST_F(RequestExecutorTest, request_existing_not_null_optional_nullable_value_using_coro) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementWithOptValueTable)));

    runIO([&](auto yield) {
        const io::Optional<OptStr> res = executor->request<io::Optional<OptStr>>(q(), yield, getOptValue);
        EXPECT_EQ(res.flatten(), std::string("val"));
    });
}

TEST_F(RequestExecutorTest, request_existing_null_optional_nullable_value_using_coro) {
    EXPECT_CALL(dbMock, fetch(_, _)).WillOnce(Invoke(forwardFetch(oneElementTable)));

    runIO([&](auto yield) {
        const io::Optional<OptStr> res = executor->request<io::Optional<OptStr>>(q(), yield, getOptValue);
        EXPECT_TRUE(res);
        EXPECT_FALSE(res.flatten());
    });
}

TEST_F(RequestExecutorTest, request_sequence_using_coro) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(twoElementTable)));

    runIO([&](auto yield) {
        const auto range = executor->request<io::Sequence<R>>(q(), yield);
        std::vector<R> res(range.begin(), range.end());
        EXPECT_THAT(res, ElementsAre(R{ "one" }, R{ "two" }));
    });
}

TEST_F(RequestExecutorTest, request_empty_sequence_using_coro) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(emptyTable)));

    runIO([&](auto yield) {
        const auto range = executor->request<io::Sequence<R>>(q(), yield);
        std::vector<R> res(range.begin(), range.end());
        EXPECT_THAT(res, IsEmpty());
    });
}

TEST_F(RequestExecutorTest, request_sequence_of_nullable_using_coro) {
    EXPECT_CALL(dbMock, request(_, _)).WillOnce(Invoke(forwardRequest(twoElementWithOneOptValueTable)));

    runIO([&](auto yield) {
        const auto range = executor->request<io::Sequence<OptStr>>(q(), yield, getOptValue);
        std::vector<OptStr> res(range.begin(), range.end());
        EXPECT_THAT(res, ElementsAre(OptStr{}, OptStr{ "val" }));
    });
}

// update

TEST_F(RequestExecutorTest, update_using_callback) {
    EXPECT_CALL(dbMock, update(_, _)).WillOnce(Invoke(forwardUpdate(1)));

    int res = 0;
    executor->update(q(), [&](auto, int val) { res = val; });
    EXPECT_EQ(res, 1);
}

TEST_F(RequestExecutorTest, update_using_sync) {
    EXPECT_CALL(dbMock, update(_, _)).WillOnce(Invoke(forwardUpdate(1)));
    EXPECT_EQ(executor->update(q()), 1);
}

TEST_F(RequestExecutorTest, update_using_future) {
    EXPECT_CALL(dbMock, update(_, _)).WillOnce(Invoke(forwardUpdate(1)));

    const auto res = executor->update(q(), io::use_future).get();
    EXPECT_EQ(res, 1);
}

TEST_F(RequestExecutorTest, update_using_coro) {
    EXPECT_CALL(dbMock, update(_, _)).WillOnce(Invoke(forwardUpdate(1)));

    runIO([&](auto yield) {
        EXPECT_EQ(executor->update(q(), yield), 1);
    });
}

// execute

TEST_F(RequestExecutorTest, execute_query_using_callback) {
    EXPECT_CALL(dbMock, execute(_, _)).WillOnce(Invoke(forwardExecute()));
    executor->execute(q(), [](auto) {});
}

TEST_F(RequestExecutorTest, execute_query_using_sync) {
    EXPECT_CALL(dbMock, execute(_, _)).WillOnce(Invoke(forwardExecute()));
    executor->execute(q());
}

TEST_F(RequestExecutorTest, execute_query_using_future) {
    EXPECT_CALL(dbMock, execute(_, _)).WillOnce(Invoke(forwardExecute()));
    executor->execute(q(), io::use_future).get();
}

TEST_F(RequestExecutorTest, execute_query_using_coro) {
    EXPECT_CALL(dbMock, execute(_, _)).WillOnce(Invoke(forwardExecute()));

    runIO([&](auto yield) {
        executor->execute(q(), yield);
    });
}

} // namespace
