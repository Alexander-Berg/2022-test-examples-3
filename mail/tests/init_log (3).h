#pragma once

#include <yplatform/log.h>

inline void init_log()
{
    yplatform::log::init_global_log_console();
}
