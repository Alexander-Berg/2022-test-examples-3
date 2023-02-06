#include <map>
#include <string>
#include <boost/property_tree/json_parser.hpp>
#include <gtest/gtest.h>
#include <yamail/data/serialization/json_writer.h>
#include <yamail/data/deserialization/foreign_ptree.h>
#include <yamail/data/deserialization/ptree_reader.h>
#include <boost/algorithm/string/join.hpp>
#include <boost/range/adaptor/transformed.hpp>

typedef std::map<std::string,std::string> StringMap;

using namespace yamail::data::serialization;
using namespace yamail::data::deserialization;

class ClassWithMap {
public:
    void setTitle(const std::string& title) {
        _title = title;
    }

    const std::string& getTitle() const {
        return _title;
    }

    void setDict(const StringMap& dict) {
        _dict = dict;
    }

    const StringMap& getDict() const {
        return _dict;
    }

    bool operator==(const ClassWithMap& other) const {
        return _title == other._title && _dict == other._dict;
    }

private:
    std::string _title;
    StringMap _dict;
};

class ClassWithProperties {
public:
    void title(const std::string& v) { _title = v; }
    const std::string& title() const { return _title;}
    void dict(const StringMap& v) { _dict = v; }
    const StringMap& dict() const { return _dict; }
private:
    std::string _title;
    StringMap _dict;
};

inline bool operator==(const ClassWithProperties& l, const ClassWithProperties& r) {
    return l.title() == r.title() && l.dict() == r.dict();
}

inline std::ostream & operator << (std::ostream& s, const StringMap& v) {
    const auto r = v | boost::adaptors::transformed([](const StringMap::value_type & p) {
        return std::string("{") + p.first + "," + p.second + "}";
    });
    return s << '{' << boost::algorithm::join( r, ",") << '}';
}

inline std::ostream & operator << (std::ostream& s, const ClassWithProperties& v) {
    return s << '{' << v.title() << ',' << v.dict() << '}';
}

inline std::ostream & operator << (std::ostream& s, const ClassWithMap& v) {
    return s << '{' << v.getTitle() << ',' << v.getDict() << '}';
}

YREFLECTION_ADAPT_ADT(ClassWithMap,
    YREFLECTION_AUTO_GETSET(Title)
    YREFLECTION_AUTO_GETSET(Dict)
)

YREFLECTION_ADAPT_ADT(ClassWithProperties,
    YREFLECTION_PROPERTY(std::string, title)
    YREFLECTION_PROPERTY(StringMap, dict)
)

template <typename T>
class GetterSetterTest : public ::testing::Test {
};

using Types = ::testing::Types<property_tree::Reader, foreign_ptree::Reader>;
TYPED_TEST_SUITE(GetterSetterTest, Types);

TYPED_TEST(GetterSetterTest, deserializeStructWithMapFromJson_sameObject) {
    ClassWithMap obj;
    obj.setTitle("object");
    obj.setDict({{"k1","v1"}, {"k2", "v2"}});

    const auto json = toJson(obj).str();
    ASSERT_EQ(json, "{\"Title\":\"object\",\"Dict\":{\"k1\":\"v1\",\"k2\":\"v2\"}}");

    std::istringstream jsonStream(json);
    boost::property_tree::ptree tree;
    boost::property_tree::json_parser::read_json(jsonStream, tree);

    ClassWithMap deserialized;
    TypeParam reader(tree);
    reader.apply(deserialized);

    ASSERT_EQ(obj, deserialized);
}

TYPED_TEST(GetterSetterTest, deserializeClassWithPropertiesFromJson_sameObject) {
    ClassWithProperties obj;
    obj.title("object");
    obj.dict({{"k1","v1"}, {"k2", "v2"}});

    const auto json = toJson(obj).str();
    ASSERT_EQ(json, "{\"title\":\"object\",\"dict\":{\"k1\":\"v1\",\"k2\":\"v2\"}}");

    std::istringstream jsonStream(json);
    boost::property_tree::ptree tree;
    boost::property_tree::json_parser::read_json(jsonStream, tree);

    ClassWithProperties deserialized;
    TypeParam reader(tree);
    reader.apply(deserialized);

    ASSERT_EQ(obj, deserialized);
}

TEST(GetterSetterTestOldInterface, deserializeStructWithMapFromJson_sameObject) {
    ClassWithMap obj;
    obj.setTitle("object");

    StringMap dict;
    dict["k1"] = "v1";
    dict["k2"] = "v2";
    dict["k3"] = "v3";
    obj.setDict(dict);

    JsonWriter<ClassWithMap> jsonWriter(obj);
    const std::string json = jsonWriter.result();

    std::istringstream jsonStream(json);
    boost::property_tree::ptree tree;
    boost::property_tree::json_parser::read_json(jsonStream, tree);
    PtreeReader<ClassWithMap> ptreeReader(tree);
    const ClassWithMap deserialized = ptreeReader.result();

    ASSERT_TRUE(obj == deserialized);
}

