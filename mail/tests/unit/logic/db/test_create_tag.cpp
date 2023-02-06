#include "connection_provider_mock.hpp"

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>

#include <src/logic/db/create_tag_impl.hpp>

namespace collie::logic {

static bool operator == (const CreatedTag& lhs, const CreatedTag& rhs) {
    return std::tie(lhs.tagId, lhs.revision) == std::tie(rhs.tagId, rhs.revision);
}

}

namespace {

using namespace testing;

using collie::error_code;
using collie::logic::CreatedTag;
using collie::logic::Error;
using collie::logic::TagId;
using collie::logic::Revision;
using collie::services::db::PassportUserId;
using collie::services::db::contacts::query::CreateTag;
using collie::TaskContextPtr;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<CreateTag>;
using ConnectionProvider = collie::tests::ConnectionProvider<CreateTag>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<CreateTag>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<CreateTag>;

using CreatedTagRows = std::vector<CreateTag::result_type>;
using CreateTagImpl = collie::logic::db::CreateTagImpl<MakeConnectionProvider>;

struct TestLogicDbCreateTag : public TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider {&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider {&makeProviderMock};
    const CreateTagImpl createTag {makeProvider};
};

TEST_F(TestLogicDbCreateTag, for_text_uid_should_return_userNotFound) {
    withSpawn([&] (const auto& context) {
        const auto result = createTag(context, "TEXT", {"tag_name", {}});

        ASSERT_FALSE(result);
        EXPECT_THAT(result.error(), error_code{Error::userNotFound});
    });
}

TEST_F(TestLogicDbCreateTag, for_existent_user_should_create_tag) {
    withSpawn([this](const auto& context) {
        const InSequence s;

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, request(CreateTag{42, {"tag_name"}, "request_id"}))
            .WillOnce(Return(CreatedTagRows{{1, 33}}));

        const auto result = createTag(context, "42", {"tag_name", {}});

        ASSERT_TRUE(result);
        EXPECT_EQ(result.value(), (CreatedTag{TagId{33}, Revision{1}}));
    });
}

}
