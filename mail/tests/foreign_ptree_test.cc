#include <yamail/data/deserialization/foreign_ptree.h>

#include <yamail/data/reflection/reflection.h>
#include <yamail/data/serialization/ptree.h>

#include <yplatform/application/config/loader.h>

#include <gmock/gmock.h>
#include <gtest/gtest.h>

using namespace yamail::data::deserialization;
using namespace yamail::data::serialization;

namespace {

struct IntString {
    int a;
    std::string b;

    friend bool operator==(const IntString& lhs, const IntString& rhs) {
        return std::tie(lhs.a, lhs.b) == std::tie(rhs.a, rhs.b);
    }
};

struct VecIntString {
    std::vector<IntString> vec;

    friend bool operator==(const VecIntString& lhs, const VecIntString& rhs) {
        return std::tie(lhs.vec) == std::tie(rhs.vec);
    }
};

struct OneNestingLevel {
    std::string s;
    VecIntString vecIntString;

    friend bool operator==(const OneNestingLevel& lhs, const OneNestingLevel& rhs) {
        return std::tie(lhs.s, lhs.vecIntString) == std::tie(rhs.s, rhs.vecIntString);
    }
};

struct TwoNestingLevels {
    bool b;
    std::vector<OneNestingLevel> vecOneNestingLevel;

    friend bool operator==(const TwoNestingLevels& lhs, const TwoNestingLevels& rhs) {
        return std::tie(lhs.b, lhs.vecOneNestingLevel) == std::tie(rhs.b, rhs.vecOneNestingLevel);
    }
};

}  // namespace

// clang-format off
BOOST_FUSION_ADAPT_STRUCT(IntString,
    a,
    b
)

BOOST_FUSION_ADAPT_STRUCT(VecIntString,
    vec
)

YREFLECTION_ADAPT_ADT(OneNestingLevel,
    YREFLECTION_AUTO_ATTR(s)
    YREFLECTION_AUTO_ATTR(vec_int_str, vecIntString)
)

YREFLECTION_ADAPT_ADT(TwoNestingLevels,
    YREFLECTION_AUTO_ATTR(b)
    YREFLECTION_AUTO_ATTR(vec_one_nest, vecOneNestingLevel)
)
//clang-format on

namespace {

TEST(DeserializationFromForeignPtreeTests, arrayOfIntStringDeserialization) {
    constexpr char yaml[] = R"(
    vec:
        - a: 1
          b: s1
        - a: 2
          b: s2
        - a: 3
          b: s3
    )";

    ptree pt;
    utils::config::loader::from_str(yaml, pt);

    VecIntString deserialized;
    ASSERT_NO_THROW(fromForeignPtree(pt, deserialized));

    const auto expected = VecIntString{{{1, "s1"}, {2, "s2"}, {3, "s3"}}};
    ASSERT_TRUE(deserialized == expected);
}

TEST(DeserializationFromForeignPtreeTests, oneElementArrayOfIntStringDeserialization) {
    constexpr char yaml[] = R"(
    vec:
        - a: 1
          b: s1
    )";

    ptree pt;
    utils::config::loader::from_str(yaml, pt);

    VecIntString deserialized;
    ASSERT_NO_THROW(fromForeignPtree(pt, deserialized));

    const auto expected = VecIntString{{{1, "s1"}}};
    ASSERT_TRUE(deserialized == expected);
}

TEST(DeserializationFromForeignPtreeTests, arrayOfOneNestingLevelDeserialization) {
    constexpr char yaml[] = R"(
    s: str
    vec_int_str:
        vec:
            - a: 1
              b: s1
            - a: 2
              b: s2
            - a: 3
              b: s3
    )";

    ptree pt;
    utils::config::loader::from_str(yaml, pt);

    OneNestingLevel deserialized;
    ASSERT_NO_THROW(fromForeignPtree(pt, deserialized));

    const auto expected = OneNestingLevel{"str", {{{1, "s1"}, {2, "s2"}, {3, "s3"}}}};
    ASSERT_TRUE(deserialized == expected);
}

TEST(DeserializationFromForeignPtreeTests, arrayOfTwoNestingLevelsDeserialization) {
    constexpr char yaml[] = R"(
    b: true
    vec_one_nest:
        - s: str2
          vec_int_str:
              vec:
                  - a: 1
                    b: s1
                  - a: 2
                    b: s2
                  - a: 3
                    b: s3
        - s: strng3
          vec_int_str:
              vec:
                  - a: 41
                    b: abc
                  - a: 42
                    b: def
                  - a: 53
                    b: ghj
    )";

    ptree pt;
    utils::config::loader::from_str(yaml, pt);

    TwoNestingLevels deserialized;
    ASSERT_NO_THROW(fromForeignPtree(pt, deserialized));

    const auto expected = TwoNestingLevels{
        true,
        {
            OneNestingLevel{"str2", {{{1, "s1"}, {2, "s2"}, {3, "s3"}}}},
            OneNestingLevel{"strng3", {{{41, "abc"}, {42, "def"}, {53, "ghj"}}}}
        }
    };
    ASSERT_TRUE(deserialized == expected);
}

}
