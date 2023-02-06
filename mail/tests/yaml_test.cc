#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <yamail/data/deserialization/yaml.h>
#include <yamail/data/serialization/yaml.h>

#include <boost/range/algorithm/copy.hpp>

using namespace testing;
using namespace yamail::data::serialization;
using namespace yamail::data::deserialization;

template <typename T>
class YamlPrimitivesTest : public Test {
    static_assert(std::is_integral<T>::value, "Expected integral type");
};

using PrimitiveTypes = Types<short, unsigned short, int, unsigned, long, unsigned long,
                             long long, unsigned long long>;
TYPED_TEST_SUITE(YamlPrimitivesTest, PrimitiveTypes);

using Map = std::map<int, std::string>;

template <typename T>
bool isPtrValuesEquals(const std::unique_ptr<T>& lhs, const std::unique_ptr<T>& rhs) {
    if (lhs == rhs) {
        return true;
    }

    if (lhs != nullptr && rhs != nullptr) {
        return *lhs == *rhs;
    }

    return false;
}

struct HostPort {
    std::string host;
    std::string port;

    bool operator==(const HostPort&) const = default;
    bool operator<(const HostPort& rhs) const { return (host < rhs.host) && (port < rhs.port); }
};

BOOST_FUSION_ADAPT_STRUCT(HostPort,
    (std::string, host)
    (std::string, port)
)


struct TestStruct {
    float floatVal;
    std::string str;
    boost::optional<short> opt;
    std::unique_ptr<TestStruct> ptr;
    std::vector<int> intVec;
    std::vector<HostPort> hostPortVec;
    Map map;
    std::set<int> intSet;
    std::set<HostPort> hostPortSet;

    bool operator== (const TestStruct& that) const {
        return floatVal == that.floatVal
            && str == that.str
            && opt == that.opt
            && isPtrValuesEquals(ptr, that.ptr)
            && intVec == that.intVec
            && hostPortVec == that.hostPortVec
            && map == that.map
            && intSet == that.intSet
            && hostPortSet == that.hostPortSet
            ;
    }
};

inline std::ostream& operator<< (std::ostream& stream, const TestStruct& value) {
    stream << "floatVal: " << value.floatVal << std::endl;
    stream << "str: " << value.str << std::endl;
    stream << "opt: ";
    if (value.opt) {
        std::cout << value.opt.get();
    }
    std::cout << std::endl;
    stream << "ptr: ";
    if (value.ptr) {
        stream << *value.ptr;
    }
    stream << std::endl;
    stream << "intVec: { ";
    boost::copy(value.intVec, std::ostream_iterator<int>(std::cout, ", "));
    stream << " }" << std::endl;
    stream << "map: { ";
    for (const auto& pair : value.map) {
        std::cout << "{" << pair.first << ", " << pair.second << "}";
    }
    stream << " }" << std::endl;
    return stream;
}

BOOST_FUSION_ADAPT_STRUCT(TestStruct,
    (float, floatVal)
    (std::string, str)
    (boost::optional<short>, opt)
    (std::unique_ptr<TestStruct>, ptr)
    (std::vector<int>, intVec)
    (std::vector<HostPort>, hostPortVec)
    (Map, map)
    (std::set<int>, intSet)
    (std::set<HostPort>, hostPortSet)
)

static TestStruct makeTestStruct() {
    auto ptr = std::make_unique<TestStruct>(TestStruct{
        .002f,
        "test",
        42,
        nullptr,
        {},
        {},
        {},
        {},
        {},
    });

    return TestStruct {
        34.55f,
        "string",
        boost::none,
        std::move(ptr),
        { 33, 0, -13 },
        { {"host", "port"}, {"another_host", "another_port"} },
        {
            { 234, "234" },
            { -32, "-32" }
        },
        {3,2,1,1},
        { {"host", "port"}, {"another_host", "another_port"}, {"host", "port"}, {"another_host", "another_port"} },
    };
}

static const TestStruct testStruct = makeTestStruct();

static const std::string testYaml =
    "floatVal: 34.55\n"
    "str: string\n"
    "ptr:\n"
    "  floatVal: 0.002\n"
    "  str: test\n"
    "  opt: 42\n"
    "  intVec:\n"
    "    []\n"
    "  hostPortVec:\n"
    "    []\n"
    "  map:\n"
    "    {}\n"
    "  intSet:\n"
    "    []\n"
    "  hostPortSet:\n"
    "    []\n"
    "intVec:\n"
    "  - 33\n"
    "  - 0\n"
    "  - -13\n"
    "hostPortVec:\n"
    "  - host: host\n"
    "    port: port\n"
    "  - host: another_host\n"
    "    port: another_port\n"
    "map:\n"
    "  -32: -32\n"
    "  234: 234\n"
    "intSet:\n"
    "  - 1\n"
    "  - 2\n"
    "  - 3\n"
    "hostPortSet:\n"
    "  - host: another_host\n"
    "    port: another_port\n"
    "  - host: host\n"
    "    port: port"
    ;

TYPED_TEST(YamlPrimitivesTest, primitiveSerialization) {
    const auto min = std::numeric_limits<TypeParam>::min();
    const auto max = std::numeric_limits<TypeParam>::max();

    EXPECT_EQ(toYaml(min), std::to_string(min));
    EXPECT_EQ(toYaml(max), std::to_string(max));
}

TYPED_TEST(YamlPrimitivesTest, primitiveDeserialization) {
    const auto min = std::numeric_limits<TypeParam>::min();
    const auto max = std::numeric_limits<TypeParam>::max();

    EXPECT_EQ(fromYaml<TypeParam>(std::to_string(min)), min);
    EXPECT_EQ(fromYaml<TypeParam>(std::to_string(max)), max);
}

TEST(YamlTest, boolToYaml) {
    EXPECT_EQ(toYaml(true), "true");
    EXPECT_EQ(toYaml(false), "false");
}

TEST(YamlTest, stringViewToYaml) {
    const char* v = "test";
    EXPECT_EQ(toYaml(std::string_view(v)), "test");
}

TEST(YamlTest, yamlToBool) {
    EXPECT_EQ(fromYaml<bool>("true"), true);
    EXPECT_EQ(fromYaml<bool>("false"), false);
}

TEST(YamlTest, structSerialization) {
    EXPECT_EQ(toYaml(testStruct), testYaml);
}

TEST(YamlTest, structDeserialization) {
    EXPECT_EQ(fromYaml<TestStruct>(testYaml), testStruct);
}

TEST(YamlTest, setDeserialization) {
    constexpr auto yaml = 
    "\n"
    "  - host: another_host\n"
    "    port: another_port\n"
    "  - host: host\n"
    "    port: port"
    ;
    decltype(TestStruct().hostPortSet) expected = { {"host", "port"}, {"another_host", "another_port"} };
    EXPECT_EQ(fromYaml<TestStruct>(testYaml + yaml).hostPortSet, expected);
}
