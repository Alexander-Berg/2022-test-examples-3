#pragma once

#include <macs/io.h>

namespace york {
namespace server {
namespace handlers {

inline auto wrap(int) {
    return macs::io::use_sync;
}

}
}
}
