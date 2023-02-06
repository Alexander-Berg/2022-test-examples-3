#pragma once

#include <src/logger/logger.h>
#include <logdog/gmock/logger_mock.h>

namespace doberman::testing {

using namespace ::testing;

using LogMock = ::logdog::testing::logger_mock;

} // namespace doberman::testing
