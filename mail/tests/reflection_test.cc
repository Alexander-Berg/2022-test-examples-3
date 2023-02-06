#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mailbox_oper/reflection.h>
#include <yamail/data/serialization/json_writer.h>
#include <yamail/data/deserialization/json_reader.h>

namespace {

using namespace mbox_oper;
using namespace testing;
using namespace yamail::data::serialization;
using namespace yamail::data::deserialization;

TEST(MidsSourceReflectionTest, serialize_directSource) {
    MidsSourcePtr source = std::make_shared<DirectMidsSource>(Mids{ "12", "34" });
    const auto actual = JsonWriter<MidsSourceData>(source->getData()).result();
    const auto expected = R"json({"mids":["12","34"],"tids":[],"fid":"","lid":"","age":{"days":0},)json"
                          R"json("subject":"","from":"","tabName":"","fromMid":"","limit":""})json";
    EXPECT_EQ(actual, expected);
}

TEST(MidsSourceReflectionTest, deserialize_directSource) {
    const auto json = R"json({"mids":["12","34"],"tids":[],"fid":"","lid":"","age":{"days":0},)json"
                      R"json("subject":"","from":"","tabName":"","fromMid":"","limit":""})json";
    const auto actual = JsonReader<MidsSourceData>(json).result();
    const auto expected = std::make_shared<DirectMidsSource>(Mids{ "12", "34" });
    EXPECT_EQ(actual, expected->getData());
}

TEST(MidsSourceReflectionTest, serialize_tidsSource) {
    MidsSourcePtr source = std::make_shared<TidsSource>(Tids{ "56", "78" });
    const auto actual = JsonWriter<MidsSourceData>(source->getData()).result();
    const auto expected = R"json({"mids":[],"tids":["56","78"],"fid":"","lid":"","age":{"days":0},)json"
                          R"json("subject":"","from":"","tabName":"","fromMid":"","limit":""})json";
    EXPECT_EQ(actual, expected);
}

TEST(MidsSourceReflectionTest, deserialize_tidsSource) {
    const auto json = R"json({"mids":[],"tids":["56","78"],"fid":"","lid":"","age":{"days":0},)json"
                      R"json("subject":"","from":"","tabName":"","fromMid":"","limit":""})json";
    const auto actual = JsonReader<MidsSourceData>(json).result();
    const auto expected = std::make_shared<TidsSource>(Tids{ "56", "78" });
    EXPECT_EQ(actual, expected->getData());
}

const FidFilter filter(10u, "hello", "hello@yandex.ru");

TEST(MidsSourceReflectionTest, serialize_fidSource) {
    MidsSourcePtr source = std::make_shared<FidSource>(Fid("123"), filter);
    const auto actual = JsonWriter<MidsSourceData>(source->getData()).result();
    const auto expected = R"json({"mids":[],"tids":[],"fid":"123","lid":"","age":{"days":10},)json"
                          R"json("subject":"hello","from":"hello@yandex.ru","tabName":"","fromMid":"","limit":""})json";
    EXPECT_EQ(actual, expected);
}

TEST(MidsSourceReflectionTest, deserialize_fidSource) {
    const auto json = R"json({"mids":[],"tids":[],"fid":"123","lid":"","age":{"days":10},)json"
                      R"json("subject":"hello","from":"hello@yandex.ru","tabName":"","fromMid":"","limit":""})json";
    const auto actual = JsonReader<MidsSourceData>(json).result();
    const auto expected = std::make_shared<FidSource>(Fid("123"), filter);
    EXPECT_EQ(actual, expected->getData());
}

TEST(MidsSourceReflectionTest, serialize_pagedFidSource) {
    MidsSourcePtr source = std::make_shared<FidSource>(Fid("123"), filter);
    MidsSourcePtr pagedSource = source->paginate("100", 42u);
    const auto actual = JsonWriter<MidsSourceData>(pagedSource->getData()).result();
    const auto expected = R"json({"mids":[],"tids":[],"fid":"123","lid":"","age":{"days":10},)json"
                          R"json("subject":"hello","from":"hello@yandex.ru","tabName":"","fromMid":"100","limit":"42"})json";
    EXPECT_EQ(actual, expected);
}

TEST(MidsSourceReflectionTest, deserialize_pagedFidSource) {
    const auto json = R"json({"mids":[],"tids":[],"fid":"123","lid":"","age":{"days":10},)json"
                      R"json("subject":"hello","from":"hello@yandex.ru","tabName":"","fromMid":"100","limit":"42"})json";
    const auto actual = JsonReader<MidsSourceData>(json).result();
    const auto expected = std::make_shared<FidSource>(Fid("123"), filter)->paginate("100", 42u);
    EXPECT_EQ(actual, expected->getData());
}

TEST(MidsSourceReflectionTest, serialize_lidSource) {
    MidsSourcePtr source = std::make_shared<LidSource>(Lid("123"));
    const auto actual = JsonWriter<MidsSourceData>(source->getData()).result();
    const auto expected = R"json({"mids":[],"tids":[],"fid":"","lid":"123","age":{"days":0},)json"
                          R"json("subject":"","from":"","tabName":"","fromMid":"","limit":""})json";
    EXPECT_EQ(actual, expected);
}

TEST(MidsSourceReflectionTest, deserialize_lidSource) {
    const auto json = R"json({"mids":[],"tids":[],"fid":"","lid":"123","age":{"days":0},)json"
                      R"json("subject":"","from":"","tabName":"","fromMid":"","limit":""})json";
    const auto actual = JsonReader<MidsSourceData>(json).result();
    const auto expected = std::make_shared<LidSource>(Lid("123"));
    EXPECT_EQ(actual, expected->getData());
}

TEST(MidsSourceReflectionTest, serialize_tabSource) {
    MidsSourcePtr source = std::make_shared<TabSource>("news");
    const auto actual = JsonWriter<MidsSourceData>(source->getData()).result();
    const auto expected = R"json({"mids":[],"tids":[],"fid":"","lid":"","age":{"days":0},)json"
                          R"json("subject":"","from":"","tabName":"news","fromMid":"","limit":""})json";
    EXPECT_EQ(actual, expected);
}

TEST(MidsSourceReflectionTest, deserialize_tabSource) {
    const auto json = R"json({"mids":[],"tids":[],"fid":"","lid":"","age":{"days":0},)json"
                      R"json("subject":"","from":"","tabName":"news","fromMid":"","limit":""})json";
    const auto actual = JsonReader<MidsSourceData>(json).result();
    const auto expected = std::make_shared<TabSource>("news");
    EXPECT_EQ(actual, expected->getData());
}

} // namespace
