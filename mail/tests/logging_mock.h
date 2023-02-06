#ifndef MAIL_GETTER_TESTS_LOGGING_MOCK_H
#define MAIL_GETTER_TESTS_LOGGING_MOCK_H

#include <memory>
#include <string>
#include <mail_getter/logging.h>

namespace mail_getter {
namespace logging {

class LogMock : public Log {
public:
    MOCK_METHOD(void, warning, (const ServiceName&, const Message&), (override));
    MOCK_METHOD(void, error, (const ServiceName&, const Message&), (override));
    MOCK_METHOD(void, notice, (const ServiceName&, const Message&), (override));
    MOCK_METHOD(void, debug, (const ServiceName&, const Message&), (override));
};

using LogMockPtr = std::shared_ptr<LogMock>;

} // namespace logging
} // namesopace mail_getter

#endif // MAIL_GETTER_TESTS_LOGGING_MOCK_H
