#include <crypta/audience/lib/native/segment_priorities/builder.h>
#include <crypta/audience/lib/native/segment_priorities/reader.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/stream/file.h>

using namespace NAudience::NSegmentPriorities;

TEST(NSegmentPriorities, Proto) {
    TString filename("base.vinyl");
    static const TString key("bySegmentType");

    NAudience::TPrioritiesByType proto;
    auto& bySegmentType = (*proto.MutablePrioritiesByType())[key];
    (*bySegmentType.MutablePriorities())[100] = 200;
    bySegmentType.SetDefaultPriority(20);

    SaveToFile(BuildFromProto(proto), filename);
    auto newProto = ReadBaseToProto(TFileInput(filename).ReadAll());

    EXPECT_EQ(
        proto.GetPrioritiesByType().at(key).GetDefaultPriority(),
        newProto.GetPrioritiesByType().at(key).GetDefaultPriority()
    );
    EXPECT_EQ(
        proto.GetPrioritiesByType().at(key).GetPriorities().at(100),
        newProto.GetPrioritiesByType().at(key).GetPriorities().at(100)
    );
}
