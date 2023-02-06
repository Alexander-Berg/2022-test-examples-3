#include <map>
#include <boost/property_tree/json_parser.hpp>
#include <gtest/gtest.h>
#include <yamail/data/serialization/json_writer.h>
#include <yamail/data/deserialization/foreign_ptree.h>
#include <yamail/data/deserialization/ptree_reader.h>

typedef std::map<std::string,std::string> StringMap;

using namespace yamail::data::serialization;
using namespace yamail::data::deserialization;

struct StructWithMap {
    std::string title;
    StringMap dict;

    bool operator==(const StructWithMap& other) const {
        return title == other.title && dict == other.dict;
    }
};

BOOST_FUSION_ADAPT_STRUCT(StructWithMap,
    (std::string, title)
    (StringMap, dict)
)

static_assert(yamail::data::reflection::is_fusion_struct_v<StructWithMap>);

template <typename T>
class PtreeReaderTest : public ::testing::Test {
};

using Types = ::testing::Types<property_tree::Reader, foreign_ptree::Reader>;
TYPED_TEST_SUITE(PtreeReaderTest, Types);

TYPED_TEST(PtreeReaderTest, deserializeKeyWithPoint_setKeyWithPoint) {
    const std::string json =
        "{\"root\":{\"title\":\"object\",\"dict\":{\"composite.key\": \"val\", \"k1\": \"v1\"}}}";

    std::istringstream jsonStream(json);
    boost::property_tree::ptree tree;
    boost::property_tree::json_parser::read_json(jsonStream, tree);

    StructWithMap deserialized;
    TypeParam reader(tree.get_child("root"));
    reader.apply(deserialized);

    StructWithMap expected;
    expected.title = "object";
    expected.dict["k1"] = "v1";
    expected.dict["composite.key"] = "val";

    ASSERT_TRUE(expected == deserialized);
}

struct StructWithOptional {
    boost::optional<int> optional;

    bool operator==(const StructWithOptional& other) const {
        return optional == other.optional;
    }
};

BOOST_FUSION_ADAPT_STRUCT(StructWithOptional,
    (boost::optional<int>, optional)
)

TYPED_TEST(PtreeReaderTest, deserializeNull_setUninitializedOptional) {
    const std::string json = R"json({"optional": null})json";

    std::istringstream jsonStream(json);
    boost::property_tree::ptree tree;
    boost::property_tree::json_parser::read_json(jsonStream, tree);

    StructWithOptional deserialized;
    TypeParam reader(tree.get_child("optional"));
    reader.apply(deserialized);

    StructWithOptional expected;

    ASSERT_TRUE(expected == deserialized);
}

struct StructWithIntStringMap {
    using Map = std::map<int, std::string>;
    Map map;
};

BOOST_FUSION_ADAPT_STRUCT(StructWithIntStringMap,
    (StructWithIntStringMap::Map, map)
)

TYPED_TEST(PtreeReaderTest, deserializeMapIntToString) {
    boost::property_tree::ptree tree;
    tree.add("map.1", "foo");
    tree.add("map.2", "bar");

    StructWithIntStringMap deserialized;
    TypeParam reader(tree);
    reader.apply(deserialized);

    StructWithIntStringMap expected;
    expected.map.emplace(1, "foo");
    expected.map.emplace(2, "bar");

    ASSERT_EQ(expected.map, deserialized.map);
}

struct StructWithIntSet {
    using Set = std::set<int>;
    Set set;
};

BOOST_FUSION_ADAPT_STRUCT(StructWithIntSet,
    (StructWithIntSet::Set, set)
)

TEST(PtreeReaderTest, deserializeSet) {
    const std::string json = R"json({"set":["1", 2, 3, 4, "2", "3"]})json";

    std::istringstream jsonStream(json);
    boost::property_tree::ptree tree;
    boost::property_tree::json_parser::read_json(jsonStream, tree);

    StructWithIntSet deserialized;
    property_tree::Reader reader(tree);
    reader.apply(deserialized);

    StructWithIntSet expected;
    expected.set = {1, 2, 3, 4};
    ASSERT_EQ(expected.set, deserialized.set);
}
