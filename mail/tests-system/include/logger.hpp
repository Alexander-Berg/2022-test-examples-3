#pragma once

#include <logdog/format/tskv.h>
#include <logdog/logger.h>
#include <logdog/backend/yplatform_log.h>

namespace apq_tester::logging {

constexpr static auto ymsettings_formatter =
    logdog::tskv::make_formatter(BOOST_HANA_STRING("apq_tester"));

inline auto makeLogger()
{
    return logdog::make_log(
        ymsettings_formatter, yplatform::log::source{ YGLOBAL_LOG_SERVICE, "apq_tester" });
}

}

#define APQ_TESTER_LOG_ERROR(...) LOGDOG_(::apq_tester::logging::makeLogger(), error, __VA_ARGS__)

#define APQ_TESTER_LOG_WARNING(...)                                                                \
    LOGDOG_(::apq_tester::logging::makeLogger(), warning, __VA_ARGS__)

#define APQ_TESTER_LOG_DEBUG(...) LOGDOG_(::apq_tester::logging::makeLogger(), debug, __VA_ARGS__)
