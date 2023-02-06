#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/ymod_cachedb/include/internal/callbacks.h>
#include <mail/ymod_cachedb/include/internal/logger.h>
#include <mail/ymod_cachedb/include/error.h>
#include <yplatform/application/config/loader.h>

using namespace ::testing;

namespace ymod_cachedb::tests {

const std::string value = "some string";
const result::Get rowGet(value);
const auto internalError = make_error(CacheResult::internalError, "some error");

TEST(OnGet, shouldPassErrorCode) {
    onGet(internalError, {rowGet}, [&](const auto& ec, const auto& val) {
        EXPECT_EQ(ec, internalError);
        EXPECT_EQ(val, std::nullopt);
    });
}

TEST(OnGet, shouldReturnErrorOnManyValues) {
    onGet(pgg::error_code(), {rowGet, rowGet}, [&](const auto& ec, const auto& val) {
        EXPECT_EQ(ec.value(), static_cast<int>(CacheResult::internalError));
        EXPECT_EQ(ec.category(), getCacheCategory());
        EXPECT_EQ(val, std::nullopt);
    });
}

TEST(OnGet, shouldReturnValueOnSinglerowGet) {
    onGet(pgg::error_code(), {rowGet}, [&](const auto& ec, const auto& val) {
        EXPECT_FALSE(ec);
        EXPECT_EQ(val->t, value);
    });
}

TEST(OnGet, shouldReturnNullOnEmptyValue) {
    onGet(pgg::error_code(), {}, [&](const auto& ec, const auto& val) {
        EXPECT_FALSE(ec);
        EXPECT_EQ(val, std::nullopt);
    });
}

TEST(OnPut, shouldPassErrorCode) {
    onPut(internalError, result::Put(true), [&](const auto& ec, const auto& val) {
        EXPECT_EQ(ec, internalError);
        EXPECT_FALSE(val);
    });
}

TEST(OnPut, shouldPassValue) {
    onPut(pgg::error_code(), result::Put(true), [&](const auto& ec, const auto& val) {
        EXPECT_FALSE(ec);
        EXPECT_TRUE(val);
    });

    onPut(pgg::error_code(), result::Put(false), [&](const auto& ec, const auto& val) {
        EXPECT_FALSE(ec);
        EXPECT_FALSE(val);
    });
}

}
