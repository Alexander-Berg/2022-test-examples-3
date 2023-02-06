#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "subscription_mock.h"
#include <src/logic/agent.h>

namespace doberman {
namespace testing {

using namespace ::testing;

TEST(ChangeFilterTest, testingChangeFilterReturnValues) {
    using namespace logic;

    Revision::Value rev = 3;
    SubscribedFolderAccessMock access;
    EXPECT_CALL(access, revision(_, _))
            .WillOnce(Return(Revision{rev - 1}))
            .WillOnce(Return(Revision{rev}))
            .WillOnce(Return(Revision{rev + 1}))
            .WillOnce(Throw(macs::system_error(error_code{macs::error::noSuchFolder})));

    Change change{1, {rev}, [](auto&){ return error_code{}; }};
    ChangeFilter filter{};
    SubscriptionMock::SubscribedFolder dst{doberman::logic::makeSubscribedFolder({""}, {{""}, ""}, &access, LabelFilter({{},{}}))};

    EXPECT_EQ(filter(change, dst), std::make_tuple(true, error_code{}));
    EXPECT_EQ(filter(change, dst), std::make_tuple(true, error_code{}));
    EXPECT_EQ(filter(change, dst), std::make_tuple(false, error_code{}));
    EXPECT_EQ(filter(change, dst), std::make_tuple(false, error_code{macs::error::noSuchFolder}));
}

} // namespace testing
} // namespace doberman

