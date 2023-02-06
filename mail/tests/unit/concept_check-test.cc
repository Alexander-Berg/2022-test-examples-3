#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/detail/concept_check.h>

TEST(ConceptCheckTest, errorMessagesGenerationTest) {
    EXPECT_EQ(DOBBY_HAS_TYPE_MESSAGE(type), " needs to have nested type 'type'");
    EXPECT_EQ(DOBBY_HAS_FIELD_MESSAGE(field, fieldType),
              " needs to have field 'field' of type 'fieldType'");
    EXPECT_EQ(DOBBY_HAS_METHOD_MESSAGE(method, " suffix", void(int, float)),
              " needs to have method 'method' with signature 'void(int, float) suffix'");
    EXPECT_EQ(DOBBY_CONSTRUCTIBLE_FROM_MESSAGE(int, std::string),
              " needs to be constructible from the following arguments list: (int, std::string)");
}

struct TestType {
    TestType(const std::string&, float);

    std::map<int, std::string> method(size_t, void*);
    void method(size_t);

    int method() const;
    void method(float) const;

    using Alias = std::string;

    int field;
    std::map<int, int> field2;
};

#define TEST_CONCEPT_CHECK(T)\
    DOBBY_CHECK_CONCEPT(T,\
        DOBBY_CONSTRUCTIBLE_FROM(const std::string&, float)\
        DOBBY_HAS_METHOD(method, std::map<int, std::string>(size_t, void*))\
        DOBBY_HAS_METHOD(method, void(size_t))\
        DOBBY_HAS_CONST_METHOD(method, int())\
        DOBBY_HAS_CONST_METHOD(method, void(float))\
        DOBBY_HAS_TYPE(Alias)\
        DOBBY_HAS_FIELD(field, int)\
        DOBBY_HAS_FIELD(field2, std::map<int, int>)\
    )

class CheckWithinClassBody {
    TEST_CONCEPT_CHECK(TestType)
};

#ifdef __clang__
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-const-variable"
#endif

// within TU scope
TEST_CONCEPT_CHECK(TestType)

#ifdef __clang__
#pragma clang diagnostic pop
#endif
