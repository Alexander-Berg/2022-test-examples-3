#pragma once

#include <src/services/db/events_queue/types/reflection/id_map_element.hpp>

namespace collie::services::db::events_queue {

static bool operator==(const IdMapElement& left, const IdMapElement& right) {
    return boost::fusion::operator==(left, right);
}

}
