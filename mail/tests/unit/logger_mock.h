#pragma once

#include <internal/logger/logger.h>
#include <logdog/gmock/logger_mock.h>

namespace york::tests {

using namespace ::testing;

using LoggerMock = logdog::testing::logger_mock;

} // namespace york::tests
