#pragma once

#include <ozo/pg/types/jsonb.h>

namespace ozo::pg {

static bool operator==(const jsonb& left, const jsonb& right) {
    return left.raw_string() == right.raw_string();
}

}
