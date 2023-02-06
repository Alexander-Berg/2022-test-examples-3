#pragma once

#include <crypta/rt_socdem/lib/cpp/model/socdem_model.h>

#include <yabs/proto/user_profile.pb.h>

namespace NCrypta::NRtSocdem::NBigb::NTestUtils {
    const TSocdemModel& GetTestModel();

    NBSYeti::TProfile PrepareTestProfile();

    NBSYeti::TProfile ConvertProfile(const yabs::proto::Profile& profile);

}
