#include "../../mocks.h"

#include <internal/mail/registration.h>

#include <gtest/gtest.h>

#include <iomanip>
#include <typeinfo>

namespace sharpei {

static inline std::ostream& operator <<(std::ostream& stream, const Shard::Database::Address& value) {
    return stream << "sharpei::Shard::Database::Address("
                  << '"' << value.host << "\", "
                  << value.port << ", "
                  << '"' << value.dbname << "\", "
                  << '"' << value.dataCenter << '"'
                  << ")";
}

namespace mail {
namespace db {

static inline bool operator ==(const RegParams& lhs, const RegParams& rhs) {
    return lhs.uid == rhs.uid
        && lhs.country == rhs.country
        && lhs.lang == rhs.lang
        && lhs.needsWelcome == rhs.needsWelcome
        && lhs.shardId == rhs.shardId
        && lhs.mdbMaster == rhs.mdbMaster
        && lhs.sharddbMasterHost == rhs.sharddbMasterHost
        && lhs.requestId == rhs.requestId
        && lhs.verstkaSessionKey == rhs.verstkaSessionKey
        && lhs.sessionId == rhs.sessionId;
}

static inline std::ostream& operator <<(std::ostream& stream, const RegParams& value) {
    return stream << "sharpei::RegParams {"
                  << value.uid.value() << ", "
                  << '"' << value.country << "\", "
                  << '"' << value.lang << "\", "
                  << std::boolalpha << value.needsWelcome << ", "
                  << value.shardId << ", "
                  << value.mdbMaster << ", "
                  << '"' << value.sharddbMasterHost << "\", "
                  << '"' << value.requestId << "\", "
                  << '"' << value.verstkaSessionKey << "\", "
                  << '"' << value.sessionId << '"'
                  << "}";
}

} // namespace db
} // namespace mail
} // namespace sharpei

namespace {

using namespace testing;
using namespace sharpei;
using namespace sharpei::mail::db;
using namespace sharpei::mail::registration;

using Response = yhttp::response;
using Request = yhttp::request;
using db::MockedMetaAdaptor;
using db::RegData;

struct MockedRegAdaptor : public RegAdaptor {
    MOCK_METHOD(void, registerUser, (const RegParams&, const Handler&), (override));
};

struct MockedHandler {
    MOCK_METHOD(void, call, (ExplainedError, Shard::Id), ());
};

struct PerformerTest : public Test {
    using RequestParameters = NiceMock<RequestParametersMock>;
    using Writer = StrictMock<WriterMock>;
    using Mapper = StrictMock<MapperMock>;

    std::shared_ptr<RequestParameters> requestInfoPtr = std::make_shared<RequestParameters>();
    std::shared_ptr<Writer> writerPtr = std::make_shared<Writer>();
    UserJournalPtr journalPtr = std::make_shared<UserJournal>(requestInfoPtr, writerPtr);
    Mapper mapper;

    using UserId = BasicUserId<std::int64_t>;

    mail::ConfigPtr config = sharpei::mail::makeTestConfig();
    std::shared_ptr<ClusterClientMock> httpClient = std::make_shared<ClusterClientMock>();
    std::shared_ptr<const MockedMetaAdaptor<UserId::Value>> metaAdaptor = std::make_shared<const MockedMetaAdaptor<UserId::Value>>();
    cache::CachePtr cache = std::make_shared<cache::Cache>(config->base()->cache.historyCapacity, config->base()->cache.errorsLimit);
    std::shared_ptr<MockedRegAdaptor> regAdaptor = std::make_shared<MockedRegAdaptor>();
    std::shared_ptr<Performer> performer = std::make_shared<Performer>(httpClient, metaAdaptor, cache, regAdaptor, config, journalPtr);
    Params params;
    MockedHandler handler;

