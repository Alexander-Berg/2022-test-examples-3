#include <yxiva_mobile/push_task_context.h>
#include <catch.hpp>

using namespace yxiva::mobile;

template <typename Context>
void check_deadline_propagation()
{
    using boost::make_shared;
    static const auto timeout = yplatform::time_traits::seconds(10);

    auto ctx = make_shared<ymod_webserver::context>();
    ctx->deadline_from_now(timeout);
    auto req = make_shared<ymod_webserver::request>(ctx);
    auto push_ctx = make_shared<Context>(req);
    CHECK(push_ctx->deadline() == ctx->deadline());
}

TEST_CASE("push_task_context/deadline_propagation")
{
    check_deadline_propagation<push_task_context>();
    check_deadline_propagation<mobile_task_context>();
    check_deadline_propagation<batch_task_context>();
    check_deadline_propagation<webpush_task_context>();
}
