#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/alabay/service/include/types.h>

using namespace ::testing;
using yamail::expected;

namespace alabay::tests {

TEST(UidSetterTest, shouldParseCorrectStringAsUid) {
    const std::string uid = "123456789";
    const Uid expectedUid {123456789};
    const ParsedEvent line { {"uid", uid} };

    const expected<DiskEvent> event = UidSetter{line}(DiskEvent{});
    EXPECT_EQ(event->uid, expectedUid);
}

TEST(UidSetterTest, shouldReturnErrorWhenLineDoesNotContainUidField) {
    const ParsedEvent line { };
    const expected<DiskEvent> event = UidSetter{line}(DiskEvent{});
    EXPECT_FALSE(event);
}

TEST(UidSetterTest, shouldReturnErrorWhenStringIsNotNumber) {
    const std::string uid = "123456789abacaba";
    const ParsedEvent line { {"uid", uid} };
    const expected<DiskEvent> event = UidSetter{line}(DiskEvent{});
    EXPECT_FALSE(event);
}

TEST(TypeSetterTest, shouldParseCorrectEventType) {
    const std::string type = "fs-copy";
    const ParsedEvent line { {"event_type", type} };

    const expected<DiskEvent> event = TypeSetter{line}(DiskEvent{});
    EXPECT_EQ(event->type, type);
}

TEST(TypeSetterTest, shouldReturnErrorWhenLineDoesNotContainTypeField) {
    const ParsedEvent line { };
    const expected<DiskEvent> event = TypeSetter{line}(DiskEvent{});
    EXPECT_FALSE(event);
}

TEST(DateSetter, shouldParseCorrectStringAsDatetime) {
    const std::string date = "12345";
    const std::uint64_t expectedDate {12345};
    const ParsedEvent line { {"tgt_utime", date} };

    const expected<DiskEvent> event = DateSetter{line}(DiskEvent{});
    EXPECT_EQ(event->date, expectedDate);
}

TEST(DateSetter, shouldReturnErrorWhenLineDoesNotContainDateField) {
    const ParsedEvent line { };
    const expected<DiskEvent> event = DateSetter{line}(DiskEvent{});
    EXPECT_FALSE(event);
}

TEST(DateSetter, shouldReturnErrorWhenStringIsNotNumber) {
    const std::string date = "123abacaba45";
    const ParsedEvent line { {"tgt_utime", date} };

    const expected<DiskEvent> event = DateSetter{line}(DiskEvent{});
    EXPECT_FALSE(event);
}

TEST(RequestIdSetterTest, shouldParseStringAsRequestId) {
    const std::string requestId = "q1w2e3r4t5y6u7i8o9";
    const ParsedEvent line { {"req_id", requestId} };

    const expected<DiskEvent> event = RequestIdSetter{line}(DiskEvent{});
    EXPECT_EQ(event->requestId, requestId);
}

TEST(RequestIdSetterTest, shouldSetEmptyWhenLineDoesNotContainRequestIdField) {
    const ParsedEvent line { };
    const expected<DiskEvent> event = RequestIdSetter{line}(DiskEvent{});
    EXPECT_EQ(event->requestId, std::nullopt);
}

TEST(OwnerUidSetterTest, shouldParseCorrectStringAsOwnerUid) {
    const std::string ownerUid = "123456789";
    const Uid expectedOwnerUid {123456789};
    const ParsedEvent line { {"owner_uid", ownerUid} };

    const expected<DiskEvent> event = OwnerUidSetter{line}(DiskEvent{});
    EXPECT_EQ(event->ownerUid, expectedOwnerUid);
}

TEST(OwnerUidSetterTest, shouldSetDefaultWhenLineDoesNotContainOwnerUidField) {
    const ParsedEvent line { };
    const expected<DiskEvent> event = OwnerUidSetter{line}(DiskEvent{});
    EXPECT_EQ(event->ownerUid, Uid{0});
}

TEST(OwnerUidSetterTest, shouldReturnErrorWhenStringIsNotNumber) {
    const std::string ownerUid = "123456789abacaba";
    const ParsedEvent line { {"owner_uid", ownerUid} };
    const expected<DiskEvent> event = OwnerUidSetter{line}(DiskEvent{});
    EXPECT_FALSE(event);
}

TEST(PathSetterTest, shouldParseStringAsPathAndCutPrefix) {
    const std::string path = "49:q1w2e3r4t5y6u7i8o9";
    const std::string expectedPath = "q1w2e3r4t5y6u7i8o9";
    const ParsedEvent line { {"tgt_rawaddress", path} };

    const expected<DiskEvent> event = PathSetter{line}(DiskEvent{});
    EXPECT_EQ(event->path, expectedPath);
}

TEST(RequestIdSetterTest, shouldSetEmptyWhenLineDoesNotContainPathField) {
    const ParsedEvent line { };
    const expected<DiskEvent> event = PathSetter{line}(DiskEvent{});
    EXPECT_EQ(event->path, "");
}

}