    const UserId uid = 13;
    const std::string uidStr = sharpei::to_string(uid);
    const std::string requestId = "requestId";
    const std::string verstkaSessionKey = "verstkaSessionKey";
    const std::string sessionId = "sessionId";
    const Shard::Id shardId = 42;
    const unsigned domainId = 146;
    const Request correctBlackBoxRequest = Request::GET(
        "?method=userinfo"
        "&uid=13"
        "&userip=userIp"
        "&dbfields=account_info%2Ecountry%2Euid"
            ",hosts%2Edb_id%2E2"
            ",subscription%2Esuid%2E2"
            ",userinfo%2Elang%2Euid"
        "&aliases=all"
    );
    const std::string blackBoxResponseBody =
        R"xml(<?xml version="1.0" encoding="UTF-8"?>
            <doc>
                <uid hosted="0">13</uid>
                <login>login</login>
                <have_password>1</have_password>
                <have_hint>1</have_hint>
                <karma confirmed="0">0</karma>
                <karma_status>0</karma_status>
                <dbfield id="account_info.country.uid">country</dbfield>
                <dbfield id="userinfo.lang.uid">lang</dbfield>
                <dbfield id="subscription.suid.2">42</dbfield>
                <dbfield id="hosts.db_id.2">pg</dbfield>"
                <aliases>
                    <alias type="1">login</alias>
                </aliases>
            </doc>
        )xml";
    RegData regData;
    RegParams regParams;
    const Shard::Database::Address shardMaster {"host", 5432, "dbname", "dataCenter"};
    const std::string metaMaster = "metaMaster";

    void SetUp() final {
        using sharpei::cache::RoleCache;

        params.uid = uid;
        params.userIp = "userIp";
        params.requestId = requestId;
        params.verstkaSessionKey = verstkaSessionKey;
        params.sessionId = sessionId;
        params.enableWelcomeLetters = true;
        params.createBaseFilters = true;
        regData.weightedShardIds.emplace_back(shardId, 1);
        cache->shardName.update(shardId, "shard");
        cache->role.update(shardId, {{shardMaster, RoleCache::OptRole(Shard::Database::Role::Master)}});
        cache->status.alive(shardId, shardMaster);
        regParams.uid = uid;
        regParams.country = "country";
        regParams.lang = "lang";
        regParams.enableWelcome = true;
        regParams.needsWelcome = true;
        regParams.baseFilters = true;
        regParams.shardId = shardId;
        regParams.mdbMaster = shardMaster;
        regParams.sharddbMasterHost = metaMaster;
        regParams.requestId = requestId;
        regParams.verstkaSessionKey = verstkaSessionKey;
        regParams.sessionId = sessionId;
    }

    void expectUserJournal() {
        expectUserJournalWithShardId(boost::make_optional(std::to_string(regParams.shardId)));
    }
    void expectUserJournalAnyShardId() {
        expectUserJournalWithShardId(boost::none);
    }

private:
    void expectUserJournalWithShardId(boost::optional<std::string> shardId) const {
        EXPECT_CALL(*requestInfoPtr, uid())
                .WillOnce(ReturnRef(uidStr));
        EXPECT_CALL(*writerPtr, write(uidStr, _))
                .WillOnce(Invoke([&mapper = this->mapper](const std::string&, const Entry& e){
                    e.map(mapper);
                }));

        EXPECT_CALL(mapper, mapValue(TypedEq<bool>(false), "hidden"));
        EXPECT_CALL(mapper, mapValue(An<const std::string &>(), "state"));
        EXPECT_CALL(mapper, mapValue(An<const Date &>(), "date"));
        EXPECT_CALL(mapper, mapValue(An<std::time_t>(), "unixtime"));
        EXPECT_CALL(mapper, mapValue(std::string("pg"), "mdb"));
        EXPECT_CALL(mapper, mapValue(TypedEq<std::size_t>(1u), "affected"));

        EXPECT_CALL(mapper, mapValue(TypedEq<const Target &>(Target::account), "target"));
        EXPECT_CALL(mapper, mapValue(TypedEq<const Operation &>(Operation::registration), "operation"));

        EXPECT_CALL(mapper, mapValue(regParams.country, "country"));
        EXPECT_CALL(mapper, mapValue(regParams.lang, "lang"));
        EXPECT_CALL(mapper, mapValue(TypedEq<bool>(regParams.enableWelcome), "enableWelcome"));
        EXPECT_CALL(mapper, mapValue(TypedEq<bool>(regParams.needsWelcome), "needsWelcome"));
        EXPECT_CALL(mapper, mapValue(TypedEq<bool>(regParams.baseFilters), "baseFilters"));
        if (shardId) {
            EXPECT_CALL(mapper, mapValue(shardId.get(), "shardId"));
        } else {
            EXPECT_CALL(mapper, mapValue(An<const std::string &>(), "shardId"));
        }
        EXPECT_CALL(mapper, mapValue(regParams.verstkaSessionKey, "verstkaSessionKey"));
        EXPECT_CALL(mapper, mapValue(regParams.sessionId, "sessionId"));
    }
};

