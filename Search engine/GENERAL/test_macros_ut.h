#pragma once

#if ASYNC_TESTS
#   include "async/test_macros_ut_impl.h"
#elif BALANCER_TESTS
#   include "balancer/test_macros_ut_impl.h"
#elif BALANCER_PROXY_TESTS
#   include "balancer/proxy/test_macros_ut_impl.h"
#elif COROUTINE_TESTS
#   include "coroutine/test_macros_ut_impl.h"
#elif FIBER_TESTS
#   include "fiber/test_macros_ut_impl.h"
#elif PHANTOM_TESTS
#   include "phantom/test_macros_ut_impl.h"
#elif ANT_TESTS
#   include "ant/test_macros_ut_impl.h"
#else
#   error "Y_SCATTER_UNIT_TEST_IMPL should be defined"
#endif

#define Y_SCATTER_UNIT_TEST(N) \
    template <typename TRunner, typename TTestTraits, typename ...Args> \
    void RunScatterTest##N(Args&& ...args); \
    Y_SCATTER_UNIT_TEST_IMPL(N) \
    template <typename TRunner, typename TTestTraits, typename ...Args> \
    void RunScatterTest##N(Args&& ...args)
