#include <collector_ng/collector_service.h>
#include <db/query_dispatcher/query_dispatcher.h>
#include <yplatform/repository.h>

#include <gtest.h>

using namespace yrpopper::collector;
using namespace yrpopper;

inline rpop_context_ptr createContext(int popid = 0)
{
    auto t = boost::make_shared<task>();
    t->popid = popid;
    t->use_imap = true;
    auto ctx =
        boost::make_shared<rpop_context>(t, "", false, false, std::make_shared<promise_void_t>());
    return ctx;
}

class CollectorServiceTest : public ::testing::Test
{
protected:
    CollectorService collector_service_;

    CollectorServiceTest()
    {
        auto repo = yplatform::repository::instance_ptr();
        repo->add_service<db::query_dispatcher>(
            "query_dispatcher", std::make_shared<db::query_dispatcher>(db::settings{}));
    }
};

TEST_F(CollectorServiceTest, create_collector_with_context)
{
    auto ctx = createContext();
    auto collector = collector_service_.getCollector(ctx);
    EXPECT_EQ(collector->getContext(), ctx);
}

TEST_F(CollectorServiceTest, update_context_on_new_iteration)
{
    auto popid = 1;
    auto old_ctx = createContext(popid);
    auto old_collector_ptr = collector_service_.getCollector(old_ctx);
    auto new_ctx = createContext(popid);
    auto new_collector_ptr = collector_service_.getCollector(new_ctx);

    EXPECT_EQ(old_collector_ptr, new_collector_ptr);
    EXPECT_NE(old_ctx, new_ctx);
    EXPECT_EQ(new_collector_ptr->getContext(), new_ctx);
}