TEST_F(PerformerTest, perform_should_use_http_client_meta_adaptor_reg_adaptor) {
    expectUserJournal();
    InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, correctBlackBoxRequest, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(metaMaster));
    EXPECT_CALL(*metaAdaptor, getUserRegData(uid, _, _)).WillOnce(InvokeArgument<1>(regData));
    EXPECT_CALL(*metaAdaptor, getDeletedUserData(uid, _, _, _)).WillOnce(InvokeArgument<2>(ExplainedError(Error::uidNotFound)));
    EXPECT_CALL(*regAdaptor, registerUser(_, _)).WillOnce(InvokeArgument<1>(ExplainedError(Error::ok)));
    EXPECT_CALL(handler, call(ExplainedError(Error::ok), shardId)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_when_user_has_zero_domain_id_should_call_register_user_with_true_needs_welcomes) {
    expectUserJournal();
    const InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(metaMaster));
    EXPECT_CALL(*metaAdaptor, getUserRegData(uid, _, _)).WillOnce(InvokeArgument<1>(regData));
    EXPECT_CALL(*metaAdaptor, getDeletedUserData(uid, _, _, _)).WillOnce(InvokeArgument<2>(ExplainedError(Error::uidNotFound)));
    EXPECT_CALL(*regAdaptor, registerUser(regParams, _)).WillOnce(InvokeArgument<1>(ExplainedError(Error::ok)));
    EXPECT_CALL(handler, call(ExplainedError(Error::ok), shardId)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_when_user_has_not_zero_domain_id_should_call_register_user_with_false_needs_welcomes) {
    const std::string blackBoxResponseBody =
        R"xml(<?xml version="1.0" encoding="UTF-8"?>
            <doc>
                <uid hosted="1" domid="146" domain="domain" mx="1" domain_ena="1" catch_all="0">13</uid>
                <login>login</login>
                <have_password>1</have_password>
                <have_hint>1</have_hint>
                <karma confirmed="0">0</karma>
                <karma_status>0</karma_status>
                <dbfield id="account_info.country.uid">country</dbfield>
                <dbfield id="userinfo.lang.uid">lang</dbfield>
                <dbfield id="subscription.suid.2">42</dbfield>
                <dbfield id="hosts.db_id.2">pg</dbfield>"
                <aliases>
                    <alias type="1">login</alias>
                </aliases>
            </doc>
        )xml";

    regParams.needsWelcome = false;

    expectUserJournal();
    const InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(metaMaster));
    EXPECT_CALL(*metaAdaptor, getUserRegData(uid, _, _)).WillOnce(InvokeArgument<1>(regData));
    EXPECT_CALL(*metaAdaptor, getDeletedUserData(uid, _, _, _)).WillOnce(InvokeArgument<2>(ExplainedError(Error::uidNotFound)));
    EXPECT_CALL(*regAdaptor, registerUser(regParams, _)).WillOnce(InvokeArgument<1>(ExplainedError(Error::ok)));
    EXPECT_CALL(handler, call(ExplainedError(Error::ok), shardId)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_when_user_has_mailish_alias_should_call_register_user_with_false_needs_welcomes) {
    const std::string blackBoxResponseBody =
        R"xml(<?xml version="1.0" encoding="UTF-8"?>
            <doc>
                <uid hosted="0">13</uid>
                <login>login</login>
                <have_password>1</have_password>
                <have_hint>1</have_hint>
                <karma confirmed="0">0</karma>
                <karma_status>0</karma_status>
                <dbfield id="account_info.country.uid">country</dbfield>
                <dbfield id="userinfo.lang.uid">lang</dbfield>
                <dbfield id="subscription.suid.2">42</dbfield>
                <dbfield id="hosts.db_id.2">pg</dbfield>"
                <aliases>
                    <alias type="12">mailish_login</alias>
                </aliases>
            </doc>
        )xml";

    regParams.needsWelcome = false;

    expectUserJournal();
    const InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(metaMaster));
    EXPECT_CALL(*metaAdaptor, getUserRegData(uid, _, _)).WillOnce(InvokeArgument<1>(regData));
    EXPECT_CALL(*metaAdaptor, getDeletedUserData(uid, _, _, _)).WillOnce(InvokeArgument<2>(ExplainedError(Error::uidNotFound)));
    EXPECT_CALL(*regAdaptor, registerUser(regParams, _)).WillOnce(InvokeArgument<1>(ExplainedError(Error::ok)));
    EXPECT_CALL(handler, call(ExplainedError(Error::ok), shardId)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_when_welcome_letters_are_disabled_should_call_register_user_with_false_enable_welcomes) {
    params.enableWelcomeLetters = false;
    regParams.enableWelcome = false;

    expectUserJournal();
    const InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(metaMaster));
    EXPECT_CALL(*metaAdaptor, getUserRegData(uid, _, _)).WillOnce(InvokeArgument<1>(regData));
    EXPECT_CALL(*metaAdaptor, getDeletedUserData(uid, _, _, _)).WillOnce(InvokeArgument<2>(ExplainedError(Error::uidNotFound)));
    EXPECT_CALL(*regAdaptor, registerUser(regParams, _)).WillOnce(InvokeArgument<1>(ExplainedError(Error::ok)));
    EXPECT_CALL(handler, call(ExplainedError(Error::ok), shardId)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_with_no_shard_from_get_reg_data_should_not_call_register_user) {
    regData.userShardId = shardId;

    InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(metaMaster));
    EXPECT_CALL(*metaAdaptor, getUserRegData(uid, _, _)).WillOnce(InvokeArgument<1>(regData));
    EXPECT_CALL(*metaAdaptor, getDeletedUserData(uid, _, _, _)).Times(0);
    EXPECT_CALL(*regAdaptor, registerUser(_, _)).Times(0);
    EXPECT_CALL(handler, call(ExplainedError(Error::ok), shardId)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_with_error_in_async_run_should_not_call_get_master) {
    InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {500, {}, "", ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).Times(0);
    EXPECT_CALL(handler, call(ExplainedError(Error::blackBoxHttpError), Shard::Id())).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_with_error_in_get_master_should_not_call_get_reg_data) {
    InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(
            InvokeArgument<3>(yhttp::errc::success, Response{200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _))
        .WillOnce(InvokeArgument<1>(ExplainedError(Error::metaMasterProviderError)));
    EXPECT_CALL(*metaAdaptor, getUserRegData(_, _, _)).Times(0);
    EXPECT_CALL(handler, call(ExplainedError(Error::metaMasterProviderError), Shard::Id()))
        .WillOnce(Return());

    performer->perform(params, [&](auto... args) { handler.call(args...); });
}

TEST_F(PerformerTest, perform_with_error_in_get_reg_data_should_not_call_register_user) {
    InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(metaMaster));
    EXPECT_CALL(*metaAdaptor, getUserRegData(_, _, _))
        .WillOnce(InvokeArgument<2>(ExplainedError(Error::metaRequestError)));
    EXPECT_CALL(*metaAdaptor, getDeletedUserData(uid, _, _, _)).Times(0);
    EXPECT_CALL(*regAdaptor, registerUser(_, _)).Times(0);
    EXPECT_CALL(handler, call(ExplainedError(Error::metaRequestError), _)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_with_error_in_register_user_should_return_same_error) {
    InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(metaMaster));
    EXPECT_CALL(*metaAdaptor, getUserRegData(uid, _, _)).WillOnce(InvokeArgument<1>(regData));
    EXPECT_CALL(*metaAdaptor, getDeletedUserData(uid, _, _, _)).WillOnce(InvokeArgument<2>(ExplainedError(Error::uidNotFound)));
    EXPECT_CALL(*regAdaptor, registerUser(_, _)).WillOnce(InvokeArgument<1>(ExplainedError(RegistrationError::registrationInProgress)));
    EXPECT_CALL(handler, call(ExplainedError(RegistrationError::registrationInProgress), _)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_when_no_alive_shards_masters_should_return_error) {
    cache->status.dead(shardId, shardMaster);

    InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(metaMaster));
    EXPECT_CALL(*metaAdaptor, getUserRegData(uid, _, _)).WillOnce(InvokeArgument<1>(regData));
    EXPECT_CALL(*metaAdaptor, getDeletedUserData(uid, _, _, _)).WillOnce(InvokeArgument<2>(ExplainedError(Error::uidNotFound)));
    EXPECT_CALL(handler, call(ExplainedError(Error::noShardWithAliveMaster), _)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_with_std_exception_in_async_run_should_return_error) {
    cache->status.dead(shardId, shardMaster);

    InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _)).WillOnce(Throw(std::exception()));
    EXPECT_CALL(handler, call(ExplainedError(Error::mailRegistrationException), _)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

struct boost_exception : public boost::exception {};

TEST_F(PerformerTest, perform_with_boost_exception_in_async_run_should_return_error) {
    cache->status.dead(shardId, shardMaster);

    InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _)).WillOnce(Throw(boost_exception()));
    EXPECT_CALL(handler, call(ExplainedError(Error::mailRegistrationException), _)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_with_error_shard_is_occupied_by_user_should_retry_with_next_shard) {
    using sharpei::cache::RoleCache;

    const auto nextShardId = shardId + 1;
    const Shard::Database::Address nextShardMaster {"next_host", 5432, "dbname", "dataCenter"};
    regData.weightedShardIds.emplace_back(nextShardId, 1);
    cache->shardName.update(nextShardId, "nextShard");
    cache->role.update(nextShardId, {{nextShardMaster, RoleCache::OptRole(Shard::Database::Role::Master)}});
    cache->status.alive(nextShardId, nextShardMaster);

    expectUserJournalAnyShardId();
    const InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(metaMaster));
    EXPECT_CALL(*metaAdaptor, getUserRegData(uid, _, _)).WillOnce(InvokeArgument<1>(regData));
    EXPECT_CALL(*metaAdaptor, getDeletedUserData(uid, _, _, _)).WillOnce(InvokeArgument<2>(ExplainedError(Error::uidNotFound)));
    EXPECT_CALL(*regAdaptor, registerUser(_, _)).WillOnce(InvokeArgument<1>(ExplainedError(RegistrationError::shardIsOccupiedByUser)));
    EXPECT_CALL(*regAdaptor, registerUser(_, _)).WillOnce(InvokeArgument<1>(ExplainedError(Error::ok)));
    EXPECT_CALL(handler, call(ExplainedError(Error::ok), _)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_with_error_shard_is_occupied_by_user_should_retry_with_every_shard_once) {
    const InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(metaMaster));
    EXPECT_CALL(*metaAdaptor, getUserRegData(uid, _, _)).WillOnce(InvokeArgument<1>(regData));
    EXPECT_CALL(*metaAdaptor, getDeletedUserData(uid, _, _, _)).WillOnce(InvokeArgument<2>(ExplainedError(Error::uidNotFound)));
    EXPECT_CALL(*regAdaptor, registerUser(_, _)).WillOnce(InvokeArgument<1>(ExplainedError(RegistrationError::shardIsOccupiedByUser)));
    EXPECT_CALL(handler, call(ExplainedError(Error::noShardWithAliveMaster), _)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_with_deleted_user_should_call_register_user_with_shard_of_deleted_user) {
    using sharpei::cache::RoleCache;

    const Shard::Id deletedShardId = 666;
    const Shard::Database::Address deletedShardMaster {"host", 5432, "dbname", "dataCenter"};

    cache->shardName.update(deletedShardId, "deletedShard");
    cache->role.update(deletedShardId, {{deletedShardMaster, RoleCache::OptRole(Shard::Database::Role::Master)}});
    cache->status.alive(deletedShardId, deletedShardMaster);

    regParams.shardId = deletedShardId;

    expectUserJournal();
    const InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(metaMaster));
    EXPECT_CALL(*metaAdaptor, getUserRegData(uid, _, _)).WillOnce(InvokeArgument<1>(regData));
    EXPECT_CALL(*metaAdaptor, getDeletedUserData(uid, _, _, _)).WillOnce(InvokeArgument<1>(deletedShardId, boost::none));
    EXPECT_CALL(*regAdaptor, registerUser(regParams, _)).WillOnce(InvokeArgument<1>(ExplainedError(Error::ok)));
    EXPECT_CALL(handler, call(ExplainedError(Error::ok), _)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_with_deleted_user_with_dead_master_should_return_error) {
    using sharpei::cache::RoleCache;

    const Shard::Id deletedShardId = 666;
    const Shard::Database::Address deletedShardMaster {"host", 5432, "dbname", "dataCenter"};

    cache->shardName.update(deletedShardId, "deletedShard");
    cache->role.update(deletedShardId, {{deletedShardMaster, RoleCache::OptRole(Shard::Database::Role::Master)}});
    cache->status.dead(shardId, shardMaster);

    const InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(metaMaster));
    EXPECT_CALL(*metaAdaptor, getUserRegData(uid, _, _)).WillOnce(InvokeArgument<1>(regData));
    EXPECT_CALL(*metaAdaptor, getDeletedUserData(uid, _, _, _)).WillOnce(InvokeArgument<1>(deletedShardId, boost::none));
    EXPECT_CALL(handler, call(ExplainedError(Error::noShardWithAliveMaster), _)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

TEST_F(PerformerTest, perform_with_error_in_get_deleted_user_data_should_return_error) {
    using sharpei::cache::RoleCache;

    const Shard::Id deletedShardId = 666;
    const Shard::Database::Address deletedShardMaster {"host", 5432, "dbname", "dataCenter"};

    cache->shardName.update(deletedShardId, "deletedShard");
    cache->role.update(deletedShardId, {{deletedShardMaster, RoleCache::OptRole(Shard::Database::Role::Master)}});
    cache->status.dead(shardId, shardMaster);

    const InSequence s;

    EXPECT_CALL(*httpClient, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response {200, {}, blackBoxResponseBody, ""}));
    EXPECT_CALL(*metaAdaptor, getMaster(_, _)).WillOnce(InvokeArgument<0>(metaMaster));
    EXPECT_CALL(*metaAdaptor, getUserRegData(uid, _, _)).WillOnce(InvokeArgument<1>(regData));
    EXPECT_CALL(*metaAdaptor, getDeletedUserData(uid, _, _, _)).WillOnce(InvokeArgument<2>(ExplainedError(Error::metaRequestError)));
    EXPECT_CALL(handler, call(ExplainedError(Error::metaRequestError), _)).WillOnce(Return());

    performer->perform(params, [&] (auto ... args) { handler.call(args ...); });
}

} // namespace
