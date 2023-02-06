#pragma once

#include <crypta/prism/lib/model/model.h>

#include <yabs/proto/user_profile.pb.h>

namespace NCrypta::NPrism::NTestUtils {
    void AssertFloatVectorsEqual(const TVector<float>& left, const TVector<float>& right);

    const TModel& GetTestModel();

    NBSYeti::TProfile PrepareTestProfile();

    NBSYeti::TProfile ConvertProfile(const yabs::proto::Profile& profile);
}
