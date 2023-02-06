#include <gtest/gtest.h>
#include <macs/tests/mocking-stickers.h>
#include "throw-wmi-helper.h"


namespace {

using namespace ::testing;

struct StickersRepositoryTest: public Test {
    using Repository = StrictMock<MockStickerRepository>;
    std::shared_ptr<Repository> stickers;

    std::time_t now;
    macs::StickerAllowedInterval allowedInterval;

    void SetUp() override {
        stickers = std::make_shared<Repository>();
        now = 100;
        allowedInterval = {
            .min = 100,
            .max = 1000
        };
    }
};


TEST_F(StickersRepositoryTest, shouldCreateStickerMinBorder) {
    
    const std::time_t pinDate = now + allowedInterval.min;

    EXPECT_NO_THROW(
        stickers->checkDateInInterval(pinDate, now, allowedInterval)
    );

}

TEST_F(StickersRepositoryTest, shouldCreateStickerMaxBorder) {
    
    const std::time_t pinDate = now + allowedInterval.max;

    EXPECT_NO_THROW(
        stickers->checkDateInInterval(pinDate, now, allowedInterval)
    );

}

TEST_F(StickersRepositoryTest, shouldCreateStickerInInterval) {

    const std::time_t pinDate = now + (allowedInterval.min + allowedInterval.max) / 2;

    EXPECT_NO_THROW(
        stickers->checkDateInInterval(pinDate, now, allowedInterval)
    );
}

TEST_F(StickersRepositoryTest, shouldNotCreateStickerLowerInterval) {
    const std::time_t pinDate = now + allowedInterval.min - 1;
    
    EXPECT_THROW_SYS(stickers->checkDateInInterval(pinDate, now, allowedInterval),
        macs::error::stickerDateOutOfInterval,
        "sticker's date is out of allowed interval"
    );

}
TEST_F(StickersRepositoryTest, shouldNotCreateStickerUpperInterval) {
    const std::time_t pinDate = now + allowedInterval.max + 1;

    EXPECT_THROW_SYS(stickers->checkDateInInterval(pinDate, now, allowedInterval),
        macs::error::stickerDateOutOfInterval,
        "sticker's date is out of allowed interval"
    );
}


}
