#include <ozo/yandex/mdb/role.h>
#include "../test_error.h"

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {

using namespace ozo::yandex::tests;

using test_conditions_constant = ozo::yandex::mdb::conditions_constant<
    error::error, errc::ok
>;

constexpr ozo::yandex::mdb::role<
    class test_role_tag,
    std::false_type,
    test_conditions_constant
> test_role;

static_assert(
    std::is_same_v<decltype(tag(test_role)), test_role_tag>,
    "tag() should return role tag type"
);

static_assert(
    std::is_same_v<decltype(force_update(test_role)), std::false_type>,
    "force_update() should return role force update type"
);

static_assert(
    std::is_same_v<decltype(conditions(test_role)), test_conditions_constant>,
    "conditions() should return role error conditions type"
);

static_assert(
    std::is_same_v<
        decltype(conditions(ozo::yandex::mdb::on<errc::error>.use(test_role))),
        ozo::yandex::mdb::conditions_constant<errc::error>
    >,
    "on<conditions...>.use(role) should set-up role error conditions type"
);

static_assert(
    std::is_same_v<
        decltype(force_update(ozo::yandex::mdb::on<>.use(test_role, ozo::yandex::mdb::with_force_update))),
        std::true_type
    >,
    "on<>.use(role, with_force_update) should set-up role force_update type as std::true_type"
);

using namespace testing;

TEST(can_recover, should_return_true_for_error_condition_in_role_conditions) {
    EXPECT_TRUE(ozo::failover::can_recover(test_role, error::error));
}

TEST(can_recover, should_return_false_for_error_condition_not_in_role_conditions) {
    EXPECT_FALSE(ozo::failover::can_recover(test_role, error::another_error));
}

} // namespace
