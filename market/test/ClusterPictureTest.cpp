#include <market/library/pictures/pictures.h>

#include <library/cpp/testing/unittest/gtest.h>

using namespace Market::Pictures;

TEST(ClusterPictureTest, findMainClusterThumb) {
    TString result;
    // small size and good proportion
    TVector<ThumbInfo> thumbs = {{Size(10, 20), Size(190, 249)}, {Size(10, 20), Size(189, 250)}};
    result = findMainClusterThumb("1", thumbs);
    EXPECT_EQ("", result);

    // incorrect proportion
    thumbs = TVector<ThumbInfo>({{Size(10, 20), Size(250, 350)}, {Size(10, 20), Size(400, 500)}});
    result = findMainClusterThumb("1", thumbs);
    EXPECT_EQ("", result);

    // 300 x 400
    thumbs = TVector<ThumbInfo>({{Size(10, 20), Size(151, 200)}, {Size(10, 20), Size(300, 400)}});
    result = findMainClusterThumb("3", thumbs);
    EXPECT_EQ("3", result);

    // 240 x 320
    thumbs = TVector<ThumbInfo>({{Size(10, 20), Size(151, 200)}, {Size(10, 20), Size(240, 320)}});
    result = findMainClusterThumb("4", thumbs);
    EXPECT_EQ("4", result);

    // 190x250
    thumbs = TVector<ThumbInfo>({{Size(10, 20), Size(151, 200)}, {Size(10, 20), Size(190, 250)}});
    result = findMainClusterThumb("5", thumbs);
    EXPECT_EQ("5", result);
}
