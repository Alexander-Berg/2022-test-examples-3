#pragma once

#include <crypta/lib/proto/user_data/user_data.pb.h>
#include <crypta/lib/proto/user_data/user_data_stats.pb.h>

namespace NCrypta::NLookalike {
    NLab::TUserDataStats PrepareTestSegmentData();
    NLab::TUserData PrepareTestUserData();
}
