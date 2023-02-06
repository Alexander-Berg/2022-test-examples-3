#include "../src/worker/xtasks_worker.h"
#include "../src/convey/gate.h"
#include <catch.hpp>

using namespace yxiva;
using namespace yxiva::hub;
using namespace yxiva::hub::worker;

struct convey_coro_fixture : public convey_coro
{
    convey_coro_fixture() : convey_coro(root, nullptr, job, sub), job(sub)
    {
    }

    coro_root root;
    xtask_context::job job;
    sub_t sub;
};

TEST_CASE_METHOD(convey_coro_fixture, "worker/gate_code_retry", "")
{
    std::vector<gate_result> retriable_gate_results = { gate_result::fail,
                                                        gate_result::rate_limit,
                                                        gate_result::push_service_fail,
                                                        gate_result::timeout,
                                                        gate_result::service_unavailable };
    std::vector<gate_result> not_retriable_gate_results = {
        gate_result::success,     gate_result::ignored,   gate_result::unsubscribe,
        gate_result::bad_request, gate_result::forbidden,
    };
    for (auto gate_code : retriable_gate_results)
    {
        CHECK(is_retriable_error(error_code_from_gate_result(gate_code)) == true);
    }
    for (auto gate_code : not_retriable_gate_results)
    {
        CHECK(is_retriable_error(error_code_from_gate_result(gate_code)) == false);
    }
}
