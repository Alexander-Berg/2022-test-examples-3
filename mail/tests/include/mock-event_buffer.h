#include <gmock/gmock.h>
#include <mail/alabay/ymod_logbroker/include/consumer/event_buffer.h>
#include <mail/alabay/ymod_logbroker/include/consumer/event_buffer_proxy.h>

namespace {

using namespace ymod_logbroker;
using namespace ::testing;

struct MockEventBuffer : public EventBuffer {
    MOCK_METHOD(std::size_t, size, (), (const, override));
    MOCK_METHOD(void, asyncAddEvents, (std::string_view raw, helpers::ParsedEvent parsed, yplatform::task_context_ptr ctx, OnProbablyFlush h), (override));
    MOCK_METHOD(void, asyncFlush, (yplatform::task_context_ptr ctx, OnFlush h), (override));
};

struct MockWithTimer : public WithTimer {

    MOCK_METHOD(time_point, now, (), (const, override));

    MockWithTimer(ymod_logbroker::EventBufferPtr ptr, duration flushInterval, time_point initTime)
        : WithTimer(ptr, flushInterval, initTime)
    { }
};

}
