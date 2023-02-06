#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "change_mock.h"
#include "subscribed_folder_mock.h"

namespace {

using namespace ::testing;
using namespace ::doberman::testing;

struct ChangeTest : public Test {
    ChangeMock mock;
    auto makeChange(::doberman::ChangeId id, ::doberman::Revision r) {
        return ::doberman::testing::makeChange(id, r, mock);
    }
};

TEST_F(ChangeTest, id_withConstructorValue_returnsValue) {
    auto change = makeChange(123, {});
    ASSERT_EQ(change->id(), 123);
}

TEST_F(ChangeTest, revision_withConstructorValue_returnsValue) {
    auto change = makeChange({}, {123});
    ASSERT_EQ(change->revision(), 123ul);
}

TEST_F(ChangeTest, apply_callsImplementation_returnsValue) {
    auto change = makeChange({}, {});
    EXPECT_CALL(mock, apply(_)).WillOnce(Return(::doberman::error_code{::macs::error::invalidArgument}));
    SubscribedFolderAccessMock folderMock;
    auto folder = doberman::logic::makeSubscribedFolder({""}, {{""}, ""}, &folderMock,
                                                        doberman::LabelFilter({{},{}}));
    ASSERT_EQ(change->apply(folder), ::macs::error::invalidArgument);
}

TEST_F(ChangeTest, apply_withImplementationThrowsSystemError_returnsErrorCode) {
    auto change = makeChange({}, {});
    EXPECT_CALL(mock, apply(_)).WillOnce(
            Throw(::macs::system_error(::doberman::error_code{::macs::error::invalidArgument})));
    SubscribedFolderAccessMock folderMock;
    auto folder = doberman::logic::makeSubscribedFolder({""}, {{""}, ""}, &folderMock,
                                                        doberman::LabelFilter({{},{}}));
    ASSERT_EQ(change->apply(folder), ::macs::error::invalidArgument);
}

TEST_F(ChangeTest, apply_withImplementationThrowsNotSystemError_throws) {
    auto change = makeChange({}, {});
    EXPECT_CALL(mock, apply(_)).WillOnce(Throw(std::runtime_error("")));
    SubscribedFolderAccessMock folderMock;
    auto folder = doberman::logic::makeSubscribedFolder({""}, {{""}, ""}, &folderMock,
                                                        doberman::LabelFilter({{},{}}));
    EXPECT_THROW(change->apply(folder), std::runtime_error);
}

}
