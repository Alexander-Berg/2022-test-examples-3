#include <crypta/prism/lib/model/evaluate.h>
#include <crypta/prism/lib/model/test_utils/utils.h>

#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/hash.h>
#include <util/stream/file.h>

using namespace NCrypta::NPrism;

TEST(Evaluate, Get) {
    const auto& profile = NTestUtils::PrepareTestProfile();
    const auto& model = NTestUtils::GetTestModel();

    const auto& result = Evaluate(model, profile);
    EXPECT_TRUE(result.Defined());
    EXPECT_EQ(3, result->BigCluster);
    EXPECT_EQ(1368532u, result->Weight);
    EXPECT_EQ(72, result->SmallCluster);
}
