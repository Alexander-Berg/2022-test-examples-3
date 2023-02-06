#pragma once

#include <boost/log/utility/setup.hpp>
#include <yplatform/log.h>

inline void init_log()
{
    boost::log::add_console_log();
}
