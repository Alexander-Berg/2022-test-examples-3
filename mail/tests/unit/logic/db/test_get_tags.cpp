#include "connection_provider_mock.hpp"

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/generic_operators.hpp>

#include <src/logic/db/get_tags_impl.hpp>
#include <src/logic/interface/types/reflection/existing_tag.hpp>


namespace collie::logic {
static bool operator == (const ExistingTag& lhs, const ExistingTag& rhs) {
    using namespace boost::fusion;
    return all(zip(lhs, rhs), [] (const auto& v) { return at_c<0>(v).get() == at_c<1>(v).get(); });
}
}

namespace {

using namespace testing;

using collie::error_code;
using collie::logic::ExistingTag;
using collie::services::db::contacts::query::GetTags;
using collie::logic::Error;
using collie::logic::Revision;
using collie::logic::TagId;
using collie::logic::Uid;
using collie::logic::TagType;
using collie::TaskContextPtr;
using collie::services::db::PassportUserId;

using ConnectionProviderMock = collie::tests::ConnectionProviderMock<GetTags>;
using ConnectionProvider = collie::tests::ConnectionProvider<GetTags>;
using MakeConnectionProviderMock = collie::tests::MakeConnectionProviderMock<GetTags>;
using MakeConnectionProvider = collie::tests::MakeConnectionProvider<GetTags>;

using ExistingTags = std::vector<ExistingTag>;
using TagRows = std::vector<GetTags::result_type>;

using GetTagsImpl = collie::logic::db::GetTagsImpl<MakeConnectionProvider>;

struct TestLogicDbGetTags : TestWithTaskContext {
    StrictMock<ConnectionProviderMock> providerMock;
    ConnectionProvider provider {&providerMock};
    StrictMock<MakeConnectionProviderMock> makeProviderMock;
    MakeConnectionProvider makeProvider {&makeProviderMock};
    const GetTagsImpl getTags {makeProvider};
};

TEST_F(TestLogicDbGetTags, for_text_uid_should_return_userNotFound) {
    withSpawn([&] (const auto& context) {
        const auto result = getTags(context, "TEXT");

        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), error_code{Error::userNotFound});
    });
}

TEST_F(TestLogicDbGetTags, for_existing_user_with_tags_should_return_existing_tags) {
    withSpawn([&] (const auto& context) {
        const InSequence s;

        EXPECT_CALL(makeProviderMock, call(context, PassportUserId {42})).WillOnce(Return(provider));
        EXPECT_CALL(providerMock, request(GetTags{42}))
            .WillOnce(Return(TagRows{
                {1, collie::services::db::contacts::TagType("user"), "userTag", 13, 42},
                {2, collie::services::db::contacts::TagType("system"), "systemTag", 10, 43}
            }));

        const auto result = getTags(context, "42");

        ASSERT_TRUE(result);
        EXPECT_THAT(result.value().tags, ElementsAre(
            ExistingTag{1, TagType::user, "userTag", 13, 42},
            ExistingTag{2, TagType::system, "systemTag", 10, 43}
        ));
    });
}

} // namespace
