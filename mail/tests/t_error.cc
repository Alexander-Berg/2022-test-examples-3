#include <yplatform/error.h>
#include <catch.hpp>

enum class test_error
{
    ok = 0,
    error1,
    error2,
    error3
};

enum class test_condition
{
    error_1_or_2 = 0
};

inline boost::system::error_code make_error_code(test_error e);
inline boost::system::error_condition make_error_condition(test_condition e);

struct test_error_category : public boost::system::error_category
{
    const char* name() const noexcept override
    {
        return "test_error_category";
    }

    std::string message(int v) const override
    {
        switch (static_cast<test_error>(v))
        {
        case test_error::ok:
            return "ok";
        case test_error::error1:
            return "error1";
        case test_error::error2:
            return "error2";
        case test_error::error3:
            return "error3";
        }
    }
};

struct test_error_condition_category : public boost::system::error_category
{
    const char* name() const noexcept override
    {
        return "test_error_condition_category";
    }

    std::string message(int v) const override
    {
        switch (static_cast<test_condition>(v))
        {
        case test_condition::error_1_or_2:
            return "error1 or error2";
        }
    }

    bool equivalent(const boost::system::error_code& code, int condition) const noexcept override
    {
        auto e = static_cast<test_condition>(condition);
        switch (e)
        {
        case test_condition::error_1_or_2:
            return code == make_error_code(test_error::error1) ||
                code == make_error_code(test_error::error2);
        default:
            return boost::system::error_category::equivalent(code, condition);
        }
    }

    bool equivalent(int code, const boost::system::error_condition& condition) const
        noexcept override
    {
        return boost::system::error_category::equivalent(code, condition);
    }
};

test_error_category& error_category(test_error)
{
    static test_error_category category;
    return category;
}

test_error_condition_category& error_category(test_condition)
{
    static test_error_condition_category category;
    return category;
}

inline boost::system::error_code make_error_code(test_error e)
{
    return boost::system::error_code(static_cast<int>(e), error_category(e));
}

inline boost::system::error_condition make_error_condition(test_condition e)
{
    return boost::system::error_condition(static_cast<int>(e), error_category(e));
}

struct custom_error_code : boost::system::error_code
{
    custom_error_code(test_error e, std::string msg)
        : boost::system::error_code(make_error_code(e)), message_(msg)
    {
    }

    const std::string& what()
    {
        return message_;
    }

    std::string message_;
};

namespace yplatform {

template <>
struct is_error_enum<test_error> : std::true_type
{
};

}

namespace boost::system {

template <>
struct is_error_condition_enum<test_condition> : std::true_type
{
};

}

TEST_CASE("error/value")
{
    yplatform::error err = test_error::error1;
    REQUIRE(err.code().value() == static_cast<int>(test_error::error1));
}

TEST_CASE("error/category")
{
    yplatform::error err = test_error::error1;
    REQUIRE(&err.code().category() == &error_category(test_error::error1));
}

TEST_CASE("error/converts_to_false")
{
    yplatform::error err;
    REQUIRE(!err);
}

TEST_CASE("error/converts_to_true")
{
    yplatform::error err = test_error::error1;
    REQUIRE(err);
}

TEST_CASE("error/equal")
{
    yplatform::error err1(test_error::error1, "extra1");
    yplatform::error err2(test_error::error1, "extra2");
    REQUIRE(err1 == err2);
}

TEST_CASE("error/not_equal")
{
    yplatform::error err1(test_error::error1, "extra1");
    yplatform::error err2(test_error::error2, "extra2");
    REQUIRE(err1 != err2);
}

TEST_CASE("error/message")
{
    yplatform::error err = test_error::error1;
    REQUIRE(err.message() == "error1");
}

TEST_CASE("error/extra_message")
{
    yplatform::error err(test_error::error1, "extra");
    REQUIRE(err.message() == "error1: extra");
}

TEST_CASE("error/copy")
{
    yplatform::error err1(test_error::error1, "extra");
    yplatform::error err2(err1);
    REQUIRE(err2.message() == "error1: extra");
}

TEST_CASE("error/copy_from_boost")
{
    boost::system::error_code boost_err = make_error_code(test_error::error1);
    yplatform::error err = boost_err;
    REQUIRE(err.message() == "error1");
}

TEST_CASE("error/condition_equal")
{
    yplatform::error err(test_error::error1);
    REQUIRE(err == test_condition::error_1_or_2);
}

TEST_CASE("error/condition_not_equal")
{
    yplatform::error err(test_error::error3);
    REQUIRE(err != test_condition::error_1_or_2);
}

TEST_CASE("error/custom_error_code")
{
    custom_error_code err1(test_error::error1, "extra");
    yplatform::error err2(err1);
    REQUIRE(err2.message() == "error1: extra");
}
